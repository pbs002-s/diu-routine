package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ClassSchedule
import com.example.data.ExamSchedule
import com.example.data.RoutineRepository
import com.example.data.RoutineTextParser
import com.example.data.StudyLog
import com.example.notification.RoutineAlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import com.example.data.GeminiClient
import com.example.data.GenerateContentRequest
import com.example.data.Content as GeminiContent
import com.example.data.Part
import com.example.data.InlineData
import com.example.data.GenerationConfig

class RoutineViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RoutineRepository
    private val sharedPrefs = application.getSharedPreferences("diu_routine_settings", Context.MODE_PRIVATE)

    // Exposed Flows
    val allClasses: StateFlow<List<ClassSchedule>>
    val allStudyLogs: StateFlow<List<StudyLog>>
    val allExams: StateFlow<List<ExamSchedule>>

    private val _isDarkMode = MutableStateFlow(sharedPrefs.getBoolean("dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _selectedDept = MutableStateFlow(sharedPrefs.getString("department", "CSE") ?: "CSE")
    val selectedDept: StateFlow<String> = _selectedDept.asStateFlow()

    private val _selectedSection = MutableStateFlow(sharedPrefs.getString("section", "A") ?: "A")
    val selectedSection: StateFlow<String> = _selectedSection.asStateFlow()

    private val _selectedDay = MutableStateFlow(getCurrentDayOfWeekString())
    val selectedDay: StateFlow<String> = _selectedDay.asStateFlow()

    // Standalone Custom Departments & Sections Lists
    private val _customDepartments = MutableStateFlow<List<String>>(
        sharedPrefs.getStringSet("custom_departments", emptySet())?.toList() ?: emptyList()
    )
    val customDepartments: StateFlow<List<String>> = _customDepartments.asStateFlow()

    private val _customSections = MutableStateFlow<List<String>>(
        sharedPrefs.getStringSet("custom_sections", emptySet())?.toList() ?: emptyList()
    )
    val customSections: StateFlow<List<String>> = _customSections.asStateFlow()

    // Global Alarm Offset setting (minutes before class starts)
    private val _alarmOffsetMinutes = MutableStateFlow(sharedPrefs.getInt("alarm_offset_minutes", 20))
    val alarmOffsetMinutes: StateFlow<Int> = _alarmOffsetMinutes.asStateFlow()

    // Gemini API Key Management
    private val _geminiApiKey = MutableStateFlow(sharedPrefs.getString("gemini_api_key", "") ?: "")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    fun setGeminiApiKey(key: String) {
        _geminiApiKey.value = key.trim()
        sharedPrefs.edit().putString("gemini_api_key", key.trim()).apply()
    }

    fun getEffectiveApiKey(): String {
        val customKey = _geminiApiKey.value.trim()
        if (customKey.isNotEmpty()) {
            return customKey
        }
        val buildKey = com.example.BuildConfig.GEMINI_API_KEY.trim()
        if (buildKey.isNotEmpty() && !buildKey.contains("your_gemini_api_key", ignoreCase = true) && !buildKey.contains("DEFAULT_API_KEY", ignoreCase = true)) {
            return buildKey
        }
        return ""
    }

    fun testGeminiConnection(onResult: (Boolean, String) -> Unit) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isEmpty()) {
            onResult(false, "Gemini API key is not configured. Please enter your API key.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = GenerateContentRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(Part(text = "Respond with 'OK' only."))
                        )
                    ),
                    generationConfig = GenerationConfig(
                        temperature = 0.0
                    )
                )
                val response = GeminiClient.api.generateContent(
                    model = "gemini-2.5-flash",
                    apiKey = apiKey,
                    request = request
                )
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) {
                    onResult(true, "Successfully connected to Gemini API!")
                } else {
                    onResult(false, "Gemini responded, but output was empty.")
                }
            } catch (e: Exception) {
                onResult(false, "Connection error: ${e.localizedMessage ?: "Please check your API key and internet connection."}")
            }
        }
    }

    // Scanner States for Class Routine
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanResult = MutableStateFlow<List<ClassSchedule>?>(null)
    val scanResult: StateFlow<List<ClassSchedule>?> = _scanResult.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    // Scanner States for Exam Routine
    private val _isScanningExams = MutableStateFlow(false)
    val isScanningExams: StateFlow<Boolean> = _isScanningExams.asStateFlow()

    private val _scanExamResult = MutableStateFlow<List<ExamSchedule>?>(null)
    val scanExamResult: StateFlow<List<ExamSchedule>?> = _scanExamResult.asStateFlow()

    private val _scanExamError = MutableStateFlow<String?>(null)
    val scanExamError: StateFlow<String?> = _scanExamError.asStateFlow()

    // PDF State Variables
    private val _classPdfPages = MutableStateFlow<List<Bitmap>?>(null)
    val classPdfPages: StateFlow<List<Bitmap>?> = _classPdfPages.asStateFlow()

    private val _classPdfCurrentPageIndex = MutableStateFlow(0)
    val classPdfCurrentPageIndex: StateFlow<Int> = _classPdfCurrentPageIndex.asStateFlow()

    private val _examPdfPages = MutableStateFlow<List<Bitmap>?>(null)
    val examPdfPages: StateFlow<List<Bitmap>?> = _examPdfPages.asStateFlow()

    private val _examPdfCurrentPageIndex = MutableStateFlow(0)
    val examPdfCurrentPageIndex: StateFlow<Int> = _examPdfCurrentPageIndex.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = RoutineRepository(database.routineDao())

        allClasses = repository.allClasses
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allStudyLogs = repository.allStudyLogs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allExams = repository.allExams
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // CRITICAL ALARM FLOW REGULATION: 
        // Whenever allClasses or allExams update in Room, Room emits the list with correct unique IDs.
        // Collecting here ensures alarms are scheduled with correct, persisted non-zero IDs, avoiding collisions!
        viewModelScope.launch(Dispatchers.IO) {
            allClasses.collectLatest { classes ->
                RoutineAlarmScheduler.rescheduleAllAlarms(getApplication(), classes)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            allExams.collectLatest { exams ->
                RoutineAlarmScheduler.rescheduleAllExams(getApplication(), exams)
            }
        }
    }

    // Settings & Personalization
    fun toggleDarkMode() {
        val next = !_isDarkMode.value
        _isDarkMode.value = next
        sharedPrefs.edit().putBoolean("dark_mode", next).apply()
    }

    fun setDepartment(dept: String) {
        _selectedDept.value = dept
        sharedPrefs.edit().putString("department", dept).apply()
    }

    fun setSection(section: String) {
        _selectedSection.value = section
        sharedPrefs.edit().putString("section", section).apply()
    }

    fun setSelectedDay(day: String) {
        _selectedDay.value = day
    }

    // Custom Department Operations
    fun addCustomDepartment(dept: String) {
        val trimmed = dept.trim().uppercase()
        if (trimmed.isEmpty()) return
        val current = _customDepartments.value.toMutableList()
        if (!current.contains(trimmed)) {
            current.add(trimmed)
            _customDepartments.value = current
            sharedPrefs.edit().putStringSet("custom_departments", current.toSet()).apply()
        }
        setDepartment(trimmed)
    }

    fun deleteCustomDepartment(dept: String) {
        val current = _customDepartments.value.toMutableList()
        if (current.remove(dept)) {
            _customDepartments.value = current
            sharedPrefs.edit().putStringSet("custom_departments", current.toSet()).apply()
            if (_selectedDept.value == dept) {
                setDepartment("CSE")
            }
        }
    }

    // Custom Section Operations
    fun addCustomSection(sec: String) {
        val trimmed = sec.trim().uppercase()
        if (trimmed.isEmpty()) return
        val current = _customSections.value.toMutableList()
        if (!current.contains(trimmed)) {
            current.add(trimmed)
            _customSections.value = current
            sharedPrefs.edit().putStringSet("custom_sections", current.toSet()).apply()
        }
        setSection(trimmed)
    }

    fun deleteCustomSection(sec: String) {
        val current = _customSections.value.toMutableList()
        if (current.remove(sec)) {
            _customSections.value = current
            sharedPrefs.edit().putStringSet("custom_sections", current.toSet()).apply()
            if (_selectedSection.value == sec) {
                setSection("A")
            }
        }
    }

    // Alarm Offset Settings
    fun setAlarmOffsetMinutes(minutes: Int) {
        _alarmOffsetMinutes.value = minutes
        sharedPrefs.edit().putInt("alarm_offset_minutes", minutes).apply()
        
        // Trigger manual reschedule of alarms in repository
        viewModelScope.launch(Dispatchers.IO) {
            RoutineAlarmScheduler.rescheduleAllAlarms(getApplication(), allClasses.value)
            RoutineAlarmScheduler.rescheduleAllExams(getApplication(), allExams.value)
        }
    }

    // Database Actions: Class Routine
    fun addClass(classSchedule: ClassSchedule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertClass(classSchedule)
        }
    }

    fun updateClass(classSchedule: ClassSchedule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateClass(classSchedule)
        }
    }

    fun deleteClass(classSchedule: ClassSchedule) {
        viewModelScope.launch(Dispatchers.IO) {
            RoutineAlarmScheduler.cancelAlarmForClass(getApplication(), classSchedule)
            repository.deleteClass(classSchedule)
        }
    }

    fun clearAllClasses() {
        viewModelScope.launch(Dispatchers.IO) {
            allClasses.value.forEach {
                RoutineAlarmScheduler.cancelAlarmForClass(getApplication(), it)
            }
            repository.clearAllClasses()
        }
    }

    fun toggleNotificationEnabled(classSchedule: ClassSchedule) {
        val updated = classSchedule.copy(notificationEnabled = !classSchedule.notificationEnabled)
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateClass(updated)
        }
    }

    fun toggleClassCompleted(classSchedule: ClassSchedule) {
        val updated = classSchedule.copy(isCompleted = !classSchedule.isCompleted)
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateClass(updated)
            if (updated.isCompleted) {
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = formatter.format(Date())
                repository.insertStudyLog(
                    StudyLog(
                        date = todayStr,
                        durationMinutes = 90,
                        description = "Attended ${updated.subjectCode} (${updated.subjectName})",
                        type = "CLASS"
                    )
                )
            }
        }
    }

    // Database Actions: Exam Routine
    fun addExam(examSchedule: ExamSchedule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertExam(examSchedule)
        }
    }

    fun updateExam(examSchedule: ExamSchedule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateExam(examSchedule)
        }
    }

    fun deleteExam(examSchedule: ExamSchedule) {
        viewModelScope.launch(Dispatchers.IO) {
            RoutineAlarmScheduler.cancelAlarmForExam(getApplication(), examSchedule)
            repository.deleteExam(examSchedule)
        }
    }

    fun clearAllExams() {
        viewModelScope.launch(Dispatchers.IO) {
            allExams.value.forEach {
                RoutineAlarmScheduler.cancelAlarmForExam(getApplication(), it)
            }
            repository.clearAllExams()
        }
    }

    fun toggleExamNotificationEnabled(examSchedule: ExamSchedule) {
        val updated = examSchedule.copy(notificationEnabled = !examSchedule.notificationEnabled)
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateExam(updated)
        }
    }

    fun toggleExamCompleted(examSchedule: ExamSchedule) {
        val updated = examSchedule.copy(isCompleted = !examSchedule.isCompleted)
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateExam(updated)
            if (updated.isCompleted) {
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = formatter.format(Date())
                repository.insertStudyLog(
                    StudyLog(
                        date = todayStr,
                        durationMinutes = 180, // Exam typically 3 hours (180 mins)
                        description = "Completed Final Exam: ${updated.subjectCode} (${updated.subjectName})",
                        type = "EXAM"
                    )
                )
            }
        }
    }

    // Study Log Actions
    fun logStudySession(durationMinutes: Int, topic: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = formatter.format(Date())
            repository.insertStudyLog(
                StudyLog(
                    date = todayStr,
                    durationMinutes = durationMinutes,
                    description = topic,
                    type = "STUDY"
                )
            )
        }
    }

    fun deleteStudyLog(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteStudyLog(id)
        }
    }

    // Scanning / Clipboard Text: Class Routine
    fun parseAndScanDocx(uri: Uri) {
        _isScanning.value = true
        _scanError.value = null
        _scanResult.value = null
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val extractedText = RoutineTextParser.extractTextFromDocx(getApplication(), uri)
                if (extractedText.startsWith("Error")) {
                    _scanError.value = extractedText
                } else {
                    val parsed = RoutineTextParser.parseRoutineText(extractedText, _selectedDept.value, _selectedSection.value)
                    _scanResult.value = parsed
                }
            } catch (e: Exception) {
                _scanError.value = "Failed to parse file: ${e.localizedMessage}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun parseAndScanText(text: String) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isNotEmpty()) {
            scanClassTextWithGemini(text)
        } else {
            _isScanning.value = true
            _scanError.value = null
            _scanResult.value = null
            viewModelScope.launch(Dispatchers.Default) {
                try {
                    val parsed = RoutineTextParser.parseRoutineText(text, _selectedDept.value, _selectedSection.value)
                    _scanResult.value = parsed
                } catch (e: Exception) {
                    _scanError.value = "Failed parsing text: ${e.localizedMessage}"
                } finally {
                    _isScanning.value = false
                }
            }
        }
    }

    fun scanClassTextWithGemini(text: String) {
        _isScanning.value = true
        _scanError.value = null
        _scanResult.value = null
        
        val apiKey = getEffectiveApiKey()
        if (apiKey.isEmpty()) {
            try {
                val parsed = RoutineTextParser.parseRoutineText(text, _selectedDept.value, _selectedSection.value)
                _scanResult.value = parsed
            } catch (e: Exception) {
                _scanError.value = "Failed parsing text: ${e.localizedMessage}"
            } finally {
                _isScanning.value = false
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prompt = """
                    Analyze the following unformatted or unstructured class routine text of Daffodil International University (DIU).
                    Extract all class schedules and map them into a structured JSON array.
                    Only output a valid JSON array and nothing else. No markdown block formatting, no extra explanation text.
                    
                    The output MUST be a JSON array where each object has these EXACT fields (and no others):
                    - "dayOfWeek": The day of the week, e.g. "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday".
                    - "subjectCode": The subject code, e.g. "CSE 322".
                    - "subjectName": The subject name, e.g. "Software Engineering".
                    - "teacherCode": The teacher code/initials, e.g. "MAM".
                    - "timeStart": The class start time in 24-hour HH:mm format, e.g. "08:30".
                    - "timeEnd": The class end time in 24-hour HH:mm format, e.g. "10:00".
                    - "roomNo": The room number, e.g. "604 MC".
                    - "department": The department, e.g. "${_selectedDept.value}".
                    - "section": The section, e.g. "${_selectedSection.value}".
                    
                    Guidelines:
                    - Parse days carefully.
                    - Convert times to HH:mm 24-hour format.
                    - Use department = "${_selectedDept.value}" and section = "${_selectedSection.value}" as defaults if not explicitly given.
                    
                    Routine Text:
                    $text
                """.trimIndent()
                
                val request = GenerateContentRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(Part(text = prompt))
                        )
                    ),
                    generationConfig = GenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.1
                    )
                )
                
                val response = GeminiClient.api.generateContent(
                    model = "gemini-2.5-flash",
                    apiKey = apiKey,
                    request = request
                )
                
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (responseText.isNullOrEmpty()) {
                    val fallback = RoutineTextParser.parseRoutineText(text, _selectedDept.value, _selectedSection.value)
                    _scanResult.value = fallback
                    return@launch
                }
                
                val cleanJson = responseText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val jsonArray = org.json.JSONArray(cleanJson)
                val list = mutableListOf<ClassSchedule>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        ClassSchedule(
                            dayOfWeek = obj.optString("dayOfWeek", "Sunday"),
                            subjectCode = obj.optString("subjectCode", ""),
                            subjectName = obj.optString("subjectName", ""),
                            teacherCode = obj.optString("teacherCode", ""),
                            timeStart = obj.optString("timeStart", ""),
                            timeEnd = obj.optString("timeEnd", ""),
                            roomNo = obj.optString("roomNo", ""),
                            department = obj.optString("department", _selectedDept.value),
                            section = obj.optString("section", _selectedSection.value),
                            notificationEnabled = true,
                            isCompleted = false
                        )
                    )
                }
                
                if (list.isEmpty()) {
                    val fallback = RoutineTextParser.parseRoutineText(text, _selectedDept.value, _selectedSection.value)
                    _scanResult.value = fallback
                } else {
                    _scanResult.value = list
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    val fallback = RoutineTextParser.parseRoutineText(text, _selectedDept.value, _selectedSection.value)
                    if (fallback.isNotEmpty()) {
                        _scanResult.value = fallback
                    } else {
                        _scanError.value = "AI Scan failed: ${e.localizedMessage ?: "Unknown error"}"
                    }
                } catch (fallbackEx: Exception) {
                    _scanError.value = "AI Scan failed: ${e.localizedMessage ?: "Unknown error"}"
                }
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun confirmImport() {
        val scanRes = _scanResult.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // Room generates the actual IDs on insertion.
            // When Room triggers an update, the flow collector reschedules with correct IDs.
            repository.insertClasses(scanRes)
            _scanResult.value = null
        }
    }

    fun cancelScan() {
        _scanResult.value = null
        _scanError.value = null
    }

    // Scanning / Clipboard Text: Exam Routine
    fun parseAndScanExamDocx(uri: Uri) {
        _isScanningExams.value = true
        _scanExamError.value = null
        _scanExamResult.value = null
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val extractedText = RoutineTextParser.extractTextFromDocx(getApplication(), uri)
                if (extractedText.startsWith("Error")) {
                    _scanExamError.value = extractedText
                } else {
                    val parsed = RoutineTextParser.parseExamRoutineText(extractedText, _selectedDept.value, _selectedSection.value)
                    _scanExamResult.value = parsed
                }
            } catch (e: Exception) {
                _scanExamError.value = "Failed to parse exam file: ${e.localizedMessage}"
            } finally {
                _isScanningExams.value = false
            }
        }
    }

    fun parseAndScanExamText(text: String) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isNotEmpty()) {
            scanExamTextWithGemini(text)
        } else {
            _isScanningExams.value = true
            _scanExamError.value = null
            _scanExamResult.value = null
            viewModelScope.launch(Dispatchers.Default) {
                try {
                    val parsed = RoutineTextParser.parseExamRoutineText(text, _selectedDept.value, _selectedSection.value)
                    _scanExamResult.value = parsed
                } catch (e: Exception) {
                    _scanExamError.value = "Failed parsing exam text: ${e.localizedMessage}"
                } finally {
                    _isScanningExams.value = false
                }
            }
        }
    }

    fun scanExamTextWithGemini(text: String) {
        _isScanningExams.value = true
        _scanExamError.value = null
        _scanExamResult.value = null
        
        val apiKey = getEffectiveApiKey()
        if (apiKey.isEmpty()) {
            try {
                val parsed = RoutineTextParser.parseExamRoutineText(text, _selectedDept.value, _selectedSection.value)
                _scanExamResult.value = parsed
            } catch (e: Exception) {
                _scanExamError.value = "Failed parsing exam text: ${e.localizedMessage}"
            } finally {
                _isScanningExams.value = false
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prompt = """
                    Analyze the following unformatted or unstructured exam schedule text of Daffodil International University (DIU).
                    Extract all exam schedules and map them into a structured JSON array.
                    Only output a valid JSON array and nothing else. No markdown block formatting, no extra explanation text.
                    
                    The output MUST be a JSON array where each object has these EXACT fields (and no others):
                    - "date": The exam date in YYYY-MM-DD format, e.g. "2026-07-20".
                    - "dayOfWeek": The day of the week, e.g. "Monday".
                    - "subjectCode": The subject code, e.g. "CSE 322".
                    - "subjectName": The subject name, e.g. "Software Engineering".
                    - "timeStart": The exam start time in 24-hour HH:mm format, e.g. "10:00".
                    - "timeEnd": The exam end time in 24-hour HH:mm format, e.g. "13:00".
                    - "roomNo": The room number, e.g. "604 MC".
                    - "seatRange": The seat range / seat plan, e.g. "Row A - Row C" or "All".
                    - "department": The department, e.g. "${_selectedDept.value}".
                    - "section": The section, e.g. "${_selectedSection.value}".
                    
                    Guidelines:
                    - Parse dates, days, and times carefully. Convert exam slots to 24-hour HH:mm format.
                    - Use department = "${_selectedDept.value}" and section = "${_selectedSection.value}" as defaults if not explicitly visible.
                    
                    Exam Text:
                    $text
                """.trimIndent()
                
                val request = GenerateContentRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(Part(text = prompt))
                        )
                    ),
                    generationConfig = GenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.1
                    )
                )
                
                val response = GeminiClient.api.generateContent(
                    model = "gemini-2.5-flash",
                    apiKey = apiKey,
                    request = request
                )
                
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (responseText.isNullOrEmpty()) {
                    val fallback = RoutineTextParser.parseExamRoutineText(text, _selectedDept.value, _selectedSection.value)
                    _scanExamResult.value = fallback
                    return@launch
                }
                
                val cleanJson = responseText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val jsonArray = org.json.JSONArray(cleanJson)
                val list = mutableListOf<ExamSchedule>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        ExamSchedule(
                            date = obj.optString("date", ""),
                            dayOfWeek = obj.optString("dayOfWeek", ""),
                            subjectCode = obj.optString("subjectCode", ""),
                            subjectName = obj.optString("subjectName", ""),
                            timeStart = obj.optString("timeStart", ""),
                            timeEnd = obj.optString("timeEnd", ""),
                            roomNo = obj.optString("roomNo", ""),
                            seatRange = obj.optString("seatRange", ""),
                            department = obj.optString("department", _selectedDept.value),
                            section = obj.optString("section", _selectedSection.value),
                            notificationEnabled = true,
                            isCompleted = false
                        )
                    )
                }
                
                if (list.isEmpty()) {
                    val fallback = RoutineTextParser.parseExamRoutineText(text, _selectedDept.value, _selectedSection.value)
                    _scanExamResult.value = fallback
                } else {
                    _scanExamResult.value = list
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    val fallback = RoutineTextParser.parseExamRoutineText(text, _selectedDept.value, _selectedSection.value)
                    if (fallback.isNotEmpty()) {
                        _scanExamResult.value = fallback
                    } else {
                        _scanExamError.value = "AI Scan failed: ${e.localizedMessage ?: "Unknown error"}"
                    }
                } catch (fallbackEx: Exception) {
                    _scanExamError.value = "AI Scan failed: ${e.localizedMessage ?: "Unknown error"}"
                }
            } finally {
                _isScanningExams.value = false
            }
        }
    }

    fun confirmExamImport() {
        val scanRes = _scanExamResult.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertExams(scanRes)
            _scanExamResult.value = null
        }
    }

    fun cancelExamScan() {
        _scanExamResult.value = null
        _scanExamError.value = null
    }

    fun generateDepartmentPreset() {
        viewModelScope.launch(Dispatchers.IO) {
            val presetClasses = RoutineTextParser.generateMockClassesForDepartment(_selectedDept.value, _selectedSection.value)
            repository.clearAllClasses()
            repository.insertClasses(presetClasses)
        }
    }

    fun exportCurrentRoutine(): String {
        return try {
            val root = org.json.JSONObject()
            
            val classArray = org.json.JSONArray()
            allClasses.value.forEach {
                val obj = org.json.JSONObject()
                obj.put("sc", it.subjectCode)
                obj.put("sn", it.subjectName)
                obj.put("tc", it.teacherCode)
                obj.put("rn", it.roomNo)
                obj.put("ts", it.timeStart)
                obj.put("te", it.timeEnd)
                obj.put("dw", it.dayOfWeek)
                obj.put("dp", it.department)
                obj.put("se", it.section)
                obj.put("ne", it.notificationEnabled)
                obj.put("ic", it.isCompleted)
                classArray.put(obj)
            }
            
            val examArray = org.json.JSONArray()
            allExams.value.forEach {
                val obj = org.json.JSONObject()
                obj.put("sc", it.subjectCode)
                obj.put("sn", it.subjectName)
                obj.put("sr", it.seatRange)
                obj.put("rn", it.roomNo)
                obj.put("ts", it.timeStart)
                obj.put("te", it.timeEnd)
                obj.put("da", it.date)
                obj.put("dw", it.dayOfWeek)
                obj.put("dp", it.department)
                obj.put("se", it.section)
                obj.put("ne", it.notificationEnabled)
                obj.put("ic", it.isCompleted)
                examArray.put(obj)
            }
            
            root.put("classes", classArray)
            root.put("exams", examArray)
            root.toString()
        } catch (e: Exception) {
            ""
        }
    }

    fun importRoutineString(jsonStr: String): Boolean {
        try {
            val root = org.json.JSONObject(jsonStr)
            val classes = mutableListOf<ClassSchedule>()
            val exams = mutableListOf<ExamSchedule>()
            
            if (root.has("classes")) {
                val classArray = root.getJSONArray("classes")
                for (i in 0 until classArray.length()) {
                    val obj = classArray.getJSONObject(i)
                    classes.add(
                        ClassSchedule(
                            subjectCode = obj.optString("sc", ""),
                            subjectName = obj.optString("sn", ""),
                            teacherCode = obj.optString("tc", ""),
                            roomNo = obj.optString("rn", ""),
                            timeStart = obj.optString("ts", ""),
                            timeEnd = obj.optString("te", ""),
                            dayOfWeek = obj.optString("dw", ""),
                            department = obj.optString("dp", ""),
                            section = obj.optString("se", ""),
                            notificationEnabled = obj.optBoolean("ne", true),
                            isCompleted = obj.optBoolean("ic", false)
                        )
                    )
                }
            }
            
            if (root.has("exams")) {
                val examArray = root.getJSONArray("exams")
                for (i in 0 until examArray.length()) {
                    val obj = examArray.getJSONObject(i)
                    exams.add(
                        ExamSchedule(
                            date = obj.optString("da", ""),
                            dayOfWeek = obj.optString("dw", ""),
                            subjectCode = obj.optString("sc", ""),
                            subjectName = obj.optString("sn", ""),
                            timeStart = obj.optString("ts", ""),
                            timeEnd = obj.optString("te", ""),
                            roomNo = obj.optString("rn", ""),
                            seatRange = obj.optString("sr", ""),
                            department = obj.optString("dp", ""),
                            section = obj.optString("se", ""),
                            notificationEnabled = obj.optBoolean("ne", true),
                            isCompleted = obj.optBoolean("ic", false)
                        )
                    )
                }
            }
            
            if (classes.isNotEmpty() || exams.isNotEmpty()) {
                viewModelScope.launch(Dispatchers.IO) {
                    if (classes.isNotEmpty()) {
                        repository.clearAllClasses()
                        repository.insertClasses(classes)
                    }
                    if (exams.isNotEmpty()) {
                        repository.clearAllExams()
                        repository.insertExams(exams)
                    }
                }
                return true
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }

    fun triggerTestNotification() {
        val context = getApplication<Application>()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "class_reminders"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "DIU Class Reminders",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders sent before classes"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val appIntent = Intent(context, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            9999,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Test Notification")
            .setContentText("DIU Routine is working perfectly! Your notifications are enabled.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
            
        notificationManager.notify(9999, notification)
    }

    private fun getCurrentDayOfWeekString(): String {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "Sunday"
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            else -> "Sunday"
        }
    }

    // PDF Rendering and Scan Helpers
    fun loadClassPdf(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val pages = renderPdfPagesFromUri(uri)
            _classPdfPages.value = pages
            _classPdfCurrentPageIndex.value = 0
        }
    }

    fun setClassPdfCurrentPageIndex(index: Int) {
        _classPdfCurrentPageIndex.value = index
    }

    fun clearClassPdf() {
        _classPdfPages.value = null
        _classPdfCurrentPageIndex.value = 0
    }

    fun loadExamPdf(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val pages = renderPdfPagesFromUri(uri)
            _examPdfPages.value = pages
            _examPdfCurrentPageIndex.value = 0
        }
    }

    fun setExamPdfCurrentPageIndex(index: Int) {
        _examPdfCurrentPageIndex.value = index
    }

    fun clearExamPdf() {
        _examPdfPages.value = null
        _examPdfCurrentPageIndex.value = 0
    }

    private fun renderPdfPagesFromUri(uri: Uri): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        try {
            val context = getApplication<Application>()
            val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            if (parcelFileDescriptor != null) {
                val pdfRenderer = PdfRenderer(parcelFileDescriptor)
                val pageCount = pdfRenderer.pageCount
                for (i in 0 until pageCount) {
                    val page = pdfRenderer.openPage(i)
                    val width = page.width * 2
                    val height = page.height * 2
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmaps.add(bitmap)
                    page.close()
                }
                pdfRenderer.close()
                parcelFileDescriptor.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bitmaps
    }

    fun scanClassPdfPage(bitmap: Bitmap) {
        _isScanning.value = true
        _scanError.value = null
        _scanResult.value = null
        
        val apiKey = getEffectiveApiKey()
        if (apiKey.isEmpty()) {
            _scanError.value = "Gemini API key is not configured. Please add your key in Settings or .env file."
            _isScanning.value = false
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val base64Image = bitmap.toBase64()
                val prompt = """
                    Analyze the provided image of a class routine / class schedule of Daffodil International University (DIU).
                    Extract all class schedules and map them into a structured JSON array.
                    Only output a valid JSON array and nothing else. No markdown block formatting, no extra explanation text.
                    
                    The output MUST be a JSON array where each object has these EXACT fields (and no others):
                    - "dayOfWeek": The day of the week, e.g. "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday".
                    - "subjectCode": The subject code, e.g. "CSE 322".
                    - "subjectName": The subject name, e.g. "Software Engineering".
                    - "teacherCode": The teacher code, e.g. "MAM".
                    - "timeStart": The class start time in 24-hour HH:mm format, e.g. "08:30".
                    - "timeEnd": The class end time in 24-hour HH:mm format, e.g. "10:00".
                    - "roomNo": The room number, e.g. "604 MC".
                    - "department": The department, e.g. "${_selectedDept.value}".
                    - "section": The section, e.g. "${_selectedSection.value}".
                    
                    Guidelines:
                    - Parse the days carefully. If classes are in columns or tables under a day, group them under that day.
                    - Convert times to HH:mm 24-hour format.
                    - Use department = "${_selectedDept.value}" and section = "${_selectedSection.value}" as defaults if not explicitly visible.
                """.trimIndent()
                
                val request = GenerateContentRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(
                                Part(text = prompt),
                                Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                            )
                        )
                    ),
                    generationConfig = GenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.1
                    )
                )
                
                val response = GeminiClient.api.generateContent(
                    model = "gemini-2.5-flash",
                    apiKey = apiKey,
                    request = request
                )
                
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (responseText.isNullOrEmpty()) {
                    _scanError.value = "Gemini returned empty response"
                    return@launch
                }
                
                val cleanJson = responseText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val jsonArray = org.json.JSONArray(cleanJson)
                val list = mutableListOf<ClassSchedule>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        ClassSchedule(
                            dayOfWeek = obj.optString("dayOfWeek", "Sunday"),
                            subjectCode = obj.optString("subjectCode", ""),
                            subjectName = obj.optString("subjectName", ""),
                            teacherCode = obj.optString("teacherCode", ""),
                            timeStart = obj.optString("timeStart", ""),
                            timeEnd = obj.optString("timeEnd", ""),
                            roomNo = obj.optString("roomNo", ""),
                            department = obj.optString("department", _selectedDept.value),
                            section = obj.optString("section", _selectedSection.value),
                            notificationEnabled = true,
                            isCompleted = false
                        )
                    )
                }
                
                if (list.isEmpty()) {
                    _scanError.value = "No schedules parsed from the page"
                } else {
                    _scanResult.value = list
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                _scanError.value = "Scan failed: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun scanExamPdfPage(bitmap: Bitmap) {
        _isScanningExams.value = true
        _scanExamError.value = null
        _scanExamResult.value = null
        
        val apiKey = getEffectiveApiKey()
        if (apiKey.isEmpty()) {
            _scanExamError.value = "Gemini API key is not configured. Please add your key in Settings or .env file."
            _isScanningExams.value = false
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val base64Image = bitmap.toBase64()
                val prompt = """
                    Analyze the provided image of an exam schedule / exam routine of Daffodil International University (DIU).
                    Extract all exam schedules and map them into a structured JSON array.
                    Only output a valid JSON array and nothing else. No markdown block formatting, no extra explanation text.
                    
                    The output MUST be a JSON array where each object has these EXACT fields (and no others):
                    - "date": The exam date in YYYY-MM-DD format, e.g. "2026-07-20".
                    - "dayOfWeek": The day of the week, e.g. "Monday".
                    - "subjectCode": The subject code, e.g. "CSE 322".
                    - "subjectName": The subject name, e.g. "Software Engineering".
                    - "timeStart": The exam start time in 24-hour HH:mm format, e.g. "10:00".
                    - "timeEnd": The exam end time in 24-hour HH:mm format, e.g. "13:00".
                    - "roomNo": The room number, e.g. "604 MC".
                    - "seatRange": The seat range / seat plan, e.g. "Row A - Row C" or "All".
                    - "department": The department, e.g. "${_selectedDept.value}".
                    - "section": The section, e.g. "${_selectedSection.value}".
                    
                    Guidelines:
                    - Parse dates, days, and times carefully. Convert exam slots to 24-hour HH:mm format.
                    - Use department = "${_selectedDept.value}" and section = "${_selectedSection.value}" as defaults if not explicitly visible.
                """.trimIndent()
                
                val request = GenerateContentRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(
                                Part(text = prompt),
                                Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                            )
                        )
                    ),
                    generationConfig = GenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.1
                    )
                )
                
                val response = GeminiClient.api.generateContent(
                    model = "gemini-2.5-flash",
                    apiKey = apiKey,
                    request = request
                )
                
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (responseText.isNullOrEmpty()) {
                    _scanExamError.value = "Gemini returned empty response"
                    return@launch
                }
                
                val cleanJson = responseText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val jsonArray = org.json.JSONArray(cleanJson)
                val list = mutableListOf<ExamSchedule>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        ExamSchedule(
                            date = obj.optString("date", ""),
                            dayOfWeek = obj.optString("dayOfWeek", ""),
                            subjectCode = obj.optString("subjectCode", ""),
                            subjectName = obj.optString("subjectName", ""),
                            timeStart = obj.optString("timeStart", ""),
                            timeEnd = obj.optString("timeEnd", ""),
                            roomNo = obj.optString("roomNo", ""),
                            seatRange = obj.optString("seatRange", ""),
                            department = obj.optString("department", _selectedDept.value),
                            section = obj.optString("section", _selectedSection.value),
                            notificationEnabled = true,
                            isCompleted = false
                        )
                    )
                }
                
                if (list.isEmpty()) {
                    _scanExamError.value = "No schedules parsed from the page"
                } else {
                    _scanExamResult.value = list
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                _scanExamError.value = "Scan failed: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isScanningExams.value = false
            }
        }
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = java.io.ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
    }
}
