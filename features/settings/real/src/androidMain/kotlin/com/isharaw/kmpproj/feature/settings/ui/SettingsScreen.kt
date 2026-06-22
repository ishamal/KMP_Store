package com.isharaw.kmpproj.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.isharaw.kmpproj.core.FeatureAction
import com.isharaw.kmpproj.core.FeatureKind
import com.isharaw.kmpproj.core.LocalNavigator
import com.isharaw.kmpproj.feature.settings.SettingsRepository

@Composable
fun SettingsScreen(
    repository: SettingsRepository,
    userEmail: String,
    actions: List<FeatureAction>,
    onLogout: () -> Unit,
) {
    var darkMode by remember { mutableStateOf(repository.darkMode) }
    var notifications by remember { mutableStateOf(repository.notifications) }
    val navigator = LocalNavigator.current

    // Settings decides how each non-common feature looks/where it sits, keyed by FeatureKind.
    val rebate = actions.firstOrNull { it.kind == FeatureKind.REBATE }
    val passwordReset = actions.firstOrNull { it.kind == FeatureKind.PASSWORD_RESET }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        Text("Signed in as", style = MaterialTheme.typography.labelMedium)
        Text(userEmail, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()

        // Rebate → card, before the toggles.
        rebate?.let { action ->
            Spacer(Modifier.height(8.dp))
            Card(
                onClick = { navigator.goTo(action.target) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    action.label,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        SettingToggle("Dark mode", darkMode) { repository.darkMode = it; darkMode = it }
        SettingToggle("Notifications", notifications) { repository.notifications = it; notifications = it }

        // Password reset → button, before Log out.
        passwordReset?.let { action ->
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { navigator.goTo(action.target) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(action.label)
            }
        }

        HorizontalDivider()
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text("Log out")
        }
    }
}

@Composable
private fun SettingToggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
