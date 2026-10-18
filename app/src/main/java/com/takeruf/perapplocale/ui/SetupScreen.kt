package com.takeruf.perapplocale.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.takeruf.perapplocale.R
import com.takeruf.perapplocale.root.RootRepository
import com.takeruf.perapplocale.root.RootState
import com.takeruf.perapplocale.shizuku.ShizukuState
import kotlinx.coroutines.launch

const val ADB_START_COMMAND: String =
    "adb shell sh /storage/emulated/0/Android/data/moe.shizuku.privileged.api/start.sh"

/**
 * The whole Shizuku story on one screen: where the app currently stands, the four steps that get
 * it to READY, and the two things people trip over (reboots, and the wireless-debugging pairing).
 */
@Composable
fun SetupScreen(
    shizuku: ShizukuState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRequestPermission: () -> Unit,
    onRecheck: () -> Unit,
    onOpenShizukuApp: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onCopy: (String) -> Unit,
) {
    DocScaffold(
        title = stringResource(R.string.shizuku_setup_guide),
        subtitle = stringResource(R.string.setup_subtitle),
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) {
        CurrentStatus(
            shizuku = shizuku,
            onRequestPermission = onRequestPermission,
            onRecheck = onRecheck,
            onOpenShizukuApp = onOpenShizukuApp,
        )

        DocSection(stringResource(R.string.why_shizuku_title)) {
            DocBody(stringResource(R.string.why_shizuku_body))
        }

        DocSection(stringResource(R.string.steps_title)) {
            DocStep(
                1,
                stringResource(R.string.setup_install_title),
                stringResource(R.string.setup_install_body),
            )
            DocStep(
                2,
                stringResource(R.string.setup_on_device_title),
                stringResource(R.string.setup_on_device_body),
            )
            DocStep(
                3,
                stringResource(R.string.setup_computer_title),
                stringResource(R.string.setup_computer_body),
            )
            DocCommand(ADB_START_COMMAND) { onCopy(ADB_START_COMMAND) }
            DocStep(
                4,
                stringResource(R.string.setup_grant_title),
                stringResource(R.string.setup_grant_body),
            )
        }

        DocCallout(
            stringResource(R.string.setup_reboot_title),
            stringResource(R.string.setup_reboot_body),
        )

        DocSection(stringResource(R.string.troubleshooting_title)) {
            DocBullet(
                stringResource(R.string.troubleshooting_wireless_title),
                stringResource(R.string.troubleshooting_wireless_body),
            )
            DocBullet(
                stringResource(R.string.troubleshooting_permission_title),
                stringResource(R.string.troubleshooting_permission_body),
            )
            DocBullet(
                stringResource(R.string.troubleshooting_battery_title),
                stringResource(R.string.troubleshooting_battery_body),
            )
        }

        DocSection(stringResource(R.string.root_section_title)) {
            DocBody(stringResource(R.string.root_section_body))
            RootRequestCard()
        }

        Row(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = { onOpenUrl("https://shizuku.rikka.app/guide/setup/") }) {
                Text(stringResource(R.string.shizuku_documentation))
            }
        }
    }
}

@Composable
private fun CurrentStatus(
    shizuku: ShizukuState,
    onRequestPermission: () -> Unit,
    onRecheck: () -> Unit,
    onOpenShizukuApp: () -> Unit,
) {
    val ready = shizuku == ShizukuState.READY
    val (title, body) = when (shizuku) {
        ShizukuState.READY ->
            stringResource(R.string.connected) to stringResource(R.string.connected_body)
        ShizukuState.PERMISSION_REQUIRED ->
            stringResource(R.string.waiting_for_permission) to stringResource(R.string.waiting_for_permission_body)
        ShizukuState.NOT_RUNNING ->
            stringResource(R.string.shizuku_installed_not_running) to stringResource(R.string.shizuku_installed_not_running_body)
        ShizukuState.NOT_INSTALLED ->
            stringResource(R.string.shizuku_not_installed) to stringResource(R.string.shizuku_not_installed_short_body)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (ready) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.errorContainer,
            contentColor = if (ready) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (shizuku == ShizukuState.PERMISSION_REQUIRED) {
                    TextButton(onClick = onRequestPermission) { Text(stringResource(R.string.grant_permission)) }
                }
                TextButton(onClick = onOpenShizukuApp) { Text(stringResource(R.string.open_shizuku)) }
                TextButton(onClick = onRecheck) { Text(stringResource(R.string.recheck)) }
            }
        }
    }
}

/**
 * A self-contained root request: it reads [RootRepository.state] and asks for root on tap.
 *
 * It owns its own state on purpose. Root is an optional extra on top of the Shizuku flow, so
 * nothing here is wired into the ViewModel or the app list — granting or refusing root changes
 * exactly one card on this screen and nothing else.
 */
@Composable
private fun RootRequestCard() {
    val state by RootRepository.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var requesting by remember { mutableStateOf(false) }

    val (title, body) = when (state) {
        RootState.UNKNOWN ->
            stringResource(R.string.root_unknown) to stringResource(R.string.root_unknown_body)
        RootState.GRANTED ->
            stringResource(R.string.root_granted) to stringResource(R.string.root_granted_body)
        RootState.DENIED ->
            stringResource(R.string.root_denied) to stringResource(R.string.root_denied_body)
        RootState.NOT_AVAILABLE ->
            stringResource(R.string.root_not_available) to stringResource(R.string.root_not_available_body)
    }
    val granted = state == RootState.GRANTED

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (granted) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (granted) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = {
                        scope.launch {
                            requesting = true
                            RootRepository.request()
                            requesting = false
                        }
                    },
                    enabled = !requesting,
                ) {
                    Text(
                        stringResource(
                            if (requesting) R.string.root_requesting else R.string.root_request,
                        ),
                    )
                }
            }
        }
    }
}
