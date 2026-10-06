package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rollNo: String,
    val name: String,
    val roomNo: String = "",
    val phone: String = "",
    val fingerprintEnrolled: Boolean = false,
    val fingerprintId: String = "",
    val fingerprintEnrolledAt: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
