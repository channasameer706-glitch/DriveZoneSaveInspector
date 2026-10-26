package com.example.drivezoneinspector.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.drivezoneinspector.model.BackupItem
import com.example.drivezoneinspector.model.DetectedFormat
import com.example.drivezoneinspector.model.InspectedFile
import com.example.drivezoneinspector.model.NumericField
import com.example.drivezoneinspector.util.BackupManager
import com.example.drivezoneinspector.util.FileParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Locale

sealed class UIStatus {
    data object Idle : UIStatus()
    data object Loading : UIStatus()
    data class Success(val message: String) : UIStatus()
    data class Error(val message: String) : UIStatus()
}

class InspectorViewModel(application: Application) : AndroidViewModel(application) {

    private val backupManager = BackupManager(application)

    private val _currentFile = MutableStateFlow<InspectedFile?>(null)
    val currentFile: StateFlow<InspectedFile?> = _currentFile.asStateFlow()

    private val _status = MutableStateFlow<UIStatus>(UIStatus.Idle)
    val status: StateFlow<UIStatus> = _status.asStateFlow()

    private val _backups = MutableStateFlow<List<BackupItem>>(emptyList())
    val backups: StateFlow<List<BackupItem>> = _backups.asStateFlow()

    init {
        loadBackups()
    }

    fun loadBackups() {
        viewModelScope.launch(Dispatchers.IO) {
            _backups.value = backupManager.getBackups()
        }
    }

    fun processSelectedFile(uri: Uri) {
        viewModelScope.launch {
            _status.value = UIStatus.Loading
            try {
                val context = getApplication<Application>()
                var fileName = "unnamed_save"
                var fileSize = 0L

                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx != -1) fileName = cursor.getString(nameIdx) ?: fileName
                        if (sizeIdx != -1) fileSize = cursor.getLong(sizeIdx)
                    }
                }

                withContext(Dispatchers.IO) {
                    backupManager.createBackup(uri, fileName)
                }
                loadBackups()

                val byteBuffer = withContext(Dispatchers.IO) {
                    val output = ByteArrayOutputStream()
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        var totalRead = 0L
                        while (input.read(buffer).also { read = it } != -1 && totalRead < 10 * 1024 * 1024) {
                            output.write(buffer, 0, read)
                            totalRead += read
                        }
                    }
                    output.toByteArray()
                }

                val isBinary = FileParser.isBinary(byteBuffer)
                if (isBinary) {
                    _currentFile.value = InspectedFile(
                        uri = uri,
                        fileName = fileName,
                        fileSizeFormatted = formatFileSize(fileSize),
                        format = DetectedFormat.BINARY_OR_ENCRYPTED,
                        rawPreviewText = null,
                        detectedNumbers = emptyList(),
                        isEditableText = false
                    )
                } else {
                    val rawText = String(byteBuffer, Charsets.UTF_8)
                    val format = FileParser.detectFormat(rawText)
                    val numbers = FileParser.extractNumericFields(rawText, format)

                    _currentFile.value = InspectedFile(
                        uri = uri,
                        fi
