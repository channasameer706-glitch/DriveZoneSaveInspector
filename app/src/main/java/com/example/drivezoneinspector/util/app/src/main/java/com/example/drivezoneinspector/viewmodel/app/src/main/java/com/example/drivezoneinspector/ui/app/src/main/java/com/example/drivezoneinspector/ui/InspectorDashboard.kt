package com.example.drivezoneinspector.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drivezoneinspector.model.BackupItem
import com.example.drivezoneinspector.model.DetectedFormat
import com.example.drivezoneinspector.model.NumericField
import com.example.drivezoneinspector.viewmodel.InspectorViewModel
import com.example.drivezoneinspector.viewmodel.UIStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectorDashboard(viewModel: InspectorViewModel) {
    val currentFile by viewModel.currentFile.collectAsState()
    val status by viewModel.status.collectAsState()
    val backups by viewModel.backups.collectAsState()

    var showBackupsDialog by remember { mutableStateOf(false) }
    var selectedFieldToEdit by remember { mutableStateOf<NumericField?>(null) }
    var editFieldValue by remember { mutableStateOf("") }
    var backupToRestore by remember { mutableStateOf<BackupItem?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.processSelectedFile(it) }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null && selectedFieldToEdit != null) {
            val num = editFieldValue.toDoubleOrNull()
            if (num != null) {
                viewModel.exportModifiedCopy(uri, selectedFieldToEdit!!, num)
            }
        }
        selectedFieldToEdit = null
    }

    val restoreDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null && backupToRestore != null) {
            viewModel.restoreBackupToExternal(backupToRestore!!, uri)
        }
        backupToRestore = null
    }

    LaunchedEffect(status) {
        when (val s = status) {
            is UIStatus.Success -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.resetStatus()
            }
            is UIStatus.Error -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.resetStatus()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Drive Zone Save Inspector", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                actions = {
                    IconButton(onClick = { showBackupsDialog = true }) {
                        Icon(Icons.Default.History, contentDescription = "Snapshots")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { openDocumentLauncher.launch(arrayOf("*/*")) },
                icon = { Icon(Icons.Default.FileOpen, contentDescription = null) },
                text = { Text("Open File") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Safety Note: Original files are never modified directly. Backups are stored safely in isolated app storage.",
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (status is UIStatus.Loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (currentFile == null) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No file loaded. Tap 'Open File' to inspect.", color = Color.Gray)
                }
            } else {
                val file = currentFile!!
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("File Properties", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Text("File Name: ${file.fileName}", fontSize = 13.sp)
                                Text("Size: ${file.fileSizeFormatted}", fontSize = 13.sp)
                                Text("Detected Format: ${file.format.name}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (file.format == DetectedFormat.BINARY_OR_ENCRYPTED) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modif
