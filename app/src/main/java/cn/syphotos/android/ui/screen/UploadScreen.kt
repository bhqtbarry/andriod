package cn.syphotos.android.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.syphotos.android.ui.state.UploadDraftUiState

@Composable
fun UploadScreen(state: UploadDraftUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Single image upload", style = MaterialTheme.typography.headlineSmall)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.fileName)
                Text(state.ratioRule)
                Text(state.exifStatus)
                Text(state.watermarkStatus)
                Text(state.registrationStatus)
                LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                Text("Retry-supported upload enters review flow after success.")
                Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                    Text("Choose Image")
                }
                OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                    Text("Retry Upload")
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Deletion rules")
                Text("Irreversible, second confirmation required, title text match required.")
                Text("Pending: editable/deletable")
                Text("Rejected: deletable only, reupload required")
                Text("Approved: deletable only")
            }
        }
    }
}

