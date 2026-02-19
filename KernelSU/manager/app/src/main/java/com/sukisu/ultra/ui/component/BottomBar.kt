package com.sukisu.ultra.ui.component

import androidx.annotation.StringRes
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import com.sukisu.ultra.Natives
import com.sukisu.ultra.R
import com.sukisu.ultra.ui.LocalMainPagerState
import com.sukisu.ultra.ui.util.rootAvailable
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs

@Composable
fun BottomBar(hazeState: HazeState, hazeStyle: HazeStyle) {
    val isManager = Natives.isManager
    val fullFeatured = isManager && !Natives.requireNewKernel() && rootAvailable()

    val mainState = LocalMainPagerState.current

    if (!fullFeatured) return

    val item = BottomBarDestination.entries.map { destination ->
        NavigationItem(
            label = stringResource(destination.label),
            icon = destination.icon,
        )
    }

    NavigationBar(
        modifier = Modifier
            .hazeEffect(hazeState) {
                style = hazeStyle
                blurRadius = 30.dp
                noiseFactor = 0f
            },
        color = Color.Transparent,
        items = item,
        selected = mainState.selectedPage,
        onClick = { targetIndex ->
            mainState.animateToPage(targetIndex)
        }
    )
}

@Composable
fun NavigationBar(
    items: List<NavigationItem>,
    selected: Int,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.surface,
    showDivider: Boolean = true,
    defaultWindowInsetsPadding: Boolean = true,
) {
    require(items.size in 2..5) { "BottomBar must have between 2 and 5 items" }

    val captionBarPaddings = WindowInsets.captionBar.only(WindowInsetsSides.Bottom).asPaddingValues()
    val captionBarBottomPaddingValue = captionBarPaddings.calculateBottomPadding()

    val animatedCaptionBarHeight by animateDpAsState(
        targetValue = if (captionBarBottomPaddingValue > 0.dp) captionBarBottomPaddingValue else 0.dp,
        animationSpec = tween(durationMillis = 300),
    )

    val currentOnClick by rememberUpdatedState(onClick)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color),
    ) {
        if (showDivider) {
            HorizontalDivider(
                thickness = 0.6.dp,
                color = MiuixTheme.colorScheme.dividerLine.copy(0.8f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val itemHeight = 64.dp
            val itemWeight = 1f / items.size

            items.forEachIndexed { index, item ->
                val isSelected = selected == index
                var isPressed by remember { mutableStateOf(false) }

                val onSurfaceContainerColor = MiuixTheme.colorScheme.onSurfaceContainer
                val onSurfaceContainerVariantColor = MiuixTheme.colorScheme.onSurfaceContainerVariant

                val tint = when {
                    isPressed -> if (isSelected) {
                        onSurfaceContainerColor.copy(alpha = 0.5f)
                    } else {
                        onSurfaceContainerVariantColor.copy(alpha = 0.5f)
                    }

                    isSelected -> onSurfaceContainerColor

                    else -> onSurfaceContainerVariantColor
                }
                val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

                Column(
                    modifier = Modifier
                        .height(itemHeight)
                        .weight(itemWeight)
                        .pointerInput(index) {
                            detectTapGestures(
                                onPress = {
                                    isPressed = true
                                    tryAwaitRelease()
                                    isPressed = false
                                },
                                onTap = { currentOnClick(index) },
                            )
                        },
                    horizontalAlignment = CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                ) {
                    Image(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(26.dp),
                        imageVector = item.icon,
                        contentDescription = item.label,
                        colorFilter = ColorFilter.tint(tint),
                    )
                    Text(
                        modifier = Modifier.padding(top = 1.dp, bottom = 8.dp),
                        text = item.label,
                        color = tint,
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = fontWeight,
                    )
                }
            }
        }
        if (defaultWindowInsetsPadding) {
            val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(navigationBarsPadding.calculateBottomPadding() + animatedCaptionBarHeight)
                    .pointerInput(Unit) { detectTapGestures { /* Do nothing to consume the click */ } },
            )
        }
    }
}

enum class BottomBarDestination(
    @get:StringRes val label: Int,
    val icon: ImageVector,
) {
    Home(R.string.home, Icons.Rounded.Cottage),
    SuperUser(R.string.superuser, Icons.Rounded.Security),
    Module(R.string.module, Icons.Rounded.Extension),
    Setting(R.string.settings, Icons.Rounded.Settings)
}

class MainPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    var isNavigating by mutableStateOf(false)
        private set

    private var navJob: Job? = null

    fun animateToPage(targetIndex: Int) {
        if (targetIndex == selectedPage) return

        navJob?.cancel()

        selectedPage = targetIndex
        isNavigating = true

        val distance = abs(targetIndex - pagerState.currentPage).coerceAtLeast(2)
        val duration = 100 * distance + 100
        val layoutInfo = pagerState.layoutInfo
        val pageSize = layoutInfo.pageSize + layoutInfo.pageSpacing
        val currentDistanceInPages = targetIndex - pagerState.currentPage - pagerState.currentPageOffsetFraction
        val scrollPixels = currentDistanceInPages * pageSize

        navJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            try {
                pagerState.animateScrollBy(
                    value = scrollPixels,
                    animationSpec = tween(easing = EaseInOut, durationMillis = duration)
                )
            } finally {
                if (navJob == myJob) {
                    isNavigating = false
                    if (pagerState.currentPage != targetIndex) {
                        selectedPage = pagerState.currentPage
                    }
                }
            }
        }
    }

    fun syncPage() {
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }
}

@Composable
fun rememberMainPagerState(
    pagerState: PagerState,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): MainPagerState {
    return remember(pagerState, coroutineScope) {
        MainPagerState(pagerState, coroutineScope)
    }
}