package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClassSchedule
import com.example.data.ExamSchedule
import com.example.ui.RoutineViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: RoutineViewModel,
    modifier: Modifier = Modifier
) {
    val allClasses by viewModel.allClasses.collectAsState()
    val allExams by viewModel.allExams.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val dept by viewModel.selectedDept.collectAsState()
    val section by viewModel.selectedSection.collectAsState()

    // 0: Class Routine, 1: Exam Routine
    var routineMode by remember { mutableIntStateOf(0) }

    var showAddClassDialog by remember { mutableStateOf(false) }
    var showAddExamDialog by remember { mutableStateOf(false) }

    // Weekdays list
    val weekdays = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    // Classes for active day
    val filteredClasses = remember(allClasses, selectedDay) {
        allClasses.filter { it.dayOfWeek.equals(selectedDay, ignoreCase = true) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("schedule_screen"),
        floatingActionButton = {
            if (routineMode == 0) {
                FloatingActionButton(
                    onClick = { showAddClassDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_class_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Class")
                }
            } else {
                FloatingActionButton(
                    onClick = { showAddExamDialog = true },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.testTag("add_exam_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Exam")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Segmented mode switcher at the top (Class vs Exam)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SegmentedButton(
                    selected = routineMode == 0,
                    onClick = { routineMode = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Classes", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Classes", fontWeight = FontWeight.Bold)
                    }
                }
                SegmentedButton(
                    selected = routineMode == 1,
                    onClick = { routineMode = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Assignment, contentDescription = "Exams", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exams", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (routineMode == 0) {
                // ================= CLASS ROUTINE VIEW =================
                ScrollableTabRow(
                    selectedTabIndex = weekdays.indexOf(selectedDay).coerceAtLeast(0),
                    edgePadding = 16.dp,
                    divider = {},
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    weekdays.forEachIndexed { index, day ->
                        val isSelected = day.equals(selectedDay, ignoreCase = true)
                        Tab(
                            selected = isSelected,
                            onClick = { viewModel.setSelectedDay(day) },
                            text = {
                                Text(
                                    text = day,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        )
                    }
                }

                AnimatedContent(
                    targetState = filteredClasses,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ClassListAnimation"
                ) { classes ->
                    if (classes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(
                                    imageVector = Icons.Default.EventNote,
                                    contentDescription = "Empty",
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    modifier = Modifier.size(80.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No classes on $selectedDay",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "You can import your university routine from the Scanner tab or tap the '+' button to add classes manually.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(classes, key = { it.id }) { schedule ->
                                ClassScheduleCard(
                                    schedule = schedule,
                                    onToggleCompleted = { viewModel.toggleClassCompleted(schedule) },
                                    onToggleNotification = { viewModel.toggleNotificationEnabled(schedule) },
                                    onDelete = { viewModel.deleteClass(schedule) }
                                )
                            }
                        }
                    }
                }
            } else {
                // ================= EXAM ROUTINE VIEW =================
                AnimatedContent(
                    targetState = allExams,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ExamListAnimation"
                ) { exams ->
                    if (exams.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(
                                    imageVector = Icons.Default.AssignmentLate,
                                    contentDescription = "No Exams",
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    modifier = Modifier.size(80.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No exams scheduled",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tap the scanner tab above to scan exam schedules from clipboard/Docx, or click the red '+' button to schedule an exam manually.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(exams, key = { it.id }) { exam ->
                                ExamScheduleCard(
                                    exam = exam,
                                    onToggleCompleted = { viewModel.toggleExamCompleted(exam) },
                                    onToggleNotification = { viewModel.toggleExamNotificationEnabled(exam) },
                                    onDelete = { viewModel.deleteExam(exam) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Manual Add Class Dialog
    if (showAddClassDialog) {
        var subCode by remember { mutableStateOf("") }
        var subName by remember { mutableStateOf("") }
        var teacher by remember { mutableStateOf("") }
        var room by remember { mutableStateOf("") }
        var startTime by remember { mutableStateOf("08:30") }
        var endTime by remember { mutableStateOf("10:00") }
        var errorText by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddClassDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Book, contentDescription = "Book", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Class Schedule")
                }
            },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        OutlinedTextField(
                            value = subCode,
                            onValueChange = { subCode = it.uppercase() },
                            label = { Text("Course Code") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. CSE 322") }
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = subName,
                            onValueChange = { subName = it },
                            label = { Text("Course Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. Software Engineering") }
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = teacher,
                            onValueChange = { teacher = it.uppercase() },
                            label = { Text("Teacher Initials") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. MAM") }
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = room,
                            onValueChange = { room = it.uppercase() },
                            label = { Text("Room No") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. 604 MC") }
                        )
                    }
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = startTime,
                                onValueChange = { startTime = it },
                                label = { Text("Start Time") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("HH:mm") }
                            )
                            OutlinedTextField(
                                value = endTime,
                                onValueChange = { endTime = it },
                                label = { Text("End Time") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("HH:mm") }
                            )
                        }
                    }
                    if (errorText != null) {
                        item {
                            Text(text = errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (subCode.trim().isEmpty() || room.trim().isEmpty() || teacher.trim().isEmpty()) {
                            errorText = "Please fill in Code, Room, and Teacher initials."
                        } else if (!startTime.contains(":") || !endTime.contains(":")) {
                            errorText = "Time must be in HH:mm format."
                        } else {
                            viewModel.addClass(
                                ClassSchedule(
                                    dayOfWeek = selectedDay,
                                    subjectCode = subCode.trim(),
                                    subjectName = if (subName.trim().isEmpty()) subCode else subName.trim(),
                                    teacherCode = teacher.trim(),
                                    timeStart = startTime.trim(),
                                    timeEnd = endTime.trim(),
                                    roomNo = room.trim(),
                                    department = dept,
                                    section = section,
                                    isCompleted = false,
                                    notificationEnabled = true
                                )
                            )
                            showAddClassDialog = false
                        }
                    }
                ) {
                    Text("Save Class")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddClassDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Manual Add Exam Dialog
    if (showAddExamDialog) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        var subCode by remember { mutableStateOf("") }
        var subName by remember { mutableStateOf("") }
        var dateVal by remember { mutableStateOf(todayStr) }
        var examDay by remember { mutableStateOf("Monday") }
        var room by remember { mutableStateOf("") }
        var seatVal by remember { mutableStateOf("Row A - B") }
        var startTime by remember { mutableStateOf("10:00") }
        var endTime by remember { mutableStateOf("13:00") }
        var errorText by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddExamDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Assignment, contentDescription = "Exam", tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Exam Session")
                }
            },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        OutlinedTextField(
                            value = subCode,
                            onValueChange = { subCode = it.uppercase() },
                            label = { Text("Course Code") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. CSE 322") }
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = subName,
                            onValueChange = { subName = it },
                            label = { Text("Course Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. Software Engineering") }
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = dateVal,
                            onValueChange = { dateVal = it },
                            label = { Text("Exam Date") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("YYYY-MM-DD") }
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = examDay,
                            onValueChange = { examDay = it },
                            label = { Text("Day of Week") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. Monday") }
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = room,
                                onValueChange = { room = it.uppercase() },
                                label = { Text("Room No") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("e.g. 604 MC") }
                            )
                            OutlinedTextField(
                                value = seatVal,
                                onValueChange = { seatVal = it },
                                label = { Text("Seat details") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("e.g. Row A - B") }
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = startTime,
                                onValueChange = { startTime = it },
                                label = { Text("Start Time") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("HH:mm") }
                            )
                            OutlinedTextField(
                                value = endTime,
                                onValueChange = { endTime = it },
                                label = { Text("End Time") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("HH:mm") }
                            )
                        }
                    }
                    if (errorText != null) {
                        item {
                            Text(text = errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (subCode.trim().isEmpty() || room.trim().isEmpty() || dateVal.trim().isEmpty()) {
                            errorText = "Please fill in Code, Room, and Date."
                        } else if (!startTime.contains(":") || !endTime.contains(":")) {
                            errorText = "Time must be in HH:mm format."
                        } else {
                            viewModel.addExam(
                                ExamSchedule(
                                    date = dateVal.trim(),
                                    dayOfWeek = examDay.trim().lowercase().replaceFirstChar { it.uppercase() },
                                    subjectCode = subCode.trim(),
                                    subjectName = if (subName.trim().isEmpty()) subCode else subName.trim(),
                                    timeStart = startTime.trim(),
                                    timeEnd = endTime.trim(),
                                    roomNo = room.trim(),
                                    seatRange = seatVal.trim(),
                                    department = dept,
                                    section = section,
                                    notificationEnabled = true,
                                    isCompleted = false
                                )
                            )
                            showAddExamDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Save Exam")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExamDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ClassScheduleCard(
    schedule: ClassSchedule,
    onToggleCompleted: () -> Unit,
    onToggleNotification: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (schedule.isCompleted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (schedule.isCompleted) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleCompleted, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (schedule.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Complete class",
                            tint = if (schedule.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = schedule.subjectCode,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (schedule.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Schedule, contentDescription = "Time", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${schedule.timeStart} - ${schedule.timeEnd}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = schedule.subjectName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 36.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.MeetingRoom, contentDescription = "Room", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Room ${schedule.roomNo}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.PersonOutline, contentDescription = "Teacher", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Initial: ${schedule.teacherCode}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleNotification, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (schedule.notificationEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = "Alarm toggle",
                            tint = if (schedule.notificationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ExamScheduleCard(
    exam: ExamSchedule,
    onToggleCompleted: () -> Unit,
    onToggleNotification: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (exam.isCompleted) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (exam.isCompleted) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleCompleted, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (exam.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Complete exam",
                            tint = if (exam.isCompleted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = exam.subjectCode,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (exam.isCompleted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Schedule, contentDescription = "Time", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${exam.timeStart} - ${exam.timeEnd}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = exam.subjectName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 36.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.MeetingRoom, contentDescription = "Room", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Room ${exam.roomNo}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AirlineSeatReclineExtra, contentDescription = "Seat", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = exam.seatRange, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleNotification, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (exam.notificationEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = "Alarm toggle",
                            tint = if (exam.notificationEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
