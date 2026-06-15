package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiManager
import com.example.data.AppDatabase
import com.example.data.Complaint
import com.example.data.ComplaintRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "AppViewModel"
    private val repository: ComplaintRepository

    // Role state: true = Government Officer/Admin, false = Citizen
    private val _isUserAdmin = MutableStateFlow(false)
    val isUserAdmin: StateFlow<Boolean> = _isUserAdmin.asStateFlow()

    // Loading indicator for AI tasks
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Last submitted complaint to display immediate summary card to the citizen
    private val _lastSubmittedComplaint = MutableStateFlow<Complaint?>(null)
    val lastSubmittedComplaint: StateFlow<Complaint?> = _lastSubmittedComplaint.asStateFlow()

    // Filters for dashboard
    private val _filterCategory = MutableStateFlow("Semua")
    val filterCategory: StateFlow<String> = _filterCategory.asStateFlow()

    private val _filterUrgency = MutableStateFlow("Semua")
    val filterUrgency: StateFlow<String> = _filterUrgency.asStateFlow()

    private val _filterDistrict = MutableStateFlow("Semua")
    val filterDistrict: StateFlow<String> = _filterDistrict.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Executive summary state
    private val _executiveSummary = MutableStateFlow("")
    val executiveSummary: StateFlow<String> = _executiveSummary.asStateFlow()

    private val _isGeneratingSummary = MutableStateFlow(false)
    val isGeneratingSummary: StateFlow<Boolean> = _isGeneratingSummary.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ComplaintRepository(database.complaintDao())

        // Check if database is empty and pre-populate with realistic Bantul local complaints
        viewModelScope.launch(Dispatchers.IO) {
            repository.allComplaints.collect { list ->
                if (list.isEmpty()) {
                    prepopulateDatabase()
                }
            }
        }
    }

    // Main complaints flow combining repository data with user filters
    val filteredComplaints: StateFlow<List<Complaint>> = combine(
        repository.allComplaints,
        _filterCategory,
        _filterUrgency,
        _filterDistrict,
        _searchQuery
    ) { complaintList, category, urgency, district, query ->
        complaintList.filter { complaint ->
            val matchesCategory = category == "Semua" || complaint.category == category
            val matchesUrgency = urgency == "Semua" || complaint.urgency == urgency
            val matchesDistrict = district == "Semua" || complaint.location.contains(district, ignoreCase = true)
            val matchesQuery = query.isEmpty() || 
                    complaint.description.contains(query, ignoreCase = true) ||
                    complaint.name.contains(query, ignoreCase = true) ||
                    complaint.location.contains(query, ignoreCase = true)

            matchesCategory && matchesUrgency && matchesDistrict && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All complaints flow exposes historical summaries
    val allComplaints: StateFlow<List<Complaint>> = repository.allComplaints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleUserRole() {
        _isUserAdmin.value = !_isUserAdmin.value
    }

    fun setFilterCategory(category: String) {
        _filterCategory.value = category
    }

    fun setFilterUrgency(urgency: String) {
        _filterUrgency.value = urgency
    }

    fun setFilterDistrict(district: String) {
        _filterDistrict.value = district
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearLastSubmittedComplaint() {
        _lastSubmittedComplaint.value = null
    }

    /**
     * Submits a fresh citizen complaint.
     * Fires asynchronous Gemini parser to construct entities & empathetic response.
     */
    fun submitComplaint(
        name: String,
        category: String,
        location: String,
        latitude: Double,
        longitude: Double,
        description: String,
        urgency: String,
        photoPath: String?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Call Gemini for analysis and empathy
                val analysis = withContext(Dispatchers.IO) {
                    GeminiManager.analyzeComplaint(description, category, location)
                }

                val complaintName = if (name.trim().isEmpty()) "Anonim" else name.trim()

                val newComplaint = Complaint(
                    name = complaintName,
                    location = analysis.location,
                    latitude = latitude,
                    longitude = longitude,
                    category = category,
                    description = description,
                    urgency = analysis.urgency, // Use AI's corrected/assessed urgency
                    photoUri = photoPath,
                    status = "Diterima",
                    aiResponse = analysis.empatheticResponse,
                    aiSummaryLocation = analysis.location,
                    aiSummaryProblem = analysis.problem,
                    aiSummaryTime = analysis.time,
                    aiSummaryUrgency = analysis.urgency
                )

                // Save to Room Database
                val insertedId = withContext(Dispatchers.IO) {
                    repository.insert(newComplaint)
                }

                // Update last submitted complaint to open success summary card
                _lastSubmittedComplaint.value = newComplaint.copy(id = insertedId.toInt())

            } catch (e: Exception) {
                Log.e(TAG, "Error submitting complaint", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Direct update for the government officer (updates status: Diterima, Diproses, Selesai)
     */
    fun updateComplaintStatus(complaint: Complaint, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = complaint.copy(status = newStatus)
            repository.update(updated)
        }
    }

    /**
     * Deletes complaint item
     */
    fun deleteComplaint(complaint: Complaint) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(complaint)
        }
    }

    /**
     * Generates executive summary report via AI
     */
    fun refreshExecutiveSummary() {
        viewModelScope.launch {
            _isGeneratingSummary.value = true
            try {
                val list = filteredComplaints.value
                val summary = withContext(Dispatchers.IO) {
                    GeminiManager.generateExecutiveSummary(list)
                }
                _executiveSummary.value = summary
            } catch (e: Exception) {
                Log.e(TAG, "Error generating summary", e)
                _executiveSummary.value = "Gagal memuat Ringkasan AI: ${e.localizedMessage}"
            } finally {
                _isGeneratingSummary.value = false
            }
        }
    }

    /**
     * GPS simulator: Returns a random local sub-district of Bantul, DIY with mock location parameters
     */
    fun simulateGPSTracking(): Map<String, Any> {
        val subdistricts = listOf(
            "Kecamatan Sewon, Bantul",
            "Kecamatan Kasihan, Bantul",
            "Kecamatan Banguntapan, Bantul",
            "Kecamatan Piyungan, Bantul",
            "Kecamatan Imogiri, Bantul",
            "Kecamatan Bantul Kota, Bantul",
            "Kecamatan Kretek, Bantul",
            "Kecamatan Pleret, Bantul"
        )
        
        // Dynamic mock coords corresponding to Bantul coordinates scope
        val latCenter = -7.8864
        val lngCenter = 110.3274
        val latOffset = (Math.random() - 0.5) * 0.08
        val lngOffset = (Math.random() - 0.5) * 0.08

        return mapOf(
            "name" to subdistricts.random(),
            "latitude" to latCenter + latOffset,
            "longitude" to lngCenter + lngOffset
        )
    }

    /**
     * Seed initial public database with rich demographic records of Yogyakarta / Bantul local complaints
     */
    private suspend fun prepopulateDatabase() {
        Log.d(TAG, "Seeding mock complaint database with regional Bantul data...")
        val seedComplaints = listOf(
            Complaint(
                name = "Ahmad Sugeng",
                location = "Kecamatan Sewon, Bantul",
                latitude = -7.8421,
                longitude = 110.3642,
                category = "Sampah",
                description = "Penumpukan sampah liar di dekat Ring Road Selatan Sewon, menyumbat saluran drainase warga dan berbau busuk.",
                urgency = "Tinggi",
                photoUri = "simulated_uri_trash",
                status = "Diproses",
                timestamp = System.currentTimeMillis() - 86400000 * 3, // 3 days ago
                aiResponse = "Halo Ahmad Sugeng, kami berterima kasih atas kepedulian Anda terhadap lingkungan Sewon. Dinas Lingkungan Hidup (DLH) Bantul telah menerima laporan penyumbatan sampah ini dan menginstruksikan regu kebersihan terdekat untuk langsung menyisir lokasi hari ini.",
                aiSummaryLocation = "Ring Road Selatan, Sewon",
                aiSummaryProblem = "Sampah menyumbat drainase pemukiman",
                aiSummaryTime = "Segera / Hari Ini",
                aiSummaryUrgency = "Tinggi"
            ),
            Complaint(
                name = "Yuni Shara",
                location = "Kecamatan Kasihan, Bantul",
                latitude = -7.8145,
                longitude = 110.3341,
                category = "Road",
                description = "Lubang aspal sangat besar di persimpangan jalan utama Kasihan-Suryodiningratan dekat halte. Sangat membahayakan pengendara motor di malam hari yang sepi.",
                urgency = "Darurat",
                photoUri = "simulated_uri_pothole",
                status = "Diterima",
                timestamp = System.currentTimeMillis() - 86400000 * 1, // 1 day ago
                aiResponse = "Halo Ibu Yuni Shara, Kantor Dinas Pekerjaan Umum (DPU) Bantul meminta maaf yang sebesar-besarnya atas kerusakan jalan ini. Petugas darurat sedang dikoordinasikan untuk menandai lubang dan melakukan tambal aspal sementara demi keselamatan berkendara malam.",
                aiSummaryLocation = "Jalan Utama Kasihan dekat Halte",
                aiSummaryProblem = "Lubang jalan kabupaten berskala membahayakan",
                aiSummaryTime = "Kurang dari 24 jam",
                aiSummaryUrgency = "Darurat"
            ),
            Complaint(
                name = "Anonim",
                location = "Kecamatan Bantul Kota, Bantul",
                latitude = -7.8864,
                longitude = 110.3274,
                category = "Road",
                description = "Lampu penerangan jalan utama dekat alun-alun Bantul padam total selama 3 hari berturut-turut. Suasana sangat gelap gulita dan memicu kriminalitas kriminal/begal.",
                urgency = "Tinggi",
                photoUri = null,
                status = "Selesai",
                timestamp = System.currentTimeMillis() - 86400000 * 5, // 5 days ago
                aiResponse = "Terima kasih atas kontribusi laporannya. Laporan mengenai padamnya penerangan jalan Alun-Alun Bantul telah berhasil diselesaikan oleh regu teknis Dinas Perhubungan (Dishub) Bantul dengan peremajaan kabel gardu jalan.",
                aiSummaryLocation = "Sekitar Alun-Alun Bantul",
                aiSummaryProblem = "Padamnya tiang lampu penerangan jalan umum",
                aiSummaryTime = "Selesai ditangani",
                aiSummaryUrgency = "Tinggi"
            ),
            Complaint(
                name = "Budi Hartono",
                location = "Kecamatan Piyungan, Bantul",
                latitude = -7.8389,
                longitude = 110.4682,
                category = "Flood",
                description = "Tanggul selokan jebol menyemburkan air luapan parit mengalir deras masuk ke areal halaman pemukiman warga Piyungan akibat hujan deras tadi sore.",
                urgency = "Darurat",
                photoUri = "simulated_uri_flood",
                status = "Diproses",
                timestamp = System.currentTimeMillis() - 3600000 * 4, // 4 hours ago
                aiResponse = "Halo Bapak Budi, kami memahami kondisi darurat yang Anda alami di Piyungan. Laporan tanggul jebol telah diteruskan secara prioritas ke BPBD (Badan Penanggulangan Bencana) DIY dan Dinas PU guna penumpukan tanggul darurat (sandbag). Mohon tetap waspada.",
                aiSummaryLocation = "Persawahan & Pemukiman Piyungan",
                aiSummaryProblem = "Jebolnya tanggul selokan pembuangan pemukiman",
                aiSummaryTime = "Respons Segera",
                aiSummaryUrgency = "Darurat"
            ),
            Complaint(
                name = "Hendry",
                location = "Kecamatan Banguntapan, Bantul",
                latitude = -7.8188,
                longitude = 110.4074,
                category = "Air Quality",
                description = "Pembakaran ban dan sampah plastik skala besar di area lahan kosong Banguntapan setiap sore menghasilkan polusi asap hitam tebal dan sesak di dada anak-anak.",
                urgency = "Tinggi",
                photoUri = null,
                status = "Diterima",
                timestamp = System.currentTimeMillis() - 86400000 * 2, // 2 days ago
                aiResponse = "Halo Bapak Hendry, terima kasih telah berani melapor. Polusi udara akibat pembuangan & pembakaran ilegal sangat merugikan kesehatan. Satpol PP Bantul beserta DLH sedang berkoordinasi melakukan inspeksi lapangan ke lokasi tersebut.",
                aiSummaryLocation = "Lahan Kosong Banguntapan",
                aiSummaryProblem = "Polusi pembakaran ilegal beracun skala besar",
                aiSummaryTime = "1-2 Hari Kerja",
                aiSummaryUrgency = "Tinggi"
            ),
            Complaint(
                name = "Anonim",
                location = "Kecamatan Imogiri, Bantul",
                latitude = -7.9351,
                longitude = 110.3951,
                category = "Other",
                description = "Ranting-ranting pohon beringin tua di tepi jalan wisata makam Imogiri sudah sangat lapuk dan miring ke arah kabel listrik tegangan tinggi PLN.",
                urgency = "Menengah",
                photoUri = null,
                status = "Diterima",
                timestamp = System.currentTimeMillis() - 86400000 * 7, // 7 days ago
                aiResponse = "Bapak/Ibu pelapor yang kami hormati, terima kasih atas laporannya. Kondisi pohon berisiko menimpa kabel ini telah dikoordinasikan oleh BPBD Bantul bersama tim pemangkasan PLN Bantul untuk pemangkasan terkawal minggu ini.",
                aiSummaryLocation = "Jalan Wisata Makam Imogiri",
                aiSummaryProblem = "Pohon lapuk menggantung di kabel PLN",
                aiSummaryTime = "3-4 Hari Kerja",
                aiSummaryUrgency = "Menengah"
            )
        )

        for (comp in seedComplaints) {
            repository.insert(comp)
        }
        Log.d(TAG, "Successfully seeded 6 complaints for Bantul, DIY!")
    }
}
