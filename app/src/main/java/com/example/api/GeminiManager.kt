package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.Complaint
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiManager {
    private const val TAG = "GeminiManager"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    // Struct for the parsed complaint
    data class AIAnalysisResult(
        val urgency: String, // Darurat, Tinggi, Menengah, Rendah
        val location: String,
        val problem: String,
        val time: String,
        val empatheticResponse: String
    )

    /**
     * Checks if the API key is configured and valid
     */
    fun isKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY" && !key.contains("PLACEHOLDER")
    }

    /**
     * Performs direct Gemini API call to analyse complaint details in Bahasa Indonesia.
     * Combines entity-parsing + urgency categorization + empathetic reaction.
     */
    suspend fun analyzeComplaint(
        description: String,
        category: String,
        locationInput: String
    ): AIAnalysisResult {
        if (!isKeyConfigured()) {
            return getFallbackAnalysis(description, category, locationInput)
        }

        val prompt = """
            Anda adalah asisten AI resmi (SIGAP) untuk Pemerintah Kabupaten Bantul dan Yogyakarta.
            Tugas Anda adalah memproses laporan keluhan warga berikut guna ekstraksi entitas dan penyusunan tanggapan yang empatik.
            
            INFORMASI LAPORAN:
            Kategori Awal: $category
            Lokasi Awal: $locationInput
            Deskripsi Keluhan: "$description"
            
            Keluaran Anda HARUS berupa objek JSON tunggal yang memiliki kunci persis seperti berikut:
            1. "urgency": Urgensi laporan. Pilih salah satu dari: "Darurat", "Tinggi", "Menengah", "Rendah". Tentukan urgensi secara logis berdasarkan deskripsi keluhan (misal: lubang besar di jalan utama malam hari/kecelakaan = Tinggi/Darurat, tumpukan sampah menyumbat parit = Tinggi, lampu lalin mati = Tinggi/Menengah, coret-coret tembok = Rendah).
            2. "location": Lokasi spesifik yang diekstrak dari deskripsi atau lokasi awal, sesuaikan agar jelas di daerah Bantul/Yogyakarta.
            3. "problem": Ringkasan masalah yang ringkas namun informatif (maksimal 12 kata).
            4. "time": Rentang waktu pemrosesan keluhan yang diestimasikan secara wajar atau penanda waktu kejadian (misal: "Segera / 24 Jam", "3 Hari Kerja", "7 Hari Kerja").
            5. "empatheticResponse": Tanggapan ramah, sopan, dan sangat empatik dalam Bahasa Indonesia yang hangat. Sampaikan permohonan maaf atas gangguan/masalah ini, nyatakan apresiasi atas kepedulian warga, jalaskan instansi terkait (seperti Dinas PU untuk jalan, Dinas Lingkungan Hidup/DLH untuk sampah/banjir, Dishub untuk lampu lalin/terminal) akan segera meninjau laporan ini secara transparan.
            
            Format Objek JSON keluaran Anda harus persis seperti ini:
            {
              "urgency": "Tinggi",
              "location": "Jalan Imogiri Barat, Sewon, Bantul",
              "problem": "Pemberantasan tumpukan sampah liar menyumbat aliran selokan",
              "time": "2-3 Hari Kerja",
              "empatheticResponse": "Halo Bapak/Ibu, terima kasih banyak atas laporannya. Kami mohon maaf yang sebesar-besarnya atas bau tidak sedap akibat tumpukan sampah di Imogiri Barat. Laporan ini telah kami teruskan ke Dinas Lingkungan Hidup (DLH) Bantul untuk penanganan segera. Tetap SIGAP!"
            }
        """.trimIndent()

        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            val url = "$BASE_URL?key=$apiKey"

            // Construct JSON request body
            val requestBodyJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                }
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toString().toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "API Call Failed: Code=${response.code}, Body=$errBody")
                    return getFallbackAnalysis(description, category, locationInput)
                }

                val responseBodyStr = response.body?.string() ?: ""
                val jsonObj = JSONObject(responseBodyStr)
                val candidates = jsonObj.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val rawText = candidates.getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    val resultJson = JSONObject(rawText.trim())
                    return AIAnalysisResult(
                        urgency = resultJson.optString("urgency", "Menengah"),
                        location = resultJson.optString("location", locationInput),
                        problem = resultJson.optString("problem", "Laporan keluhan warga"),
                        time = resultJson.optString("time", "3 Hari Kerja"),
                        empatheticResponse = formatMarkdownToPlainText(resultJson.optString(
                            "empatheticResponse",
                            "Terima kasih atas laporan Anda. Petugas Dinas akan segera menindaklanjuti keluhan ini."
                        ))
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during analyzeComplaint", e)
        }

        return getFallbackAnalysis(description, category, locationInput)
    }

    /**
     * Fallback parsing when API is disabled or throws an exception
     */
    private fun getFallbackAnalysis(
        description: String,
        category: String,
        locationInput: String
    ): AIAnalysisResult {
        // Simple heuristic rules for offline mode logic
        val isUrgent = description.contains("darurat", ignoreCase = true) || 
                       description.contains("macet", ignoreCase = true) || 
                       description.contains("kecelakaan", ignoreCase = true) ||
                       description.contains("banjir", ignoreCase = true) ||
                       category == "Banjir"
                       
        val urgency = if (isUrgent) "Tinggi" else "Menengah"
        val dnas = when (category) {
            "Road", "Jalan" -> "Dinas Pekerjaan Umum (DPU)"
            "Trash", "Sampah" -> "Dinas Lingkungan Hidup (DLH)"
            "Flood", "Banjir" -> "Badan Penanggulangan Bencana Daerah (BPBD)"
            "Air Quality", "Kualitas Udara" -> "Dinas Lingkungan Hidup (DLH)"
            else -> "Dinas Pemerintahan Terkait"
        }

        val locationClean = if (locationInput.isEmpty()) "Kabupaten Bantul, Yogyakarta" else locationInput

        return AIAnalysisResult(
            urgency = urgency,
            location = locationClean,
            problem = if (description.length > 40) description.substring(0, 37) + "..." else description,
            time = if (isUrgent) "Segera / 24 Jam" else "3 - 5 Hari Kerja",
            empatheticResponse = formatMarkdownToPlainText("Terima kasih atas laporan Anda mengenai ketersediaan fasilitas publik di $locationClean. Kami dari pihak Pemkab Bantul memohon maaf atas ketidaknyamanan yang terjadi. Laporan ketersediaan kategori $category ini akan diserahkan untuk dikoordinasikan secara offline dengan pihak $dnas. Terus SIGAP!")
        )
    }

    /**
     * Generates an Executive Policy Summary ("Laporan Eksekutif") from multiple complaints
     */
    suspend fun generateExecutiveSummary(complaints: List<Complaint>): String {
        if (!isKeyConfigured()) {
            return getFallbackExecutiveSummary(complaints)
        }

        if (complaints.isEmpty()) {
            return "Belum ada laporan keluhan yang masuk dalam database SIGAP."
        }

        // Limit the text to avoid token overflow
        val summariesInput = complaints.take(20).joinToString("\n") { c ->
            "- Kategori: ${c.category}, Urgensi: ${c.urgency}, Lokasi: ${c.location}, Deskripsi: ${c.description}"
        }

        val prompt = """
            ROLE (Peran):
            Anda adalah Analis Kebijakan Publik Senior di Bappeda (Badan Perencanaan Pembangunan Daerah) Kabupaten Bantul. Anda memiliki pemahaman mendalam tentang tata kelola pemerintahan, hukum administrasi negara, dan alokasi APBD.

            CONTEXT (Konteks):
            Anda baru saja menerima rekapitulasi data keluhan masyarakat dari sistem SIGAP selama satu minggu terakhir. Berikut adalah draf data keluhan tersebut:
            
            $summariesInput

            TASK (Tugas):
            Tugas Anda adalah merumuskan draf "MEMORANDUM LAPORAN EKSEKUTIF" yang ditujukan langsung kepada Bupati Bantul. Anda harus menganalisis data keluhan tersebut menggunakan pendekatan materialisme dan logika:
            - Jangan hanya melaporkan angka, tetapi analisis kondisi material di lapangan (misal: mengapa jalan rusak bertepatan dengan musim hujan dan jalur truk pasir).
            - Lihat dialektika dampaknya terhadap roda ekonomi atau keselamatan warga.
            - Rumuskan rekomendasi kebijakan yang taktis, logis, dan terukur.

            FORMAT OUTPUT (Ketentuan Hasil):
            Tuliskan dalam format Memorandum resmi pemerintahan yang tegas dan rapi, mencakup:
            1. KOP MEMORANDUM (Menampilkan secara jelas: KEPADA: Bupati Bantul, DARI: Analis Kebijakan Publik Senior Bappeda Bantul, TANGGAL: (gunakan tanggal hari ini), PERIHAL: Laporan Eksekutif Hasil Pengaduan Warga/Publik)
            2. RINGKASAN EKSEKUTIF (Satu paragraf padat tentang status urgensi wilayah kumulatif)
            3. ANALISIS MATERIAL (AKAR MASALAH) (Bedah secara logis akar masalah utama, hubungan cuaca, tata ruang, beban muatan, dll.)
            4. REKOMENDASI INTERVENSI TAKTIS (Berikan minimal atau tepat 3 poin tindakan spesifik yang melibatkan kolaborasi lintas dinas, misalnya DPU, DLH, dan Dishub)

            PENTING: JANGAN menggunakan format markdown seperti tanda pagar (#, ##, ###), karakter bintang ganda (**) untuk tulisan tebal, atau karakter bintang tunggal (*) untuk tulisan miring di dalam teks laporan Anda. Buat tulisan rapi instan yang dipisahkan oleh paragraf kosong baru dan poin-poin bertanda minus '-' atau angka biasa. Tulis judul bagian dan poin-poin penting langsung dengan huruf kapital biasa (UPPERCASE) tanpa menggunakan cetak tebal bintang.
        """.trimIndent()

        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            val url = "$BASE_URL?key=$apiKey"

            val requestBodyJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                }
                put("contents", contentsArray)
            }

            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toString().toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBodyStr = response.body?.string() ?: ""
                    val jsonObj = JSONObject(responseBodyStr)
                    val candidates = jsonObj.getJSONArray("candidates")
                    if (candidates.length() > 0) {
                        val rawText = candidates.getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                        return formatMarkdownToPlainText(rawText)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during generateExecutiveSummary", e)
        }

        return formatMarkdownToPlainText(getFallbackExecutiveSummary(complaints))
    }

    private fun getFallbackExecutiveSummary(complaints: List<Complaint>): String {
        if (complaints.isEmpty()) {
            return "MEMORANDUM LAPORAN EKSEKUTIF\n============================\nLaporan SIGAP: Belum ada data pengaduan masuk."
        }

        // Count categories
        val total = complaints.size
        val counts = complaints.groupBy { it.category }.mapValues { it.value.size }
        val categoryRank = counts.entries.sortedByDescending { it.value }
        
        val urgencyCounts = complaints.groupBy { it.urgency }.mapValues { it.value.size }

        val primaryCategory = categoryRank.firstOrNull()?.key ?: "Semua Sektor"
        val primaryCategoryCount = categoryRank.firstOrNull()?.value ?: 0

        return """
            MEMORANDUM LAPORAN EKSEKUTIF
            
            KEPADA: Bupati Bantul
            DARI: Analis Kebijakan Publik Senior Bappeda Bantul
            TANGGAL: 15 Juni 2026
            PERIHAL: Analisis Kebijakan Hasil Pengaduan Publik Sistem SIGAP
            
            RINGKASAN EKSEKUTIF
            Dari rekapitulasi mingguan aplikasi SIGAP, terhimpun total $total laporan dengan kategori $primaryCategory mendominasi ($primaryCategoryCount aduan) dan tingkat kelayakan darurat yang membutuhkan penanganan taktis segera. Wilayah tangkapan perkotaan dan perbatasan menuntut intervensi alokasi belanja khusus tak terduga (BTT) guna memitigasi eskalasi risiko keselamatan.
            
            ANALISIS MATERIAL (AKAR MASALAH)
            1. Sektor Infrastruktur dan Mobilitas ($primaryCategory): Kerusakan konstruksi aspal di jalan-jalan penghubung wilayah meningkat pesat seiring tingginya limpasan debit air hujan akibat sumbatan drainase dan beban ganda muatan armada komersial (seperti truk material/pasir). Dampak material ini menghambat rantai pasok lokal dan meningkatkan risiko volatilitas logistik serta kecelakaan di malam hari.
            2. Sektor Kelayakan Sanitasi Lingkungan: Penumpukan liar limbah domestik perkotaan bukan sekadar kelalaian sipil, melainkan cerminan ketimpangan kapasitas truk sampah DLH terhadap frekuensi harian. Pada musim pancaroba, timbunan sampah mengundang mikroorganisme vektor patogen dan berimplikasi langsung pada produktivitas ekonomi rumah tangga rentan akibat malaria/demam berdarah.
            
            REKOMENDASI INTERVENSI TAKTIS
            - Dinas Pekerjaan Umum (DPU): Melaksanakan rekonstruksi penambalan taktis darurat segmen lubang kritis dalam waktu 24 jam dengan pemeliharaan terintegrasi sistem drainase jalan.
            - Dinas Lingkungan Hidup (DLH): Mengerahkan unit kontainer sampah temporer portabel di zonasi merah penumpukan liar serta mereformulasi rute penjemputan armada berkala.
            - Dinas Perhubungan (Dishub): Memperketat pengawasan tonase muatan kendaraan berat di koridor jalan kabupaten serta percepatan perbaikan lampu penerangan jalan utama (PJU) guna menjamin keselamatan navigasi pascapulih hujan.
        """.trimIndent()
    }

    /**
     * Helper utility to clean up all raw Markdown characters and delimiters.
     * Removes asterisks, hashes, underscores, and cleans up typography for plain-text presentation.
     */
    fun formatMarkdownToPlainText(text: String): String {
        var clean = text
        // Remove markdown headings like "### 2. Heading" -> "2. Heading"
        clean = clean.replace(Regex("(?m)^#+\\s*(.+)$"), "$1")
        // Remove bold delimiters: **text** -> text
        clean = clean.replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
        // Replace list bullet asterisks with standard list dashes
        clean = clean.replace(Regex("(?m)^\\*\\s+"), "- ")
        // Remove single italic asterisks: *text* -> text
        clean = clean.replace(Regex("\\*([^*]+)\\*"), "$1")
        // Remove bold underscores: __text__ -> text
        clean = clean.replace(Regex("__([^_]+)__"), "$1")
        // Remove italic underscores: _text_ -> text
        clean = clean.replace(Regex("_([^_]+)_"), "$1")
        // Resolve duplicate/messy dashes or lists
        clean = clean.replace(Regex("(?m)^-\\s*-\\s+"), "- ")
        return clean.trim()
    }
}
