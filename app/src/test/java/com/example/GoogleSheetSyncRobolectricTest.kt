package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MealDatabase
import com.example.data.local.MealType
import com.example.data.local.StudentEntity
import com.example.data.repository.MealAttendanceRepository
import com.example.sync.GoogleSheetSyncRequest
import com.example.sync.SheetAttendancePayload
import com.example.sync.SheetStudentPayload
import com.example.sync.SyncPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GoogleSheetSyncRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: MealDatabase
    private lateinit var repository: MealAttendanceRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = MealDatabase.getDatabase(context)
        repository = MealAttendanceRepository(database.studentDao(), database.attendanceDao())
    }

    @Test
    fun testPreferencesSaveAndRetrieve() = runBlocking {
        val testUrl = "https://script.google.com/macros/s/AKfycbxTest/exec"
        SyncPreferences.setWebhookUrl(context, testUrl)

        val retrievedUrl = SyncPreferences.getWebhookUrlOnce(context)
        assertEquals(testUrl, retrievedUrl)

        SyncPreferences.setAutoSyncEnabled(context, true)
        val isAutoSync = SyncPreferences.isAutoSyncEnabled(context).first()
        assertTrue(isAutoSync)

        SyncPreferences.updateSyncSuccess(context, 12)
        val status = SyncPreferences.getLastSyncStatus(context).first()
        assertTrue(status.contains("Successfully synced 12 records"))
    }

    @Test
    fun testPayloadSerialization() {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val adapter = moshi.adapter(GoogleSheetSyncRequest::class.java)

        val sampleRequest = GoogleSheetSyncRequest(
            action = "SYNC_ATTENDANCE",
            timestamp = 1700000000000L,
            students = listOf(
                SheetStudentPayload(
                    id = 1L,
                    rollNo = "101",
                    name = "Aarav Sharma",
                    roomNo = "A-201",
                    phone = "9876543210"
                )
            ),
            attendance = listOf(
                SheetAttendancePayload(
                    studentRollNo = "101",
                    studentName = "Aarav Sharma",
                    roomNo = "A-201",
                    date = "2026-09-21",
                    mealType = "BREAKFAST",
                    status = "PRESENT",
                    recordedAt = 1700000000000L
                )
            )
        )

        val json = adapter.toJson(sampleRequest)
        assertNotNull(json)
        assertTrue(json.contains("SYNC_ATTENDANCE"))
        assertTrue(json.contains("Aarav Sharma"))
        assertTrue(json.contains("PRESENT"))
    }

    @Test
    fun testRoomDatabaseSnapshotQueries() = runBlocking {
        val studentId = repository.addStudent("Test Sync Student", "S-999", "Room-1", "9000000000")
        repository.setAttendance(studentId, "2026-09-21", MealType.LUNCH, true)

        val students = database.studentDao().getAllStudentsSnapshot()
        val attendance = database.attendanceDao().getAllAttendanceSnapshot()

        assertTrue(students.any { it.rollNo == "S-999" })
        assertTrue(attendance.any { it.studentId == studentId && it.isPresent })
    }

    @Test
    fun testDeletedRollNumbersPreference() = runBlocking {
        SyncPreferences.addDeletedRollNumber(context, "S-999")
        val deleted = SyncPreferences.getDeletedRollNumbers(context)
        assertTrue(deleted.contains("s-999"))

        SyncPreferences.clearDeletedRollNumbers(context)
        val cleared = SyncPreferences.getDeletedRollNumbers(context)
        assertTrue(cleared.isEmpty())
    }
}
