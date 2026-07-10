package com.example.ui.components

import android.net.Uri
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClassSchedule
import com.example.data.ExamSchedule
import com.example.ui.RoutineViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: RoutineViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    // Class scanning states
    val isScanningClass by viewModel.isScanning.collectAsState()
    val scanClassResult by viewModel.scanResult.collectAsState()
    val scanClassError by viewModel.scanError.collectAsState()

    // Exam scanning states
    val isScanningExam by viewModel.isScanningExams.collectAsState()
    val scanExamResult by viewModel.scanExamResult.collectAsState()
    val scanExamError by viewModel.scanExamError.collectAsState()

    val dept by viewModel.selectedDept.collectAsState()
    val section by viewModel.selectedSection.collectAsState()

    // Master Selector: 0 = Class Routine, 1 = Exam Routine
    var routineMode by remember { mutableIntStateOf(0) }

    // Input States
    var textToScanClass by remember { mutableStateOf("") }
    var textToScanExam by remember { mutableStateOf("") }
    var activeTabClass by remember { mutableIntStateOf(0) } // 0: Docx, 1: PDF, 2: Paste Text, 3: Presets
    var activeTabExam by remember { mutableIntStateOf(0) } // 0: Docx, 1: PDF, 2: Paste Text

    // Animation for laser scan sweep
    val infiniteTransition = rememberInfiniteTransition(label = "ScanLaserTransition")
    val laserYPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserY"
    )

    // Activity result launcher for class docx file selection
    val classFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                viewModel.parseAndScanDocx(uri)
            }
        }
    )

    // Activity result launcher for exam docx file selection
    val examFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                viewModel.parseAndScanExamDocx(uri)
            }
        }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("scanner_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Selector Mode (Class vs Exam)
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = routineMode == 0,
                onClick = { routineMode = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Class Routine", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Class Routine 📅", fontWeight = FontWeight.Bold)
                }
            }
            SegmentedButton(
                selected = routineMode == 1,
                onClick = { routineMode = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(imageVector = Icons.Default.Assignment, contentDescription = "Exam Routine", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exam Routine 📝", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (routineMode == 0) {
            // ================= CLASS ROUTINE PARSER UI =================
            TabRow(
                selectedTabIndex = activeTabClass,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface),
                divider = {}
            ) {
                Tab(
                    selected = activeTabClass == 0,
                    onClick = { activeTabClass = 0; viewModel.cancelScan() },
                    text = { Text("Docx File", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTabClass == 1,
                    onClick = { activeTabClass = 1; viewModel.cancelScan() },
                    text = { Text("PDF File 📄", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTabClass == 2,
                    onClick = { activeTabClass = 2; viewModel.cancelScan() },
                    text = { Text("Paste Text", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTabClass == 3,
                    onClick = { activeTabClass = 3; viewModel.cancelScan() },
                    text = { Text("Presets", fontWeight = FontWeight.Bold) }
                )
            }

            if (isScanningClass) {
                // Scanning Laser sweep Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                val laserY = size.height * laserYPosition
                                drawLine(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, Color(0xFF00ADB5), Color(0xFF00F5FF), Color(0xFF00ADB5), Color.Transparent)
                                    ),
                                    start = Offset(0f, laserY),
                                    end = Offset(size.width, laserY),
                                    strokeWidth = 5.dp.toPx()
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("SCANNING CLASS ROUTINE...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            Text("Decoding departments, class hours, and classrooms", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else if (scanClassResult != null) {
                // Scanned Class Routine Preview
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
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
                            Text("Parsed Classes (${scanClassResult!!.size} Classes)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { viewModel.cancelScan() }) { Text("Cancel") }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(scanClassResult!!) { classSchedule ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(classSchedule.subjectCode, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text(classSchedule.subjectName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${classSchedule.dayOfWeek} • ${classSchedule.timeStart} - ${classSchedule.timeEnd}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Room ${classSchedule.roomNo}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Text("Initial: ${classSchedule.teacherCode}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.confirmImport() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save & Add to My Calendar")
                        }
                    }
                }
            } else {
                // Inputs UI
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                        when (activeTabClass) {
                            0 -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = "Docx File", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Upload Class Routine File", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Select any official DIU class routine document (.docx) to parse schedules offline directly into the app.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
                                }
                                Button(
                                    onClick = { classFileLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = "Upload")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Select Class DOCX File")
                                }
                            }
                            1 -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.weight(1f)) {
                                    val pdfPages by viewModel.classPdfPages.collectAsState()
                                    val currentPageIndex by viewModel.classPdfCurrentPageIndex.collectAsState()

                                    if (pdfPages == null) {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = "PDF File",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(72.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Upload Class Routine PDF", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Select any official DIU class routine PDF. You can zoom in/out of the pages to verify details and scan via Gemini AI.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        val classPdfLauncher = rememberLauncherForActivityResult(
                                            contract = ActivityResultContracts.OpenDocument(),
                                            onResult = { uri: Uri? ->
                                                if (uri != null) {
                                                    viewModel.loadClassPdf(uri)
                                                }
                                            }
                                        )
                                        Button(
                                            onClick = { classPdfLauncher.launch(arrayOf("application/pdf")) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.CloudUpload, contentDescription = "Upload")
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Select Class PDF File")
                                        }
                                    } else {
                                        Text(
                                            "Page ${currentPageIndex + 1} of ${pdfPages!!.size}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        PdfPageViewer(
                                            bitmap = pdfPages!![currentPageIndex],
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                IconButton(
                                                    onClick = { 
                                                        if (currentPageIndex > 0) {
                                                            viewModel.setClassPdfCurrentPageIndex(currentPageIndex - 1)
                                                        }
                                                    },
                                                    enabled = currentPageIndex > 0
                                                ) {
                                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Page")
                                                }
                                                IconButton(
                                                    onClick = { 
                                                        if (currentPageIndex < pdfPages!!.size - 1) {
                                                            viewModel.setClassPdfCurrentPageIndex(currentPageIndex + 1)
                                                        }
                                                    },
                                                    enabled = currentPageIndex < pdfPages!!.size - 1
                                                ) {
                                                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Page")
                                                }
                                            }

                                            Button(
                                                onClick = { viewModel.scanClassPdfPage(pdfPages!![currentPageIndex]) },
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = "Scan Page")
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Scan This Page")
                                            }

                                            TextButton(onClick = { viewModel.clearClassPdf() }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Clear")
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> {
                                Column(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Paste PDF Class Routine Text", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        TextButton(
                                            onClick = {
                                                val text = clipboardManager.getText()?.text
                                                if (!text.isNullOrEmpty()) textToScanClass = text
                                            }
                                        ) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Paste Clipboard")
                                        }
                                    }
                                    OutlinedTextField(
                                        value = textToScanClass,
                                        onValueChange = { textToScanClass = it },
                                        placeholder = { Text("Paste routine lines copied from PDF. E.g.\nCSE 322 Software Engineering 08:30-10:00 Room 604 MC MAM") },
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { if (textToScanClass.trim().isNotEmpty()) viewModel.parseAndScanText(textToScanClass) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Scan Clipboard Text")
                                }
                            }
                            3 -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = "Presets", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(72.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Load Department Preset Routine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No routine file at hand? Load a simulated DIU preset routine with realistic classes, teacher codes, and locations.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
                                }
                                Button(
                                    onClick = {
                                        viewModel.generateDepartmentPreset()
                                        coroutineScope.launch {
                                            delay(300)
                                            activeTabClass = 0
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Load")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Load $dept Preset (Section $section)")
                                }
                            }
                        }
                    }
                }
            }

            if (scanClassError != null) {
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(scanClassError!!, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

        } else {
            // ================= EXAM ROUTINE PARSER UI =================
            TabRow(
                selectedTabIndex = activeTabExam,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface),
                divider = {}
            ) {
                Tab(
                    selected = activeTabExam == 0,
                    onClick = { activeTabExam = 0; viewModel.cancelExamScan() },
                    text = { Text("Docx File", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTabExam == 1,
                    onClick = { activeTabExam = 1; viewModel.cancelExamScan() },
                    text = { Text("PDF File 📄", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTabExam == 2,
                    onClick = { activeTabExam = 2; viewModel.cancelExamScan() },
                    text = { Text("Paste Text", fontWeight = FontWeight.Bold) }
                )
            }

            if (isScanningExam) {
                // Laser scan card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                val laserY = size.height * laserYPosition
                                drawLine(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, Color(0xFFFF5252), Color(0xFFFF8A80), Color(0xFFFF5252), Color.Transparent)
                                    ),
                                    start = Offset(0f, laserY),
                                    end = Offset(size.width, laserY),
                                    strokeWidth = 5.dp.toPx()
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("SCANNING EXAM ROUTINE...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error)
                            Text("Identifying dates, sessions, seat plans, and classrooms", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else if (scanExamResult != null) {
                // Exam scan preview panel
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
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
                            Text("Parsed Exams (${scanExamResult!!.size} Sessions)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { viewModel.cancelExamScan() }) { Text("Cancel") }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(scanExamResult!!) { exam ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(exam.subjectCode, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text(exam.subjectName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("📅 ${exam.date} (${exam.dayOfWeek}) • 🕒 ${exam.timeStart} - ${exam.timeEnd}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Room ${exam.roomNo}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                                            Text("Seat: ${exam.seatRange}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.confirmExamImport() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save & Add Exams to My Calendar")
                        }
                    }
                }
            } else {
                // Exam Inputs UI
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                        when (activeTabExam) {
                            0 -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = "Docx File", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(72.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Upload Exam Schedule File", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Select any midterm or semester final exam routine document (.docx) to parse schedules offline directly into the app.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
                                }
                                Button(
                                    onClick = { examFileLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = "Upload")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Select Exam DOCX File")
                                }
                            }
                            1 -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.weight(1f)) {
                                    val pdfPages by viewModel.examPdfPages.collectAsState()
                                    val currentPageIndex by viewModel.examPdfCurrentPageIndex.collectAsState()

                                    if (pdfPages == null) {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = "PDF File",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(72.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Upload Exam Routine PDF", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Select any official DIU exam schedule PDF. You can zoom in/out of the pages to verify details and scan via Gemini AI.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        val examPdfLauncher = rememberLauncherForActivityResult(
                                            contract = ActivityResultContracts.OpenDocument(),
                                            onResult = { uri: Uri? ->
                                                if (uri != null) {
                                                    viewModel.loadExamPdf(uri)
                                                }
                                            }
                                        )
                                        Button(
                                            onClick = { examPdfLauncher.launch(arrayOf("application/pdf")) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Icon(Icons.Default.CloudUpload, contentDescription = "Upload")
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Select Exam PDF File")
                                        }
                                    } else {
                                        Text(
                                            "Page ${currentPageIndex + 1} of ${pdfPages!!.size}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        PdfPageViewer(
                                            bitmap = pdfPages!![currentPageIndex],
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                IconButton(
                                                    onClick = { 
                                                        if (currentPageIndex > 0) {
                                                            viewModel.setExamPdfCurrentPageIndex(currentPageIndex - 1)
                                                        }
                                                    },
                                                    enabled = currentPageIndex > 0
                                                ) {
                                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Page")
                                                }
                                                IconButton(
                                                    onClick = { 
                                                        if (currentPageIndex < pdfPages!!.size - 1) {
                                                            viewModel.setExamPdfCurrentPageIndex(currentPageIndex + 1)
                                                        }
                                                    },
                                                    enabled = currentPageIndex < pdfPages!!.size - 1
                                                ) {
                                                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Page")
                                                }
                                            }

                                            Button(
                                                onClick = { viewModel.scanExamPdfPage(pdfPages!![currentPageIndex]) },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                            ) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = "Scan Page")
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Scan This Page")
                                            }

                                            TextButton(onClick = { viewModel.clearExamPdf() }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Clear", color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> {
                                Column(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Paste PDF Exam Routine Text", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        TextButton(
                                            onClick = {
                                                val text = clipboardManager.getText()?.text
                                                if (!text.isNullOrEmpty()) textToScanExam = text
                                            }
                                        ) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Paste Clipboard")
                                        }
                                    }
                                    OutlinedTextField(
                                        value = textToScanExam,
                                        onValueChange = { textToScanExam = it },
                                        placeholder = { Text("Paste midterm or semester final exam lines. E.g.\nDate: 2026-07-20\nCSE 322 Software Engineering 10:00-13:00 Room 604 MC\nDate: 2026-07-22\nCSE 313 Compiler Design 10:00-13:00 Room 501 MC") },
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { if (textToScanExam.trim().isNotEmpty()) viewModel.parseAndScanExamText(textToScanExam) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Scan Exam Text")
                                }
                            }
                        }
                    }
                }
            }

            if (scanExamError != null) {
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(scanExamError!!, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun PdfPageViewer(
    bitmap: Bitmap,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .background(Color.DarkGray)
            .clipToBounds()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "PDF Page",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    ),
                contentScale = ContentScale.Fit
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = { scale = (scale + 0.5f).coerceAtMost(5f) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White)
            }
            Text(
                "${(scale * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = { 
                    scale = (scale - 0.5f).coerceAtLeast(1f)
                    if (scale == 1f) {
                        offsetX = 0f
                        offsetY = 0f
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White)
            }
            IconButton(
                onClick = { 
                    scale = 1f
                    offsetX = 0f
                    offsetY = 0f
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset Zoom", tint = Color.White)
            }
        }
    }
}
