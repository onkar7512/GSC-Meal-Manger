package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance",
    indices = [
        Index(value = ["studentId", "date", "mealType"], unique = true),
        Index(value = ["date", "mealType"]),
        Index(value = ["date"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val date: String, // format: "yyyy-MM-dd"
    val mealType: String, // "BREAKFAST", "LUNCH", "DINNER", "YOGA", "MEDITATION"
    val isPresent: Boolean,
    val recordedAt: Long = System.currentTimeMillis(),
    val verificationMethod: String = "MANUAL", // "FINGERPRINT" or "MANUAL"
    val fingerprintId: String = ""
) {
    val timeStr: String
        get() {
            return try {
                val instant = java.time.Instant.ofEpochMilli(recordedAt)
                val zdt = java.time.ZonedDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
                zdt.format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.US))
            } catch (e: Exception) {
                ""
            }
        }
}
