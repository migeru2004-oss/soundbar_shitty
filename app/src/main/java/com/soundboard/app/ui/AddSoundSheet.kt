package com.soundboard.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSoundSheet(
    viewModel: SoundViewModel,
    onDismiss: () -> Unit,
    onError: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Add Sound",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(16.dp))

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("From File") },
                icon = { Icon(Icons.Default.FileOpen, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("From YouTube") },
                icon = { Icon(Icons.Default.VideoLibrary, contentDescription = null) }
            )
        }

        Spacer(Modifier.height(16.dp))

        when (selectedTab) {
            0 -> FileTab(
                viewModel = viewModel,
                onSoundAdded = onDismiss,
                onError = onError
            )
            1 -> YouTubeTab(
                viewModel = viewModel,
                onSoundAdded = onDismiss,
                onError = onError
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun FileTab(
    viewModel: SoundViewModel,
    onSoundAdded: () -> Unit,
    onError: (String) -> Unit
) {
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var title by remember { mutableStateOf("") }
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedFileUri = it
            val name = viewModel.getFileName(it)
            title = name ?: "Sound"
        }
    }

    Column {
        OutlinedButton(
            onClick = { filePickerLauncher.launch(arrayOf("audio/*")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(text = if (selectedFileUri != null) "Change File" else "Select Audio File")
        }

        if (selectedFileUri != null) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Sound Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (title.isNotBlank() && selectedFileUri != null) {
                        viewModel.addSoundFromFile(
                            uri = selectedFileUri!!,
                            title = title,
                            onComplete = { onSoundAdded() }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Sound")
            }
        }
    }
}

@Composable
fun YouTubeTab(
    viewModel: SoundViewModel,
    onSoundAdded: () -> Unit,
    onError: (String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    val isDownloading by viewModel.isDownloading.collectAsState()

    Column {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("YouTube URL") },
            placeholder = { Text("https://youtube.com/watch?v=...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (url.isNotBlank()) {
                    viewModel.downloadFromYoutube(
                        url = url,
                        onComplete = { onSoundAdded() },
                        onError = { msg -> onError(msg) }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = url.isNotBlank() && !isDownloading
        ) {
            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("Downloading...")
            } else {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Download Audio")
            }
        }
    }
}
