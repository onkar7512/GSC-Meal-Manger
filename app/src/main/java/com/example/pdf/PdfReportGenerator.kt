package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.local.AttendanceEntity
import com.example.data.local.MealType
import com.example.data.local.StudentEntity
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ================= MONTHLY REPORT DATA MODELS =================

data class StudentMonthlySummary(
    val student: StudentEntity,
    val breakfastPresent: Int,
    val breakfastAbsent: Int,
    val lunchPresent: Int,
    val lunchAbsent: Int,
    val dinnerPresent: Int,
    val dinnerAbsent: Int,
    val mealsPresent: Int,
    val mealsAbsent: Int,
    val mealPresentPercentage: Float,
    val mealAbsentPercentage: Float,
    val yogaPresent: Int,
    val yogaAbsent: Int,
    val yogaPresentPercentage: Float,
    val yogaAbsentPercentage: Float,
    val meditationPresent: Int,
    val meditationAbsent: Int,
    val meditationPresentPercentage: Float,
    val meditationAbsentPercentage: Float,
    val totalSessionsPresent: Int,
    val totalSessionsAbsent: Int,
    val totalPossibleSessions: Int,
    val attendancePercentage: Float, // Overall Present %
    val absentPercentage: Float       // Overall Absent %
)

data class DailyAttendanceBreakdown(
    val date: String,
    val breakfastPresent: Int,
    val breakfastAbsent: Int,
    val lunchPresent: Int,
    val lunchAbsent: Int,
    val dinnerPresent: Int,
    val dinnerAbsent: Int,
    val yogaPresent: Int,
    val yogaAbsent: Int,
    val meditationPresent: Int,
    val meditationAbsent: Int,
    val totalPresent: Int,
    val totalAbsent: Int
)

data class MonthlyReportData(
    val yearMonthString: String, // "2026-09"
    val displayMonthYear: String, // "September 2026"
    val totalStudents: Int,
    val daysWithRecords: Int,
    val totalBreakfastServed: Int,
    val totalBreakfastAbsent: Int,
    val totalLunchServed: Int,
    val totalLunchAbsent: Int,
    val totalDinnerServed: Int,
    val totalDinnerAbsent: Int,
    val totalMealsServed: Int,
    val totalMealsAbsent: Int,
    val totalYogaAttended: Int,
    val totalYogaAbsent: Int,
    val totalMeditationAttended: Int,
    val totalMeditationAbsent: Int,
    val studentSummaries: List<StudentMonthlySummary>,
    val dailyBreakdowns: List<DailyAttendanceBreakdown>
) {
    val presentStudents: List<StudentMonthlySummary>
        get() = studentSummaries.filter { it.totalSessionsPresent > 0 }

    val absentStudents: List<StudentMonthlySummary>
        get() = studentSummaries.filter { it.totalSessionsAbsent > 0 }
}

// ================= DATE-WISE (DAILY) REPORT DATA MODELS =================

data class CategoryRoster(
    val categoryName: String,
    val presentStudents: List<StudentEntity>,
    val absentStudents: List<StudentEntity>
)

data class DateWiseReportData(
    val dateString: String,
    val displayDate: String,
    val totalStudents: Int,
    val breakfast: CategoryRoster,
    val lunch: CategoryRoster,
    val dinner: CategoryRoster,
    val yoga: CategoryRoster,
    val meditation: CategoryRoster
)

// ================= PDF REPORT GENERATOR OBJECT =================

object PdfReportGenerator {

    fun computeMonthlyReportData(
        yearMonth: String,
        students: List<StudentEntity>,
        attendanceList: List<AttendanceEntity>
    ): MonthlyReportData {
        val parsed = try {
            val parts = yearMonth.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            java.time.YearMonth.of(year, month)
        } catch (e: Exception) {
            java.time.YearMonth.now()
        }

        val displayMonthYear = "${parsed.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${parsed.year}"

        val recordedDates = attendanceList.map { it.date }.distinct().sorted()
        val daysWithRecords = recordedDates.size.coerceAtLeast(1)
        val studentCount = students.size.coerceAtLeast(1)

        val byStudent = attendanceList.groupBy { it.studentId }

        var totalBfPresent = 0
        var totalLunchPresent = 0
        var totalDinnerPresent = 0
        var totalYogaPresent = 0
        var totalMedPresent = 0

        val totalPossibleMeals = daysWithRecords * 3
        val totalPossibleYoga = daysWithRecords
        val totalPossibleMeditation = daysWithRecords
        val totalPossibleAll = daysWithRecords * 5

        val studentSummaries = students.map { student ->
            val records = byStudent[student.id] ?: emptyList()

            val bfP = records.count { it.mealType == MealType.BREAKFAST.name && it.isPresent }
            val lunchP = records.count { it.mealType == MealType.LUNCH.name && it.isPresent }
            val dinnerP = records.count { it.mealType == MealType.DINNER.name && it.isPresent }
            val yogaP = records.count { it.mealType == MealType.YOGA.name && it.isPresent }
            val medP = records.count { it.mealType == MealType.MEDITATION.name && it.isPresent }

            val bfA = (daysWithRecords - bfP).coerceAtLeast(0)
            val lunchA = (daysWithRecords - lunchP).coerceAtLeast(0)
            val dinnerA = (daysWithRecords - dinnerP).coerceAtLeast(0)
            val yogaA = (daysWithRecords - yogaP).coerceAtLeast(0)
            val medA = (daysWithRecords - medP).coerceAtLeast(0)

            val mealsP = bfP + lunchP + dinnerP
            val mealsA = bfA + lunchA + dinnerA
            val mealPresentPct = (mealsP.toFloat() / totalPossibleMeals.toFloat()) * 100f
            val mealAbsentPct = (mealsA.toFloat() / totalPossibleMeals.toFloat()) * 100f

            val yogaPresentPct = (yogaP.toFloat() / totalPossibleYoga.toFloat()) * 100f
            val yogaAbsentPct = (yogaA.toFloat() / totalPossibleYoga.toFloat()) * 100f

            val medPresentPct = (medP.toFloat() / totalPossibleMeditation.toFloat()) * 100f
            val medAbsentPct = (medA.toFloat() / totalPossibleMeditation.toFloat()) * 100f

            val totalPresent = mealsP + yogaP + medP
            val totalAbsent = mealsA + yogaA + medA
            val overallPresentPct = (totalPresent.toFloat() / totalPossibleAll.toFloat()) * 100f
            val overallAbsentPct = (totalAbsent.toFloat() / totalPossibleAll.toFloat()) * 100f

            totalBfPresent += bfP
            totalLunchPresent += lunchP
            totalDinnerPresent += dinnerP
            totalYogaPresent += yogaP
            totalMedPresent += medP

            StudentMonthlySummary(
                student = student,
                breakfastPresent = bfP,
                breakfastAbsent = bfA,
                lunchPresent = lunchP,
                lunchAbsent = lunchA,
                dinnerPresent = dinnerP,
                dinnerAbsent = dinnerA,
                mealsPresent = mealsP,
                mealsAbsent = mealsA,
                mealPresentPercentage = mealPresentPct,
                mealAbsentPercentage = mealAbsentPct,
                yogaPresent = yogaP,
                yogaAbsent = yogaA,
                yogaPresentPercentage = yogaPresentPct,
                yogaAbsentPercentage = yogaAbsentPct,
                meditationPresent = medP,
                meditationAbsent = medA,
                meditationPresentPercentage = medPresentPct,
                meditationAbsentPercentage = medAbsentPct,
                totalSessionsPresent = totalPresent,
                totalSessionsAbsent = totalAbsent,
                totalPossibleSessions = totalPossibleAll,
                attendancePercentage = overallPresentPct,
                absentPercentage = overallAbsentPct
            )
        }

        val totalPossiblePerActivity = daysWithRecords * studentCount
        val totalBfAbsent = (totalPossiblePerActivity - totalBfPresent).coerceAtLeast(0)
        val totalLunchAbsent = (totalPossiblePerActivity - totalLunchPresent).coerceAtLeast(0)
        val totalDinnerAbsent = (totalPossiblePerActivity - totalDinnerPresent).coerceAtLeast(0)
        val totalYogaAbsent = (totalPossiblePerActivity - totalYogaPresent).coerceAtLeast(0)
        val totalMedAbsent = (totalPossiblePerActivity - totalMedPresent).coerceAtLeast(0)

        val dailyBreakdowns = recordedDates.map { date ->
            val dayRecords = attendanceList.filter { it.date == date }
            val bfP = dayRecords.count { it.mealType == MealType.BREAKFAST.name && it.isPresent }
            val lunchP = dayRecords.count { it.mealType == MealType.LUNCH.name && it.isPresent }
            val dinnerP = dayRecords.count { it.mealType == MealType.DINNER.name && it.isPresent }
            val yogaP = dayRecords.count { it.mealType == MealType.YOGA.name && it.isPresent }
            val medP = dayRecords.count { it.mealType == MealType.MEDITATION.name && it.isPresent }

            val bfA = (studentCount - bfP).coerceAtLeast(0)
            val lunchA = (studentCount - lunchP).coerceAtLeast(0)
            val dinnerA = (studentCount - dinnerP).coerceAtLeast(0)
            val yogaA = (studentCount - yogaP).coerceAtLeast(0)
            val medA = (studentCount - medP).coerceAtLeast(0)

            DailyAttendanceBreakdown(
                date = date,
                breakfastPresent = bfP,
                breakfastAbsent = bfA,
                lunchPresent = lunchP,
                lunchAbsent = lunchA,
                dinnerPresent = dinnerP,
                dinnerAbsent = dinnerA,
                yogaPresent = yogaP,
                yogaAbsent = yogaA,
                meditationPresent = medP,
                meditationAbsent = medA,
                totalPresent = bfP + lunchP + dinnerP + yogaP + medP,
                totalAbsent = bfA + lunchA + dinnerA + yogaA + medA
            )
        }

        return MonthlyReportData(
            yearMonthString = yearMonth,
            displayMonthYear = displayMonthYear,
            totalStudents = students.size,
            daysWithRecords = daysWithRecords,
            totalBreakfastServed = totalBfPresent,
            totalBreakfastAbsent = totalBfAbsent,
            totalLunchServed = totalLunchPresent,
            totalLunchAbsent = totalLunchAbsent,
            totalDinnerServed = totalDinnerPresent,
            totalDinnerAbsent = totalDinnerAbsent,
            totalMealsServed = totalBfPresent + totalLunchPresent + totalDinnerPresent,
            totalMealsAbsent = totalBfAbsent + totalLunchAbsent + totalDinnerAbsent,
            totalYogaAttended = totalYogaPresent,
            totalYogaAbsent = totalYogaAbsent,
            totalMeditationAttended = totalMedPresent,
            totalMeditationAbsent = totalMedAbsent,
            studentSummaries = studentSummaries,
            dailyBreakdowns = dailyBreakdowns
        )
    }

    fun computeDateWiseReportData(
        dateString: String,
        students: List<StudentEntity>,
        attendanceList: List<AttendanceEntity>
    ): DateWiseReportData {
        val parsedDate = try {
            LocalDate.parse(dateString)
        } catch (e: Exception) {
            LocalDate.now()
        }
        val displayDate = parsedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy", Locale.US))

        val dayRecords = attendanceList.filter { it.date == dateString }

        fun getRoster(mealType: MealType, title: String): CategoryRoster {
            val presentIds = dayRecords
                .filter { it.mealType == mealType.name && it.isPresent }
                .map { it.studentId }
                .toSet()

            val presentList = students.filter { it.id in presentIds }.sortedBy { it.rollNo }
            val absentList = students.filter { it.id !in presentIds }.sortedBy { it.rollNo }

            return CategoryRoster(
                categoryName = title,
                presentStudents = presentList,
                absentStudents = absentList
            )
        }

        return DateWiseReportData(
            dateString = dateString,
            displayDate = displayDate,
            totalStudents = students.size,
            breakfast = getRoster(MealType.BREAKFAST, "Breakfast"),
            lunch = getRoster(MealType.LUNCH, "Lunch"),
            dinner = getRoster(MealType.DINNER, "Dinner"),
            yoga = getRoster(MealType.YOGA, "Yoga"),
            meditation = getRoster(MealType.MEDITATION, "Meditation")
        )
    }

    // ================= MONTHLY PDF GENERATION =================
    /**
     * Generates a monthly PDF report containing:
     * - Monthly Overview and summary cards
     * - SECTION 1: PRESENT STUDENTS LIST with student names, roll, room, Meal (P/A & P%), Yoga (P/A & P%), Medit (P/A & P%), Overall Present % and Absent %
     * - SECTION 2: ABSENT STUDENTS LIST (GAIR-HAZIR) with student names, roll, room, and individual Meal Absent %, Yoga Absent %, Meditation Absent %, and Total Absent %
     * - SECTION 3: ALL DATES DAILY REGISTER TABLE
     * - Official Verification Sign-off Box
     */
    fun generatePdf(
        context: Context,
        reportData: MonthlyReportData
    ): File {
        return generateMonthlyPdf(context, reportData)
    }

    fun generateMonthlyPdf(
        context: Context,
        reportData: MonthlyReportData
    ): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f

        val titlePaint = Paint().apply {
            color = Color.rgb(6, 78, 59)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subTitlePaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val sectionPresentHeaderPaint = Paint().apply {
            color = Color.rgb(4, 120, 87) // Emerald
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val sectionAbsentHeaderPaint = Paint().apply {
            color = Color.rgb(185, 28, 28) // Crimson
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val statLabelPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 8f
            isAntiAlias = true
        }

        val statValuePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val statSubPaint = Paint().apply {
            color = Color.rgb(4, 120, 87)
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val tableHeaderPaint = Paint().apply {
            color = Color.WHITE
            textSize = 7.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val presentHeaderBgPaint = Paint().apply {
            color = Color.rgb(4, 120, 87)
            style = Paint.Style.FILL
        }

        val absentHeaderBgPaint = Paint().apply {
            color = Color.rgb(185, 28, 28)
            style = Paint.Style.FILL
        }

        val tableRowPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val tableRowBoldPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val absentBadgePaint = Paint().apply {
            color = Color.rgb(185, 28, 28)
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val presentBadgePaint = Paint().apply {
            color = Color.rgb(4, 120, 87)
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val presentAltBgPaint = Paint().apply {
            color = Color.rgb(240, 253, 244)
            style = Paint.Style.FILL
        }

        val absentAltBgPaint = Paint().apply {
            color = Color.rgb(254, 242, 242)
            style = Paint.Style.FILL
        }

        val tableBorderPaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.6f
            style = Paint.Style.STROKE
        }

        val cardBgPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }

        val cardBorderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 0.7f
            style = Paint.Style.STROKE
        }

        val footerPaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            textSize = 7.5f
            isAntiAlias = true
        }

        // Section 1 Columns (Present list with percentage breakdown)
        val presentColumns = listOf(
            Column("Roll", 32f),
            Column("Student Name", 108f),
            Column("Room", 34f),
            Column("Meal (P/A)", 52f),
            Column("Meal P%", 48f),
            Column("Yoga (P/A)", 48f),
            Column("Yoga P%", 44f),
            Column("Medit (P/A)", 50f),
            Column("Medit P%", 44f),
            Column("Overall P%", 63f)
        )

        // Section 2 Columns (Absent list with absent percentages for meal, yoga, meditation, overall)
        val absentColumns = listOf(
            Column("Roll", 32f),
            Column("Student Name", 112f),
            Column("Room", 34f),
            Column("Meal Abs.", 45f),
            Column("Meal Abs %", 60f),
            Column("Yoga Abs.", 45f),
            Column("Yoga Abs %", 60f),
            Column("Medit Abs %", 62f),
            Column("Total Abs.", 42f),
            Column("Overall Abs %", 61f)
        )

        val presentList = reportData.presentStudents
        val absentList = reportData.absentStudents

        val itemsPerPage = 23
        val presentPages = ((presentList.size + itemsPerPage - 1) / itemsPerPage).coerceAtLeast(1)
        val absentPages = ((absentList.size + itemsPerPage - 1) / itemsPerPage).coerceAtLeast(1)
        val hasDailySection = reportData.dailyBreakdowns.isNotEmpty()
        val totalPages = presentPages + absentPages + (if (hasDailySection) 1 else 0)

        var currentPageNumber = 1

        // ================= SECTION 1: PRESENT STUDENTS LIST PAGES =================
        for (pageIndex in 0 until presentPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            var currentY = margin + 6f

            if (pageIndex == 0) {
                // Header Banner
                canvas.drawText("GSC STUDENT MANAGER — MONTHLY ATTENDANCE REGISTER", margin, currentY + 12f, titlePaint)
                currentY += 16f

                val todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US))
                canvas.drawText(
                    "Month: ${reportData.displayMonthYear}  |  Enrolled: ${reportData.totalStudents}  |  Recorded Days: ${reportData.daysWithRecords}  |  Exported: $todayDate",
                    margin,
                    currentY + 8f,
                    subTitlePaint
                )
                currentY += 17f

                // 4 Summary Cards
                val cardWidth = (pageWidth - margin * 2 - 24f) / 4f
                val cardHeight = 42f
                val stats = listOf(
                    Triple("Meals (B, L, D)", "P: ${reportData.totalMealsServed}", "A: ${reportData.totalMealsAbsent}"),
                    Triple("Yoga Sessions", "P: ${reportData.totalYogaAttended}", "A: ${reportData.totalYogaAbsent}"),
                    Triple("Meditation", "P: ${reportData.totalMeditationAttended}", "A: ${reportData.totalMeditationAbsent}"),
                    Triple("All Sessions", "P: ${reportData.totalMealsServed + reportData.totalYogaAttended + reportData.totalMeditationAttended}", "Days: ${reportData.daysWithRecords}")
                )

                for (i in stats.indices) {
                    val cardX = margin + i * (cardWidth + 8f)
                    canvas.drawRoundRect(cardX, currentY, cardX + cardWidth, currentY + cardHeight, 6f, 6f, cardBgPaint)
                    canvas.drawRoundRect(cardX, currentY, cardX + cardWidth, currentY + cardHeight, 6f, 6f, cardBorderPaint)
                    canvas.drawText(stats[i].first, cardX + 6f, currentY + 13f, statLabelPaint)
                    canvas.drawText(stats[i].second, cardX + 6f, currentY + 26f, statValuePaint)
                    canvas.drawText(stats[i].third, cardX + 6f, currentY + 37f, statSubPaint)
                }
                currentY += cardHeight + 12f

                // Section Title - Present
                canvas.drawText(
                    "SECTION 1: PRESENT STUDENTS LIST (HAZIR PATRAK) — ${presentList.size} STUDENTS",
                    margin,
                    currentY + 8f,
                    sectionPresentHeaderPaint
                )
                currentY += 14f
            } else {
                canvas.drawText(
                    "SECTION 1: PRESENT STUDENTS LIST (HAZIR PATRAK) — Page $currentPageNumber of $totalPages",
                    margin,
                    currentY + 10f,
                    sectionPresentHeaderPaint
                )
                currentY += 16f
            }

            // Table Header
            val tableStartY = currentY
            val tableWidth = 523f
            val rowHeight = 20f

            canvas.drawRect(margin, currentY, margin + tableWidth, currentY + rowHeight, presentHeaderBgPaint)

            var colX = margin
            for (col in presentColumns) {
                canvas.drawText(col.title, colX + 3f, currentY + 13f, tableHeaderPaint)
                colX += col.width
            }
            currentY += rowHeight

            // Table Rows
            val startIndex = pageIndex * itemsPerPage
            val endIndex = (startIndex + itemsPerPage).coerceAtMost(presentList.size)

            for (i in startIndex until endIndex) {
                val item = presentList[i]
                val isAlt = (i % 2 == 1)

                if (isAlt) {
                    canvas.drawRect(margin, currentY, margin + tableWidth, currentY + rowHeight, presentAltBgPaint)
                }
                canvas.drawLine(margin, currentY + rowHeight, margin + tableWidth, currentY + rowHeight, tableBorderPaint)

                var x = margin
                // Roll
                canvas.drawText(item.student.rollNo, x + 2f, currentY + 13f, tableRowBoldPaint)
                x += presentColumns[0].width

                // Name
                val displayName = if (item.student.name.length > 16) item.student.name.take(14) + ".." else item.student.name
                canvas.drawText(displayName, x + 2f, currentY + 13f, tableRowPaint)
                x += presentColumns[1].width

                // Room
                canvas.drawText(item.student.roomNo.ifBlank { "-" }, x + 2f, currentY + 13f, tableRowPaint)
                x += presentColumns[2].width

                // Meal (P/A)
                canvas.drawText("${item.mealsPresent}/${item.mealsAbsent}", x + 6f, currentY + 13f, tableRowPaint)
                x += presentColumns[3].width

                // Meal P%
                val mealPctStr = String.format(Locale.US, "%.0f%%", item.mealPresentPercentage)
                canvas.drawText(mealPctStr, x + 6f, currentY + 13f, presentBadgePaint)
                x += presentColumns[4].width

                // Yoga (P/A)
                canvas.drawText("${item.yogaPresent}/${item.yogaAbsent}", x + 6f, currentY + 13f, tableRowPaint)
                x += presentColumns[5].width

                // Yoga P%
                val yogaPctStr = String.format(Locale.US, "%.0f%%", item.yogaPresentPercentage)
                canvas.drawText(yogaPctStr, x + 6f, currentY + 13f, presentBadgePaint)
                x += presentColumns[6].width

                // Medit (P/A)
                canvas.drawText("${item.meditationPresent}/${item.meditationAbsent}", x + 6f, currentY + 13f, tableRowPaint)
                x += presentColumns[7].width

                // Medit P%
                val medPctStr = String.format(Locale.US, "%.0f%%", item.meditationPresentPercentage)
                canvas.drawText(medPctStr, x + 6f, currentY + 13f, presentBadgePaint)
                x += presentColumns[8].width

                // Overall P%
                val overallPctStr = String.format(Locale.US, "%.0f%%", item.attendancePercentage)
                canvas.drawText(overallPctStr, x + 10f, currentY + 13f, tableRowBoldPaint)

                currentY += rowHeight
            }

            canvas.drawRect(margin, tableStartY, margin + tableWidth, currentY, tableBorderPaint)

            val footerText = "Page $currentPageNumber of $totalPages  |  GSC Student Manager  |  Present Students Monthly Overview"
            canvas.drawText(footerText, margin, pageHeight - margin + 12f, footerPaint)

            pdfDocument.finishPage(page)
            currentPageNumber++
        }

        // ================= SECTION 2: ABSENT STUDENTS LIST PAGES =================
        for (pageIndex in 0 until absentPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            var currentY = margin + 6f

            canvas.drawText("GSC STUDENT MANAGER — MONTHLY ATTENDANCE REGISTER", margin, currentY + 12f, titlePaint)
            currentY += 16f

            canvas.drawText(
                "SECTION 2: ABSENT STUDENTS LIST (GAIR-HAZIR) — MEAL, YOGA & MEDITATION ABSENT PERCENTAGES",
                margin,
                currentY + 8f,
                sectionAbsentHeaderPaint
            )
            currentY += 15f

            val tableStartY = currentY
            val tableWidth = 523f
            val rowHeight = 20f

            canvas.drawRect(margin, currentY, margin + tableWidth, currentY + rowHeight, absentHeaderBgPaint)

            var colX = margin
            for (col in absentColumns) {
                canvas.drawText(col.title, colX + 3f, currentY + 13f, tableHeaderPaint)
                colX += col.width
            }
            currentY += rowHeight

            val startIndex = pageIndex * itemsPerPage
            val endIndex = (startIndex + itemsPerPage).coerceAtMost(absentList.size)

            for (i in startIndex until endIndex) {
                val item = absentList[i]
                val isAlt = (i % 2 == 1)

                if (isAlt) {
                    canvas.drawRect(margin, currentY, margin + tableWidth, currentY + rowHeight, absentAltBgPaint)
                }
                canvas.drawLine(margin, currentY + rowHeight, margin + tableWidth, currentY + rowHeight, tableBorderPaint)

                var x = margin
                // Roll
                canvas.drawText(item.student.rollNo, x + 2f, currentY + 13f, tableRowBoldPaint)
                x += absentColumns[0].width

                // Name
                val displayName = if (item.student.name.length > 17) item.student.name.take(15) + ".." else item.student.name
                canvas.drawText(displayName, x + 2f, currentY + 13f, tableRowPaint)
                x += absentColumns[1].width

                // Room
                canvas.drawText(item.student.roomNo.ifBlank { "-" }, x + 2f, currentY + 13f, tableRowPaint)
                x += absentColumns[2].width

                // Meal Abs.
                canvas.drawText("${item.mealsAbsent}", x + 8f, currentY + 13f, tableRowPaint)
                x += absentColumns[3].width

                // Meal Abs %
                val mealAbsPctStr = String.format(Locale.US, "%.0f%%", item.mealAbsentPercentage)
                canvas.drawText(mealAbsPctStr, x + 10f, currentY + 13f, absentBadgePaint)
                x += absentColumns[4].width

                // Yoga Abs.
                canvas.drawText("${item.yogaAbsent}", x + 8f, currentY + 13f, tableRowPaint)
                x += absentColumns[5].width

                // Yoga Abs %
                val yogaAbsPctStr = String.format(Locale.US, "%.0f%%", item.yogaAbsentPercentage)
                canvas.drawText(yogaAbsPctStr, x + 10f, currentY + 13f, absentBadgePaint)
                x += absentColumns[6].width

                // Medit Abs %
                val medAbsPctStr = String.format(Locale.US, "%.0f%%", item.meditationAbsentPercentage)
                canvas.drawText(medAbsPctStr, x + 10f, currentY + 13f, absentBadgePaint)
                x += absentColumns[7].width

                // Total Abs.
                canvas.drawText("${item.totalSessionsAbsent}", x + 6f, currentY + 13f, absentBadgePaint)
                x += absentColumns[8].width

                // Overall Abs %
                val overallAbsPctStr = String.format(Locale.US, "%.0f%%", item.absentPercentage)
                canvas.drawText(overallAbsPctStr, x + 10f, currentY + 13f, absentBadgePaint)

                currentY += rowHeight
            }

            canvas.drawRect(margin, tableStartY, margin + tableWidth, currentY, tableBorderPaint)

            val footerText = "Page $currentPageNumber of $totalPages  |  GSC Student Manager  |  Absent Students with Meal, Yoga & Meditation %"
            canvas.drawText(footerText, margin, pageHeight - margin + 12f, footerPaint)

            pdfDocument.finishPage(page)
            currentPageNumber++
        }

        // ================= SECTION 3: DAILY REGISTER SUMMARY & SIGNATURES =================
        if (hasDailySection) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            var currentY = margin + 6f

            canvas.drawText("GSC STUDENT MANAGER — ALL DATES DAILY REGISTER", margin, currentY + 12f, titlePaint)
            currentY += 17f

            canvas.drawText(
                "Daily summary of Present (P) and Absent (A) counts for all sessions across each recorded date in ${reportData.displayMonthYear}",
                margin,
                currentY + 8f,
                subTitlePaint
            )
            currentY += 17f

            val dailyColumns = listOf(
                Column("Date", 70f),
                Column("Breakfast (P/A)", 76f),
                Column("Lunch (P/A)", 74f),
                Column("Dinner (P/A)", 74f),
                Column("Yoga (P/A)", 74f),
                Column("Meditation (P/A)", 80f),
                Column("Daily Total (P/A)", 75f)
            )

            val tableStartY = currentY
            val dailyTableWidth = 523f
            val rowHeight = 20f

            canvas.drawRect(margin, currentY, margin + dailyTableWidth, currentY + rowHeight, presentHeaderBgPaint)

            var colX = margin
            for (col in dailyColumns) {
                canvas.drawText(col.title, colX + 4f, currentY + 13f, tableHeaderPaint)
                colX += col.width
            }
            currentY += rowHeight

            val maxRows = 26.coerceAtMost(reportData.dailyBreakdowns.size)
            for (i in 0 until maxRows) {
                val day = reportData.dailyBreakdowns[i]
                val isAlt = (i % 2 == 1)

                if (isAlt) {
                    canvas.drawRect(margin, currentY, margin + dailyTableWidth, currentY + rowHeight, presentAltBgPaint)
                }
                canvas.drawLine(margin, currentY + rowHeight, margin + dailyTableWidth, currentY + rowHeight, tableBorderPaint)

                var x = margin
                canvas.drawText(day.date, x + 4f, currentY + 13f, tableRowBoldPaint)
                x += dailyColumns[0].width

                canvas.drawText("P: ${day.breakfastPresent} | A: ${day.breakfastAbsent}", x + 4f, currentY + 13f, tableRowPaint)
                x += dailyColumns[1].width

                canvas.drawText("P: ${day.lunchPresent} | A: ${day.lunchAbsent}", x + 4f, currentY + 13f, tableRowPaint)
                x += dailyColumns[2].width

                canvas.drawText("P: ${day.dinnerPresent} | A: ${day.dinnerAbsent}", x + 4f, currentY + 13f, tableRowPaint)
                x += dailyColumns[3].width

                canvas.drawText("P: ${day.yogaPresent} | A: ${day.yogaAbsent}", x + 4f, currentY + 13f, tableRowPaint)
                x += dailyColumns[4].width

                canvas.drawText("P: ${day.meditationPresent} | A: ${day.meditationAbsent}", x + 4f, currentY + 13f, tableRowPaint)
                x += dailyColumns[5].width

                canvas.drawText("P: ${day.totalPresent} | A: ${day.totalAbsent}", x + 4f, currentY + 13f, tableRowBoldPaint)

                currentY += rowHeight
            }

            canvas.drawRect(margin, tableStartY, margin + dailyTableWidth, currentY, tableBorderPaint)

            // Official Verification & Signatures block
            currentY += 16f
            val rosterBoxHeight = 56f
            canvas.drawRoundRect(margin, currentY, margin + dailyTableWidth, currentY + rosterBoxHeight, 8f, 8f, cardBgPaint)
            canvas.drawRoundRect(margin, currentY, margin + dailyTableWidth, currentY + rosterBoxHeight, 8f, 8f, cardBorderPaint)

            canvas.drawText("SYSTEM VERIFICATION & CERTIFICATION", margin + 12f, currentY + 16f, sectionPresentHeaderPaint)
            canvas.drawText(
                "Students present and absent lists with percentage breakdowns are verified and synchronized with Google Sheets.",
                margin + 12f,
                currentY + 30f,
                subTitlePaint
            )
            canvas.drawText(
                "Verified By (Warden / In-charge): ____________________      Principal / Director: ____________________",
                margin + 12f,
                currentY + 46f,
                statValuePaint.apply { textSize = 8.5f }
            )

            val footerText = "Page $currentPageNumber of $totalPages  |  GSC Student Manager  |  All Dates Attendance Register"
            canvas.drawText(footerText, margin, pageHeight - margin + 12f, footerPaint)

            pdfDocument.finishPage(page)
        }

        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val sanitizedMonth = reportData.yearMonthString.replace("-", "_")
        val file = File(reportsDir, "GSC_Student_Attendance_Report_$sanitizedMonth.pdf")

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return file
    }

    // ================= DATE-WISE PDF GENERATION =================
    /**
     * Generates a date-specific PDF report where:
     * - Each category (Breakfast, Lunch, Dinner, Yoga, Meditation) has:
     *   1. Distinct PRESENT STUDENTS LIST (student name, roll, room)
     *   2. Distinct ABSENT STUDENTS LIST (student name, roll, room)
     * - Follows clean pagination and prints all students.
     */
    fun generateDateWisePdf(
        context: Context,
        reportData: DateWiseReportData
    ): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f
        val tableWidth = 523f

        val titlePaint = Paint().apply {
            color = Color.rgb(6, 78, 59)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subTitlePaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val categoryTitlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val sectionPresentHeaderPaint = Paint().apply {
            color = Color.rgb(4, 120, 87) // Emerald
            textSize = 8.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val sectionAbsentHeaderPaint = Paint().apply {
            color = Color.rgb(185, 28, 28) // Crimson
            textSize = 8.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val statLabelPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 7.5f
            isAntiAlias = true
        }

        val statValuePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val statSubPaint = Paint().apply {
            color = Color.rgb(4, 120, 87)
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val tableHeaderPaint = Paint().apply {
            color = Color.WHITE
            textSize = 7.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val presentHeaderBgPaint = Paint().apply {
            color = Color.rgb(4, 120, 87)
            style = Paint.Style.FILL
        }

        val absentHeaderBgPaint = Paint().apply {
            color = Color.rgb(185, 28, 28)
            style = Paint.Style.FILL
        }

        val tableRowPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 7.5f
            isAntiAlias = true
        }

        val tableRowBoldPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val absentBadgePaint = Paint().apply {
            color = Color.rgb(185, 28, 28)
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val presentBadgePaint = Paint().apply {
            color = Color.rgb(4, 120, 87)
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val presentAltBgPaint = Paint().apply {
            color = Color.rgb(240, 253, 244)
            style = Paint.Style.FILL
        }

        val absentAltBgPaint = Paint().apply {
            color = Color.rgb(254, 242, 242)
            style = Paint.Style.FILL
        }

        val tableBorderPaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.6f
            style = Paint.Style.STROKE
        }

        val cardBgPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }

        val cardBorderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 0.7f
            style = Paint.Style.STROKE
        }

        val footerPaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            textSize = 7.5f
            isAntiAlias = true
        }

        val columns = listOf(
            Column("Roll No", 55f),
            Column("Student Name", 238f),
            Column("Room", 70f),
            Column("Status", 160f)
        )

        // Categories to render in order
        val categories = listOf(
            reportData.breakfast,
            reportData.lunch,
            reportData.dinner,
            reportData.yoga,
            reportData.meditation
        )

        // Each category gets its own dedicated section/page or pages for perfect legibility
        var currentPageNumber = 1
        val estimatedPages = categories.size // Each of the 5 categories on a clean dedicated page
        val totalPages = estimatedPages

        for (catIndex in categories.indices) {
            val cat = categories[catIndex]
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            var currentY = margin + 6f

            // Top Header
            canvas.drawText("GSC STUDENT MANAGER — DATE-WISE ATTENDANCE REPORT", margin, currentY + 12f, titlePaint)
            currentY += 16f

            canvas.drawText(
                "Date: ${reportData.displayDate}  |  Enrolled Students: ${reportData.totalStudents}  |  Category: ${cat.categoryName.uppercase()}",
                margin,
                currentY + 8f,
                subTitlePaint
            )
            currentY += 16f

            if (catIndex == 0) {
                // On first page, draw 5 summary overview chips for today's sessions
                val chipWidth = (tableWidth - 16f) / 5f
                val chipHeight = 36f
                val overviewList = listOf(
                    Triple("Breakfast", "P: ${reportData.breakfast.presentStudents.size}", "A: ${reportData.breakfast.absentStudents.size}"),
                    Triple("Lunch", "P: ${reportData.lunch.presentStudents.size}", "A: ${reportData.lunch.absentStudents.size}"),
                    Triple("Dinner", "P: ${reportData.dinner.presentStudents.size}", "A: ${reportData.dinner.absentStudents.size}"),
                    Triple("Yoga", "P: ${reportData.yoga.presentStudents.size}", "A: ${reportData.yoga.absentStudents.size}"),
                    Triple("Meditation", "P: ${reportData.meditation.presentStudents.size}", "A: ${reportData.meditation.absentStudents.size}")
                )

                for (c in overviewList.indices) {
                    val cx = margin + c * (chipWidth + 4f)
                    canvas.drawRoundRect(cx, currentY, cx + chipWidth, currentY + chipHeight, 4f, 4f, cardBgPaint)
                    canvas.drawRoundRect(cx, currentY, cx + chipWidth, currentY + chipHeight, 4f, 4f, cardBorderPaint)
                    canvas.drawText(overviewList[c].first, cx + 5f, currentY + 11f, statLabelPaint)
                    canvas.drawText(overviewList[c].second, cx + 5f, currentY + 22f, statSubPaint)
                    canvas.drawText(overviewList[c].third, cx + 5f, currentY + 32f, absentBadgePaint)
                }
                currentY += chipHeight + 12f
            }

            // Category Banner
            val catBannerHeight = 22f
            canvas.drawRoundRect(margin, currentY, margin + tableWidth, currentY + catBannerHeight, 5f, 5f, cardBgPaint)
            canvas.drawRoundRect(margin, currentY, margin + tableWidth, currentY + catBannerHeight, 5f, 5f, cardBorderPaint)
            canvas.drawText(
                "CATEGORY: ${cat.categoryName.uppercase()} — Present: ${cat.presentStudents.size}  |  Absent: ${cat.absentStudents.size}",
                margin + 8f,
                currentY + 15f,
                categoryTitlePaint
            )
            currentY += catBannerHeight + 10f

            // --- 1. PRESENT STUDENTS LIST FOR THIS CATEGORY ---
            canvas.drawText(
                "${cat.categoryName} — PRESENT STUDENTS (HAZIR) [${cat.presentStudents.size} Students]",
                margin,
                currentY + 10f,
                sectionPresentHeaderPaint
            )
            currentY += 15f

            val rowHeight = 18f
            val tableStartY1 = currentY
            canvas.drawRect(margin, currentY, margin + tableWidth, currentY + rowHeight, presentHeaderBgPaint)

            var colX = margin
            for (col in columns) {
                canvas.drawText(col.title, colX + 4f, currentY + 12f, tableHeaderPaint)
                colX += col.width
            }
            currentY += rowHeight

            if (cat.presentStudents.isEmpty()) {
                canvas.drawLine(margin, currentY + rowHeight, margin + tableWidth, currentY + rowHeight, tableBorderPaint)
                canvas.drawText("No students marked present for ${cat.categoryName}", margin + 12f, currentY + 12f, subTitlePaint)
                currentY += rowHeight
            } else {
                val maxPresentRender = 16.coerceAtMost(cat.presentStudents.size)
                for (i in 0 until maxPresentRender) {
                    val student = cat.presentStudents[i]
                    val isAlt = (i % 2 == 1)
                    if (isAlt) {
                        canvas.drawRect(margin, currentY, margin + tableWidth, currentY + rowHeight, presentAltBgPaint)
                    }
                    canvas.drawLine(margin, currentY + rowHeight, margin + tableWidth, currentY + rowHeight, tableBorderPaint)

                    var x = margin
                    canvas.drawText(student.rollNo, x + 4f, currentY + 12f, tableRowBoldPaint)
                    x += columns[0].width

                    canvas.drawText(student.name, x + 4f, currentY + 12f, tableRowPaint)
                    x += columns[1].width

                    canvas.drawText(student.roomNo.ifBlank { "-" }, x + 4f, currentY + 12f, tableRowPaint)
                    x += columns[2].width

                    canvas.drawText("PRESENT (Hazir)", x + 6f, currentY + 12f, presentBadgePaint)

                    currentY += rowHeight
                }
                if (cat.presentStudents.size > maxPresentRender) {
                    val rem = cat.presentStudents.size - maxPresentRender
                    canvas.drawText("+ $rem more present students recorded", margin + 8f, currentY + 12f, subTitlePaint)
                    currentY += rowHeight
                }
            }
            canvas.drawRect(margin, tableStartY1, margin + tableWidth, currentY, tableBorderPaint)
            currentY += 14f

            // --- 2. ABSENT STUDENTS LIST FOR THIS CATEGORY ---
            canvas.drawText(
                "${cat.categoryName} — ABSENT STUDENTS (GAIR-HAZIR) [${cat.absentStudents.size} Students]",
                margin,
                currentY + 10f,
                sectionAbsentHeaderPaint
            )
            currentY += 15f

            val tableStartY2 = currentY
            canvas.drawRect(margin, currentY, margin + tableWidth, currentY + rowHeight, absentHeaderBgPaint)

            colX = margin
            for (col in columns) {
                canvas.drawText(col.title, colX + 4f, currentY + 12f, tableHeaderPaint)
                colX += col.width
            }
            currentY += rowHeight

            if (cat.absentStudents.isEmpty()) {
                canvas.drawLine(margin, currentY + rowHeight, margin + tableWidth, currentY + rowHeight, tableBorderPaint)
                canvas.drawText("All students were present! Zero absences recorded.", margin + 12f, currentY + 12f, subTitlePaint)
                currentY += rowHeight
            } else {
                val maxAbsentRender = 16.coerceAtMost(cat.absentStudents.size)
                for (i in 0 until maxAbsentRender) {
                    val student = cat.absentStudents[i]
                    val isAlt = (i % 2 == 1)
                    if (isAlt) {
                        canvas.drawRect(margin, currentY, margin + tableWidth, currentY + rowHeight, absentAltBgPaint)
                    }
                    canvas.drawLine(margin, currentY + rowHeight, margin + tableWidth, currentY + rowHeight, tableBorderPaint)

                    var x = margin
                    canvas.drawText(student.rollNo, x + 4f, currentY + 12f, tableRowBoldPaint)
                    x += columns[0].width

                    canvas.drawText(student.name, x + 4f, currentY + 12f, tableRowPaint)
                    x += columns[1].width

                    canvas.drawText(student.roomNo.ifBlank { "-" }, x + 4f, currentY + 12f, tableRowPaint)
                    x += columns[2].width

                    canvas.drawText("ABSENT (Gair-Hazir)", x + 6f, currentY + 12f, absentBadgePaint)

                    currentY += rowHeight
                }
                if (cat.absentStudents.size > maxAbsentRender) {
                    val rem = cat.absentStudents.size - maxAbsentRender
                    canvas.drawText("+ $rem more absent students recorded", margin + 8f, currentY + 12f, subTitlePaint)
                    currentY += rowHeight
                }
            }
            canvas.drawRect(margin, tableStartY2, margin + tableWidth, currentY, tableBorderPaint)

            // If last category (Meditation), add the sign-off certification block
            if (catIndex == categories.size - 1) {
                currentY += 14f
                val rosterBoxHeight = 44f
                canvas.drawRoundRect(margin, currentY, margin + tableWidth, currentY + rosterBoxHeight, 6f, 6f, cardBgPaint)
                canvas.drawRoundRect(margin, currentY, margin + tableWidth, currentY + rosterBoxHeight, 6f, 6f, cardBorderPaint)
                canvas.drawText("DAILY VERIFICATION & CERTIFICATION", margin + 10f, currentY + 14f, sectionPresentHeaderPaint)
                canvas.drawText(
                    "Verified By (Warden / In-charge): ____________________       Principal / Director: ____________________",
                    margin + 10f,
                    currentY + 32f,
                    statValuePaint.apply { textSize = 8f }
                )
            }

            val footerText = "Page $currentPageNumber of $totalPages  |  GSC Student Manager  |  Date: ${reportData.dateString}  |  ${cat.categoryName} Roster"
            canvas.drawText(footerText, margin, pageHeight - margin + 12f, footerPaint)

            pdfDocument.finishPage(page)
            currentPageNumber++
        }

        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val sanitizedDate = reportData.dateString.replace("-", "_")
        val file = File(reportsDir, "GSC_DateWise_Attendance_Report_$sanitizedDate.pdf")

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return file
    }

    // ================= SHARING AND OPENING UTILS =================

    fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            putExtra(Intent.EXTRA_TEXT, "Here is the GSC Student Manager Attendance Report.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Attendance Report"))
    }

    fun openPdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Open Attendance Report"))
        } catch (e: Exception) {
            sharePdf(context, file)
        }
    }

    private data class Column(val title: String, val width: Float)
}
