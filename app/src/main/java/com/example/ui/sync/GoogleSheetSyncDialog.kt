package com.example.ui.sync

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sync.GoogleAppsScriptTemplate
import com.example.sync.GoogleSheetSyncManager
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GoogleSheetSyncDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val webhookUrl by viewModel.sheetWebhookUrl.collectAsStateWithLifecycle()
    val isAutoSyncEnabled by viewModel.isAutoSyncEnabled.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val lastSyncStatus by viewModel.lastSyncStatus.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsStateWithLifecycle()

    var inputUrl by remember(webhookUrl) { mutableStateOf(webhookUrl) }
    var showScriptInstructions by remember { mutableStateOf(false) }

    val formattedLastSyncTime = if (lastSyncTime > 0) {
        val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
        sdf.format(Date(lastSyncTime))
    } else {
        "Never"
    }

    val isSpreadsheetLink = remember(inputUrl) {
        GoogleSheetSyncManager.isGoogleSpreadsheetUrl(inputUrl) && !GoogleSheetSyncManager.isGoogleAppsScriptUrl(inputUrl)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF047857).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = Color(0xFF047857),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column {
                    Text(
                        text = "Google Sheets Sync",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Two-way live sync & offline backup",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .testTag("google_sheet_sync_dialog_content"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Network Status Indicator Chip
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isNetworkAvailable) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isNetworkAvailable) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = if (isNetworkAvailable) Color(0xFF15803D) else Color(0xFFB45309),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isNetworkAvailable) "Mobile Online — Connected to Google Cloud"
                            else "Mobile Offline — Data stored locally in app; auto-syncs when online",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isNetworkAvailable) Color(0xFF15803D) else Color(0xFFB45309)
                        )
                    }
                }

                // Link Input Field with Paste Button
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Google Sheet / Script URL",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val item = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                                if (!item.isNullOrBlank()) {
                                    inputUrl = item
                                    viewModel.saveSheetWebhookUrl(item)
                                    Toast.makeText(context, "Pasted & saved link!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("btn_paste_sheet_link")
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste Link", fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_sheet_webhook_url"),
                        placeholder = {
                            Text("Paste Web App URL or Google Sheet link", fontSize = 12.sp)
                        },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (inputUrl.isNotBlank() && inputUrl != webhookUrl) {
                                TextButton(
                                    onClick = {
                                        viewModel.saveSheetWebhookUrl(inputUrl)
                                        Toast.makeText(context, "URL Saved", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("btn_save_webhook_url")
                                ) {
                                    Text("Save", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    )

                    if (isSpreadsheetLink) {
                        Text(
                            text = "Direct Google Sheet link detected! You can tap 'Get Data' to import students. For bidirectional auto-updates, deploy the Web App script.",
                            color = Color(0xFF047857),
                            fontSize = 11.sp
                        )
                    }
                }

                // Automated Two-Way Sync Primary Button
                Button(
                    onClick = {
                        if (inputUrl.isNotBlank() && inputUrl != webhookUrl) {
                            viewModel.saveSheetWebhookUrl(inputUrl)
                        }
                        viewModel.autoSyncWithGoogleSheet(inputUrl.ifBlank { null }) { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = !isSyncing && (inputUrl.isNotBlank() || webhookUrl.isNotBlank()),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_auto_sync_two_way")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Auto Syncing (Get Sheet Data -> Update Sheet)...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sync (1. Get from Sheet -> 2. Put on Sheet)", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "Workflow: First gets students & logs from your Google Sheet, and after that puts/updates attendance records onto your Google Sheet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF047857),
                    fontSize = 11.5.sp
                )

                // Optional Individual Operations
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (inputUrl.isNotBlank() && inputUrl != webhookUrl) {
                                viewModel.saveSheetWebhookUrl(inputUrl)
                            }
                            viewModel.fetchDataFromGoogleSheet(inputUrl.ifBlank { null }) { success, message ->
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = !isSyncing && (inputUrl.isNotBlank() || webhookUrl.isNotBlank()),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_get_data_from_sheet")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Get Only", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            if (inputUrl.isNotBlank() && inputUrl != webhookUrl) {
                                viewModel.saveSheetWebhookUrl(inputUrl)
                            }
                            viewModel.syncNowWithGoogleSheet(inputUrl.ifBlank { null }) { success, message ->
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = !isSyncing && (inputUrl.isNotBlank() || webhookUrl.isNotBlank()),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_send_data_to_sheet")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Update Only", fontSize = 12.sp)
                    }
                }

                // Automatic Sync Toggle (Offline-to-Online queueing)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-update data to Google Sheet",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "When offline, data is safely held. Once mobile goes online, all student records and attendance automatically sync to your sheet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isAutoSyncEnabled,
                            onCheckedChange = { viewModel.toggleAutoSync(it) },
                            modifier = Modifier.testTag("switch_auto_sync")
                        )
                    }
                }

                // Sync Status Summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Last Synced Time:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formattedLastSyncTime,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Status:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = lastSyncStatus,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (lastSyncStatus.startsWith("Successfully") || lastSyncStatus.startsWith("Synced") || lastSyncStatus.startsWith("Fetched")) Color(0xFF15803D)
                                else if (lastSyncStatus.startsWith("Failed") || lastSyncStatus.startsWith("Error")) Color(0xFFB91C1C)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Setup Guide Dropdown / Button
                OutlinedButton(
                    onClick = { showScriptInstructions = !showScriptInstructions },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_toggle_setup_instructions"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (showScriptInstructions) Icons.Default.Info else Icons.Default.HelpOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showScriptInstructions) "Hide Setup Guide" else "How to connect Google Sheet")
                }

                if (showScriptInstructions) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Quick 1-Minute Setup:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "1. Open your Google Sheet.\n" +
                                        "2. Go to Extensions > Apps Script.\n" +
                                        "3. Replace Code.gs with the script code.\n" +
                                        "4. Click Deploy > New Deployment > Web app.\n" +
                                        "5. Set Who has access: 'Anyone'.\n" +
                                        "6. Click Deploy, copy Web App URL, and paste here!\n\n" +
                                        "Tip: You can also paste your Google Sheet link directly to pull your student roster.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Google Apps Script", GoogleAppsScriptTemplate.SCRIPT_CODE)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Apps Script code copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_copy_apps_script_code"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Apps Script Code")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_close_sync_dialog")
            ) {
                Text("Done")
            }
        },
        dismissButton = null
    )
}
