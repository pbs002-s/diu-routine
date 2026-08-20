package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.RoutineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: RoutineViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val dept by viewModel.selectedDept.collectAsState()
    val section by viewModel.selectedSection.collectAsState()
    val customDepts by viewModel.customDepartments.collectAsState()
    val customSections by viewModel.customSections.collectAsState()
    val alarmOffset by viewModel.alarmOffsetMinutes.collectAsState()
    val storedApiKey by viewModel.geminiApiKey.collectAsState()

    var apiKeyInput by remember(storedApiKey) { mutableStateOf(storedApiKey) }
    var isKeyVisible by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionTestMessage by remember { mutableStateOf<String?>(null) }
    var connectionTestSuccess by remember { mutableStateOf<Boolean?>(null) }

    var showResetClassesDialog by remember { mutableStateOf(false) }
    var showResetExamsDialog by remember { mutableStateOf(false) }

    var showImportRoutineDialog by remember { mutableStateOf(false) }
    var importRoutineTextInput by remember { mutableStateOf("") }
    var importRoutineErrorText by remember { mutableStateOf<String?>(null) }
    
    var showExportRoutineDialog by remember { mutableStateOf(false) }
    var exportedRoutineCode by remember { mutableStateOf("") }
    
    // Custom inputs dialogs
    var showAddDeptDialog by remember { mutableStateOf(false) }
    var newDeptText by remember { mutableStateOf("") }
    
    var showAddSectionDialog by remember { mutableStateOf(false) }
    var newSectionText by remember { mutableStateOf("") }

    // Preset / Suggested items
    val suggestedDepts = listOf("CSE", "SWE", "EEE", "BBA", "English", "Civil", "Pharmacy", "LLB", "MCT", "NFE")
    val suggestedSections = listOf("A", "B", "C", "D", "PC-A", "PC-B", "PC-C", "Eve-A")

    val finalDepartments = remember(customDepts) { suggestedDepts + customDepts }
    val finalSections = remember(customSections) { suggestedSections + customSections }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Gemini AI Configuration Card
        item {
            val effectiveKey = viewModel.getEffectiveApiKey()
            val isKeyConfigured = effectiveKey.isNotEmpty()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Gemini AI",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Gemini AI Integration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            color = if (isKeyConfigured) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isKeyConfigured) "Active" else "Not Configured",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isKeyConfigured) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Text(
                        text = "Connect Google Gemini API to enable AI-powered PDF parsing, image scanning, and intelligent text extraction.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (isKeyVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                Icon(
                                    imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isKeyVisible) "Hide Key" else "Show Key"
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Paste Button
                        OutlinedButton(
                            onClick = {
                                try {
                                    val clip = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                    val text = clip?.primaryClip?.getItemAt(0)?.text?.toString()
                                    if (!text.isNullOrBlank()) {
                                        apiKeyInput = text.trim()
                                    } else {
                                        android.widget.Toast.makeText(context, "Clipboard is empty", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Failed to read clipboard", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste", fontSize = 12.sp)
                        }

                        // Save Button
                        Button(
                            onClick = {
                                viewModel.setGeminiApiKey(apiKeyInput)
                                connectionTestMessage = null
                                connectionTestSuccess = null
                                android.widget.Toast.makeText(context, "API Key saved successfully", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", fontSize = 12.sp)
                        }

                        // Test Connection Button
                        FilledTonalButton(
                            onClick = {
                                if (apiKeyInput.isNotBlank() && apiKeyInput != storedApiKey) {
                                    viewModel.setGeminiApiKey(apiKeyInput)
                                }
                                isTestingConnection = true
                                connectionTestMessage = null
                                connectionTestSuccess = null
                                viewModel.testGeminiConnection { success, message ->
                                    isTestingConnection = false
                                    connectionTestSuccess = success
                                    connectionTestMessage = message
                                }
                            },
                            enabled = !isTestingConnection && (apiKeyInput.isNotBlank() || isKeyConfigured),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.NetworkCheck, contentDescription = "Test", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test", fontSize = 12.sp)
                            }
                        }
                    }

                    if (connectionTestMessage != null) {
                        Surface(
                            color = if (connectionTestSuccess == true) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (connectionTestSuccess == true) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = "Status",
                                    tint = if (connectionTestSuccess == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = connectionTestMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (connectionTestSuccess == true) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/"))
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "Get API Key", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Get free key from Google AI Studio", fontSize = 11.sp)
                        }

                        if (storedApiKey.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    viewModel.setGeminiApiKey("")
                                    apiKeyInput = ""
                                    connectionTestMessage = null
                                    connectionTestSuccess = null
                                }
                            ) {
                                Text("Clear Key", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // App Theme Settings Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = "Dark Theme",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Dark Theme Mode",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Better readability for night study sessions",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.toggleDarkMode() },
                            modifier = Modifier.testTag("dark_mode_switch")
                        )
                    }
                }
            }
        }

        // Profile / University Settings Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Academic Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Department Selector
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Department",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(
                                onClick = { 
                                    newDeptText = ""
                                    showAddDeptDialog = true 
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add custom department",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            finalDepartments.forEach { department ->
                                val selected = dept == department
                                val isCustom = customDepts.contains(department)
                                
                                InputChip(
                                    selected = selected,
                                    onClick = { viewModel.setDepartment(department) },
                                    label = { Text(department) },
                                    trailingIcon = if (isCustom) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete",
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable { viewModel.deleteCustomDepartment(department) }
                                            )
                                        }
                                    } else null,
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Section Selector
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Section",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(
                                onClick = { 
                                    newSectionText = ""
                                    showAddSectionDialog = true 
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add custom section",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            finalSections.forEach { sec ->
                                val selected = section == sec
                                val isCustom = customSections.contains(sec)
                                
                                InputChip(
                                    selected = selected,
                                    onClick = { viewModel.setSection(sec) },
                                    label = { Text(sec) },
                                    trailingIcon = if (isCustom) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete",
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable { viewModel.deleteCustomSection(sec) }
                                            )
                                        }
                                    } else null,
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Notification & Alarms Settings Card (Manual offset reminder adjustment)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Alarms & Reminders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Trigger class reminders early:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val offsetOptions = listOf(0, 5, 10, 15, 20, 30)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        offsetOptions.forEach { opt ->
                            val selected = alarmOffset == opt
                            val label = if (opt == 0) "At start" else "$opt mins before"
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.setAlarmOffsetMinutes(opt) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.triggerTestNotification() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Test Notification"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Alarm Notification")
                    }
                }
            }
        }

        // Routine Portability & Sync Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Routine Portability & Sync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Share your compiled routine with classmates or restore schedules from a clipboard code instantly.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Export Button
                        Button(
                            onClick = {
                                val code = viewModel.exportCurrentRoutine()
                                exportedRoutineCode = code
                                showExportRoutineDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export"
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export", fontSize = 13.sp)
                        }

                        // Import Button
                        Button(
                            onClick = {
                                importRoutineTextInput = ""
                                importRoutineErrorText = null
                                showImportRoutineDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Import"
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Maintenance Settings Card (Reset Databases)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Database Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )

                    // Clear Classes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reset Class Routine",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Deletes all stored class routine entries",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        IconButton(
                            onClick = { showResetClassesDialog = true },
                            modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = "Reset Classes",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Clear Exams
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reset Exam Routine",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Deletes all stored final and midterm exam cards",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        IconButton(
                            onClick = { showResetExamsDialog = true },
                            modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = "Reset Exams",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        // Beautiful Developer Credits Banner Card with GitHub Link
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/pbs002-s"))
                        context.startActivity(browserIntent)
                    }
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = "Code",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DEVELOPER CREDITS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                letterSpacing = 1.2.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                             text = "Created by pbs002-s",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Check out my other projects on GitHub!",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "GitHub Link",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // Custom Department Dialog
    if (showAddDeptDialog) {
        AlertDialog(
            onDismissRequest = { showAddDeptDialog = false },
            title = { Text("Add Department") },
            text = {
                OutlinedTextField(
                    value = newDeptText,
                    onValueChange = { newDeptText = it },
                    label = { Text("Department Name (e.g. TE, LLB)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDeptText.trim().isNotEmpty()) {
                            viewModel.addCustomDepartment(newDeptText)
                            showAddDeptDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDeptDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Custom Section Dialog
    if (showAddSectionDialog) {
        AlertDialog(
            onDismissRequest = { showAddSectionDialog = false },
            title = { Text("Add Section") },
            text = {
                OutlinedTextField(
                    value = newSectionText,
                    onValueChange = { newSectionText = it },
                    label = { Text("Section Name (e.g. PC-C, Eve-B)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSectionText.trim().isNotEmpty()) {
                            viewModel.addCustomSection(newSectionText)
                            showAddSectionDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSectionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset Class Routine Confirmation Dialog
    if (showResetClassesDialog) {
        AlertDialog(
            onDismissRequest = { showResetClassesDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Classes?")
                }
            },
            text = {
                Text("This will delete all classes from your routine calendar and clear their scheduled early alarm notification events.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllClasses()
                        showResetClassesDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetClassesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset Exam Routine Confirmation Dialog
    if (showResetExamsDialog) {
        AlertDialog(
            onDismissRequest = { showResetExamsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Exams?")
                }
            },
            text = {
                Text("This will delete all exam records from your routine calendar and cancel their corresponding alarm reminders.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllExams()
                        showResetExamsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetExamsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export Routine Dialog
    if (showExportRoutineDialog) {
        AlertDialog(
            onDismissRequest = { showExportRoutineDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Routine Code")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Copy the code below and send it to your classmates! They can paste it in the import tab to instantly sync their routines.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = exportedRoutineCode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Routine Data Code") },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("DIU Routine Code", exportedRoutineCode)
                        clipboardManager.setPrimaryClip(clip)
                        showExportRoutineDialog = false
                    }
                ) {
                    Text("Copy Code")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportRoutineDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Import Routine Dialog
    if (showImportRoutineDialog) {
        AlertDialog(
            onDismissRequest = { showImportRoutineDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Import",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Routine Code")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Paste the exported routine sync code below to instantly load the classes and exams schedule.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                try {
                                    val clip = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                    val text = clip?.primaryClip?.getItemAt(0)?.text?.toString()
                                    if (!text.isNullOrBlank()) {
                                        importRoutineTextInput = text.trim()
                                        importRoutineErrorText = null
                                    } else {
                                        android.widget.Toast.makeText(context, "Clipboard is empty", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Failed to read clipboard", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste from Clipboard", fontSize = 12.sp)
                        }
                    }
                    OutlinedTextField(
                        value = importRoutineTextInput,
                        onValueChange = { 
                            importRoutineTextInput = it 
                            importRoutineErrorText = null
                        },
                        label = { Text("Routine Data / Sync Code") },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        placeholder = { Text("e.g. {\"classes\": ...}") }
                    )
                    if (importRoutineErrorText != null) {
                        Text(
                            text = importRoutineErrorText!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importRoutineTextInput.trim().isEmpty()) {
                            importRoutineErrorText = "Please paste a sync code."
                        } else {
                            val success = viewModel.importRoutineString(importRoutineTextInput.trim())
                            if (success) {
                                showImportRoutineDialog = false
                            } else {
                                importRoutineErrorText = "Invalid routine sync code. Please check and try again."
                            }
                        }
                    }
                ) {
                    Text("Import Routine")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportRoutineDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
