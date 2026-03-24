package cn.syphotos.android.ui.screen

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.syphotos.android.ui.i18n.AppLanguage
import cn.syphotos.android.ui.i18n.LocalAppStrings
import cn.syphotos.android.ui.state.MyUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyScreen(
    state: MyUiState,
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onLogin: (String, String) -> Unit,
    onLogout: () -> Unit,
) {
    val strings = LocalAppStrings.current
    var loginInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(strings.account) {
                if (!state.authSession.isLoggedIn) {
                    OutlinedTextField(
                        value = loginInput,
                        onValueChange = { loginInput = it },
                        label = { Text(strings.loginLabel) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text(strings.passwordField) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { onLogin(loginInput, passwordInput) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(strings.signIn)
                    }
                    state.authErrorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                } else {
                    Text("${strings.userLabel}: ${state.user.username}")
                    Text("${strings.emailLabel}: ${state.user.email}")
                    Text(if (state.user.emailVerified) strings.emailVerified else strings.emailVerificationRequired)
                    Text(strings.passwordChange)
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(strings.signOut)
                    }
                }
            }
        }
        state.errorMessage?.let { message ->
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
        if (state.isLoading) {
            item {
                CircularProgressIndicator(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
        if (state.authSession.isLoggedIn) {
            item {
                SectionCard(strings.myWorks) {
                    Text(strings.worksCount(state.summary.allPhotos))
                }
            }
            item {
                SectionCard(strings.myLikes) {
                    Text(strings.likesCount(state.summary.likedPhotos))
                }
            }
            item {
                SectionCard(strings.pending) {
                    Text(strings.editableCount(state.summary.pendingPhotos))
                }
            }
            item {
                SectionCard(strings.rejected) {
                    Text(strings.editableCount(state.summary.rejectedPhotos))
                    state.rejected.forEach {
                        Text(strings.reason(it.rejectionReason ?: "-"))
                        Text(strings.admin(it.adminComment ?: "-"))
                    }
                }
            }
            item {
                SectionCard(strings.languageTitle) {
                    Text(strings.languageSubtitle)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AppLanguage.entries.forEach { language ->
                            FilterChip(
                                selected = language == selectedLanguage,
                                onClick = { onLanguageSelected(language) },
                                label = { Text(language.nativeName) },
                            )
                        }
                    }
                }
            }
            item {
                SectionCard(strings.devices) {
                    Text("${strings.currentDevice} + ${strings.revocable}")
                }
            }
            items(state.sessions, key = { it.id }) { session ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(session.deviceName, style = MaterialTheme.typography.titleMedium)
                        Text("${session.systemVersion} • ${session.ipAddress}")
                        Text("${strings.loginLabel}: ${session.loginTime}")
                        Text(if (session.isCurrent) strings.currentDevice else strings.revocable)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}
