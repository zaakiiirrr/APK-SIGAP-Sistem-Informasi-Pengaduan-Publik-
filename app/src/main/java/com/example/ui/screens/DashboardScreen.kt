package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Complaint
import com.example.ui.components.CategoryTrendLineChart
import com.example.ui.components.KecamatanBarChart
import com.example.ui.theme.*
import com.example.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val complaints by viewModel.filteredComplaints.collectAsState()
    val allComplaintsRaw by viewModel.allComplaints.collectAsState()

    // Filter flows observed
    val currentCategory by viewModel.filterCategory.collectAsState()
    val currentUrgency by viewModel.filterUrgency.collectAsState()
    val currentDistrict by viewModel.filterDistrict.collectAsState()
    val currentQuery by viewModel.searchQuery.collectAsState()

    // Selection expanded indicators
    var isCategoryExp by remember { mutableStateOf(false) }
    var isUrgencyExp by remember { mutableStateOf(false) }
    var isDistrictExp by remember { mutableStateOf(false) }

    var showCharts by remember { mutableStateOf(false) }

    val districts = listOf("Semua", "Sewon", "Kasihan", "Banguntapan", "Piyungan", "Bantul Kota", "Imogiri")
    val listCategories = listOf("Semua", "Road", "Trash", "Flood", "Air Quality", "Other")
    val listCategoriesIndoLabels = mapOf(
        "Semua" to "Semua Kategori",
        "Road" to "Jalan Rusak",
        "Trash" to "Sampah",
        "Flood" to "Banjir",
        "Air Quality" to "Kualitas Udara",
        "Other" to "Lainnya"
    )

    val urgencies = listOf("Semua", "Darurat", "Tinggi", "Menengah", "Rendah")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        // Search & Filter header cards
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = GovSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Search Input
                OutlinedTextField(
                    value = currentQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Cari pengaduan warga di Bantul...", fontSize = 14.sp, color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.DarkGray) },
                    trailingIcon = {
                        if (currentQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.DarkGray)
                            }
                         }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("admin_search"),
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
                        focusedTrailingIconColor = GovBlue,
                        unfocusedTrailingIconColor = Color.DarkGray,
                        focusedBorderColor = GovBlue,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable filter parameters row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Filter Kecamatan Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { isDistrictExp = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentDistrict == "Semua") Color(0xFFF1F5F9) else GovBlueLight
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (currentDistrict == "Semua") "Kecamatan" else currentDistrict,
                                    color = if (currentDistrict == "Semua") Color.DarkGray else GovBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = GovBlue, modifier = Modifier.size(16.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = isDistrictExp,
                            onDismissRequest = { isDistrictExp = false }
                        ) {
                            districts.forEach { dist ->
                                DropdownMenuItem(
                                    text = { Text(dist, fontSize = 13.sp) },
                                    onClick = {
                                        viewModel.setFilterDistrict(dist)
                                        isDistrictExp = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // 2. Filter Category Dropdown
                    Box(modifier = Modifier.weight(1.2f)) {
                        Button(
                            onClick = { isCategoryExp = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentCategory == "Semua") Color(0xFFF1F5F9) else GovBlueLight
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = listCategoriesIndoLabels[currentCategory] ?: currentCategory,
                                    color = if (currentCategory == "Semua") Color.DarkGray else GovBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = GovBlue, modifier = Modifier.size(16.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = isCategoryExp,
                            onDismissRequest = { isCategoryExp = false }
                        ) {
                            listCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(listCategoriesIndoLabels[cat] ?: cat, fontSize = 13.sp) },
                                    onClick = {
                                        viewModel.setFilterCategory(cat)
                                        isCategoryExp = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // 3. Filter Urgensi Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { isUrgencyExp = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentUrgency == "Semua") Color(0xFFF1F5F9) else GovBlueLight
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentUrgency,
                                    color = if (currentUrgency == "Semua") Color.DarkGray else GovBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = GovBlue, modifier = Modifier.size(16.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = isUrgencyExp,
                            onDismissRequest = { isUrgencyExp = false }
                        ) {
                            urgencies.forEach { urg ->
                                DropdownMenuItem(
                                    text = { Text(urg, fontSize = 13.sp) },
                                    onClick = {
                                        viewModel.setFilterUrgency(urg)
                                        isUrgencyExp = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Toggle Statistic Charts section
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = GovBlueLight),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clickable { showCharts = !showCharts }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = GovBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (showCharts) "Sembunyikan Grafik Statistik AI" else "Tampilkan Grafik Statistik AI",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GovBlue
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = GovBlue,
                    modifier = Modifier.rotate(if (showCharts) 180f else 0f)
                )
            }
        }

        // Expanded Charts Panel
        AnimatedVisibility(
            visible = showCharts,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                KecamatanBarChart(
                    complaints = allComplaintsRaw,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                CategoryTrendLineChart(
                    complaints = allComplaintsRaw,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            }
        }

        // complaints list headline count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Daftar Keluhan (${complaints.size} laporan)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )

            // Clear all filters shortcut if active
            if (currentCategory != "Semua" || currentUrgency != "Semua" || currentDistrict != "Semua" || currentQuery.isNotEmpty()) {
                Text(
                    text = "Reset Filter",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GovDanger,
                    modifier = Modifier.clickable {
                        viewModel.setFilterCategory("Semua")
                        viewModel.setFilterUrgency("Semua")
                        viewModel.setFilterDistrict("Semua")
                        viewModel.setSearchQuery("")
                    }
                )
            }
        }

        // Scrollable List of complaints representing modern RecyclerView
        if (complaints.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tidak ada laporan pengaduan ditemukan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("complaints_recycler_view"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(complaints, key = { it.id }) { complaint ->
                    ComplaintRowCard(
                        complaint = complaint,
                        isAdmin = viewModel.isUserAdmin.collectAsState().value,
                        onStatusChange = { newStatus ->
                            viewModel.updateComplaintStatus(complaint, newStatus)
                            Toast.makeText(context, "Status Laporan Berhasil Diperbarui ke: $newStatus", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = {
                            viewModel.deleteComplaint(complaint)
                            Toast.makeText(context, "Laporan pengaduan dihapus dari sistem", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ComplaintRowCard(
    complaint: Complaint,
    isAdmin: Boolean,
    onStatusChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(complaint.timestamp) {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        sdf.format(Date(complaint.timestamp))
    }

    val mappedIndoCategory = when (complaint.category) {
        "Road" -> "Jalan Dan Jembatan"
        "Trash" -> "Sampah & Lingkungan"
        "Flood" -> "Penanggulangan Banjir"
        "Air Quality" -> "Kualitas Udara"
        else -> "Lainnya"
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GovSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("complaint_item_${complaint.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header: Category & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mappedIndoCategory.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = GovBlue
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Badges row: Urgency and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Urgency Badges
                val (urgencyColor, urgencyBg) = when (complaint.urgency) {
                    "Darurat" -> GovDanger to GovDangerLight
                    "Tinggi" -> GovWarn to GovWarnLight
                    "Menengah" -> Color(0xFFCA8A04) to Color(0xFFFEF9C3) // Yellow parameters
                    "Rendah" -> GovGreen to GovGreenLight
                    else -> GovBlue to GovBlueLight
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(urgencyBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = complaint.urgency,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = urgencyColor,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Status Badges
                val (statusColor, statusBg) = when (complaint.status) {
                    "Selesai" -> GovGreen to GovGreenLight
                    "Diproses" -> GovWarn to GovWarnLight
                    else -> GovBlue to GovBlueLight // Diterima State
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = complaint.status,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Delete Button for Officer
                if (isAdmin) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = GovDanger, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body Description
            Text(
                text = "${complaint.name} di ${complaint.location}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = complaint.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray,
                lineHeight = 18.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            // Show Photo Label if attached
            if (complaint.photoUri != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Foto Bukti Terlampir",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // AI summary indicators inside item
            if (complaint.aiSummaryProblem != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Done, contentDescription = null, tint = GovBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ringkasan AI: ${complaint.aiSummaryProblem}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Admin Actions Row: Update Status (Diterima -> Diproses -> Selesai)
            if (isAdmin) {
                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(8.dp))

                Column {
                    Text(
                        text = "Ubah Status Keluhan (Admin Panel):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusAdminButton(
                            label = "TERIMA",
                            active = complaint.status == "Diterima",
                            color = GovBlue,
                            onClick = { onStatusChange("Diterima") },
                            modifier = Modifier.weight(1f)
                        )
                        StatusAdminButton(
                            label = "PROSES",
                            active = complaint.status == "Diproses",
                            color = GovWarn,
                            onClick = { onStatusChange("Diproses") },
                            modifier = Modifier.weight(1f)
                        )
                        StatusAdminButton(
                            label = "SELESAI",
                            active = complaint.status == "Selesai",
                            color = GovGreen,
                            onClick = { onStatusChange("Selesai") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusAdminButton(
    label: String,
    active: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonBg = if (active) color else Color.White
    val buttonText = if (active) Color.White else color
    val borderStroke = if (active) null else BorderStroke(1.dp, color)

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = buttonBg),
        border = borderStroke,
        contentPadding = PaddingValues(vertical = 4.dp),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier.height(32.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = buttonText
        )
    }
}
