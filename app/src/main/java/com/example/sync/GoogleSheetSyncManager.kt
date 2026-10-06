package com.example.sync

import android.content.Context
import android.util.Log
import com.example.data.local.AttendanceEntity
import com.example.data.local.MealDatabase
import com.example.data.local.MealType
import com.example.data.local.StudentEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class SyncFetchResult(
    val studentCount: Int,
    val attendanceCount: Int,
    val message: String
)

data class TwoWaySyncResult(
    val studentsFetched: Int,
    val recordsUpdated: Int,
    val message: String
)

object GoogleSheetSyncManager {
    private const val TAG = "GoogleSheetSync"

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    fun isGoogleSpreadsheetUrl(url: String): Boolean {
        return url.contains("docs.google.com/spreadsheets", ignoreCase = true)
    }

    fun isGoogleAppsScriptUrl(url: String): Boolean {
        return url.contains("script.google.com", ignoreCase = true)
    }

    fun extractSpreadsheetId(url: String): String? {
        val regex = Regex("""spreadsheets/d/([a-zA-Z0-9-_]+)""")
        return regex.find(url)?.groupValues?.get(1)
    }

    /**
     * Automatic Two-Phase Sync:
     * 1. FIRST: Automatically gets/pulls latest data from Google Sheet into local database.
     * 2. AFTER: Updates Google Sheet with all local attendance records (PRESENT/ABSENT) from the app.
     * Fulfills "auto first get data from sheet after update by app".
     */
    suspend fun autoSyncTwoWay(
        context: Context,
        customUrl: String? = null
    ): Result<TwoWaySyncResult> = withContext(Dispatchers.IO) {
        val targetUrl = (customUrl ?: SyncPreferences.getWebhookUrlOnce(context)).trim()
        if (targetUrl.isBlank()) {
            val errorMsg = "Google Sheet URL not configured"
            SyncPreferences.updateSyncFailure(context, errorMsg)
            return@withContext Result.failure(IllegalStateException(errorMsg))
        }

        var fetchedStudents = 0
        // Step 1: FIRST get data from Google Sheet
        try {
            val fetchResult = fetchDataFromGoogleSheet(context, targetUrl)
            if (fetchResult.isSuccess) {
                fetchedStudents = fetchResult.getOrNull()?.studentCount ?: 0
                Log.d(TAG, "Step 1 complete: Fetched $fetchedStudents students from Google Sheet")
            } else {
                Log.w(TAG, "Step 1 note: ${fetchResult.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Step 1 fetch error (proceeding to push): ${e.message}")
        }

        // Step 2: AFTER that, update Google Sheet with local app attendance
        val pushResult = syncLocalDatabaseToGoogleSheet(context)
        if (pushResult.isSuccess) {
            val recordCount = pushResult.getOrNull() ?: 0
            val successMsg = if (fetchedStudents > 0) {
                "Synced! First retrieved $fetchedStudents students from sheet, then updated $recordCount attendance records."
            } else {
                "Synced! Updated $recordCount attendance records to Google Sheet."
            }
            SyncPreferences.updateSyncSuccess(context, recordCount)
            Result.success(TwoWaySyncResult(fetchedStudents, recordCount, successMsg))
        } else {
            val err = pushResult.exceptionOrNull()?.message ?: "Failed to update Google Sheet"
            // If direct spreadsheet URL was provided without apps script, inform user
            if (isGoogleSpreadsheetUrl(targetUrl) && !isGoogleAppsScriptUrl(targetUrl) && fetchedStudents > 0) {
                val partialMsg = "Retrieved $fetchedStudents students from Google Sheet! (To enable two-way writes, deploy Apps Script Web App)"
                SyncPreferences.updateSyncSuccess(context, fetchedStudents)
                Result.success(TwoWaySyncResult(fetchedStudents, 0, partialMsg))
            } else {
                SyncPreferences.updateSyncFailure(context, err)
                Result.failure(Exception(err))
            }
        }
    }

    /**
     * Pushes all enrolled students and all attendance records across all dates to Google Sheet.
     * Generates explicit PRESENT / ABSENT status so the spreadsheet reflects full data.
     */
    suspend fun syncLocalDatabaseToGoogleSheet(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val webhookUrl = SyncPreferences.getWebhookUrlOnce(context).trim()
            if (webhookUrl.isBlank()) {
                val errorMsg = "Google Sheet Webhook URL not configured"
                Log.w(TAG, errorMsg)
                SyncPreferences.updateSyncFailure(context, errorMsg)
                return@withContext Result.failure(IllegalStateException(errorMsg))
            }

            if (isGoogleSpreadsheetUrl(webhookUrl) && !isGoogleAppsScriptUrl(webhookUrl)) {
                val note = "To push data automatically, deploy the Google Apps Script Web App URL."
                Log.w(TAG, note)
                SyncPreferences.updateSyncFailure(context, note)
                return@withContext Result.failure(IllegalStateException(note))
            }

            val db = MealDatabase.getDatabase(context)
            val students = db.studentDao().getAllStudentsSnapshot()
            val attendances = db.attendanceDao().getAllAttendanceSnapshot()

            val studentMap = students.associateBy { it.id }

            val studentPayloads = students.map {
                SheetStudentPayload(
                    id = it.id,
                    rollNo = it.rollNo,
                    name = it.name,
                    roomNo = it.roomNo,
                    phone = it.phone
                )
            }

            // Group existing attendance by key (date_mealType_studentId)
            val existingAttendanceMap = attendances.associateBy { "${it.date}_${it.mealType}_${it.studentId}" }

            // Extract all distinct dates in the database
            val distinctDates = attendances.map { it.date }.distinct().sortedDescending()
            val allMealTypes = listOf(
                MealType.BREAKFAST.name,
                MealType.LUNCH.name,
                MealType.DINNER.name,
                MealType.YOGA.name,
                MealType.MEDITATION.name
            )

            val fullAttendancePayloads = mutableListOf<SheetAttendancePayload>()

            if (distinctDates.isNotEmpty()) {
                // Generate comprehensive status (PRESENT or ABSENT) for every student across all dates & sessions
                for (date in distinctDates) {
                    for (meal in allMealTypes) {
                        for (student in students) {
                            val key = "${date}_${meal}_${student.id}"
                            val existing = existingAttendanceMap[key]
                            val isPresent = existing?.isPresent == true
                            val recordedAt = existing?.recordedAt ?: System.currentTimeMillis()

                            fullAttendancePayloads.add(
                                SheetAttendancePayload(
                                    studentRollNo = student.rollNo,
                                    studentName = student.name,
                                    roomNo = student.roomNo,
                                    date = date,
                                    mealType = meal,
                                    status = if (isPresent) "PRESENT" else "ABSENT",
                                    recordedAt = recordedAt
                                )
                            )
                        }
                    }
                }
            } else {
                // If no dates yet, just map raw attendances
                for (att in attendances) {
                    val student = studentMap[att.studentId]
                    fullAttendancePayloads.add(
                        SheetAttendancePayload(
                            studentRollNo = student?.rollNo ?: "ID:${att.studentId}",
                            studentName = student?.name ?: "Unknown",
                            roomNo = student?.roomNo ?: "",
                            date = att.date,
                            mealType = att.mealType,
                            status = if (att.isPresent) "PRESENT" else "ABSENT",
                            recordedAt = att.recordedAt
                        )
                    )
                }
            }

            val syncRequest = GoogleSheetSyncRequest(
                action = "SYNC_ATTENDANCE",
                timestamp = System.currentTimeMillis(),
                students = studentPayloads,
                attendance = fullAttendancePayloads
            )

            val adapter = moshi.adapter(GoogleSheetSyncRequest::class.java)
            val jsonBody = adapter.toJson(syncRequest)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(webhookUrl)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorMsg = "HTTP ${response.code}: ${response.message}"
                Log.e(TAG, "Sync failed with $errorMsg")
                SyncPreferences.updateSyncFailure(context, errorMsg)
                return@withContext Result.failure(Exception(errorMsg))
            }

            val responseBody = response.body?.string().orEmpty()
            Log.d(TAG, "Google Sheet sync response: $responseBody")

            val totalSyncedRecords = fullAttendancePayloads.size
            SyncPreferences.updateSyncSuccess(context, totalSyncedRecords)
            SyncPreferences.clearDeletedRollNumbers(context)
            Result.success(totalSyncedRecords)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing to Google Sheet", e)
            val message = e.localizedMessage ?: "Unknown network error"
            SyncPreferences.updateSyncFailure(context, message)
            Result.failure(e)
        }
    }

    /**
     * Pulls data from Google Sheet.
     * Supports both Apps Script Web App URLs and standard Google Sheet URLs (via CSV export).
     */
    suspend fun fetchDataFromGoogleSheet(
        context: Context,
        customUrl: String? = null
    ): Result<SyncFetchResult> = withContext(Dispatchers.IO) {
        try {
            val targetUrl = (customUrl ?: SyncPreferences.getWebhookUrlOnce(context)).trim()
            if (targetUrl.isBlank()) {
                return@withContext Result.failure(IllegalStateException("No Google Sheet link provided"))
            }

            val db = MealDatabase.getDatabase(context)

            if (isGoogleSpreadsheetUrl(targetUrl) && !isGoogleAppsScriptUrl(targetUrl)) {
                // Handle direct Google Sheet link via CSV export
                val sheetId = extractSpreadsheetId(targetUrl)
                    ?: return@withContext Result.failure(IllegalArgumentException("Invalid Google Sheet URL format"))

                // Try fetching "Students" sheet or first sheet as CSV
                val csvUrl = "https://docs.google.com/spreadsheets/d/$sheetId/export?format=csv&sheet=Students"
                val request = Request.Builder().url(csvUrl).get().build()
                val response = okHttpClient.newCall(request).execute()

                val body = if (response.isSuccessful) {
                    response.body?.string().orEmpty()
                } else {
                    // Fallback to primary sheet gid=0
                    val fallbackUrl = "https://docs.google.com/spreadsheets/d/$sheetId/export?format=csv&gid=0"
                    val fallbackReq = Request.Builder().url(fallbackUrl).get().build()
                    val fallbackResp = okHttpClient.newCall(fallbackReq).execute()
                    if (!fallbackResp.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("Unable to read Google Sheet. Ensure sheet sharing is set to 'Anyone with the link can view' or use Apps Script Web App.")
                        )
                    }
                    fallbackResp.body?.string().orEmpty()
                }
                val deletedRolls = SyncPreferences.getDeletedRollNumbers(context)
                val allImported = parseStudentsFromCsv(body)
                val importedStudents = allImported.filter { !deletedRolls.contains(it.rollNo.trim().lowercase()) }
                if (importedStudents.isEmpty() && allImported.isEmpty()) {
                    return@withContext Result.failure(Exception("No valid student rows found in Google Sheet"))
                }

                val sheetRollNos = importedStudents.map { it.rollNo.trim().lowercase() }.toSet()
                val existingList = db.studentDao().getAllStudentsSnapshot()

                // Remove students locally that were deleted on Google Sheet or locally
                for (localStudent in existingList) {
                    val rollLower = localStudent.rollNo.trim().lowercase()
                    if (deletedRolls.contains(rollLower) || (!sheetRollNos.contains(rollLower) && sheetRollNos.isNotEmpty())) {
                        db.studentDao().deleteStudent(localStudent)
                    }
                }

                // Upsert remaining students into Room
                val currentExisting = db.studentDao().getAllStudentsSnapshot().associateBy { it.rollNo.trim().lowercase() }
                val toInsert = mutableListOf<StudentEntity>()

                for (s in importedStudents) {
                    val existingStudent = currentExisting[s.rollNo.trim().lowercase()]
                    if (existingStudent != null) {
                        db.studentDao().updateStudent(
                            existingStudent.copy(
                                name = s.name,
                                roomNo = s.roomNo,
                                phone = s.phone
                            )
                        )
                    } else {
                        toInsert.add(s)
                    }
                }

                if (toInsert.isNotEmpty()) {
                    db.studentDao().insertStudents(toInsert)
                }

                val result = SyncFetchResult(
                    studentCount = importedStudents.size,
                    attendanceCount = 0,
                    message = "Successfully imported ${importedStudents.size} students from Google Sheet!"
                )
                SyncPreferences.updateSyncSuccess(context, importedStudents.size)
                return@withContext Result.success(result)

            } else {
                // Apps Script Web App URL
                val getRequest = Request.Builder().url(targetUrl).get().build()
                val response = okHttpClient.newCall(getRequest).execute()

                val json = if (response.isSuccessful) {
                    response.body?.string().orEmpty()
                } else {
                    // Try POST action: GET_DATA
                    val postBody = """{"action":"GET_DATA"}""".toRequestBody("application/json".toMediaType())
                    val postReq = Request.Builder().url(targetUrl).post(postBody).build()
                    val postResp = okHttpClient.newCall(postReq).execute()
                    if (!postResp.isSuccessful) {
                        return@withContext Result.failure(Exception("HTTP ${postResp.code}: ${postResp.message}"))
                    }
                    postResp.body?.string().orEmpty()
                }

                val adapter = moshi.adapter(GoogleSheetFetchResponse::class.java)
                val fetchResponse = adapter.fromJson(json)
                    ?: return@withContext Result.failure(Exception("Invalid response format from Google Apps Script"))

                val deletedRolls = SyncPreferences.getDeletedRollNumbers(context)
                val rawStudentsFromSheet = fetchResponse.students.orEmpty()
                val studentsFromSheet = rawStudentsFromSheet.filter {
                    val roll = it.rollNo.trim().lowercase()
                    !deletedRolls.contains(roll)
                }
                val attendanceFromSheet = fetchResponse.attendance.orEmpty()

                val sheetRollNos = studentsFromSheet.map { it.rollNo.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
                val existingList = db.studentDao().getAllStudentsSnapshot()

                // Delete local students that were removed from the Google Sheet or marked deleted by user
                for (localStudent in existingList) {
                    val rollLower = localStudent.rollNo.trim().lowercase()
                    if (deletedRolls.contains(rollLower) || (!sheetRollNos.contains(rollLower) && sheetRollNos.isNotEmpty())) {
                        db.studentDao().deleteStudent(localStudent)
                    }
                }

                val currentExisting = db.studentDao().getAllStudentsSnapshot().associateBy { it.rollNo.trim().lowercase() }
                var studentCount = 0
                val newStudents = mutableListOf<StudentEntity>()

                for (s in studentsFromSheet) {
                    if (s.name.isBlank() && s.rollNo.isBlank()) continue
                    val roll = s.rollNo.ifBlank { "10${studentCount + 1}" }
                    val name = s.name.ifBlank { "Student $roll" }
                    val rollLower = roll.trim().lowercase()
                    val existingStudent = currentExisting[rollLower]

                    if (existingStudent != null) {
                        db.studentDao().updateStudent(
                            existingStudent.copy(
                                name = name,
                                roomNo = s.roomNo.orEmpty(),
                                phone = s.phone.orEmpty()
                            )
                        )
                    } else {
                        newStudents.add(
                            StudentEntity(
                                name = name,
                                rollNo = roll,
                                roomNo = s.roomNo.orEmpty(),
                                phone = s.phone.orEmpty()
                            )
                        )
                    }
                    studentCount++
                }

                if (newStudents.isNotEmpty()) {
                    db.studentDao().insertStudents(newStudents)
                }

                // Refresh student lookup
                val allStudents = db.studentDao().getAllStudentsSnapshot().associateBy { it.rollNo.lowercase() }
                var attendanceCount = 0
                val attendanceToInsert = mutableListOf<AttendanceEntity>()

                for (att in attendanceFromSheet) {
                    val student = allStudents[att.studentRollNo.lowercase()]
                    if (student != null && att.date.isNotBlank() && att.mealType.isNotBlank()) {
                        attendanceToInsert.add(
                            AttendanceEntity(
                                studentId = student.id,
                                date = att.date,
                                mealType = att.mealType,
                                isPresent = att.status.equals("PRESENT", ignoreCase = true),
                                recordedAt = att.recordedAt ?: System.currentTimeMillis()
                            )
                        )
                        attendanceCount++
                    }
                }

                if (attendanceToInsert.isNotEmpty()) {
                    db.attendanceDao().insertOrUpdateAll(attendanceToInsert)
                }

                val result = SyncFetchResult(
                    studentCount = studentCount,
                    attendanceCount = attendanceCount,
                    message = "Fetched $studentCount students and $attendanceCount attendance logs from Google Sheet!"
                )
                SyncPreferences.updateSyncSuccess(context, studentCount + attendanceCount)
                return@withContext Result.success(result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from Google Sheet", e)
            val msg = e.localizedMessage ?: "Failed to fetch data"
            SyncPreferences.updateSyncFailure(context, msg)
            Result.failure(e)
        }
    }

    private fun parseStudentsFromCsv(csvText: String): List<StudentEntity> {
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size <= 1) return emptyList()

        val headerTokens = parseCsvRow(lines[0]).map { it.trim().lowercase() }
        var rollIdx = headerTokens.indexOfFirst { it.contains("roll") || it.contains("id") }
        var nameIdx = headerTokens.indexOfFirst { it.contains("name") || it.contains("student") }
        var roomIdx = headerTokens.indexOfFirst { it.contains("room") || it.contains("hostel") }
        var phoneIdx = headerTokens.indexOfFirst { it.contains("phone") || it.contains("mobile") || it.contains("contact") }

        if (rollIdx == -1) rollIdx = 0
        if (nameIdx == -1) nameIdx = if (headerTokens.size > 1) 1 else 0
        if (roomIdx == -1) roomIdx = if (headerTokens.size > 2) 2 else -1
        if (phoneIdx == -1) phoneIdx = if (headerTokens.size > 3) 3 else -1

        val students = mutableListOf<StudentEntity>()
        for (i in 1 until lines.size) {
            val cols = parseCsvRow(lines[i])
            if (cols.isEmpty()) continue

            val roll = if (rollIdx < cols.size) cols[rollIdx].trim() else ""
            val name = if (nameIdx < cols.size) cols[nameIdx].trim() else ""
            val room = if (roomIdx in cols.indices) cols[roomIdx].trim() else ""
            val phone = if (phoneIdx in cols.indices) cols[phoneIdx].trim() else ""

            if (name.isNotBlank() || roll.isNotBlank()) {
                students.add(
                    StudentEntity(
                        name = name.ifBlank { "Student $roll" },
                        rollNo = roll.ifBlank { "10$i" },
                        roomNo = room,
                        phone = phone
                    )
                )
            }
        }
        return students
    }

    private fun parseCsvRow(row: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (ch in row) {
            when {
                ch == '\"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(sb.toString().trim())
                    sb.clear()
                }
                else -> sb.append(ch)
            }
        }
        result.add(sb.toString().trim())
        return result
    }
}
