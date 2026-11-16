package com.unofficial.zai.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.unofficial.zai.BuildConfig
import com.unofficial.zai.CrashStore
import com.unofficial.zai.ui.components.AmbientBackground
import com.unofficial.zai.ui.components.BrandMark
import com.unofficial.zai.ui.components.CircleGlassButton
import com.unofficial.zai.ui.components.GlassCard
import com.unofficial.zai.ui.components.GradientButton
import com.unofficial.zai.ui.components.HairlineDivider
import com.unofficial.zai.ui.components.IconOrb
import com.unofficial.zai.ui.components.Pill
import com.unofficial.zai.ui.components.StatusDot
import com.unofficial.zai.ui.theme.BrandAqua
import com.unofficial.zai.ui.theme.BrandGradient
import com.unofficial.zai.ui.theme.BrandMint
import com.unofficial.zai.ui.theme.BrandViolet
import com.unofficial.zai.ui.web.WebViewModel
import kotlinx.coroutines.launch

const val ZAI_HOME_URL = "https://chat.z.ai/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebScreen(
    navController: NavController,
    vm: WebViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showMenu by remember { mutableStateOf(false) }
    var isOnline by remember { mutableStateOf(isNetworkAvailable(context)) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val clipData = data?.clipData
            val uris: Array<Uri>? = if (clipData != null) {
                Array(clipData.itemCount) { index -> clipData.getItemAt(index).uri }
            } else if (data?.data != null) {
                arrayOf(data.data!!)
            } else {
                null
            }
            vm.deliverFileSelection(uris)
        } else {
            vm.cancelFileSelection()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        vm.onPermissionResult(grants.isNotEmpty() && grants.values.all { it })
    }

    SideEffect {
        vm.launchFileChooser = { intent -> runCatching { filePicker.launch(intent) } }
        vm.launchPermissions = { perms -> runCatching { permissionLauncher.launch(perms) } }
    }

    // A renderer crash drops the dead view and bumps the generation; rebuilding the
    // AndroidView gives us a fresh renderer instead of a permanently blank page.
    LaunchedEffect(vm.webViewGeneration) {
        val fresh = vm.obtain(context)
        if (fresh.url == null) fresh.loadUrl(ZAI_HOME_URL)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                android.webkit.CookieManager.getInstance().flush()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOnline = true
            }

            override fun onLost(network: Network) {
                isOnline = isNetworkAvailable(context)
            }
        }
        runCatching { cm.registerDefaultNetworkCallback(callback) }
        onDispose { runCatching { cm.unregisterNetworkCallback(callback) } }
    }

    BackHandler(enabled = !showMenu && vm.canGoBack) {
        vm.webView?.goBack()
    }

    val dismissThen: (() -> Unit) -> Unit = { action ->
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            showMenu = false
            action()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(Modifier.fillMaxSize()) {
            ZaiHeader(
                canGoBack = vm.canGoBack,
                title = "z.ai",
                subtitle = vm.pageTitle.takeIf { it.isNotBlank() }
                    ?: "Unofficial GLM client",
                statusColor = when {
                    !isOnline -> MaterialTheme.colorScheme.error
                    vm.renderFailed || vm.blankHint -> Color(0xFFFFB020)
                    else -> BrandMint
                },
                onBack = { vm.webView?.goBack() },
                onNewChat = {
                    vm.resetPageState()
                    vm.webView?.loadUrl(ZAI_HOME_URL)
                },
                onMenu = { showMenu = true }
            )

            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.08f),
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
                    )
            ) {
                key(vm.webViewGeneration) {
                    AndroidView(
                        factory = { vm.obtain(context) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                val error = vm.errorMessage
                when {
                    !isOnline -> StatusOverlay(
                        icon = Icons.Rounded.WifiOff,
                        title = "You're offline",
                        body = "Reconnect to continue talking to GLM. Your session is kept safe.",
                        primaryLabel = "Try again",
                        onPrimary = {
                            isOnline = isNetworkAvailable(context)
                            if (isOnline) vm.webView?.reload()
                        }
                    )

                    error != null -> StatusOverlay(
                        icon = Icons.Rounded.CloudOff,
                        title = "Page failed to load",
                        body = error,
                        primaryLabel = "Reload page",
                        onPrimary = {
                            vm.errorMessage = null
                            vm.webView?.reload()
                        },
                        secondaryLabel = "Open in browser",
                        onSecondary = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://z.ai"))
                            )
                        }
                    )

                    vm.renderFailed -> StatusOverlay(
                        icon = Icons.Rounded.WarningAmber,
                        title = "This WebView is too old",
                        body = "z.ai needs a Chromium 115+ engine to lay out its interface. " +
                            (if (vm.webViewVersion.isNotEmpty())
                                "This device is using System WebView ${vm.webViewVersion}."
                            else "This device's WebView is below that line.") +
                            " Updating it usually fixes this in a minute.",
                        steps = listOf(
                            "Open the Play Store and search for \u201CAndroid System WebView\u201D.",
                            "Update it (and Chrome, if present), then come back here.",
                            "Restart this app \u2014 the chat will render normally."
                        ),
                        primaryLabel = "Update WebView",
                        onPrimary = { openWebViewInPlayStore(context) },
                        secondaryLabel = "Continue in browser",
                        onSecondary = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://z.ai"))
                            )
                        },
                        tertiaryLabel = "Show the page anyway",
                        onTertiary = { vm.renderFailed = false }
                    )

                    !vm.paintChecked -> LoadingSkeleton()
                }

                if (vm.blankHint && !vm.renderFailed && !vm.isLoading) {
                    BlankHintCard(
                        engine = vm.webViewVersion,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp),
                        onReload = {
                            vm.resetPageState()
                            vm.webView?.reload()
                        },
                        onDismiss = { vm.blankHint = false },
                        onBrowser = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://z.ai"))
                            )
                        }
                    )
                }

                if (vm.isLoading && vm.errorMessage == null && isOnline && !vm.renderFailed) {
                    LoadingLine(
                        Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                    )
                }

                if (BuildConfig.DEBUG) {
                    val crash = remember { CrashStore.last(context) }
                    if (crash != null) {
                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xF00B0F14))
                                .border(
                                    1.dp,
                                    Color(0xFFFF7A6E).copy(alpha = 0.5f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "CRASH LOG\n" + crash.take(700),
                                color = Color(0xFF7CF7B8),
                                fontSize = 8.sp,
                                lineHeight = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }

    if (showMenu) {
        ModalBottomSheet(
            onDismissRequest = { showMenu = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            scrimColor = Color.Black.copy(alpha = 0.62f),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = {
                Box(
                    Modifier
                        .padding(top = 12.dp, bottom = 4.dp)
                        .width(38.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                )
            }
        ) {
            MenuSheetContent(
                desktopMode = vm.desktopMode,
                onToggleDesktop = { enabled -> vm.setDesktopMode(context, enabled) },
                onNewChat = {
                    dismissThen {
                        vm.resetPageState()
                        vm.webView?.loadUrl(ZAI_HOME_URL)
                    }
                },
                onReload = {
                    dismissThen {
                        vm.resetPageState()
                        vm.webView?.reload()
                    }
                },
                onShare = {
                    dismissThen {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Unofficial z.ai client: https://github.com/sajjaddasti/Z.ai-Unofficial-App"
                            )
                        }
                        context.startActivity(Intent.createChooser(send, "Share app"))
                    }
                },
                onBrowser = {
                    dismissThen {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://z.ai")))
                    }
                },
                onSettings = { dismissThen { navController.navigate("settings") } },
                onAbout = { dismissThen { navController.navigate("settings") } }
            )
        }
    }

    val jsDialog = vm.dialog
    if (jsDialog != null) {
        var dialogText by remember(jsDialog) { mutableStateOf(jsDialog.defaultValue) }
        AlertDialog(
            onDismissRequest = {
                jsDialog.resolve(false)
                vm.dialog = null
            },
            title = { Text("z.ai") },
            text = {
                Column {
                    Text(jsDialog.message)
                    if (jsDialog.kind == WebViewModel.JsDialog.Kind.Prompt) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = dialogText,
                            onValueChange = { dialogText = it },
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        jsDialog.resolve(true, dialogText)
                        vm.dialog = null
                    }
                ) { Text("OK") }
            },
            dismissButton = if (jsDialog.kind == WebViewModel.JsDialog.Kind.Alert) {
                null
            } else {
                {
                    TextButton(
                        onClick = {
                            jsDialog.resolve(false)
                            vm.dialog = null
                        }
                    ) { Text("Cancel") }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

/* ------------------------------------------------------------------------------------------ */
/* Header                                                                                      */
/* ------------------------------------------------------------------------------------------ */

@Composable
private fun ZaiHeader(
    canGoBack: Boolean,
    title: String,
    subtitle: String,
    statusColor: Color,
    onBack: () -> Unit,
    onNewChat: () -> Unit,
    onMenu: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimatedVisibility(
            visible = canGoBack,
            enter = fadeIn(tween(180)) + expandHorizontally(tween(180)),
            exit = fadeOut(tween(140)) + shrinkHorizontally(tween(140))
        ) {
            CircleGlassButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                onClick = onBack
            )
        }

        val shape = RoundedCornerShape(22.dp)
        Row(
            modifier = Modifier
                .weight(1f)
                .height(60.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = shape,
                    ambientColor = BrandMint.copy(alpha = 0.16f),
                    spotColor = Color.Black.copy(alpha = 0.35f)
                )
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                        )
                    ),
                    shape = shape
                )
                .padding(start = 10.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandMark(size = 38.dp, corner = 13.dp)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(color = statusColor, size = 5.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            CircleGlassButton(
                icon = Icons.Rounded.Add,
                contentDescription = "New chat",
                onClick = onNewChat,
                size = 38.dp
            )
            Spacer(Modifier.width(6.dp))
            CircleGlassButton(
                icon = Icons.Rounded.MoreHoriz,
                contentDescription = "More",
                onClick = onMenu,
                size = 38.dp
            )
        }
    }
}

/* ------------------------------------------------------------------------------------------ */
/* Menu sheet                                                                                  */
/* ------------------------------------------------------------------------------------------ */

@Composable
private fun MenuSheetContent(
    desktopMode: Boolean,
    onToggleDesktop: (Boolean) -> Unit,
    onNewChat: () -> Unit,
    onReload: () -> Unit,
    onShare: () -> Unit,
    onBrowser: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark(size = 48.dp, corner = 16.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "z.ai",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Unofficial GLM client",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Pill(
                text = "v" + BuildConfig.VERSION_NAME,
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel("Shortcuts")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile(
                    icon = Icons.Rounded.AutoAwesome,
                    label = "New chat",
                    onClick = onNewChat,
                    modifier = Modifier.weight(1f)
                )
                QuickTile(
                    icon = Icons.Rounded.Refresh,
                    label = "Reload",
                    onClick = onReload,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile(
                    icon = Icons.Rounded.Share,
                    label = "Share app",
                    onClick = onShare,
                    modifier = Modifier.weight(1f)
                )
                QuickTile(
                    icon = Icons.Rounded.OpenInNew,
                    label = "In browser",
                    onClick = onBrowser,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                    RoundedCornerShape(22.dp)
                )
        ) {
            MenuToggleRow(
                icon = Icons.Rounded.Language,
                title = "Desktop site",
                subtitle = "Load the desktop layout with a Chrome UA",
                checked = desktopMode,
                onCheckedChange = onToggleDesktop
            )
            HairlineDivider(Modifier.padding(horizontal = 14.dp))
            MenuRow(
                icon = Icons.Rounded.Settings,
                title = "Settings",
                subtitle = "Appearance, security, session",
                onClick = onSettings
            )
            HairlineDivider(Modifier.padding(horizontal = 14.dp))
            MenuRow(
                icon = Icons.Rounded.Info,
                title = "About & diagnostics",
                subtitle = "Version, WebView health, source code",
                onClick = onAbout
            )
        }

        Text(
            text = "Unofficial client \u00B7 not affiliated with z.ai",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun QuickTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .height(98.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        IconOrb(icon = icon, size = 36.dp)
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconOrb(icon = icon, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun MenuToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconOrb(icon = icon, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF04150F),
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

/* ------------------------------------------------------------------------------------------ */
/* Blank-page hint                                                                             */
/* ------------------------------------------------------------------------------------------ */

@Composable
private fun BlankHintCard(
    engine: String,
    modifier: Modifier = Modifier,
    onReload: () -> Unit,
    onDismiss: () -> Unit,
    onBrowser: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 470.dp)
            .shadow(18.dp, shape, spotColor = Color.Black.copy(alpha = 0.55f))
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), shape)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconOrb(icon = Icons.Rounded.WarningAmber, size = 34.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Page looks empty",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    if (engine.isNotBlank())
                        "z.ai loaded, but WebView $engine painted nothing."
                    else
                        "z.ai loaded, but the page painted nothing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(6.dp))
            CircleGlassButton(
                icon = Icons.Rounded.Close,
                contentDescription = "Dismiss",
                onClick = onDismiss,
                size = 32.dp
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GradientButton(
                text = "Reload",
                onClick = onReload,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onBrowser,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
            ) {
                Text(
                    "Open in browser",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------------------------ */
/* Overlays                                                                                    */
/* ------------------------------------------------------------------------------------------ */

@Composable
private fun StatusOverlay(
    icon: ImageVector,
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    steps: List<String> = emptyList(),
    tertiaryLabel: String? = null,
    onTertiary: (() -> Unit)? = null
) {
    Box(Modifier.fillMaxSize()) {
        AmbientBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .shadow(
                        elevation = 22.dp,
                        shape = RoundedCornerShape(26.dp),
                        ambientColor = BrandMint.copy(alpha = 0.35f),
                        spotColor = BrandViolet.copy(alpha = 0.30f)
                    )
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(BrandMint.copy(alpha = 0.45f), Color.Transparent)
                        ),
                        RoundedCornerShape(26.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 360.dp)
            )

            if (steps.isNotEmpty()) {
                Spacer(Modifier.height(22.dp))
                GlassCard(
                    corner = 22.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 380.dp)
                ) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        steps.forEachIndexed { index, step ->
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(BrandGradient)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF04150F)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            GradientButton(
                text = primaryLabel,
                onClick = onPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp)
            )

            if (secondaryLabel != null && onSecondary != null) {
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = onSecondary) {
                    Text(
                        secondaryLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (tertiaryLabel != null && onTertiary != null) {
                TextButton(onClick = onTertiary) {
                    Text(
                        tertiaryLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

/* ------------------------------------------------------------------------------------------ */
/* Loading                                                                                     */
/* ------------------------------------------------------------------------------------------ */

@Composable
private fun LoadingLine(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "topline")
    val x by transition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1250, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "x"
    )
    BoxWithConstraints(
        modifier
            .height(2.5.dp)
            .clip(CircleShape)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        )
        Box(
            Modifier
                .offset(x = maxWidth * x)
                .width(maxWidth * 0.42f)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, BrandMint, BrandAqua, Color.Transparent)
                    )
                )
        )
    }
}

@Composable
private fun LoadingSkeleton() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val bar = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.16f)

    Box(Modifier.fillMaxSize()) {
        AmbientBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(Modifier.alpha(alpha)) {
                BrandMark(size = 62.dp, corner = 20.dp)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Connecting to z.ai",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Warming up GLM\u2026",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(34.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(3) { index ->
                    Row(Modifier.fillMaxWidth()) {
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(bar)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(if (index == 2) 0.45f else 0.62f)
                                    .height(12.dp)
                                    .clip(CircleShape)
                                    .background(bar)
                            )
                            Box(
                                Modifier
                                    .fillMaxWidth(if (index == 1) 1f else 0.86f)
                                    .height(10.dp)
                                    .clip(CircleShape)
                                    .background(bar.copy(alpha = alpha * 0.10f))
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------------------------------ */

private fun openWebViewInPlayStore(context: Context) {
    val pkg = runCatching {
        androidx.webkit.WebViewCompat.getCurrentWebViewPackage(context)?.packageName
    }.getOrNull() ?: "com.google.android.webview"
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.onFailure {
        runCatching {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

private fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
