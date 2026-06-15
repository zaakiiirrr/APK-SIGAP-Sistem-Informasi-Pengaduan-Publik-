package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Complaint
import com.example.ui.theme.*
import com.example.viewmodel.AppViewModel
import com.example.api.GeminiManager

@Composable
fun LaporanScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val complaints by viewModel.filteredComplaints.collectAsState()
    val executiveSummary by viewModel.executiveSummary.collectAsState()
    val isGenerating by viewModel.isGeneratingSummary.collectAsState()

    val scrollState = rememberScrollState()

    // Generate Top 3 categories with Recommended Action Tags
    val topCategories = remember(complaints) {
        complaints.groupBy { it.category }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(3)
    }

    val actionRecommendations = mapOf(
        "Road" to Pair("PERBAIKAN JALAN DPU", "Segera terjunkan tim penambal aspal jalan utama."),
        "Trash" to Pair("EVAKUASI SAMPAH DLH", "Armada truk sampah tambahan menyisir area penumpukan."),
        "Flood" to Pair("REHABILITASI SUNGAI BPBD", "Normalisasi drainase parit mencegah luapan banjir."),
        "Air Quality" to Pair("SANKSI POLUSI DLH", "Razia penegakan hukum pembakaran sampah ilegal."),
        "Other" to Pair("TINDAKAN SATPOL PP", "Koordinasi lintas instansi penertiban tata tertib publik.")
    )

    val categoriesIndoNames = mapOf(
        "Road" to "Jalan Rusak & Infrastruktur",
        "Trash" to "Ekosistem Sampah Liar",
        "Flood" to "Genangan Kebanjiran Wilayah",
        "Air Quality" to "Polusi Asap & Kualitas Udara",
        "Other" to "Lainnya / Keluhan Umum"
    )

    // Trigger AI summary if empty initially
    LaunchedEffect(complaints) {
        if (executiveSummary.isEmpty() && complaints.isNotEmpty()) {
            viewModel.refreshExecutiveSummary()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Welcome and Heading of Executive Policy
        Text(
            text = "Laporan Eksekutif Kebijakan",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = GovBlue,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 4.dp, start = 4.dp)
        )
        Text(
            text = "Rangkuman sektoral & arahan taktis untuk Kepala Daerah Kabupaten Bantul",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 16.dp, start = 4.dp)
        )

        // Segment 1: Matrix of Top 3 Problem Areas
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = GovSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🏆 Top 3 Sektor Keluhan Terbanyak",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (topCategories.isEmpty()) {
                    Text(
                        text = "Data pengaduan nihil. Silakan laporkan kasus untuk mengisi dasbor analisis sektoral.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                } else {
                    topCategories.forEachIndexed { index, entry ->
                        val cat = entry.key
                        val count = entry.value
                        val (actionTag, actionDetail) = actionRecommendations[cat] ?: Pair("REKOMENDASI AKSelerasi", "Segera ambil tindakan tanggap darurat.")
                        val indoName = categoriesIndoNames[cat] ?: cat

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(GovBlue.copy(alpha = 0.1f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            fontWeight = FontWeight.Bold,
                                            color = GovBlue,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = indoName,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Black
                                    )
                                }

                                Text(
                                    text = "$count Laporan",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GovDanger,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Action Tag Badge and Description
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 34.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(GovGreenLight)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = actionTag,
                                        color = GovGreen,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 9.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = actionDetail,
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )
                            }

                            if (index < topCategories.size - 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }

        // Segment 2: AI Executive Summary generated via Gemini API
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = GovSurface),
            border = BorderStroke(1.dp, GovBlue.copy(alpha = 0.15f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = GovGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Laporan Eksekutif Bupati (AI)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = GovGreen
                        )
                    }

                    IconButton(
                        onClick = { viewModel.refreshExecutiveSummary() },
                        enabled = !isGenerating
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = GovBlue)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isGenerating) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = GovGreen, strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Menganalisis pola sektoral & menyusun kebijakan...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GovGreen
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                            .border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = if (executiveSummary.isEmpty()) {
                                "Memulai penyusunan laporan..."
                            } else {
                                GeminiManager.formatMarkdownToPlainText(executiveSummary)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }

        // Segment 3: Export/Share Actions Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Copy to Clipboard
            Button(
                onClick = {
                    if (executiveSummary.isNotEmpty()) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("SIGAP Executive Summary", executiveSummary)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Laporan disalin ke papan klip!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Belum ada laporan untuk disalin!", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GovBlue),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("export_copy_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Salin Laporan", fontWeight = FontWeight.Bold)
            }

            // 2. Share via Intent
            Button(
                onClick = {
                    if (executiveSummary.isNotEmpty()) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Laporan Kebijakan SIGAP Bantul")
                            putExtra(Intent.EXTRA_TEXT, executiveSummary)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Laporan Eksekutif"))
                    } else {
                        Toast.makeText(context, "Belum ada laporan untuk dibagikan!", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GovGreen),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("export_share_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Bagikan Teks", fontWeight = FontWeight.Bold)
            }
        }
    }
}
