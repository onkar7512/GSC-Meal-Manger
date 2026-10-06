package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AttendanceCategory
import com.example.data.local.AttendanceEntity
import com.example.data.local.MealDatabase
import com.example.data.local.MealType
import com.example.data.local.StudentEntity
import com.example.data.repository.MealAttendanceRepository
import com.example.pdf.DateWiseReportData
import com.example.pdf.MonthlyReportData
import com.example.pdf.PdfReportGenerator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.sync.GoogleSheetSyncManager
import com.example.sync.GoogleSheetSyncWorker
import com.example.sync.NetworkConnectivityObserver
import com.example.sync.SyncPreferences
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

enum class AppTab(val title: String, val testTag: String) {
    STUDENTS("Students", "tab_students"),
    ATTENDANCE("Attendance", "tab_attendance"),
    DASHBOARD("Dashboard", "tab_dashboard")
}

enum class DashboardFilter(val displayName: String, val emoji: String) {
    ALL("All", "📊"),
    MEAL("Meals", "🍽️"),
    YOGA("Yoga", "🧘"),
    MEDITATION("Meditation", "🕯️")
}

data class MealAttendanceSummary(
    val mealType: MealType,
    val presentCount: Int,
    val absentCount: Int,
    val unrecordedCount: Int,
    val totalStudents: Int,
    val attendancePercentage: Float
)

data class DayAttendanceStats(
    val date: String,
    val totalStudents: Int,
    val breakfast: MealAttendanceSummary,
    val lunch: MealAttendanceSummary,
    val dinner: MealAttendanceSummary,
    val yoga: MealAttendanceSummary,
    val meditation: MealAttendanceSummary,
    val totalMealsServed: Int,
    val totalYogaAttended: Int,
    val totalMeditationAttended: Int
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MealAttendanceRepository

    // --- Google Sheets Synchronization State ---
    val sheetWebhookUrl: StateFlow<String> = SyncPreferences.getWebhookUrl(application)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    val isAutoSyncEnabled: StateFlow<Boolean> = SyncPreferences.isAutoSyncEnabled(application)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val lastSyncTime: StateFlow<Long> = SyncPreferences.getLastSyncTime(application)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    val lastSyncStatus: StateFlow<String> = SyncPreferences.getLastSyncStatus(application)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Not synced yet"
        )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val connectivityObserver = NetworkConnectivityObserver(application)
    val isNetworkAvailable: StateFlow<Boolean> = connectivityObserver.isNetworkAvailable

    init {
        val database = MealDatabase.getDatabase(application)
        repository = MealAttendanceRepository(database.studentDao(), database.attendanceDao())

        // Clean start with no default/demo data seeded automatically

        // Schedule periodic sync via WorkManager
        GoogleSheetSyncWorker.schedulePeriodicSync(application)

        // Listen for network connectivity to automatically trigger upload when internet becomes available
        connectivityObserver.startListening {
            viewModelScope.launch {
                val autoSync = isAutoSyncEnabled.value
                val url = SyncPreferences.getWebhookUrlOnce(getApplication())
                if (autoSync && url.isNotBlank()) {
                    triggerSyncInternal()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        connectivityObserver.stopListening()
    }

    fun saveSheetWebhookUrl(url: String, autoFetchIfEmpty: Boolean = false, onFetchResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            val trimmedUrl = url.trim()
            SyncPreferences.setWebhookUrl(getApplication(), trimmedUrl)
            if (trimmedUrl.isNotBlank()) {
                // If local students are empty, pull data automatically
                val studentCount = repository.getStudentCountNow()
                if (studentCount == 0 || autoFetchIfEmpty) {
                    fetchDataFromGoogleSheet(trimmedUrl) { success, msg ->
                        onFetchResult?.invoke(success, msg)
                    }
                } else if (isAutoSyncEnabled.value && isNetworkAvailable.value) {
                    triggerSyncInternal()
                }
            }
        }
    }

    fun toggleAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            SyncPreferences.setAutoSyncEnabled(getApplication(), enabled)
            if (enabled) {
                GoogleSheetSyncWorker.schedulePeriodicSync(getApplication())
            } else {
                GoogleSheetSyncWorker.cancelSync(getApplication())
            }
        }
    }

    private fun triggerAutoSyncIfEnabled() {
        viewModelScope.launch {
            if (isAutoSyncEnabled.value) {
                if (isNetworkAvailable.value) {
                    triggerSyncInternal()
                } else {
                    // When offline, enqueue WorkManager task to automatically run when network is restored
                    GoogleSheetSyncWorker.enqueueAutoSyncWhenOnline(getApplication())
                }
            }
        }
    }

    fun syncNowWithGoogleSheet(customUrl: String? = null, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                // Automated two-way sync: First pulls/gets students from Google Sheet, then pushes/updates attendance
                val result = GoogleSheetSyncManager.autoSyncTwoWay(getApplication(), customUrl)
                if (result.isSuccess) {
                    val res = result.getOrNull()
                    val msg = res?.message ?: "Sync completed successfully!"
                    onResult(true, msg)
                } else {
                    val msg = result.exceptionOrNull()?.message ?: "Sync failed"
                    onResult(false, msg)
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun autoSyncWithGoogleSheet(customUrl: String? = null, onResult: (Boolean, String) -> Unit) {
        syncNowWithGoogleSheet(customUrl, onResult)
    }

    fun fetchDataFromGoogleSheet(customUrl: String? = null, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val result = GoogleSheetSyncManager.fetchDataFromGoogleSheet(getApplication(), customUrl)
                if (result.isSuccess) {
                    val data = result.getOrNull()
                    val msg = data?.message ?: "Data successfully fetched from Google Sheet!"
                    onResult(true, msg)
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Failed to fetch from Google Sheet"
                    onResult(false, err)
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private suspend fun triggerSyncInternal() {
        if (_isSyncing.value) return
        _isSyncing.value = true
        try {
            // First get data from Google Sheet, and after that update Google Sheet
            GoogleSheetSyncManager.autoSyncTwoWay(getApplication())
        } finally {
            _isSyncing.value = false
        }
    }

    // Active Tab Navigation
    private val _currentTab = MutableStateFlow(AppTab.ATTENDANCE)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    // --- Students Tab State ---
    val allStudents: StateFlow<List<StudentEntity>> = repository.allStudents
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _studentSearchQuery = MutableStateFlow("")
    val studentSearchQuery: StateFlow<String> = _studentSearchQuery.asStateFlow()

    fun updateStudentSearchQuery(query: String) {
        _studentSearchQuery.value = query
    }

    val filteredStudents: StateFlow<List<StudentEntity>> = combine(
        allStudents,
        _studentSearchQuery
    ) { students, query ->
        if (query.isBlank()) {
            students
        } else {
            val trimmed = query.trim().lowercase()
            students.filter {
                it.name.lowercase().contains(trimmed) ||
                        it.rollNo.lowercase().contains(trimmed) ||
                        it.roomNo.lowercase().contains(trimmed)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Automatically calculates the next available roll number based on existing students.
     * Extracts integer sequences or defaults to 101.
     */
    fun getNextAutoRollNumber(): String {
        val students = allStudents.value
        if (students.isEmpty()) {
            return "101"
        }

        var maxNumber = 0
        var commonPrefix = ""
        var maxDigits = 0

        for (s in students) {
            val raw = s.rollNo.trim()
            val numMatch = Regex("""\d+""").findAll(raw).lastOrNull()
            if (numMatch != null) {
                val numVal = numMatch.value.toIntOrNull() ?: 0
                if (numVal > maxNumber) {
                    maxNumber = numVal
                    maxDigits = numMatch.value.length
                    commonPrefix = raw.substring(0, numMatch.range.first)
                }
            }
        }

        if (maxNumber > 0) {
            val nextVal = maxNumber + 1
            return if (commonPrefix.isNotEmpty()) {
                val formattedNum = String.format(java.util.Locale.US, "%0${maxDigits}d", nextVal)
                "$commonPrefix$formattedNum"
            } else {
                nextVal.toString()
            }
        }

        return "101"
    }

    fun addStudent(name: String, rollNo: String, roomNo: String, phone: String) {
        viewModelScope.launch {
            val finalRoll = if (rollNo.isBlank()) getNextAutoRollNumber() else rollNo.trim()
            repository.addStudent(name.trim(), finalRoll, roomNo.trim(), phone.trim())
            triggerAutoSyncIfEnabled()
        }
    }

    fun updateStudent(student: StudentEntity) {
        viewModelScope.launch {
            repository.updateStudent(student)
            triggerAutoSyncIfEnabled()
        }
    }

    fun deleteStudent(student: StudentEntity) {
        viewModelScope.launch {
            // Track deleted roll number so subsequent fetches do NOT re-import this student
            SyncPreferences.addDeletedRollNumber(getApplication(), student.rollNo)
            repository.deleteStudent(student)
            // Push updated roster directly to Google Sheet so student is removed from sheet as well
            if (isNetworkAvailable.value && sheetWebhookUrl.value.isNotBlank()) {
                GoogleSheetSyncManager.syncLocalDatabaseToGoogleSheet(getApplication())
            }
        }
    }

    fun clearAllData(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            SyncPreferences.clearDeletedRollNumbers(getApplication())
            repository.clearAllData()
            if (isNetworkAvailable.value && sheetWebhookUrl.value.isNotBlank()) {
                GoogleSheetSyncManager.syncLocalDatabaseToGoogleSheet(getApplication())
            }
            onComplete?.invoke()
        }
    }

    // --- Attendance Tab State ---
    private val _selectedAttendanceDate = MutableStateFlow(LocalDate.now())
    val selectedAttendanceDate: StateFlow<LocalDate> = _selectedAttendanceDate.asStateFlow()

    private val _selectedAttendanceCategory = MutableStateFlow(AttendanceCategory.MEAL)
    val selectedAttendanceCategory: StateFlow<AttendanceCategory> = _selectedAttendanceCategory.asStateFlow()

    private val _selectedMeal = MutableStateFlow(MealType.BREAKFAST)
    val selectedMeal: StateFlow<MealType> = _selectedMeal.asStateFlow()

    fun setAttendanceDate(date: LocalDate) {
        _selectedAttendanceDate.value = date
    }

    fun setSelectedAttendanceCategory(category: AttendanceCategory) {
        _selectedAttendanceCategory.value = category
        when (category) {
            AttendanceCategory.MEAL -> {
                if (!_selectedMeal.value.isMeal) {
                    _selectedMeal.value = MealType.BREAKFAST
                }
            }
            AttendanceCategory.YOGA -> {
                _selectedMeal.value = MealType.YOGA
            }
            AttendanceCategory.MEDITATION -> {
                _selectedMeal.value = MealType.MEDITATION
            }
        }
    }

    fun setSelectedMeal(meal: MealType) {
        _selectedMeal.value = meal
        _selectedAttendanceCategory.value = meal.category
    }

    // Attendance entries for selected date
    val attendanceForSelectedDate: StateFlow<List<AttendanceEntity>> = _selectedAttendanceDate
        .flatMapLatest { date ->
            repository.getAttendanceForDate(date.toString())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleStudentAttendance(studentId: Long, isPresent: Boolean) {
        viewModelScope.launch {
            val dateStr = _selectedAttendanceDate.value.toString()
            val meal = _selectedMeal.value
            repository.setAttendance(studentId, dateStr, meal, isPresent)
            triggerAutoSyncIfEnabled()
        }
    }

    fun markAllForSelectedMeal(isPresent: Boolean) {
        viewModelScope.launch {
            val dateStr = _selectedAttendanceDate.value.toString()
            val meal = _selectedMeal.value
            val students = allStudents.value
            if (students.isNotEmpty()) {
                repository.markAllForMeal(students, dateStr, meal, isPresent)
                triggerAutoSyncIfEnabled()
            }
        }
    }

    fun clearSelectedMealAttendance() {
        viewModelScope.launch {
            val dateStr = _selectedAttendanceDate.value.toString()
            val meal = _selectedMeal.value
            repository.clearMealAttendance(dateStr, meal)
            triggerAutoSyncIfEnabled()
        }
    }

    // --- Dashboard State ---
    private val _dashboardDate = MutableStateFlow(LocalDate.now())
    val dashboardDate: StateFlow<LocalDate> = _dashboardDate.asStateFlow()

    private val _dashboardFilter = MutableStateFlow(DashboardFilter.ALL)
    val dashboardFilter: StateFlow<DashboardFilter> = _dashboardFilter.asStateFlow()

    // Dedicated activity selected for the Dashboard student list (Meal, Yoga, or Meditation)
    private val _dashboardSelectedActivity = MutableStateFlow(MealType.BREAKFAST)
    val dashboardSelectedActivity: StateFlow<MealType> = _dashboardSelectedActivity.asStateFlow()

    fun setDashboardDate(date: LocalDate) {
        _dashboardDate.value = date
    }

    fun setDashboardFilter(filter: DashboardFilter) {
        _dashboardFilter.value = filter
        // If filter changes to Yoga or Meditation, automatically sync the dashboard selected activity
        when (filter) {
            DashboardFilter.YOGA -> _dashboardSelectedActivity.value = MealType.YOGA
            DashboardFilter.MEDITATION -> _dashboardSelectedActivity.value = MealType.MEDITATION
            DashboardFilter.MEAL -> if (!_dashboardSelectedActivity.value.isMeal) {
                _dashboardSelectedActivity.value = MealType.BREAKFAST
            }
            DashboardFilter.ALL -> Unit
        }
    }

    fun setDashboardSelectedActivity(activity: MealType) {
        _dashboardSelectedActivity.value = activity
    }

    fun toggleAttendanceForDateAndActivity(studentId: Long, date: LocalDate, meal: MealType, isPresent: Boolean) {
        viewModelScope.launch {
            repository.setAttendance(studentId, date.toString(), meal, isPresent)
            triggerAutoSyncIfEnabled()
        }
    }

    fun markAllForDateAndActivity(date: LocalDate, meal: MealType, isPresent: Boolean) {
        viewModelScope.launch {
            val students = allStudents.value
            if (students.isNotEmpty()) {
                repository.markAllForMeal(students, date.toString(), meal, isPresent)
                triggerAutoSyncIfEnabled()
            }
        }
    }

    val dashboardAttendance: StateFlow<List<AttendanceEntity>> = _dashboardDate
        .flatMapLatest { date ->
            repository.getAttendanceForDate(date.toString())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val dashboardStats: StateFlow<DayAttendanceStats> = combine(
        allStudents,
        dashboardAttendance,
        _dashboardDate
    ) { students, records, date ->
        val total = students.size

        fun buildSummary(mealType: MealType): MealAttendanceSummary {
            val mealRecords = records.filter { it.mealType == mealType.name }
            val presentCount = mealRecords.count { it.isPresent }
            // Automatic Absent rule: any student not marked present is automatically ABSENT
            val absentCount = (total - presentCount).coerceAtLeast(0)
            val unrecordedCount = 0
            val pct = if (total > 0) (presentCount.toFloat() / total.toFloat()) * 100f else 0f

            return MealAttendanceSummary(
                mealType = mealType,
                presentCount = presentCount,
                absentCount = absentCount,
                unrecordedCount = unrecordedCount,
                totalStudents = total,
                attendancePercentage = pct
            )
        }

        val bf = buildSummary(MealType.BREAKFAST)
        val lunch = buildSummary(MealType.LUNCH)
        val dinner = buildSummary(MealType.DINNER)
        val yoga = buildSummary(MealType.YOGA)
        val meditation = buildSummary(MealType.MEDITATION)

        DayAttendanceStats(
            date = date.toString(),
            totalStudents = total,
            breakfast = bf,
            lunch = lunch,
            dinner = dinner,
            yoga = yoga,
            meditation = meditation,
            totalMealsServed = bf.presentCount + lunch.presentCount + dinner.presentCount,
            totalYogaAttended = yoga.presentCount,
            totalMeditationAttended = meditation.presentCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DayAttendanceStats(
            date = LocalDate.now().toString(),
            totalStudents = 0,
            breakfast = MealAttendanceSummary(MealType.BREAKFAST, 0, 0, 0, 0, 0f),
            lunch = MealAttendanceSummary(MealType.LUNCH, 0, 0, 0, 0, 0f),
            dinner = MealAttendanceSummary(MealType.DINNER, 0, 0, 0, 0, 0f),
            yoga = MealAttendanceSummary(MealType.YOGA, 0, 0, 0, 0, 0f),
            meditation = MealAttendanceSummary(MealType.MEDITATION, 0, 0, 0, 0, 0f),
            totalMealsServed = 0,
            totalYogaAttended = 0,
            totalMeditationAttended = 0
        )
    )

    // --- Export PDF Feature (Monthly & Date-Wise) ---
    private val _exportYearMonth = MutableStateFlow(YearMonth.now())
    val exportYearMonth: StateFlow<YearMonth> = _exportYearMonth.asStateFlow()

    private val _exportDate = MutableStateFlow(LocalDate.now())
    val exportDate: StateFlow<LocalDate> = _exportDate.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportedFile = MutableStateFlow<File?>(null)
    val exportedFile: StateFlow<File?> = _exportedFile.asStateFlow()

    private val _exportReportData = MutableStateFlow<MonthlyReportData?>(null)
    val exportReportData: StateFlow<MonthlyReportData?> = _exportReportData.asStateFlow()

    private val _exportDateWiseReportData = MutableStateFlow<DateWiseReportData?>(null)
    val exportDateWiseReportData: StateFlow<DateWiseReportData?> = _exportDateWiseReportData.asStateFlow()

    fun setExportYearMonth(yearMonth: YearMonth) {
        _exportYearMonth.value = yearMonth
        loadMonthlyPreview(yearMonth)
    }

    fun setExportDate(date: LocalDate) {
        _exportDate.value = date
        loadDateWisePreview(date)
    }

    fun loadMonthlyPreview(yearMonth: YearMonth) {
        viewModelScope.launch {
            val prefix = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            val records = repository.getAttendanceForMonthSnapshot(prefix)
            val students = allStudents.value
            val data = PdfReportGenerator.computeMonthlyReportData(prefix, students, records)
            _exportReportData.value = data
        }
    }

    fun loadDateWisePreview(date: LocalDate) {
        viewModelScope.launch {
            val dateStr = date.toString()
            val records = repository.getAttendanceForDateSnapshot(dateStr)
            val students = allStudents.value
            val data = PdfReportGenerator.computeDateWiseReportData(dateStr, students, records)
            _exportDateWiseReportData.value = data
        }
    }

    fun generatePdfReport(onComplete: (File) -> Unit) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val yearMonth = _exportYearMonth.value
                val prefix = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                val records = repository.getAttendanceForMonthSnapshot(prefix)
                val students = allStudents.value
                val data = PdfReportGenerator.computeMonthlyReportData(prefix, students, records)
                _exportReportData.value = data

                val file = PdfReportGenerator.generateMonthlyPdf(getApplication(), data)
                _exportedFile.value = file
                onComplete(file)
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun generateDateWisePdfReport(onComplete: (File) -> Unit) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val date = _exportDate.value
                val dateStr = date.toString()
                val records = repository.getAttendanceForDateSnapshot(dateStr)
                val students = allStudents.value
                val data = PdfReportGenerator.computeDateWiseReportData(dateStr, students, records)
                _exportDateWiseReportData.value = data

                val file = PdfReportGenerator.generateDateWisePdf(getApplication(), data)
                _exportedFile.value = file
                onComplete(file)
            } finally {
                _isExporting.value = false
            }
        }
    }
}
