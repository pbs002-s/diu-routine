package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StudyLog
import com.example.ui.RoutineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    viewModel: RoutineViewModel,
    modifier: Modifier = Modifier
) {
    val allStudyLogs by viewModel.allStudyLogs.collectAsState()
    val classes by viewModel.allClasses.collectAsState()

    // Calculations
    val totalStudyMinutes = remember(allStudyLogs) {
        allStudyLogs.filter { it.type == "STUDY" }.sumOf { it.durationMinutes }
    }
    val totalClassMinutes = remember(allStudyLogs) {
        allStudyLogs.filter { it.type == "CLASS" }.sumOf { it.durationMinutes }
    }

    val studyHours = (totalStudyMinutes / 60f)
    val classHours = (totalClassMinutes / 60f)
    val totalHours = studyHours + classHours

    // Achievement badges logic
    val achievements = remember(allStudyLogs, classes, totalHours) {
        listOf(
            AchievementBadge(
                title = "Academic Pioneer",
                description = "Successfully imported/added a routine schedule",
                icon = Icons.Default.Map,
                unlocked = classes.isNotEmpty(),
                color = Color(0xFF4CAF50)
            ),
            AchievementBadge(
                title = "Early Bird",
                description = "Attended a class scheduled at 08:30 AM",
                icon = Icons.Default.WbSunny,
                unlocked = allStudyLogs.any { it.description.contains("08:30") || it.description.contains("8:30") },
                color = Color(0xFFFF9800)
            ),
            AchievementBadge(
                title = "Dedicated Scholar",
                description = "Log at least 5 hours of total study/academic time",
                icon = Icons.Default.EmojiEvents,
                unlocked = totalHours >= 5f,
                color = Color(0xFFE91E63)
            ),
            AchievementBadge(
                title = "Knowledge Seeker",
                description = "Log your first custom study session",
                icon = Icons.Default.Search,
                unlocked = allStudyLogs.any { it.type == "STUDY" },
                color = Color(0xFF00BCD4)
            )
        )
    }

    val unlockedCount = achievements.count { it.unlocked }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("progress_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "ACADEMIC PROGRESS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = String.format("%.1f Hrs", totalHours),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = "Total Logged Focus Time",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%.1f h", studyHours),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "Self Study",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%.1f h", classHours),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "Lectures",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$unlockedCount / ${achievements.size}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "Badges",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // Achievements Badges section
        item {
            Column {
                Text(
                    text = "Achievements & Badges",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    achievements.forEach { badge ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(115.dp)
                                .clip(RoundedCornerShape(24.dp)),
                            colors = CardDefaults.cardColors(
                                containerColor = if (badge.unlocked) {
                                    MaterialTheme.colorScheme.surface
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                }
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (badge.unlocked) badge.color.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (badge.unlocked) badge.color.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.1f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = badge.icon,
                                            contentDescription = badge.title,
                                            tint = if (badge.unlocked) badge.color else Color.Gray.copy(alpha = 0.4f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = badge.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center,
                                    color = if (badge.unlocked) MaterialTheme.colorScheme.onSurface else Color.Gray.copy(alpha = 0.5f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (badge.unlocked) "Unlocked" else "Locked",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (badge.unlocked) badge.color else Color.Gray.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Focus Logs section
        item {
            Text(
                text = "History & Logs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        if (allStudyLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = "Empty log",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No study logs recorded yet",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(allStudyLogs, key = { it.id }) { log ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (log.type == "STUDY") {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (log.type == "STUDY") {
                                            Icons.Default.School
                                        } else {
                                            Icons.Default.Class
                                        },
                                        contentDescription = log.type,
                                        tint = if (log.type == "STUDY") {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = log.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${log.date} • ${log.durationMinutes} Minutes Focus",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.deleteStudyLog(log.id) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Log",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

data class AchievementBadge(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val unlocked: Boolean,
    val color: Color
)
