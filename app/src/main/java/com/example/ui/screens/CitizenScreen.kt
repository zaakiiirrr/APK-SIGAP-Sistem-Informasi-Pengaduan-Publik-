package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.GeminiManager
import com.example.data.Complaint
import com.example.ui.theme.*
import com.example.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitizenScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val lastSubmitted by viewModel.lastSubmittedComplaint.collectAsState()

    // Form states
    var name by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf(-7.8864) }
    var longitude by remember { mutableStateOf(110.3274) }
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    var description by remember { mutableStateOf("") }
    var selectedUrgency by remember { mutableStateOf("Menengah") }
    var photoPath by remember { mutableStateOf<String?>(null) }

    // Categories mappings
    val categories = listOf("Road", "Trash", "Flood", "Air Quality", "Other")
    val categoriesIndo = listOf(
        "Jalan Rusak / Lubang",
        "Sampah & Saluran Limbah",
        "Banjir & Luapan Air",
        "Polusi & Kualitas Udara",
        "Keluhan Publik Lainnya"
    )
    val urgencyLevels = listOf("Darurat", "Tinggi", "Menengah", "Rendah")

    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var isUrgencyDropdownExpanded by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Warning if Gemini Key is missing, advising users gracefully
        if (!GeminiManager.isKeyConfigured()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = GovWarnLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GovWarn.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Peringatan Layanan",
                        tint = GovWarn,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Mode Simulasi Aktif",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GovWarn
                        )
                        Text(
                            text = "Kunci API Gemini belum dipasang di Secrets Panel. Aplikasi menggunakan logika analisis lokal offline yang tetap SIGAP.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7A4300)
                        )
                    }
                }
            }
        }

        // Submitted AI Report summary Card
        AnimatedVisibility(
            visible = lastSubmitted != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            lastSubmitted?.let { complaint ->
                AISummaryCard(
                    complaint = complaint,
                    onDismiss = { viewModel.clearLastSubmittedComplaint() }
                )
            }
        }

        // Only show submission form if no active summary card is requested/presented
        if (lastSubmitted == null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GovSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = "Formulir Pengaduan Warga",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GovBlue,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // 1. Nama Warna (Optional)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Pelapor (Kosongkan jika Anonim)", color = Color.DarkGray) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.DarkGray) },
                        placeholder = { Text("Nama Anda...", color = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("name_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedLabelColor = GovBlue,
                            unfocusedLabelColor = Color.DarkGray,
                            focusedPlaceholderColor = Color.Gray,
                            unfocusedPlaceholderColor = Color.Gray,
                            focusedLeadingIconColor = GovBlue,
                            unfocusedLeadingIconColor = Color.DarkGray,
                            focusedBorderColor = GovBlue,
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    // 2. Kategori Keperluan Dropdown
                    ExposedDropdownMenuBox(
                        expanded = isCategoryDropdownExpanded,
                        onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded },
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        OutlinedTextField(
                            value = categoriesIndo[selectedCategoryIndex],
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kategori Keluhan Sektoral", color = Color.DarkGray) },
                            leadingIcon = { Icon(Icons.Default.List, contentDescription = null, tint = Color.DarkGray) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedLabelColor = GovBlue,
                                unfocusedLabelColor = Color.DarkGray,
                                focusedLeadingIconColor = GovBlue,
                                unfocusedLeadingIconColor = Color.DarkGray,
                                focusedTrailingIconColor = GovBlue,
                                unfocusedTrailingIconColor = Color.DarkGray,
                                focusedBorderColor = GovBlue,
                                unfocusedBorderColor = Color.Gray
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = isCategoryDropdownExpanded,
                            onDismissRequest = { isCategoryDropdownExpanded = false }
                        ) {
                            categoriesIndo.forEachIndexed { index, name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        selectedCategoryIndex = index
                                        isCategoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 3. Lokasi Keluhan (GPS Auto-fill)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = locationName,
                            onValueChange = { locationName = it },
                            label = { Text("Lokasi Kejadian / Alamat", color = Color.DarkGray) },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.DarkGray) },
                            placeholder = { Text("Gunakan GPS atau ketik alamat...", color = Color.Gray) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("location_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedLabelColor = GovBlue,
                                unfocusedLabelColor = Color.DarkGray,
                                focusedPlaceholderColor = Color.Gray,
                                unfocusedPlaceholderColor = Color.Gray,
                                focusedLeadingIconColor = GovBlue,
                                unfocusedLeadingIconColor = Color.DarkGray,
                                focusedBorderColor = GovBlue,
                                unfocusedBorderColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val simulated = viewModel.simulateGPSTracking()
                                locationName = simulated["name"] as String
                                latitude = simulated["latitude"] as Double
                                longitude = simulated["longitude"] as Double
                                Toast.makeText(context, "GPS Berhasil Terdeteksi!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GovGreen),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("gps_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Dapatkan GPS", tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("GPS", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (latitude != -7.8864 || longitude != 110.3274) {
                        Text(
                            text = "Koordinat terdeteksi: Lat ${String.format("%.4f", latitude)}, Lng ${String.format("%.4f", longitude)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GovGreen,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                        )
                    }

                    // 4. Deskripsi Keluhan
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Deskripsi Detail Kejadian", color = Color.DarkGray) },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color.DarkGray) },
                        placeholder = { Text("Laporkan secara detail masalah Anda agar mudah diverifikasi AI...", color = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .padding(bottom = 12.dp)
                            .testTag("description_input"),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedLabelColor = GovBlue,
                            unfocusedLabelColor = Color.DarkGray,
                            focusedPlaceholderColor = Color.Gray,
                            unfocusedPlaceholderColor = Color.Gray,
                            focusedLeadingIconColor = GovBlue,
                            unfocusedLeadingIconColor = Color.DarkGray,
                            focusedBorderColor = GovBlue,
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    // 5. Urgensi Laporan
                    ExposedDropdownMenuBox(
                        expanded = isUrgencyDropdownExpanded,
                        onExpandedChange = { isUrgencyDropdownExpanded = !isUrgencyDropdownExpanded },
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedUrgency,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Penilaian Urgensi Awal", color = Color.DarkGray) },
                            leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color.DarkGray) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUrgencyDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedLabelColor = GovBlue,
                                unfocusedLabelColor = Color.DarkGray,
                                focusedLeadingIconColor = GovBlue,
                                unfocusedLeadingIconColor = Color.DarkGray,
                                focusedTrailingIconColor = GovBlue,
                                unfocusedTrailingIconColor = Color.DarkGray,
                                focusedBorderColor = GovBlue,
                                unfocusedBorderColor = Color.Gray
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = isUrgencyDropdownExpanded,
                            onDismissRequest = { isUrgencyDropdownExpanded = false }
                        ) {
                            urgencyLevels.forEach { level ->
                                DropdownMenuItem(
                                    text = { Text(level) },
                                    onClick = {
                                        selectedUrgency = level
                                        isUrgencyDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 6. Photo Attachment Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                // Simulate mock camera path
                                photoPath = "mock_photo_bantul_${System.currentTimeMillis()}.jpg"
                                Toast.makeText(context, "Foto berhasil dilampirkan!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AddCircle, contentDescription = "Lampirkan Foto", tint = Color.DarkGray)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Lampirkan Bukti Foto", color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (photoPath != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, GovGreen, RoundedCornerShape(8.dp))
                                    .background(GovGreenLight)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = GovGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Terlampir", color = GovGreen, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Hapus",
                                        tint = GovDanger,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { photoPath = null }
                                    )
                                }
                            }
                        }
                    }

                    // 7. Submit Action Button
                    Button(
                        onClick = {
                            if (description.trim().isEmpty() || locationName.trim().isEmpty()) {
                                Toast.makeText(context, "Harap isi Alamat Lokasi dan Deskripsi Keluhan!", Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.submitComplaint(
                                    name = name,
                                    category = categories[selectedCategoryIndex],
                                    location = locationName,
                                    latitude = latitude,
                                    longitude = longitude,
                                    description = description,
                                    urgency = selectedUrgency,
                                    photoPath = photoPath
                                )
                                // Reset fields
                                name = ""
                                locationName = ""
                                description = ""
                                photoPath = null
                                latitude = -7.8864
                                longitude = 110.3274
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GovBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_button"),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Diproses AI SIGAP...", color = Color.White, fontWeight = FontWeight.Bold)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Kirim Laporan Pengaduan", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AISummaryCard(
    complaint: Complaint,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GovBlueLight),
        border = BorderStroke(1.5.dp, GovBlue),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "AI Intellect",
                        tint = GovBlue,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Respons Analisis AI SIGAP",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GovBlue
                        )
                        Text(
                            text = "Laporan Selesai Diterapkan & Ditransformasikan",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.DarkGray)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Empathetic response box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.82f), RoundedCornerShape(10.dp))
                    .padding(14.dp)
                    .border(0.5.dp, GovBlue.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            ) {
                Column {
                    Text(
                        text = "Tanggapan Petugas (Disediakan AI):",
                        style = MaterialTheme.typography.bodySmall,
                        color = GovBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = GeminiManager.formatMarkdownToPlainText(complaint.aiResponse ?: "Pesan empati gagal di generate."),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grid Tabulating AI Structured Extracted Entities
            Text(
                text = "Entitas Ekraksi Terstruktur AI",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = GovBlue,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Table Structure
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, GovBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            ) {
                // Headers row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GovBlue.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Atribut Ekstraksi",
                        modifier = Modifier.weight(1.2f),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        color = GovBlue
                    )
                    Text(
                        text = "Metadata Teranalisis",
                        modifier = Modifier.weight(1.8f),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        color = GovBlue
                    )
                }

                Divider(color = GovBlue.copy(alpha = 0.3f))

                // Table Items
                AIEntityRow(name = "Lokasi Kejadian", value = complaint.aiSummaryLocation ?: complaint.location, isEven = false)
                AIEntityRow(name = "Rumusan Masalah", value = complaint.aiSummaryProblem ?: "Tindakan pemeriksaan publik", isEven = true)
                AIEntityRow(name = "Estimasi Tindak", value = complaint.aiSummaryTime ?: "Segera diperiksa", isEven = false)
                AIEntityRow(
                    name = "Tingkat Urgensi",
                    value = complaint.aiSummaryUrgency ?: complaint.urgency,
                    isEven = true,
                    highlightColor = getUrgencyColor(complaint.aiSummaryUrgency ?: complaint.urgency)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GovBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buat Pengaduan Keluhan Baru", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AIEntityRow(
    name: String,
    value: String,
    isEven: Boolean,
    highlightColor: Color? = null
) {
    val rowBg = if (isEven) GovBlueLight.copy(alpha = 0.3f) else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            modifier = Modifier.weight(1.2f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray
        )
        if (highlightColor != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(highlightColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = value.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = highlightColor
                )
            }
        } else {
            Text(
                text = value,
                modifier = Modifier.weight(1.8f),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )
        }
    }
}

fun getUrgencyColor(urgency: String): Color {
    return when (urgency.trim()) {
        "Darurat" -> GovDanger
        "Tinggi" -> GovWarn
        "Menengah" -> Color(0xFFEAB308) // Bright Yellow
        "Rendah" -> GovGreen
        else -> GovBlue
    }
}
