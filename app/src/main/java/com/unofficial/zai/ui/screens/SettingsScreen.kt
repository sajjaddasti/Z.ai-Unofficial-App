package com.unofficial.zai.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.webkit.WebViewCompat
import com.unofficial.zai.BuildConfig
import com.unofficial.zai.ui.components.AccentRule
import com.unofficial.zai.ui.components.AmbientBackground
import com.unofficial.zai.ui.components.CircleGlassButton
import com.unofficial.zai.ui.components.GlassCard
import com.unofficial.zai.ui.components.HairlineDivider
import com.unofficial.zai.ui.components.IconOrb
import com.unofficial.zai.ui.components.Pill
import com.unofficial.zai.ui.theme.BrandGradient
import com.unofficial.zai.ui.theme.BrandMint
import com.unofficial.zai.ui.theme.ThemeMode
import com.unofficial.zai.ui.web.WebViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: WebViewModel,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val context = LocalContext.current
    var biometricEnabled by remember { mutableStateOf(false) }
    val biometricAvailable = remember {
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }
    val webViewPkg = remember {
        runCatching {
            WebViewCompat.getCurrentWebViewPackage(context)?.packageName
        }.getOrNull() ?: "Unknown"
    }
    val webViewVer = remember {
        runCatching {
            WebViewCompat.getCurrentWebViewPackage(context)?.versionName
        }.getOrNull() ?: vm.webViewVersion.ifBlank { "Unknown" }
    }
    val webViewOutdated = vm.webViewMajor in 1..114

    Box(Modifier.fillMaxSize()) {
        AmbientBackground()

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(top = 8.dp, bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleGlassButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack
                )
                Spacer(Modifier.weight(1f))
                Pill(
                    text = "v" + BuildConfig.VERSION_NAME,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(22.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Appearance, security and diagnostics.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            AccentRule()
            Spacer(Modifier.height(26.dp))

            SettingsSection(title = "Appearance") {
                SettingRow(
                    icon = Icons.Rounded.Palette,
                    title = "Theme",
                    subtitle = "Follow the system or pick a side",
                    stacked = true
                )
                Spacer(Modifier.height(12.dp))
                ThemeSegmented(selected = themeMode, onSelect = onThemeModeChange)
            }

            Spacer(Modifier.height(18.dp))

            SettingsSection(title = "Security") {
                SettingRow(
                    icon = Icons.Rounded.Fingerprint,
                    title = "Biometric lock",
                    subtitle = if (biometricAvailable)
                        "Require fingerprint or face to open the app"
                    else "Not available on this device",
                    trailing = {
                        Switch(
                            checked = biometricEnabled,
                            enabled = biometricAvailable,
                            onCheckedChange = { enabled ->
                                if (enabled && biometricAvailable) {
                                    authenticate(context) { ok ->
                                        if (ok) biometricEnabled = true
                                    }
                                } else {
                                    biometricEnabled = false
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF04150F),
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                uncheckedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                )
            }

            Spacer(Modifier.height(18.dp))

            SettingsSection(title = "Session") {
                SettingRow(
                    icon = Icons.Rounded.Logout,
                    title = "Log out",
                    subtitle = "Clear stored cookies and sign in again",
                    trailing = {
                        TextButton(
                            onClick = {
                                CookieManager.getInstance().removeAllCookies(null)
                                CookieManager.getInstance().flush()
                                Toast.makeText(context, "Cookies cleared", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Clear", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            }

            Spacer(Modifier.height(18.dp))

            SettingsSection(title = "Diagnostics") {
                SettingRow(
                    icon = Icons.Rounded.PhoneAndroid,
                    title = "System WebView",
                    subtitle = webViewPkg,
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Pill(
                                text = webViewVer,
                                color = if (webViewOutdated)
                                    Color(0xFFFFB020).copy(alpha = 0.16f)
                                else BrandMint.copy(alpha = 0.16f),
                                contentColor = if (webViewOutdated)
                                    Color(0xFFB86A00)
                                else MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { openWebViewStore(context) }
                            ) {
                                Icon(
                                    Icons.Rounded.OpenInNew,
                                    contentDescription = "Update WebView",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                )

                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (webViewOutdated) Color(0xFFFFB020).copy(alpha = 0.10f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (webViewOutdated) Icons.Rounded.BugReport
                        else Icons.Rounded.Verified,
                        contentDescription = null,
                        tint = if (webViewOutdated) Color(0xFFB86A00)
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(16.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (webViewOutdated)
                            "This engine is older than Chromium 115, which z.ai needs to render. " +
                                "Update \u201CAndroid System WebView\u201D from the Play Store, then " +
                                "restart the app."
                        else
                            "The engine that draws z.ai. Keep it updated for the smoothest " +
                                "chat experience.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (webViewOutdated) Color(0xFFB86A00)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Terminal,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Web console",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { vm.consoleLines.clear() }) {
                        Text("Clear", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 92.dp, max = 220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF07100C))
                        .border(
                            1.dp,
                            BrandMint.copy(alpha = 0.18f),
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (vm.consoleLines.isEmpty()) {
                            Text(
                                text = "No messages yet.",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF6B7F76)
                            )
                        } else {
                            vm.consoleLines.takeLast(14).forEach { line ->
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF8FE7BE)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            SettingsSection(title = "About") {
                SettingRow(
                    icon = Icons.Rounded.Code,
                    title = "Source code",
                    subtitle = "github.com/sajjaddasti/Z.ai-Unofficial-App",
                    modifier = Modifier.clickable {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/sajjaddasti/Z.ai-Unofficial-App")
                            )
                        )
                    },
                    trailing = {
                        Icon(
                            Icons.Rounded.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                HairlineDivider(Modifier.padding(vertical = 12.dp))
                SettingRow(
                    icon = Icons.Rounded.BrightnessAuto,
                    title = "App version",
                    subtitle = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Unofficial client for z.ai. Not affiliated with, endorsed by, or " +
                        "supported by z.ai. Your login happens on the real z.ai website and " +
                        "stays inside this app's private WebView storage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------------------------ */

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 6.dp)
        )
        GlassCard(corner = 26.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    stacked: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconOrb(icon = icon, size = 40.dp)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (trailing != null && !stacked) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
private fun ThemeSegmented(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    val options = listOf(
        Triple(ThemeMode.System, "Auto", Icons.Rounded.BrightnessAuto),
        Triple(ThemeMode.Light, "Light", Icons.Rounded.LightMode),
        Triple(ThemeMode.Dark, "Dark", Icons.Rounded.DarkMode)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (mode, label, icon) ->
            val active = mode == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (active) Brush.horizontalGradient(BrandGradient)
                        else Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.Transparent)
                        )
                    )
                    .clickable { onSelect(mode) },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (active) Color(0xFF04150F)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) Color(0xFF04150F)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun openWebViewStore(context: Context) {
    val pkg = runCatching {
        WebViewCompat.getCurrentWebViewPackage(context)?.packageName
    }.getOrNull() ?: "com.google.android.webview"
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
        )
    }.onFailure {
        runCatching {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
                )
            )
        }
    }
}

private fun authenticate(context: Context, onResult: (Boolean) -> Unit) {
    val activity = context as? FragmentActivity ?: return
    val executor = ContextCompat.getMainExecutor(context)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onResult(true)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onResult(false)
            }
        }
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock z.ai")
        .setSubtitle("Confirm your identity")
        .setNegativeButtonText("Cancel")
        .build()
    prompt.authenticate(info)
}
