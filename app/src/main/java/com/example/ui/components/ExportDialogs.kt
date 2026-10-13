package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.localization.LocalizedStrings
import com.example.model.AppLanguage
import com.example.service.PngExportResult

@Composable
fun PngExportPreviewDialog(
    exportResult: PngExportResult.Success,
    characterName: String,
    language: AppLanguage,
    strings: LocalizedStrings,
    onExportNeutralToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var neutralChecked by remember { mutableStateOf(false) }

    val savePngLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(exportResult.pngBytes)
                }
            } catch (_: Exception) {
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.exportPngBtn,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    bitmap = exportResult.bitmap.asImageBitmap(),
                    contentDescription = characterName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("png_export_preview_image")
                )

                Text(
                    text = "${exportResult.bitmap.width}×${exportResult.bitmap.height} PNG (${exportResult.pngBytes.size / 1024} KB)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                OutlinedButton(
                    onClick = {
                        neutralChecked = !neutralChecked
                        onExportNeutralToggle(neutralChecked)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (language == AppLanguage.AR) {
                            if (neutralChecked) "استخدام المشهد الطبيعي" else "استخدام خلفية محايدة للتصدير"
                        } else {
                            if (neutralChecked) "Use Outdoor Scene Background" else "Use Neutral Studio Background"
                        }
                    )
                }

                Button(
                    onClick = {
                        val safe = characterName.replace(" ", "_").ifBlank { "trailkit_character" }
                        savePngLauncher.launch("${safe}.png")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_png_file_btn")
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.shareOrSaveFileBtn)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.closeBtn)
            }
        }
    )
}

@Composable
fun JsonExportDialog(
    jsonContent: String,
    characterName: String,
    strings: LocalizedStrings,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    val saveJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(jsonContent.toByteArray(Charsets.UTF_8))
                }
            } catch (_: Exception) {
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = strings.exportJsonBtn, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = jsonContent,
                    onValueChange = {},
                    readOnly = true,
                    maxLines = 10,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("exported_json_textfield")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(jsonContent))
                            copied = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_json_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (copied) strings.copiedToClipboard else strings.copyJsonBtn)
                    }

                    Button(
                        onClick = {
                            val safe = characterName.replace(" ", "_").ifBlank { "trailkit_backup" }
                            saveJsonLauncher.launch("${safe}.json")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_json_file_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.shareOrSaveFileBtn)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.closeBtn)
            }
        }
    )
}

@Composable
fun JsonImportDialog(
    errorMessage: String?,
    strings: LocalizedStrings,
    onValidateAndImport: (String) -> Boolean,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var rawJsonInput by remember { mutableStateOf("") }

    val openJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                if (!text.isNullOrBlank()) {
                    rawJsonInput = text
                }
            } catch (_: Exception) {
            }
        }
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(text = strings.importJsonTitle, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = strings.importJsonHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = { openJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_json_file_btn")
                ) {
                    Icon(imageVector = Icons.Default.UploadFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.importPickFileBtn)
                }

                OutlinedTextField(
                    value = rawJsonInput,
                    onValueChange = { rawJsonInput = it },
                    placeholder = { Text("{ \"schemaVersion\": 1, ... }") },
                    maxLines = 10,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .testTag("import_json_input_field")
                )

                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_json_error_banner")
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ok = onValidateAndImport(rawJsonInput)
                    if (ok) {
                        onCancel()
                    }
                },
                modifier = Modifier.testTag("confirm_import_json_btn")
            ) {
                Text(strings.importValidateAndSave)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.testTag("cancel_import_json_btn")
            ) {
                Text(strings.cancel)
            }
        }
    )
}
