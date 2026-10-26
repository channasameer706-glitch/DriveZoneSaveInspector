package com.example.drivezoneinspector.model

import android.net.Uri

enum class DetectedFormat {
    JSON,
    XML,
    PLAIN_TEXT,
    BINARY_OR_ENCRYPTED,
    UNKNOWN
}

data class NumericField(
    val keyPath: String,
    var value: Double,
    val isInteger: Boolean
)

data class InspectedFile(
    val uri: Uri,
    val fileName: String,
    val fileSizeFormatted: String,
    val format: DetectedFormat,
    val rawPreviewText: String?,
    val detectedNumbers: List<NumericField>,
    val isEditableText: Boolean
)

data class BackupItem(
    val fileName: String,
    val dateCreated: String,
    val absolutePath: String
)
