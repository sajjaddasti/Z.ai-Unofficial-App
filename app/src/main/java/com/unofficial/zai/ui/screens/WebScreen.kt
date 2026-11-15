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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val lifecycleOwner = LocalLifecycleOwner.current

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
        vm.launchFileChooser = { intent ->
            runCatching { filePicker.launch(intent) }
        }
        vm.launchPermissions = { perms ->
            runCatching { permissionLauncher.launch(perms) }
        }
    }

    val webView = remember { vm.obtain(context) }

    LaunchedEffect(Unit) {
        if (webView.url == null) webView.loadUrl(ZAI_HOME_URL)
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

    BackHandler(enabled = drawerState.isOpen || vm.canGoBack) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            vm.webView?.goBack()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                DrawerHeader()
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                DrawerItem(
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    label = "New chat"
                ) {
                    scope.launch { drawerState.close() }
                    vm.webView?.loadUrl(ZAI_HOME_URL)
                }
                DrawerItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = "Home"
                ) {
                    scope.launch { drawerState.close() }
                    vm.webView?.loadUrl(ZAI_HOME_URL)
                }
                DrawerItem(
                    icon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                    label = "Reload page"
                ) {
                    scope.launch { drawerState.close() }
                    vm.errorMessage = null
                    vm.webView?.reload()
                }
                DrawerItem(
                    icon = { Icon(Icons.Filled.OpenInNew, contentDescription = null) },
                    label = "Open in browser"
                ) {
                    scope.launch { drawerState.close() }
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://z.ai"))
                    )
                }
                DrawerItem(
                    icon = { Icon(Icons.Filled.Share, contentDescription = null) },
                    label = "Share app"
                ) {
                    scope.launch { drawerState.close() }
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Unofficial z.ai client: https://github.com/sajjaddasti/Z.ai-Unofficial-App"
                        )
                    }
                    context.startActivity(Intent.createChooser(send, "Share app"))
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                DrawerItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = "Settings"
                ) {
                    scope.launch { drawerState.close() }
                    navController.navigate("settings")
                }
                DrawerItem(
                    icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                    label = "About"
                ) {
                    scope.launch { drawerState.close() }
                    navController.navigate("settings")
                }

                Spacer(Modifier.weight(1f))
                Text(
                    text = "Unofficial client · not affiliated with z.ai",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BrandBadge()
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    "z.ai",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "GLM chat",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { vm.webView?.loadUrl(ZAI_HOME_URL) }) {
                            Icon(Icons.Filled.Add, contentDescription = "New chat")
                        }
                        IconButton(
                            onClick = {
                                vm.errorMessage = null
                                vm.webView?.reload()
                            }
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                AndroidView(
                    factory = { webView },
                    modifier = Modifier.fillMaxSize()
                )

                val error = vm.errorMessage
                if (!isOnline) {
                    StatusOverlay(
                        emoji = "📡",
                        title = "You're offline",
                        body = "Check your connection and try again.",
                        primaryLabel = "Retry",
                        onPrimary = {
                            isOnline = isNetworkAvailable(context)
                            if (isOnline) vm.webView?.reload()
                        },
                        onSecondary = null
                    )
                } else if (error != null) {
                    StatusOverlay(
                        emoji = "⚠️",
                        title = "Page failed to load",
                        body = error,
                        primaryLabel = "Retry",
                        onPrimary = {
                            vm.errorMessage = null
                            vm.webView?.reload()
                        },
                        onSecondary = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://z.ai"))
                            )
                        }
                    )
                } else if (vm.progress in 0..8) {
                    ShimmerSkeleton()
                }

                if (vm.isLoading && vm.errorMessage == null && isOnline) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.TopCenter),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandBadge() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Z",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DrawerHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BrandBadge()
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                "z.ai",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Unofficial client",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DrawerItem(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = icon,
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

@Composable
private fun StatusOverlay(
    emoji: String,
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onSecondary: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineLarge)
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Button(onClick = onPrimary) { Text(primaryLabel) }
            if (onSecondary != null) {
                OutlinedButton(onClick = onSecondary) { Text("Open in browser") }
            }
        }
    }
}

@Composable
private fun ShimmerSkeleton() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val barColor = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(2) { row ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(barColor)
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .height(14.dp)
                        .fillMaxWidth(0.4f)
                        .clip(RoundedCornerShape(7.dp))
                        .background(barColor)
                )
            }
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth(if (row % 2 == 0) 0.95f else 0.75f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(barColor)
            )
            Box(
                Modifier
                    .fillMaxWidth(0.6f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(barColor)
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

private fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
