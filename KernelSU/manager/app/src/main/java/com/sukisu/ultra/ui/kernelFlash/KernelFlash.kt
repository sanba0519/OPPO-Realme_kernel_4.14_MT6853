package com.sukisu.ultra.ui.kernelFlash

import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.R
import com.sukisu.ultra.ui.component.KeyEventBlocker
import com.sukisu.ultra.ui.util.reboot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.sukisu.ultra.ui.kernelFlash.state.*
import com.sukisu.ultra.ui.navigation3.LocalNavigator
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.FileDownloads
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author ShirkNeko
 * @date 2025/5/31.
 */
private object KernelFlashStateHolder {
    var currentState: HorizonKernelState? = null
    var currentUri: Uri? = null
    var currentSlot: String? = null
    var currentKpmPatchEnabled: Boolean = false
    var currentKpmUndoPatch: Boolean = false
    var isFlashing = false
    
    fun clear() {
        currentState = null
        currentUri = null
        currentSlot = null
        currentKpmPatchEnabled = false
        currentKpmUndoPatch = false
        isFlashing = false
    }
}

@Composable
fun KernelFlashScreen(
    kernelUri: Uri,
    selectedSlot: String? = null,
    kpmPatchEnabled: Boolean = false,
    kpmUndoPatch: Boolean = false
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var logText by rememberSaveable { mutableStateOf("") }
    var showFloatAction by rememberSaveable { mutableStateOf(false) }
    val logContent = rememberSaveable { StringBuilder() }
    val horizonKernelState = remember {
        if (KernelFlashStateHolder.currentState != null &&
            KernelFlashStateHolder.currentUri == kernelUri &&
            KernelFlashStateHolder.currentSlot == selectedSlot &&
            KernelFlashStateHolder.currentKpmPatchEnabled == kpmPatchEnabled &&
            KernelFlashStateHolder.currentKpmUndoPatch == kpmUndoPatch) {
            KernelFlashStateHolder.currentState!!
        } else {
            HorizonKernelState().also {
                KernelFlashStateHolder.currentState = it
                KernelFlashStateHolder.currentUri = kernelUri
                KernelFlashStateHolder.currentSlot = selectedSlot
                KernelFlashStateHolder.currentKpmPatchEnabled = kpmPatchEnabled
                KernelFlashStateHolder.currentKpmUndoPatch = kpmUndoPatch
                KernelFlashStateHolder.isFlashing = false
            }
        }
    }

    val flashState by horizonKernelState.state.collectAsState()
    val activity = LocalActivity.current

    val onFlashComplete = {
        showFloatAction = true
        KernelFlashStateHolder.isFlashing = false
    }

    val flashComplete = stringResource(R.string.horizon_flash_complete)
    
    // 如果是从外部打开的内核刷写，延迟1.5秒后自动退出
    LaunchedEffect(flashState.isCompleted, flashState.error) {
        if (flashState.isCompleted && flashState.error.isEmpty()) {
            val intent = activity?.intent
            val isFromExternalIntent = intent?.action?.let { action ->
                action == Intent.ACTION_VIEW ||
                action == Intent.ACTION_SEND ||
                action == Intent.ACTION_SEND_MULTIPLE
            } ?: false

            if (isFromExternalIntent) {
                delay(1500)
                KernelFlashStateHolder.clear()
                activity.finish()
            }
        }
    }

    // 开始刷写
    LaunchedEffect(Unit) {
        if (!KernelFlashStateHolder.isFlashing && !flashState.isCompleted && flashState.error.isEmpty()) {
            withContext(Dispatchers.IO) {
                KernelFlashStateHolder.isFlashing = true
                val worker = HorizonKernelWorker(
                    context = context,
                    state = horizonKernelState,
                    slot = selectedSlot,
                    kpmPatchEnabled = kpmPatchEnabled,
                    kpmUndoPatch = kpmUndoPatch
                )
                worker.uri = kernelUri
                worker.setOnFlashCompleteListener(onFlashComplete)
                worker.start()

                // 监听日志更新
                while (flashState.error.isEmpty()) {
                    if (flashState.logs.isNotEmpty()) {
                        logText = flashState.logs.joinToString("\n")
                        logContent.clear()
                        logContent.append(logText)
                    }
                    delay(100)
                }

                if (flashState.error.isNotEmpty()) {
                    logText += "\n${flashState.error}\n"
                    logContent.append("\n${flashState.error}\n")
                    KernelFlashStateHolder.isFlashing = false
                }
            }
        } else {
            logText = flashState.logs.joinToString("\n")
            if (flashState.error.isNotEmpty()) {
                logText += "\n${flashState.error}\n"
            } else if (flashState.isCompleted) {
                logText += "\n$flashComplete\n\n\n"
                showFloatAction = true
            }
        }
    }

    val onBack: () -> Unit = {
        if (!flashState.isFlashing || flashState.isCompleted || flashState.error.isNotEmpty()) {
            if (flashState.isCompleted || flashState.error.isNotEmpty()) {
                KernelFlashStateHolder.clear()
            }
            navigator.pop()
        }
    }

    // 清理状态
    DisposableEffect(Unit) {
        onDispose {
            if (flashState.isCompleted || flashState.error.isNotEmpty()) {
                KernelFlashStateHolder.clear()
            }
        }
    }

    BackHandler {
        onBack()
    }

    KeyEventBlocker {
        it.key == Key.VolumeDown || it.key == Key.VolumeUp
    }

    Scaffold(
        topBar = {
            TopBar(
                flashState = flashState,
                onBack = onBack,
                onSave = {
                    scope.launch {
                        val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
                        val date = format.format(Date())
                        val file = File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            "KernelSU_kernel_flash_log_${date}.log"
                        )
                        file.writeText(logContent.toString())
                    }
                }
            )
        },
        floatingActionButton = {
            if (showFloatAction) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                reboot()
                            }
                        }
                    },
                    modifier = Modifier.padding(bottom = 20.dp, end = 20.dp)
                ) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = stringResource(id = R.string.reboot)
                    )
                }
            }
        },
        popupHost = { }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .scrollEndHaptic(),
        ) {
            FlashProgressIndicator(flashState, kpmPatchEnabled, kpmUndoPatch)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                LaunchedEffect(logText) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = logText,
                    fontFamily = FontFamily.Monospace,
                    color = colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun FlashProgressIndicator(
    flashState: FlashState,
    kpmPatchEnabled: Boolean = false,
    kpmUndoPatch: Boolean = false
) {
    val statusColor = when {
        flashState.error.isNotEmpty() -> colorScheme.error
        flashState.isCompleted -> colorScheme.primary
        else -> colorScheme.primary
    }

    val progress = animateFloatAsState(
        targetValue = flashState.progress.coerceIn(0f, 1f),
        label = "FlashProgress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = when {
                        flashState.error.isNotEmpty() -> stringResource(R.string.flash_failed)
                        flashState.isCompleted -> stringResource(R.string.flash_success)
                        else -> stringResource(R.string.flashing)
                    },
                    fontSize = MiuixTheme.textStyles.title4.fontSize,
                    fontWeight = FontWeight.Medium,
                    color = statusColor
                )

                when {
                    flashState.error.isNotEmpty() -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = colorScheme.error
                        )
                    }
                    flashState.isCompleted -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = colorScheme.primary
                        )
                    }
                }
            }

            // KPM状态显示
            if (kpmPatchEnabled || kpmUndoPatch) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (kpmUndoPatch) stringResource(R.string.kpm_undo_patch_mode)
                    else stringResource(R.string.kpm_patch_mode),
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                    color = colorScheme.onSurfaceVariantSummary
                )
            }

            if (flashState.currentStep.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = flashState.currentStep,
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                    color = colorScheme.onSurfaceVariantSummary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = progress.value,
                modifier = Modifier.fillMaxWidth()
            )

            if (flashState.error.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = flashState.error,
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                    color = colorScheme.onErrorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .background(
                            colorScheme.errorContainer
                        )
                        .padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun TopBar(
    flashState: FlashState,
    onBack: () -> Unit,
    onSave: () -> Unit = {}
) {
    SmallTopAppBar(
        title = stringResource(
            when {
                flashState.error.isNotEmpty() -> R.string.flash_failed
                flashState.isCompleted -> R.string.flash_success
                else -> R.string.kernel_flashing
            }
        ),
        navigationIcon = {
            IconButton(
                modifier = Modifier.padding(start = 16.dp),
                onClick = onBack
            ) {
                val layoutDirection = LocalLayoutDirection.current
                Icon(
                    modifier = Modifier.graphicsLayer {
                        if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
                    },
                    imageVector = MiuixIcons.Back,
                    contentDescription = null,
                    tint = colorScheme.onBackground
                )
            }
        },
        actions = {
            IconButton(
                modifier = Modifier.padding(end = 16.dp),
                onClick = onSave
            ) {
                Icon(
                    imageVector = MiuixIcons.FileDownloads,
                    contentDescription = stringResource(id = R.string.save_log),
                    tint = colorScheme.onBackground
                )
            }
        }
    )
}