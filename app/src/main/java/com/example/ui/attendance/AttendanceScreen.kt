package com.example.ui.attendance

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.biometric.BiometricManagerHelper
import com.example.data.local.AttendanceCategory
import com.example.data.local.AttendanceEntity
import com.example.data.local.MealType
import com.example.data.local.StudentEntity
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.biometric.BiometricAttendanceScannerDialog
import com.example.ui.components.AttendanceCategoryTabs
import com.example.ui.components.AttendanceStatusBadge
import com.example.ui.components.DateSelectorBar
import com.example.ui.components.MealSelectorTabs
import kotlinx.coroutines.launch

@Composable
fun AttendanceScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedAttendanceDate.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedAttendanceCategory.collectAsStateWithLifecycle()
    val selectedMeal by viewModel.selectedMeal.collectAsStateWithLifecycle()
    val students by viewModel.allStudents.collectAsStateWithLifecycle()
    val attendanceRecords by viewModel.attendanceForSelectedDate.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showBiometricScanner by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf<String>("ALL") } // "ALL", "PRESENT", "ABSENT", "UNMARKED"

    // Map studentId -> AttendanceEntity for this date and selected meal/activity
    val mealAttendanceMap = remember(attendanceRecords, selectedMeal) {
        attendanceRecords
            .filter { it.mealType == selectedMeal.name }
            .associateBy { it.studentId }
    }

    val presentCount = students.count { mealAttendanceMap[it.id]?.isPresent == true }
    val absentCount = (students.size - presentCount).coerceAtLeast(0)

    val displayedStudents = remember(students, searchQuery, filterStatus, mealAttendanceMap) {
        var list = students
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) ||
                        it.rollNo.lowercase().contains(q) ||
                        it.roomNo.lowercase().contains(q)
            }
        }
        when (filterStatus) {
            "PRESENT" -> list.filter { mealAttendanceMap[it.id]?.isPresent == true }
            "ABSENT" -> list.filter { mealAttendanceMap[it.id]?.isPresent != true }
            else -> list
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Step 1: Date Selector (Applies uniformly across all activities)
        DateSelectorBar(
            selectedDate = selectedDate,
            onDateSelected = { viewModel.setAttendanceDate(it) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Step 2: Activity Category Tabs (Meals / Yoga / Meditation)
        AttendanceCategoryTabs(
            selectedCategory = selectedCategory,
            onCategorySelected = { viewModel.setSelectedAttendanceCategory(it) }
        )

        // Step 3: If Meals is selected, show Breakfast / Lunch / Dinner sub-tabs
        if (selectedCategory == AttendanceCategory.MEAL) {
            Spacer(modifier = Modifier.height(8.dp))
            MealSelectorTabs(
                selectedMeal = selectedMeal,
                onMealSelected = { viewModel.setSelectedMeal(it) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Activity Summary Banner + Bulk Controls
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("attendance_summary_banner"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val sessionTitle = if (selectedMeal.isMeal) {
                            "${selectedMeal.emoji} ${selectedMeal.displayName} Attendance"
                        } else {
                            "${selectedMeal.emoji} ${selectedMeal.displayName} Session Attendance"
                        }
                        Text(
                            text = sessionTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "$presentCount Present",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF15803D)
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "$absentCount Absent",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFB91C1C)
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "${students.size} Total",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Progress indicator % text
                    val pct = if (students.isNotEmpty()) (presentCount * 100) / students.size else 0
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "$pct%",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bulk Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("btn_mark_all_present"),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFDCFCE7),
                        onClick = { viewModel.markAllForSelectedMeal(isPresent = true) }
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
                            .testTag("btn_mark_all_absent"),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEE2E2),
                        onClick = { viewModel.markAllForSelectedMeal(isPresent = false) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RemoveDone,
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

                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_clear_meal_attendance"),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        onClick = { viewModel.clearSelectedMealAttendance() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset Attendance for Meal",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Biometric Gate Check-In Button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("btn_open_biometric_scanner"),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF15803D),
                    onClick = { showBiometricScanner = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Biometric Check-In Station",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "FINGERPRINT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search & Filter row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter student...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Filter",
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("input_attendance_search")
            )

            // Status filter chips
            val filterOptions = listOf(
                "ALL" to "All (${students.size})",
                "PRESENT" to "Present ($presentCount)",
                "ABSENT" to "Absent ($absentCount)"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                filterOptions.forEach { (key, label) ->
                    val isSelected = filterStatus == key
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("filter_status_$key"),
                        onClick = { filterStatus = key }
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Student Attendance Entry List
        if (students.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Students Found",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Please add students in the Students tab first to mark meal attendance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.selectTab(AppTab.STUDENTS) },
                            modifier = Modifier.testTag("btn_go_to_students")
                        ) {
                            Text("Go to Students")
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.autoSyncWithGoogleSheet { success, msg ->
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.testTag("btn_attendance_quick_sheet_pull")
                        ) {
                            Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pull from Sheet")
                        }
                    }
                }
            }
        } else if (displayedStudents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No students match the current filter",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("attendance_student_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
            ) {
                items(displayedStudents, key = { it.id }) { student ->
                    val record = mealAttendanceMap[student.id]
                    AttendanceStudentRow(
                        student = student,
                        record = record,
                        onMarkPresent = {
                            viewModel.toggleStudentAttendance(student.id, isPresent = true)
                        },
                        onMarkAbsent = {
                            viewModel.toggleStudentAttendance(student.id, isPresent = false)
                        },
                        onMarkBiometric = {
                            coroutineScope.launch {
                                viewModel.recordFingerprintAttendance(
                                    studentId = student.id,
                                    mealTypeOverride = selectedMeal
                                )
                                BiometricManagerHelper.triggerSuccessVibration(context)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showBiometricScanner) {
        BiometricAttendanceScannerDialog(
            viewModel = viewModel,
            students = students,
            selectedMeal = selectedMeal,
            onMealChange = { viewModel.setSelectedMeal(it) },
            onDismiss = { showBiometricScanner = false }
        )
    }
}

@Composable
fun AttendanceStudentRow(
    student: StudentEntity,
    record: AttendanceEntity?,
    onMarkPresent: () -> Unit,
    onMarkAbsent: () -> Unit,
    onMarkBiometric: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPresent = record?.isPresent == true

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("attendance_row_${student.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = {
            if (isPresent) onMarkAbsent() else onMarkPresent()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Roll No Badge
            Box(
                modifier = Modifier
                    .size(38.dp)
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

            // Student Name & Room & Verification method
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (student.roomNo.isNotBlank()) {
                        Text(
                            text = "Room ${student.roomNo}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isPresent && record != null) {
                        if (record.verificationMethod == "FINGERPRINT") {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "FP • ${record.timeStr}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D)
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "Manual • ${record.timeStr}",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Present / Absent / Fingerprint Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Quick Fingerprint Punch Button
                Surface(
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("btn_row_punch_fp_${student.id}"),
                    shape = RoundedCornerShape(10.dp),
                    color = if (student.fingerprintEnrolled) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = if (student.fingerprintEnrolled) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF15803D)) else null,
                    onClick = onMarkBiometric
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Punch with Fingerprint",
                            tint = if (student.fingerprintEnrolled) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Present Button
                val isPresentActive = isPresent
                val presentBg by animateColorAsState(
                    targetValue = if (isPresentActive) Color(0xFF15803D) else Color(0xFFDCFCE7),
                    label = "presentBg"
                )
                val presentText by animateColorAsState(
                    targetValue = if (isPresentActive) Color.White else Color(0xFF15803D),
                    label = "presentText"
                )

                Surface(
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("btn_mark_present_${student.id}"),
                    shape = RoundedCornerShape(10.dp),
                    color = presentBg,
                    onClick = onMarkPresent
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Mark Present",
                            tint = presentText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Present",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = presentText
                        )
                    }
                }

                // Absent Button (Auto Absent active if not marked present)
                val isAbsentActive = !isPresent
                val absentBg by animateColorAsState(
                    targetValue = if (isAbsentActive) Color(0xFFB91C1C) else Color(0xFFFEE2E2),
                    label = "absentBg"
                )
                val absentText by animateColorAsState(
                    targetValue = if (isAbsentActive) Color.White else Color(0xFFB91C1C),
                    label = "absentText"
                )

                Surface(
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("btn_mark_absent_${student.id}"),
                    shape = RoundedCornerShape(10.dp),
                    color = absentBg,
                    onClick = onMarkAbsent
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Mark Absent",
                            tint = absentText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Absent",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = absentText
                        )
                    }
                }
            }
        }
    }
}
