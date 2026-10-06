package com.example.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AttendanceEntity
import com.example.data.local.MealType
import com.example.data.local.StudentEntity
import com.example.ui.AppTab
import com.example.ui.DashboardFilter
import com.example.ui.MainViewModel
import com.example.ui.MealAttendanceSummary
import com.example.ui.components.AttendanceStatusBadge
import com.example.ui.components.DateSelectorBar
import com.example.ui.export.PdfExportDialog
import com.example.ui.sync.GoogleSheetSyncDialog
import java.time.LocalDate

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.dashboardDate.collectAsStateWithLifecycle()
    val dashboardFilter by viewModel.dashboardFilter.collectAsStateWithLifecycle()
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val allStudents by viewModel.allStudents.collectAsStateWithLifecycle()
    val dashboardAttendance by viewModel.dashboardAttendance.collectAsStateWithLifecycle()
    val selectedDashboardActivity by viewModel.dashboardSelectedActivity.collectAsStateWithLifecycle()
    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val lastSyncStatus by viewModel.lastSyncStatus.collectAsStateWithLifecycle()
    val sheetWebhookUrl by viewModel.sheetWebhookUrl.collectAsStateWithLifecycle()

    var showExportDialog by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }

    val isToday = selectedDate == LocalDate.now()
    val isYesterday = selectedDate == LocalDate.now().minusDays(1)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("dashboard_scroll_content"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
    ) {
        // Step 1: Date Filter Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateSelectorBar(
                    selectedDate = selectedDate,
                    onDateSelected = { viewModel.setDashboardDate(it) }
                )

                // Quick Date Presets Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isToday,
                        onClick = { viewModel.setDashboardDate(LocalDate.now()) },
                        label = { Text("Today") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Today,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("chip_preset_today")
                    )

                    FilterChip(
                        selected = isYesterday,
                        onClick = { viewModel.setDashboardDate(LocalDate.now().minusDays(1)) },
                        label = { Text("Yesterday") },
                        modifier = Modifier.testTag("chip_preset_yesterday")
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Quick Jump to attendance entry for this date
                    TextButton(
                        onClick = {
                            viewModel.setAttendanceDate(selectedDate)
                            viewModel.selectTab(AppTab.ATTENDANCE)
                        },
                        modifier = Modifier.testTag("btn_quick_mark_attendance")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditCalendar,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Take Roll")
                    }
                }
            }
        }

        // Step 2: Dashboard Filter Buttons (All / Meals / Yoga / Meditation)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Filter Activities",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        DashboardFilter.ALL,
                        DashboardFilter.MEAL,
                        DashboardFilter.YOGA,
                        DashboardFilter.MEDITATION
                    )
                    filters.forEach { filter ->
                        val isSelected = dashboardFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setDashboardFilter(filter) },
                            label = {
                                Text(
                                    text = "${filter.emoji} ${filter.displayName}",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("chip_filter_${filter.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // Export to PDF Banner Card (Green & White Theme)
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_export_banner"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Color(0xFFF0FDF4) // Light Mint Green
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF047857)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Monthly PDF Report",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF064E3B)
                            )
                            Text(
                                text = "Export report for Meals, Yoga & Meditation",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF047857)
                            )
                        }
                    }

                    Button(
                        onClick = { showExportDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF047857)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_open_pdf_export_dialog")
                    ) {
                        Text(
                            text = "Export",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Google Sheets Cloud Sync Card
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_google_sheet_sync_banner"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Color(0xFFF0FDF4)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF16A34A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Google Sheets Sync",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF166534)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isNetworkAvailable) Color(0xFFDCFCE7) else Color(0xFFFEF3C7))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isNetworkAvailable) "Online" else "Offline",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isNetworkAvailable) Color(0xFF15803D) else Color(0xFFB45309)
                                    )
                                }
                            }
                            Text(
                                text = if (sheetWebhookUrl.isBlank()) "Setup Google Sheet webhook for auto-backup"
                                else if (isSyncing) "Syncing attendance data now..."
                                else lastSyncStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF15803D),
                                maxLines = 1
                            )
                        }
                    }

                    Button(
                        onClick = { showSyncDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF16A34A)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_open_google_sheet_sync_dialog")
                    ) {
                        Text(
                            text = if (sheetWebhookUrl.isBlank()) "Connect" else "Sync",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Key Metric: Total Students & Total Meals Served Cards (With Present & Absent counts)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total Students Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("metric_card_total_students"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Enrolled Students",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDCFCE7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = Color(0xFF047857),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${allStudents.size}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Active Mess Members",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Total Meals Served Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("metric_card_total_meals_served"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Meals Served",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDCFCE7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestaurantMenu,
                                    contentDescription = null,
                                    tint = Color(0xFF15803D),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${stats.totalMealsServed}",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF15803D)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Served",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF15803D),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        val maxPossible = (allStudents.size * 3).coerceAtLeast(1)
                        val missedMeals = (maxPossible - stats.totalMealsServed).coerceAtLeast(0)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "$missedMeals Absent",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFFB91C1C)
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            val dayRate = (stats.totalMealsServed * 100) / maxPossible
                            Text(
                                text = "$dayRate% turnout",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Secondary Metrics: Yoga & Meditation Cards (Clear Present and Absent counts)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Yoga Attended Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("metric_card_yoga_attendance"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Yoga Session",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDCFCE7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🧘",
                                    fontSize = 15.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${stats.yoga.presentCount}",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF15803D)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Present",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF15803D),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "${stats.yoga.absentCount} Absent",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFFB91C1C)
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            val yogaTurnout = if (allStudents.isNotEmpty()) (stats.yoga.presentCount * 100) / allStudents.size else 0
                            Text(
                                text = "$yogaTurnout% turnout",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Meditation Attended Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("metric_card_meditation_attendance"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Meditation",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDCFCE7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🕯️",
                                    fontSize = 15.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${stats.meditation.presentCount}",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF15803D)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Present",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF15803D),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "${stats.meditation.absentCount} Absent",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFFB91C1C)
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            val medTurnout = if (allStudents.isNotEmpty()) (stats.meditation.presentCount * 100) / allStudents.size else 0
                            Text(
                                text = "$medTurnout% turnout",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Section Title: Attendance Breakdown based on selected filter
        item {
            val title = when (dashboardFilter) {
                DashboardFilter.ALL -> "Daily Attendance Breakdown"
                DashboardFilter.MEAL -> "Meal-Wise Attendance Breakdown"
                DashboardFilter.YOGA -> "Yoga Session Attendance"
                DashboardFilter.MEDITATION -> "Meditation Session Attendance"
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Meal Cards (Shown when filter is ALL or MEAL)
        if (dashboardFilter == DashboardFilter.ALL || dashboardFilter == DashboardFilter.MEAL) {
            // Breakfast Breakdown Card
            item {
                MealDashboardCard(
                    mealType = MealType.BREAKFAST,
                    summary = stats.breakfast,
                    onTakeAttendance = {
                        viewModel.setAttendanceDate(selectedDate)
                        viewModel.setSelectedMeal(MealType.BREAKFAST)
                        viewModel.selectTab(AppTab.ATTENDANCE)
                    }
                )
            }

            // Lunch Breakdown Card
            item {
                MealDashboardCard(
                    mealType = MealType.LUNCH,
                    summary = stats.lunch,
                    onTakeAttendance = {
                        viewModel.setAttendanceDate(selectedDate)
                        viewModel.setSelectedMeal(MealType.LUNCH)
                        viewModel.selectTab(AppTab.ATTENDANCE)
                    }
                )
            }

            // Dinner Breakdown Card
            item {
                MealDashboardCard(
                    mealType = MealType.DINNER,
                    summary = stats.dinner,
                    onTakeAttendance = {
                        viewModel.setAttendanceDate(selectedDate)
                        viewModel.setSelectedMeal(MealType.DINNER)
                        viewModel.selectTab(AppTab.ATTENDANCE)
                    }
                )
            }
        }

        // Yoga Breakdown Card (Shown when filter is ALL or YOGA)
        if (dashboardFilter == DashboardFilter.ALL || dashboardFilter == DashboardFilter.YOGA) {
            item {
                MealDashboardCard(
                    mealType = MealType.YOGA,
                    summary = stats.yoga,
                    onTakeAttendance = {
                        viewModel.setAttendanceDate(selectedDate)
                        viewModel.setSelectedMeal(MealType.YOGA)
                        viewModel.selectTab(AppTab.ATTENDANCE)
                    }
                )
            }
        }

        // Meditation Breakdown Card (Shown when filter is ALL or MEDITATION)
        if (dashboardFilter == DashboardFilter.ALL || dashboardFilter == DashboardFilter.MEDITATION) {
            item {
                MealDashboardCard(
                    mealType = MealType.MEDITATION,
                    summary = stats.meditation,
                    onTakeAttendance = {
                        viewModel.setAttendanceDate(selectedDate)
                        viewModel.setSelectedMeal(MealType.MEDITATION)
                        viewModel.selectTab(AppTab.ATTENDANCE)
                    }
                )
            }
        }

        // STEP 3: Complete Student Attendance List on the Dashboard (Present & Absent)
        item {
            DashboardStudentAttendanceSection(
                students = allStudents,
                attendanceRecords = dashboardAttendance,
                selectedDate = selectedDate,
                selectedActivity = selectedDashboardActivity,
                onSelectActivity = { viewModel.setDashboardSelectedActivity(it) },
                onToggleAttendance = { studentId, isPresent ->
                    viewModel.toggleAttendanceForDateAndActivity(
                        studentId = studentId,
                        date = selectedDate,
                        meal = selectedDashboardActivity,
                        isPresent = isPresent
                    )
                },
                onMarkAll = { isPresent ->
                    viewModel.markAllForDateAndActivity(
                        date = selectedDate,
                        meal = selectedDashboardActivity,
                        isPresent = isPresent
                    )
                }
            )
        }
    }

    // PDF Export Dialog
    if (showExportDialog) {
        PdfExportDialog(
            viewModel = viewModel,
            onDismiss = { showExportDialog = false }
        )
    }

    // Google Sheets Cloud Sync Dialog
    if (showSyncDialog) {
        GoogleSheetSyncDialog(
            viewModel = viewModel,
            onDismiss = { showSyncDialog = false }
        )
    }
}

/**
 * Interactive Student Attendance Section on Dashboard
 * Allows viewing all students, filtering by Present / Absent, searching,
 * switching between Meals / Yoga / Meditation, and toggling attendance directly!
 */
@Composable
fun DashboardStudentAttendanceSection(
    students: List<StudentEntity>,
    attendanceRecords: List<AttendanceEntity>,
    selectedDate: LocalDate,
    selectedActivity: MealType,
    onSelectActivity: (MealType) -> Unit,
    onToggleAttendance: (studentId: Long, isPresent: Boolean) -> Unit,
    onMarkAll: (isPresent: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") } // "ALL", "PRESENT", "ABSENT"

    val activityRecords = remember(attendanceRecords, selectedActivity) {
        attendanceRecords
            .filter { it.mealType == selectedActivity.name }
            .associateBy { it.studentId }
    }

    val presentCount = students.count { activityRecords[it.id]?.isPresent == true }
    // Non-present students are automatically absent
    val absentCount = (students.size - presentCount).coerceAtLeast(0)

    val displayedStudents = remember(students, searchQuery, statusFilter, activityRecords) {
        var list = students
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) ||
                        it.rollNo.lowercase().contains(q) ||
                        it.roomNo.lowercase().contains(q)
            }
        }
        when (statusFilter) {
            "PRESENT" -> list.filter { activityRecords[it.id]?.isPresent == true }
            "ABSENT" -> list.filter { activityRecords[it.id]?.isPresent != true }
            else -> list
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_student_attendance_section"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with title and quick counters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Student Attendance Register",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${selectedActivity.emoji} ${selectedActivity.displayName} • Auto-absent active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Summary Counters in Header
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFDCFCE7)
                    ) {
                        Text(
                            text = "$presentCount Present",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF15803D),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEE2E2)
                    ) {
                        Text(
                            text = "$absentCount Absent",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFB91C1C),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Activity Selector Chips (Breakfast, Lunch, Dinner, Yoga, Meditation)
            val activities = listOf(
                MealType.BREAKFAST,
                MealType.LUNCH,
                MealType.DINNER,
                MealType.YOGA,
                MealType.MEDITATION
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                activities.forEach { act ->
                    val isSelected = act == selectedActivity
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .testTag("dashboard_activity_chip_${act.name.lowercase()}"),
                        onClick = { onSelectActivity(act) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = act.emoji, fontSize = 14.sp)
                            Text(
                                text = act.displayName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Bulk Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("btn_dashboard_all_present"),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFDCFCE7),
                    onClick = { onMarkAll(true) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "All Present",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF15803D)
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("btn_dashboard_all_absent"),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEE2E2),
                    onClick = { onMarkAll(false) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color(0xFFB91C1C),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "All Absent",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFB91C1C)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar & Filter Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, roll...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_dashboard_student_search")
                )

                // Filter chips (All / Present / Absent)
                listOf(
                    "ALL" to "All (${students.size})",
                    "PRESENT" to "P ($presentCount)",
                    "ABSENT" to "A ($absentCount)"
                ).forEach { (key, label) ->
                    val isSelected = statusFilter == key
                    val chipBg = if (isSelected) {
                        when (key) {
                            "PRESENT" -> Color(0xFF15803D)
                            "ABSENT" -> Color(0xFFB91C1C)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                    val chipText = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = chipBg,
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("filter_dashboard_$key"),
                        onClick = { statusFilter = key }
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = chipText
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Student list items
            if (displayedStudents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No students matching \"$searchQuery\""
                        else if (statusFilter == "PRESENT") "No students marked present"
                        else if (statusFilter == "ABSENT") "No students marked absent"
                        else "No students enrolled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    displayedStudents.forEach { student ->
                        val isPresent = activityRecords[student.id]?.isPresent == true

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dashboard_student_item_${student.id}"),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            onClick = {
                                onToggleAttendance(student.id, !isPresent)
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Roll Number Avatar (Green if Present, Red if Absent)
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isPresent) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = student.rollNo,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isPresent) Color(0xFF15803D) else Color(0xFFB91C1C)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Name & Room
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = student.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (student.roomNo.isNotBlank()) {
                                        Text(
                                            text = "Room ${student.roomNo}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Present / Absent Badge
                                AttendanceStatusBadge(isPresent = isPresent)

                                Spacer(modifier = Modifier.width(8.dp))

                                // Toggle Action Button
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isPresent) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                                    onClick = { onToggleAttendance(student.id, !isPresent) },
                                    modifier = Modifier.testTag("btn_toggle_dashboard_${student.id}")
                                ) {
                                    Text(
                                        text = if (isPresent) "Set Absent" else "Set Present",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isPresent) Color(0xFFB91C1C) else Color(0xFF15803D),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Summary card for each individual meal or session (Breakfast, Lunch, Dinner, Yoga, Meditation).
 * Clean green & white theme showing Present & Absent counts prominently.
 */
@Composable
fun MealDashboardCard(
    mealType: MealType,
    summary: MealAttendanceSummary,
    onTakeAttendance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = if (summary.totalStudents > 0) summary.attendancePercentage / 100f else 0f,
        label = "progress_${mealType.name}"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_meal_${mealType.name.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Emoji + Name + Attendance Rate Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mealType.emoji,
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = mealType.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${summary.presentCount} of ${summary.totalStudents} present",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFDCFCE7),
                    onClick = onTakeAttendance,
                    modifier = Modifier.testTag("btn_edit_meal_${mealType.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.0f", summary.attendancePercentage)}%",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF15803D)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Linear Progress Bar (Green)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF16A34A),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Present & Absent Stats Row (Clear Present, Absent, and Total Turnout)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Present
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "${summary.presentCount}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF15803D)
                        )
                        Text(
                            text = "Present",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Absent (Auto-absent)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEE2E2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color(0xFFB91C1C),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "${summary.absentCount}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFB91C1C)
                        )
                        Text(
                            text = "Absent",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Total Enrolled
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "${summary.totalStudents}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
