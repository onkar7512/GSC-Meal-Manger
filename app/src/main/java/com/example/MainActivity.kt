package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.attendance.AttendanceScreen
import com.example.ui.biometric.BiometricAttendanceScannerDialog
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.export.PdfExportDialog
import com.example.ui.sync.GoogleSheetSyncDialog
import com.example.ui.students.StudentsScreen
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.material.icons.filled.Fingerprint

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MealAttendanceApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealAttendanceApp(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val allStudents by viewModel.allStudents.collectAsStateWithLifecycle()
    val selectedMeal by viewModel.selectedMeal.collectAsStateWithLifecycle()
    var showPdfExportDialog by remember { mutableStateOf(false) }
    var showSheetSyncDialog by remember { mutableStateOf(false) }
    var showBiometricScannerDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent
                    ) {
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(R.drawable.gsc_logo),
                                contentDescription = "GSC Logo",
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            androidx.compose.foundation.layout.Spacer(
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "GSC Student Manager",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showBiometricScannerDialog = true },
                        modifier = Modifier.testTag("btn_top_app_bar_biometric_scanner")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometric Check-In Station",
                            tint = Color(0xFF15803D)
                        )
                    }
                    IconButton(
                        onClick = { showSheetSyncDialog = true },
                        modifier = Modifier.testTag("btn_top_app_bar_google_sheet_sync")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Google Sheets Sync",
                            tint = Color(0xFF16A34A)
                        )
                    }
                    IconButton(
                        onClick = { showPdfExportDialog = true },
                        modifier = Modifier.testTag("btn_top_app_bar_pdf_export")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export Monthly & Date-Wise PDF Report",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                // Tab 1: Students
                NavigationBarItem(
                    selected = currentTab == AppTab.STUDENTS,
                    onClick = { viewModel.selectTab(AppTab.STUDENTS) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "Students"
                        )
                    },
                    label = { Text("Students") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag(AppTab.STUDENTS.testTag)
                )

                // Tab 2: Attendance Entry
                NavigationBarItem(
                    selected = currentTab == AppTab.ATTENDANCE,
                    onClick = { viewModel.selectTab(AppTab.ATTENDANCE) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.FactCheck,
                            contentDescription = "Attendance Entry"
                        )
                    },
                    label = { Text("Attendance") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag(AppTab.ATTENDANCE.testTag)
                )

                // Tab 3: Dashboard
                NavigationBarItem(
                    selected = currentTab == AppTab.DASHBOARD,
                    onClick = { viewModel.selectTab(AppTab.DASHBOARD) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Dashboard"
                        )
                    },
                    label = { Text("Dashboard") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag(AppTab.DASHBOARD.testTag)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
            ) {
                when (currentTab) {
                    AppTab.STUDENTS -> StudentsScreen(viewModel = viewModel)
                    AppTab.ATTENDANCE -> AttendanceScreen(viewModel = viewModel)
                    AppTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                }
            }
        }
    }

    if (showPdfExportDialog) {
        PdfExportDialog(
            viewModel = viewModel,
            onDismiss = { showPdfExportDialog = false }
        )
    }

    if (showSheetSyncDialog) {
        GoogleSheetSyncDialog(
            viewModel = viewModel,
            onDismiss = { showSheetSyncDialog = false }
        )
    }

    if (showBiometricScannerDialog) {
        BiometricAttendanceScannerDialog(
            viewModel = viewModel,
            students = allStudents,
            selectedMeal = selectedMeal,
            onMealChange = { viewModel.setSelectedMeal(it) },
            onDismiss = { showBiometricScannerDialog = false }
        )
    }
}
