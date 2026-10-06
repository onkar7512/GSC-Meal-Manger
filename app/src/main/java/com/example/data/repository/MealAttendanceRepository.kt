package com.example.data.repository

import com.example.data.local.AttendanceDao
import com.example.data.local.AttendanceEntity
import com.example.data.local.MealType
import com.example.data.local.StudentDao
import com.example.data.local.StudentEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class MealAttendanceRepository(
    private val studentDao: StudentDao,
    private val attendanceDao: AttendanceDao
) {
    val allStudents: Flow<List<StudentEntity>> = studentDao.getAllStudents()
    val studentCount: Flow<Int> = studentDao.getStudentCount()

    suspend fun getStudentCountNow(): Int = studentDao.getStudentCountNow()

    fun getAttendanceForDate(date: String): Flow<List<AttendanceEntity>> {
        return attendanceDao.getAttendanceForDate(date)
    }

    fun getAttendanceForDateAndMeal(date: String, mealType: MealType): Flow<List<AttendanceEntity>> {
        return attendanceDao.getAttendanceForDateAndMeal(date, mealType.name)
    }

    fun getAttendanceForMonth(monthPrefix: String): Flow<List<AttendanceEntity>> {
        return attendanceDao.getAttendanceForMonth(monthPrefix)
    }

    suspend fun getAttendanceForMonthSnapshot(monthPrefix: String): List<AttendanceEntity> {
        return attendanceDao.getAttendanceForMonthSnapshot(monthPrefix)
    }

    suspend fun getAttendanceForDateSnapshot(date: String): List<AttendanceEntity> {
        return attendanceDao.getAttendanceForDateSnapshot(date)
    }

    suspend fun addStudent(name: String, rollNo: String, roomNo: String, phone: String): Long {
        val student = StudentEntity(
            name = name.trim(),
            rollNo = rollNo.trim(),
            roomNo = roomNo.trim(),
            phone = phone.trim()
        )
        return studentDao.insertStudent(student)
    }

    suspend fun updateStudent(student: StudentEntity) {
        studentDao.updateStudent(student)
    }

    suspend fun deleteStudent(student: StudentEntity) {
        studentDao.deleteStudent(student)
    }

    suspend fun setAttendance(
        studentId: Long,
        date: String,
        mealType: MealType,
        isPresent: Boolean
    ) {
        val record = AttendanceEntity(
            studentId = studentId,
            date = date,
            mealType = mealType.name,
            isPresent = isPresent,
            recordedAt = System.currentTimeMillis()
        )
        attendanceDao.insertOrUpdate(record)
    }

    suspend fun markAllForMeal(
        students: List<StudentEntity>,
        date: String,
        mealType: MealType,
        isPresent: Boolean
    ) {
        val records = students.map { student ->
            AttendanceEntity(
                studentId = student.id,
                date = date,
                mealType = mealType.name,
                isPresent = isPresent,
                recordedAt = System.currentTimeMillis()
            )
        }
        attendanceDao.insertOrUpdateAll(records)
    }

    suspend fun clearMealAttendance(date: String, mealType: MealType) {
        attendanceDao.clearMealAttendance(date, mealType.name)
    }

    suspend fun clearAllData() {
        attendanceDao.clearAllAttendance()
        studentDao.deleteAllStudents()
    }
}
