package com.example.ui.export

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Today
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.pdf.PdfReportGenerator
import com.example.ui.MainViewModel
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ExportMode(val title: String, val testTag: String) {
    DATE_WISE("Date-Wise (Daily)", "tab_export_date_wise"),
    MONTHLY("Monthly Register", "tab_export_monthly")
}

@Composable
fun PdfExportDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf(ExportMode.DATE_WISE) }

    val exportDate by viewModel.exportDate.collectAsStateWithLifecycle()
    val exportYearMonth by viewModel.exportYearMonth.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
    val monthlyData by viewModel.exportReportData.collectAsStateWithLifecycle()
    val dateWiseData by viewModel.exportDateWiseReportData.collectAsStateWithLifecycle()

    var generatedFile by remember { mutableStateOf<File?>(null) }

    // Load initial data snapshots
    LaunchedEffect(exportDate) {
        viewModel.loadDateWisePreview(exportDate)
    }

    LaunchedEffect(exportYearMonth) {
        viewModel.loadMonthlyPreview(exportYearMonth)
    }

    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy", Locale.US)
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF047857).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Color(0xFF047857),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "PDF Attendance Export",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Choose Date-Wise or Monthly report",
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
                    .testTag("pdf_export_dialog_content"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Two Export Options: Date-Wise vs Monthly
                TabRow(
                    selectedTabIndex = if (selectedMode == ExportMode.DATE_WISE) 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedMode == ExportMode.DATE_WISE,
                        onClick = {
                            selectedMode = ExportMode.DATE_WISE
                            generatedFile = null
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Date-Wise", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        },
                        modifier = Modifier.testTag("tab_export_date_wise")
                    )
                    Tab(
                        selected = selectedMode == ExportMode.MONTHLY,
                        onClick = {
                            selectedMode = ExportMode.MONTHLY
                            generatedFile = null
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Monthly", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        },
                        modifier = Modifier.testTag("tab_export_monthly")
                    )
                }

                // ================= OPTION 1: DATE-WISE EXPORT =================
                if (selectedMode == ExportMode.DATE_WISE) {
                    // Date Selector Bar
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = {
                                        viewModel.setExportDate(exportDate.minusDays(1))
                                        generatedFile = null
                                    },
                                    modifier = Modifier.testTag("btn_export_prev_date")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Day")
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = exportDate.format(dateFormatter),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (exportDate == LocalDate.now()) {
                                        Text(
                                            text = "(Today)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF047857),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.setExportDate(exportDate.plusDays(1))
                                        generatedFile = null
                                    },
                                    modifier = Modifier.testTag("btn_export_next_date")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day")
                                }
                            }

                            if (exportDate != LocalDate.now()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    TextButton(
                                        onClick = {
                                            viewModel.setExportDate(LocalDate.now())
                                            generatedFile = null
                                        },
                                        modifier = Modifier.testTag("btn_export_today_date")
                                    ) {
                                        Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Go to Today", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Date-Wise Category Preview Card
                    dateWiseData?.let { data ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Date-Wise Roster Breakdown (Present / Absent)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF047857)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    CategoryChip("Breakfast", data.breakfast.presentStudents.size, data.breakfast.absentStudents.size)
                                    CategoryChip("Lunch", data.lunch.presentStudents.size, data.lunch.absentStudents.size)
                                    CategoryChip("Dinner", data.dinner.presentStudents.size, data.dinner.absentStudents.size)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    CategoryChip("Yoga", data.yoga.presentStudents.size, data.yoga.absentStudents.size)
                                    CategoryChip("Meditation", data.meditation.presentStudents.size, data.meditation.absentStudents.size)
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Students", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${data.totalStudents}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = "• PDF includes distinct Present & Absent student names, roll numbers, and room numbers for Breakfast, Lunch, Dinner, Yoga, and Meditation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ================= OPTION 2: MONTHLY EXPORT =================
                if (selectedMode == ExportMode.MONTHLY) {
                    // Month Selector Bar
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.setExportYearMonth(exportYearMonth.minusMonths(1))
                                    generatedFile = null
                                },
                                modifier = Modifier.testTag("btn_export_prev_month")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                            }

                            Text(
                                text = exportYearMonth.format(monthFormatter),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            IconButton(
                                onClick = {
                                    viewModel.setExportYearMonth(exportYearMonth.plusMonths(1))
                                    generatedFile = null
                                },
                                modifier = Modifier.testTag("btn_export_next_month")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                            }
                        }
                    }

                    // Monthly Summary Preview Card
                    monthlyData?.let { data ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Monthly Overview: Present & Absent Stats",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF047857)
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Students", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${data.totalStudents}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Column {
                                        Text("Meals (P / A)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${data.totalMealsServed} / ${data.totalMealsAbsent}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Column {
                                        Text("Yoga (P / A)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${data.totalYogaAttended} / ${data.totalYogaAbsent}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Column {
                                        Text("Medit. (P / A)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${data.totalMeditationAttended} / ${data.totalMeditationAbsent}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Breakfast: ${data.totalBreakfastServed} | Lunch: ${data.totalLunchServed} | Dinner: ${data.totalDinnerServed} • ${data.daysWithRecords} Recorded Dates",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Text(
                        text = "• PDF provides individual student Meal Absent %, Yoga Absent %, Meditation Absent %, and Total Absent % with distinct Present and Absent lists.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // If PDF is generated, show action buttons (Open & Share)
                generatedFile?.let { file ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFDCFCE7),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF15803D),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PDF Generated: ${file.name}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF15803D)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        PdfReportGenerator.openPdf(context, file)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_open_pdf_viewer")
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("View PDF")
                                }

                                OutlinedButton(
                                    onClick = {
                                        PdfReportGenerator.sharePdf(context, file)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_share_pdf_file")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (generatedFile == null) {
                Button(
                    onClick = {
                        if (selectedMode == ExportMode.DATE_WISE) {
                            viewModel.generateDateWisePdfReport { file ->
                                generatedFile = file
                                Toast.makeText(context, "Date-Wise Report generated!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            viewModel.generatePdfReport { file ->
                                generatedFile = file
                                Toast.makeText(context, "Monthly Report generated!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isExporting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                    modifier = Modifier.testTag("btn_generate_pdf_confirm")
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generating PDF...")
                    } else {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (selectedMode == ExportMode.DATE_WISE) "Generate Date-Wise PDF" else "Generate Monthly PDF")
                    }
                }
            } else {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_done_pdf_dialog")
                ) {
                    Text("Done")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_close_pdf_dialog")
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun CategoryChip(
    title: String,
    presentCount: Int,
    absentCount: Int
) {
    Column(
        modifier = Modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$presentCount", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF047857))
            Text(" / ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$absentCount", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFB91C1C))
        }
    }
}
