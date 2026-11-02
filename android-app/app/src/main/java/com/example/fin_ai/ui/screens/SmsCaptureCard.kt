package com.example.fin_ai.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fin_ai.sms.SmsCapture
import com.example.fin_ai.ui.theme.GreenCredit
import com.example.fin_ai.ui.theme.OutlineViolet
import com.example.fin_ai.ui.theme.SoftRed
import com.example.fin_ai.ui.theme.VioletLight
import com.example.fin_ai.ui.theme.VioletPrimary
import com.example.fin_ai.ui.viewmodel.SmsSetupStep
import com.example.fin_ai.ui.viewmodel.SmsViewModel

private val Amber = Color(0xFFF59E0B)

@Composable
fun SmsCaptureCard(onImported: () -> Unit, viewModel: SmsViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var showBlocked by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) { viewModel.onImported = onImported }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.onPermissionResult() }

    LaunchedEffect(state.step) {
        if (state.step == SmsSetupStep.REQUEST_PERMISSION) {
            permissionLauncher.launch(SmsCapture.REQUIRED_PERMISSIONS.toTypedArray())
        }
    }
    when (state.step) {
        SmsSetupStep.ENTER_PHONE -> PhoneDialog(
            working = state.working,
            error = state.phoneError,
            onSubmit = viewModel::linkPhone,
            onDismiss = viewModel::cancelSetup
        )
        SmsSetupStep.SWITCH_OWNER -> AlertDialog(
            onDismissRequest = viewModel::cancelSetup,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            title = { Text("Use SMS capture with this account?", fontSize = 18.sp) },
            text = {
                Text(
                    "Bank SMS on this phone are currently captured into another FinAI account. " +
                        "Only one account per phone can capture them, so that account will stop " +
                        "receiving new SMS transactions. Messages it already imported are not copied here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            },
            confirmButton = { TextButton(onClick = viewModel::confirmSwitchOwner) { Text("Switch to this account", color = VioletLight) } },
            dismissButton = { TextButton(onClick = viewModel::cancelSetup) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        )
        else -> Unit
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, OutlineViolet, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Sms, contentDescription = null, tint = VioletLight, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Bank SMS auto-capture",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    "Verified bank alerts are added automatically. Fakes are blocked.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = state.enabled,
                onCheckedChange = { on -> if (on) viewModel.requestEnable() else viewModel.disable() },
                colors = SwitchDefaults.colors(checkedTrackColor = VioletPrimary)
            )
        }

        if (state.ownedByOtherAccount) {
            Spacer(Modifier.height(10.dp))
            Text(
                "SMS capture on this phone is linked to another FinAI account, so this account can't import " +
                    "its messages. Turn the switch on to move it here.",
                color = Amber,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        state.linkedPhone?.let { phone ->
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Linked number: ${maskPhone(phone)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                SmallAction("Unlink", enabled = !state.working, tint = MaterialTheme.colorScheme.onSurfaceVariant, onClick = viewModel::unlinkPhone)
            }
        }

        if (state.enabled) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SmallAction("Import last 30 days", enabled = !state.working) { viewModel.importInbox(30) }
                Spacer(Modifier.width(12.dp))
                if (state.working) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = VioletLight)
                }
            }
        }

        state.status?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 16.sp)
        }

        if (state.review.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            SectionTitle(Icons.Filled.HelpOutline, Amber, "Needs your review (${state.review.size})")
            Text(
                "These look like bank alerts but come from a sender that isn't on our verified bank list. " +
                    "Trust the sender only if it's your bank.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            state.review.take(5).forEach { item ->
                Spacer(Modifier.height(8.dp))
                MessageRow(
                    title = "${item.header}  ·  ${if (item.tx.type == "debit") "-" else "+"}₹%,.2f".format(item.tx.amount),
                    body = item.preview,
                    tint = Amber
                ) {
                    SmallAction("Trust ${item.header}") { viewModel.trust(item.header) }
                    Spacer(Modifier.width(10.dp))
                    SmallAction("Ignore", tint = MaterialTheme.colorScheme.onSurfaceVariant) { viewModel.dismissReview(item) }
                }
            }
        }

        if (state.blocked.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showBlocked = !showBlocked }
            ) {
                SectionTitle(Icons.Filled.GppBad, SoftRed, "Fake / suspicious SMS blocked (${state.blocked.size})")
                Spacer(Modifier.weight(1f))
                Text(if (showBlocked) "Hide" else "Show", color = VioletLight, fontSize = 12.sp)
            }
            if (showBlocked) {
                state.blocked.forEach { item ->
                    Spacer(Modifier.height(8.dp))
                    MessageRow(title = item.sender, body = item.preview, footer = item.reason, tint = SoftRed)
                }
                Spacer(Modifier.height(8.dp))
                SmallAction("Clear list", tint = MaterialTheme.colorScheme.onSurfaceVariant, onClick = viewModel::clearBlocked)
            }
        } else if (state.enabled && state.status != null) {
            Spacer(Modifier.height(10.dp))
            Text("No fake bank messages detected.", color = GreenCredit, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SectionTitle(icon: ImageVector, tint: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = tint, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun MessageRow(
    title: String,
    body: String,
    tint: Color,
    footer: String? = null,
    actions: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.07f))
            .border(1.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        footer?.let { Text(it, color = tint, fontSize = 12.sp, lineHeight = 16.sp) }
        actions?.let {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) { it() }
        }
    }
}

@Composable
private fun SmallAction(label: String, enabled: Boolean = true, tint: Color = VioletLight, onClick: () -> Unit) {
    Text(
        label,
        color = if (enabled) tint else tint.copy(alpha = 0.4f),
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}

@Composable
private fun PhoneDialog(working: Boolean, error: String?, onSubmit: (String) -> Unit, onDismiss: () -> Unit) {
    var number by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        title = { Text("Link your mobile number", fontSize = 18.sp) },
        text = {
            Column {
                Text(
                    "Bank SMS from this number will be captured into this account only. " +
                        "A number can be linked to just one FinAI account, so your other accounts " +
                        "won't get duplicate transactions.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(14.dp))
                FinTextField(value = number, onValueChange = { number = it.take(15) }, placeholder = "10-digit mobile number", numeric = true)
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = SoftRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(number) }, enabled = !working && number.isNotBlank()) {
                if (working) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = VioletLight)
                else Text("Link number", color = VioletLight)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    )
}

/** "+919876543210" -> "+91 98•••••210" */
private fun maskPhone(phone: String): String {
    val digits = phone.removePrefix("+91")
    return if (digits.length == 10) "+91 ${digits.take(2)}•••••${digits.takeLast(3)}" else phone
}
