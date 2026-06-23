package com.example.fundsmanager.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ProofSourceDialog(
    onDismiss: () -> Unit,
    onSelectFile: () -> Unit,
    onOpenCamera: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Laporkan bukti", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ListItem(
                    headlineContent = { Text("Pilih file") },
                    supportingContent = { Text("Upload bukti dari dokumen atau galeri") },
                    leadingContent = { Icon(Icons.Default.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = androidx.compose.ui.Modifier.clickable { onSelectFile() }
                )
                ListItem(
                    headlineContent = { Text("Ambil foto") },
                    supportingContent = { Text("Buka kamera untuk foto bukti") },
                    leadingContent = { Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = androidx.compose.ui.Modifier.clickable { onOpenCamera() }
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
