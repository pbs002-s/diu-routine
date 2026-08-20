package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClassSchedule
import com.example.ui.RoutineViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: RoutineViewModel,
    onNavigateToScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allClasses by viewModel.allClasses.collectAsState()
    val allStudyLogs by viewModel.allStudyLogs.collectAsState()
    val dept by viewModel.selectedDept.collectAsState()
    val sec by viewModel.selectedSection.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var showStudyLogDialog by remember { mutableStateOf(false) }

    // Filter classes for today
    val todayString = remember { getCurrentDayOfWeek() }
    val todayClasses = remember(allClasses) {
        allClasses.filter { it.dayOfWeek.equals(todayString, ignoreCase = true) }
    }

    // Calculations
    val completedToday = todayClasses.count { it.isCompleted }
    val totalToday = todayClasses.size
    val completionRatio = if (totalToday > 0) completedToday.toFloat() / totalToday else 0f

    // Next upcoming class calculation
    val upcomingClass = remember(allClasses) {
        getUpcomingClass(allClasses)
    }

    // Total study/academic hours calculation
    val totalMinutes = remember(allStudyLogs) {
        allStudyLogs.sumOf { it.durationMinutes }
    }
    val totalHours = totalMinutes / 60f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Bento Style Header Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DAFFODIL INT. UNIVERSITY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "$dept Department",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Dark Mode Toggle Button Styled as a beautiful circular pill
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Dark Mode",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Profile Initials Box "PS"
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PS",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Live Next Class Card (Featured Green Card)
        item {
            AnimatedContent(
                targetState = upcomingClass,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "UpcomingClassTransition"
            ) { targetClass ->
                val cardShape = RoundedCornerShape(28.dp)
                val isLight = !isDarkMode

                // Define customized green container colors matching the bento theme
                val containerColor = if (isLight) Color(0xFFD3E8D3) else Color(0xFF1B2F25)
                val borderColor = if (isLight) Color(0xFFBFC9BF) else Color(0xFF2E483B)
                val textColor = if (isLight) Color(0xFF002114) else Color(0xFFD4EAD6)
                val subTextColor = if (isLight) Color(0xFF414941) else Color(0xFFA2AFA4)
                val badgeBg = if (isLight) Color(0xFF006C4C) else Color(0xFF81D4B4)
                val badgeText = if (isLight) Color.White else Color(0xFF003825)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = containerColor),
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        if (targetClass != null) {
                            // Top Row: Status Tag & Time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(badgeBg)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "UPCOMING • TODAY",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeText
                                    )
                                }
                                Text(
                                    text = targetClass.timeStart,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = subTextColor
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Middle Section: Class Subject Info
                            Text(
                                text = targetClass.subjectName,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                lineHeight = 26.sp
                            )
                            Text(
                                text = "Course Code: ${targetClass.subjectCode}",
                                fontSize = 12.sp,
                                color = subTextColor,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Room ${targetClass.roomNo} • Teacher: ${targetClass.teacherCode}",
                                fontSize = 13.sp,
                                color = subTextColor,
                                fontWeight = FontWeight.SemiBold
                            )

                            ClassCountdown(targetClass, isLight)

                            Spacer(modifier = Modifier.height(16.dp))

                            // Bottom Time indicator progress bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isLight) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.25f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction = 0.75f) // Mock time progress
                                            .clip(CircleShape)
                                            .background(badgeBg)
                                    )
                                }
                                Text(
                                    text = "75% WEEK",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLight) Color(0xFF006C4C) else Color(0xFF81D4B4)
                                )
                            }
                        } else {
                            // Empty state class card styled as Bento green
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(if (isLight) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Done icon",
                                        tint = if (isLight) Color(0xFF006C4C) else Color(0xFF81D4B4),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No More Scheduled Classes Today!",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "All lectures completed or free day. Sec $sec is clear!",
                                    fontSize = 11.sp,
                                    color = subTextColor,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Daily Motivation Quote Card (Minimalist Bento Row)
        item {
            MotivationalQuoteCard()
        }

        // Action Blocks (Upload Routine & Study Tracker Progress side-by-side)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick Scan Card (Featured Purple Card, equivalent of left column)
                val isLight = !isDarkMode
                val purpleContainerColor = if (isLight) Color(0xFFE8DEF8) else Color(0xFF2E1C4B)
                val purpleBorderColor = if (isLight) Color(0xFFD0BCFF) else Color(0xFF4B3275)
                val purpleTextColor = if (isLight) Color(0xFF21005D) else Color(0xFFE8DEF8)
                val purpleSubTextColor = if (isLight) Color(0xFF49454F) else Color(0xFFD0BCFF).copy(alpha = 0.7f)
                val purpleIconBox = if (isLight) Color(0xFF6750A4) else Color(0xFFD0BCFF)
                val purpleIconTint = if (isLight) Color.White else Color(0xFF21005D)

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onNavigateToScan() },
                    colors = CardDefaults.cardColors(containerColor = purpleContainerColor),
                    border = BorderStroke(1.dp, purpleBorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Purple Icon Frame
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(purpleIconBox),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Scan icon",
                                tint = purpleIconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Upload Routine",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = purpleTextColor
                            )
                            Text(
                                text = "PDF / DOCX Scanning",
                                fontSize = 10.sp,
                                color = purpleSubTextColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Study Tracker Card (Minimalist chart equivalent of right column)
                Card(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { showStudyLogDialog = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Header info: total study hours
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1f", totalHours),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "HRS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }

                        // Cute mini bar chart graphic to mimic the bento tracker progress
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            // Bar 1 (Mon)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(0.35f)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                            // Bar 2 (Tue)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(0.60f)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                            // Bar 3 (Wed - highlighted active bar)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(0.90f)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            // Bar 4 (Thu)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(0.45f)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                            // Bar 5 (Fri)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(0.20f)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }

                        Text(
                            text = "STUDY PROGRESS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // Class completion metrics row
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventAvailable,
                                contentDescription = "Schedule indicator",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Attendance Rate",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$completedToday / $totalToday Attended Today",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${(completionRatio * 100).toInt()}% Done",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Today's Agenda Checklist (Bento Style Card)
        if (todayClasses.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Today's Agenda",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$completedToday / $totalToday Attended",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        todayClasses.sortedBy { it.timeStart }.forEachIndexed { index, cls ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { viewModel.toggleClassCompleted(cls) },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = cls.isCompleted,
                                        onCheckedChange = { viewModel.toggleClassCompleted(cls) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MaterialTheme.colorScheme.primary,
                                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = cls.subjectName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (cls.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${cls.timeStart} • Room ${cls.roomNo} • ${cls.teacherCode}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                            if (index < todayClasses.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Study Log Dialog
    if (showStudyLogDialog) {
        var durationInput by remember { mutableStateOf("") }
        var topicInput by remember { mutableStateOf("") }
        var errorText by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showStudyLogDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Log",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Custom Study")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Add study session hours below. Completed classes are automatically logged for you!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = durationInput,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() }) durationInput = it
                        },
                        label = { Text("Duration (Minutes)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. 60, 90, 120") }
                    )
                    OutlinedTextField(
                        value = topicInput,
                        onValueChange = { topicInput = it },
                        label = { Text("Topic / Course Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Algorithms Revision") }
                    )
                    if (errorText != null) {
                        Text(
                            text = errorText!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val duration = durationInput.toIntOrNull()
                        if (duration == null || duration <= 0) {
                            errorText = "Please enter valid minutes."
                        } else if (topicInput.trim().isEmpty()) {
                            errorText = "Please enter a topic name."
                        } else {
                            viewModel.logStudySession(duration, topicInput.trim())
                            showStudyLogDialog = false
                        }
                    }
                ) {
                    Text("Log Session")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStudyLogDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Utility Helpers
private fun getCurrentDayOfWeek(): String {
    val sdf = SimpleDateFormat("EEEE", Locale.US)
    return sdf.format(Date())
}

private fun getUpcomingClass(classes: List<ClassSchedule>): ClassSchedule? {
    if (classes.isEmpty()) return null
    val todayName = getCurrentDayOfWeek()
    val todayClasses = classes.filter { it.dayOfWeek.equals(todayName, ignoreCase = true) }
    if (todayClasses.isEmpty()) return null

    // Get current time as minutes from midnight
    val calendar = Calendar.getInstance()
    val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

    return todayClasses
        .filter { !it.isCompleted } // Filter out completed classes
        .mapNotNull { schedule ->
            val parts = schedule.timeStart.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toIntOrNull() ?: 0
                val min = parts[1].toIntOrNull() ?: 0
                val classMinutes = hour * 60 + min
                schedule to classMinutes
            } else {
                null
            }
        }
        .filter { (_, classMinutes) -> classMinutes > currentMinutes } // Must start in the future
        .minByOrNull { (_, classMinutes) -> classMinutes }
        ?.first
}

@Composable
fun ClassCountdown(targetClass: ClassSchedule, isLight: Boolean) {
    var timeRemaining by remember(targetClass) { mutableStateOf("") }
    LaunchedEffect(targetClass) {
        while (true) {
            val parts = targetClass.timeStart.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toIntOrNull() ?: 0
                val min = parts[1].toIntOrNull() ?: 0
                val cal = Calendar.getInstance()
                val currentHour = cal.get(Calendar.HOUR_OF_DAY)
                val currentMin = cal.get(Calendar.MINUTE)
                val currentSec = cal.get(Calendar.SECOND)
                
                val currentTotalSec = (currentHour * 3600) + (currentMin * 60) + currentSec
                val classTotalSec = (hour * 3600) + (min * 60)
                
                val diffSec = classTotalSec - currentTotalSec
                if (diffSec > 0) {
                    val h = diffSec / 3600
                    val m = (diffSec % 3600) / 60
                    val s = diffSec % 60
                    timeRemaining = if (h > 0) {
                        "${h}h ${m}m ${s}s"
                    } else if (m > 0) {
                        "${m}m ${s}s"
                    } else {
                        "${s}s"
                    }
                } else {
                    timeRemaining = "Starting..."
                }
            } else {
                timeRemaining = ""
            }
            kotlinx.coroutines.delay(1000L)
        }
    }
    
    val textColor = if (isLight) Color(0xFF006C4C) else Color(0xFF81D4B4)

    if (timeRemaining.isNotEmpty()) {
        Text(
            text = "Starts in: $timeRemaining",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
