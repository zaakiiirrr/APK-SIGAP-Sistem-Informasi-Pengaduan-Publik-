package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.GovBlue
import com.example.ui.theme.GovBlueLight
import com.example.ui.theme.GovGreen
import com.example.ui.theme.GovGreenLight
import com.example.ui.theme.GovSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.screens.CitizenScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LaporanScreen
import com.example.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SigapApp()
            }
        }
    }
}

enum class SigapTab(val title: String, val icon: ImageVector) {
    ADUAN("Aduan", Icons.Default.Edit),
    DASHBOARD("Peta/Dashboard", Icons.Default.List),
    LAPORAN("Laporan", Icons.Default.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SigapApp(
    viewModel: AppViewModel = viewModel()
) {
    var currentTab by remember { mutableStateOf(SigapTab.ADUAN) }
    val isUserAdmin by viewModel.isUserAdmin.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SIGAP",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 1.sp,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Sistem Pengaduan Publik Bantul",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                    }
                },
                actions = {
                    // Modern Personas switcher representing Citizen/Officer Toggle
                    Row(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable { viewModel.toggleUserRole() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isUserAdmin) Icons.Default.Star else Icons.Default.Person,
                            contentDescription = "Ganti Peran",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isUserAdmin) "Mode Petugas (Admin)" else "Mode Warga Negara",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GovBlue,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = GovSurface,
                tonalElevation = 8.dp
            ) {
                SigapTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GovBlue,
                            selectedTextColor = GovBlue,
                            indicatorColor = GovBlueLight,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        ),
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                SigapTab.ADUAN -> {
                    // Check if they are admin but viewing aduan
                    CitizenScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                SigapTab.DASHBOARD -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                SigapTab.LAPORAN -> {
                    LaporanScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
