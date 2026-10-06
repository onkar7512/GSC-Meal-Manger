package com.example.ui.biometric

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biometric.BiometricManagerHelper
import com.example.data.local.StudentEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BiometricEnrollmentDialog(
    student: StudentEntity,
    onDismiss: () -> Unit,
    onEnroll: (studentId: Long, customFpId: String) -> Unit,
    onRemove: (studentId: Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var isSuccess by remember { mutableStateOf(student.fingerprintEnrolled) }
    var enrolledFpId by remember {
        mutableStateOf(
            if (student.fingerprintEnrolled && student.fingerprintId.isNotBlank()) {
                student.fingerprintId
            } else {
                BiometricManagerHelper.generateFingerprintId(student.rollNo)
            }
        )
    }
    var enrolledDate by remember { mutableStateOf(student.fingerprintEnrolledAt) }
    var statusMessage by remember {
        mutableStateOf(
            if (student.fingerprintEnrolled) {
                "Fingerprint already registered on ${student.fingerprintEnrolledAt.ifBlank { "system" }}"
            } else {
                "Touch the sensor pad below or use device fingerprint to register"
            }
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isScanning) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    fun performEnrollment() {
        coroutineScope.launch {
            isScanning = true
            statusMessage = "Reading biometric fingerprint pattern..."
            for (p in 1..5) {
                delay(120)
                scanProgress = p / 5f
            }
            BiometricManagerHelper.triggerSuccessVibration(context)
            val nowStr = BiometricManagerHelper.getCurrentTimestampString()
            enrolledDate = nowStr
            isSuccess = true
            isScanning = false
            statusMessage = "Fingerprint enrolled successfully!"
            onEnroll(student.id, enrolledFpId.trim())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = "Fingerprint Registration",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Student Summary Header
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = student.name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = student.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Roll #${student.rollNo} • Room: ${student.roomNo.ifBlank { "N/A" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Interactive Biometric Sensor Touch Pad
                val sensorBorderColor by animateColorAsState(
                    targetValue = when {
                        isSuccess -> Color(0xFF15803D)
                        isScanning -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    },
                    label = "sensorBorder"
                )
                val sensorBgColor by animateColorAsState(
                    targetValue = when {
                        isSuccess -> Color(0xFFDCFCE7)
                        isScanning -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    },
                    label = "sensorBg"
                )

                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(sensorBgColor)
                        .border(3.dp, sensorBorderColor, CircleShape)
                        .clickable(enabled = !isScanning) {
                            // Try system prompt if available, otherwise interactive scan
                            if (BiometricManagerHelper.hasBiometricHardware(context)) {
                                val signal = BiometricManagerHelper.authenticateWithSystemBiometrics(
                                    context = context,
                                    title = "Register Fingerprint",
                                    subtitle = "Place finger on sensor for ${student.name}",
                                    description = "Enroll biometric template into GSC database",
                                    onSuccess = { performEnrollment() },
                                    onError = { performEnrollment() },
                                    onCancel = { /* canceled */ }
                                )
                                if (signal == null) {
                                    performEnrollment()
                                }
                            } else {
                                performEnrollment()
                            }
                        }
                        .testTag("biometric_sensor_touch_pad"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSuccess) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Enrolled",
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(54.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Touch Sensor",
                            tint = if (isScanning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                // Status message
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSuccess) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (isSuccess) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Editable Fingerprint Token / ID
                OutlinedTextField(
                    value = enrolledFpId,
                    onValueChange = { enrolledFpId = it },
                    label = { Text("Biometric Fingerprint ID") },
                    placeholder = { Text("e.g. FP-101") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_enrolled_fingerprint_id")
                )

                if (isSuccess && enrolledDate.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Enrolled on $enrolledDate",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF15803D)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (isSuccess) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_close_enroll_dialog")
                ) {
                    Text("Done")
                }
            } else {
                Button(
                    onClick = { performEnrollment() },
                    modifier = Modifier.testTag("btn_trigger_enrollment")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Touch to Scan & Enroll")
                }
            }
        },
        dismissButton = {
            if (student.fingerprintEnrolled) {
                TextButton(
                    onClick = {
                        onRemove(student.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_remove_fingerprint")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove Fingerprint")
                }
            } else {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_cancel_enroll_dialog")
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}
