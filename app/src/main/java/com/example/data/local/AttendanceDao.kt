package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE date = :date")
    fun getAttendanceForDate(date: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE date = :date")
    suspend fun getAttendanceForDateSnapshot(date: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE date = :date AND mealType = :mealType")
    fun getAttendanceForDateAndMeal(date: String, mealType: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE date LIKE :monthPrefix || '%'")
    fun getAttendanceForMonth(monthPrefix: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE date LIKE :monthPrefix || '%'")
    suspend fun getAttendanceForMonthSnapshot(monthPrefix: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance ORDER BY recordedAt DESC")
    suspend fun getAllAttendanceSnapshot(): List<AttendanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(attendance: AttendanceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(attendances: List<AttendanceEntity>)

    @Query("DELETE FROM attendance WHERE studentId = :studentId AND date = :date AND mealType = :mealType")
    suspend fun deleteSpecificAttendance(studentId: Long, date: String, mealType: String)

    @Query("DELETE FROM attendance WHERE date = :date AND mealType = :mealType")
    suspend fun clearMealAttendance(date: String, mealType: String)

    @Query("DELETE FROM attendance")
    suspend fun clearAllAttendance()
}
