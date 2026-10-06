package com.example.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SheetStudentPayload(
    @Json(name = "id") val id: Long,
    @Json(name = "rollNo") val rollNo: String,
    @Json(name = "name") val name: String,
    @Json(name = "roomNo") val roomNo: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "fingerprintEnrolled") val fingerprintEnrolled: Boolean = false,
    @Json(name = "fingerprintId") val fingerprintId: String = "",
    @Json(name = "fingerprintEnrolledAt") val fingerprintEnrolledAt: String = ""
)

@JsonClass(generateAdapter = true)
data class SheetAttendancePayload(
    @Json(name = "studentRollNo") val studentRollNo: String,
    @Json(name = "studentName") val studentName: String,
    @Json(name = "roomNo") val roomNo: String,
    @Json(name = "date") val date: String,
    @Json(name = "mealType") val mealType: String,
    @Json(name = "status") val status: String, // "PRESENT" or "ABSENT"
    @Json(name = "verificationMethod") val verificationMethod: String = "MANUAL", // "FINGERPRINT" or "MANUAL"
    @Json(name = "fingerprintId") val fingerprintId: String = "",
    @Json(name = "recordedAt") val recordedAt: Long
)

@JsonClass(generateAdapter = true)
data class GoogleSheetSyncRequest(
    @Json(name = "action") val action: String = "SYNC_ATTENDANCE",
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
    @Json(name = "students") val students: List<SheetStudentPayload>,
    @Json(name = "attendance") val attendance: List<SheetAttendancePayload>
)

@JsonClass(generateAdapter = true)
data class GoogleSheetSyncResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "updatedRows") val updatedRows: Int? = null,
    @Json(name = "studentCount") val studentCount: Int? = null
)

@JsonClass(generateAdapter = true)
data class SheetFetchStudentItem(
    @Json(name = "rollNo") val rollNo: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "roomNo") val roomNo: String? = "",
    @Json(name = "phone") val phone: String? = "",
    @Json(name = "fingerprintEnrolled") val fingerprintEnrolled: Boolean? = false,
    @Json(name = "fingerprintId") val fingerprintId: String? = "",
    @Json(name = "fingerprintEnrolledAt") val fingerprintEnrolledAt: String? = ""
)

@JsonClass(generateAdapter = true)
data class SheetFetchAttendanceItem(
    @Json(name = "date") val date: String = "",
    @Json(name = "mealType") val mealType: String = "",
    @Json(name = "studentRollNo") val studentRollNo: String = "",
    @Json(name = "studentName") val studentName: String? = "",
    @Json(name = "roomNo") val roomNo: String? = "",
    @Json(name = "status") val status: String = "PRESENT",
    @Json(name = "verificationMethod") val verificationMethod: String? = "MANUAL",
    @Json(name = "fingerprintId") val fingerprintId: String? = "",
    @Json(name = "recordedAt") val recordedAt: Long? = null
)

@JsonClass(generateAdapter = true)
data class GoogleSheetFetchResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "students") val students: List<SheetFetchStudentItem>? = null,
    @Json(name = "attendance") val attendance: List<SheetFetchAttendanceItem>? = null
)
