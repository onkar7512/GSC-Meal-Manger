package com.example.ui.biometric

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biometric.BiometricManagerHelper
import com.example.data.local.MealType
import com.example.data.local.StudentEntity
import com.example.ui.BiometricPunchResult
import com.example.ui.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecentPunchItem(
    val studentName: String,
    val rollNo: String,
    val timeStr: String,
    val mealType: MealType,
    val isDuplicate: Boolean = false
)

@Composable
fun BiometricAttendanceScannerDialog(
    viewModel: MainViewModel,
    students: List<StudentEntity>,
    selectedMeal: MealType,
    onMealChange: (MealType) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeMeal by remember { mutableStateOf(selectedMeal) }
    var selectedStudent by remember {
        mutableStateOf<StudentEntity?>(students.firstOrNull { it.fingerprintEnrolled } ?: students.firstOrNull())
    }
    var codeSearchQuery by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    var punchFeedback by remember { mutableStateOf<BiometricPunchResult?>(null) }
    val recentPunches = remember { mutableStateListOf<RecentPunchItem>() }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isScanning) 1.15f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    fun processPunch(student: StudentEntity) {
        coroutineScope.launch {
            isScanning = true
            delay(180)
            val result = viewModel.recordFingerprintAttendance(
                studentId = student.id,
                mealTypeOverride = activeMeal
            )
            isScanning = false
            punchFeedback = result

            when (result) {
                is BiometricPunchResult.Success -> {
                    BiometricManagerHelper.triggerSuccessVibration(context)
                    recentPunches.add(
                        0,
                        RecentPunchItem(
                            studentName = student.name,
                            rollNo = student.rollNo,
                            timeStr = result.timeStr,
                            mealType = activeMeal,
                            isDuplicate = false
                        )
                    )
                }
                is BiometricPunchResult.AlreadyMarked -> {
                    BiometricManagerHelper.triggerErrorVibration(context)
                    recentPunches.add(
                        0,
                        RecentPunchItem(
                            studentName = student.name,
                            rollNo = student.rollNo,
                            timeStr = result.recordedTimeStr,
                            mealType = activeMeal,
                            isDuplicate = true
                        )
                    )
                }
                else -> {
                    BiometricManagerHelper.triggerErrorVibration(context)
                }
            }
        }
    }

    fun punchViaCode() {
        val q = codeSearchQuery.trim()
        if (q.isBlank()) return
        coroutineScope.launch {
            isScanning = true
            val result = viewModel.recordFingerprintAttendanceByCode(
                code = q,
                mealTypeOverride = activeMeal
            )
            isScanning = false
            punchFeedback = result
            codeSearchQuery = ""

            when (result) {
                is BiometricPunchResult.Success -> {
                    BiometricManagerHelper.triggerSuccessVibration(context)
                    recentPunches.add(
                        0,
                        RecentPunchItem(
                            studentName = result.student.name,
                            rollNo = result.student.rollNo,
                            timeStr = result.timeStr,
                            mealType = activeMeal,
                            isDuplicate = false
                        )
                    )
                }
                is BiometricPunchResult.AlreadyMarked -> {
                    BiometricManagerHelper.triggerErrorVibration(context)
                }
                else -> {
                    BiometricManagerHelper.triggerErrorVibration(context)
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        tint = Color(0xFF15803D),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Biometric Check-In Station",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Session Selector Tabs
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(MealType.values()) { meal ->
                        val isSel = activeMeal == meal
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("scanner_tab_${meal.name}"),
                            onClick = {
                                activeMeal = meal
                                onMealChange(meal)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${meal.emoji} ${meal.displayName}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Student Selection / Quick Scanner Code Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = codeSearchQuery,
                        onValueChange = { codeSearchQuery = it },
                        placeholder = { Text("FP-ID or Roll No (e.g. 101)") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { punchViaCode() }),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_scanner_code")
                    )

                    Button(
                        onClick = { punchViaCode() },
                        enabled = codeSearchQuery.isNotBlank(),
                        modifier = Modifier.testTag("btn_punch_by_code")
                    ) {
                        Text("Punch")
                    }
                }

                // Selected Student Picker for Fingerprint touch
                val enrolledStudents = remember(students) {
                    students.sortedWith(compareByDescending<StudentEntity> { it.fingerprintEnrolled }.thenBy { it.rollNo })
                }

                Text(
                    text = "Select Student for Biometric Punch:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(enrolledStudents) { st ->
                        val isSelected = selectedStudent?.id == st.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF15803D)) else null,
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("picker_student_${st.id}"),
                            onClick = { selectedStudent = st }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (st.fingerprintEnrolled) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = st.name,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "#${st.rollNo} • ${if (st.fingerprintEnrolled) st.fingerprintId.ifBlank { "FP" } else "No FP"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Big Interactive Fingerprint Sensor Pad
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Color(0xFFDCFCE7).copy(alpha = 0.8f))
                        .border(3.5.dp, Color(0xFF15803D), CircleShape)
                        .clickable(enabled = selectedStudent != null && !isScanning) {
                            selectedStudent?.let { targetStudent ->
                                if (BiometricManagerHelper.hasBiometricHardware(context)) {
                                    val signal = BiometricManagerHelper.authenticateWithSystemBiometrics(
                                        context = context,
                                        title = "${activeMeal.displayName} Attendance",
                                        subtitle = "Verify finger for ${targetStudent.name}",
                                        description = "Biometric Gate Check-In",
                                        onSuccess = { processPunch(targetStudent) },
                                        onError = { processPunch(targetStudent) },
                                        onCancel = { /* manual/cancel */ }
                                    )
                                    if (signal == null) {
                                        processPunch(targetStudent)
                                    }
                                } else {
                                    processPunch(targetStudent)
                                }
                            }
                        }
                        .testTag("scanner_touch_sensor_pad"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Sensor Touch Pad",
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "TOUCH TO PUNCH",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                    }
                }

                Text(
                    text = selectedStudent?.let { "Touch pad to mark ${it.name} (#${it.rollNo})" }
                        ?: "Select a student or enter FP ID above",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Feedback Banner
                punchFeedback?.let { res ->
                    when (res) {
                        is BiometricPunchResult.Success -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFDCFCE7),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("banner_punch_success")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Attendance Recorded (Fingerprint)",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF15803D)
                                        )
                                        Text(
                                            text = "${res.student.name} (#${res.student.rollNo}) • ${res.mealType.displayName} • ${res.timeStr}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF14532D)
                                        )
                                    }
                                }
                            }
                        }
                        is BiometricPunchResult.AlreadyMarked -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFEF3C7),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("banner_punch_already_marked")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Already Marked Present",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFB45309)
                                        )
                                        Text(
                                            text = "${res.student.name} marked ${res.mealType.displayName} at ${res.recordedTimeStr}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF78350F)
                                        )
                                    }
                                }
                            }
                        }
                        is BiometricPunchResult.NotFound -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFEE2E2),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = Color(0xFFB91C1C),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = res.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF7F1D1D)
                                    )
                                }
                            }
                        }
                        is BiometricPunchResult.Error -> {
                            Text(
                                text = res.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Recent Punches List
                if (recentPunches.isNotEmpty()) {
                    Text(
                        text = "Recent Punches (This Session):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        recentPunches.take(3).forEach { p ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (p.isDuplicate) Color(0xFFFEF3C7) else Color(0xFFDCFCE7).copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = null,
                                            tint = if (p.isDuplicate) Color(0xFFD97706) else Color(0xFF15803D),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${p.studentName} (#${p.rollNo})",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    }
                                    Text(
                                        text = "${p.timeStr} • ${if (p.isDuplicate) "DUP" else "PRESENT"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (p.isDuplicate) Color(0xFFB45309) else Color(0xFF15803D)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_close_scanner_dialog")
            ) {
                Text("Close")
            }
        }
    )
}
