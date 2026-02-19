package com.sukisu.ultra.ui.susfs.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukisu.ultra.R
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.extra.SuperDropdown
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

sealed class DialogField {
    data class TextField(
        val value: String,
        val onValueChange: (String) -> Unit,
        val labelRes: Int,
        val enabled: Boolean = true,
        val modifier: Modifier = Modifier.fillMaxWidth()
    ) : DialogField()

    data class Dropdown(
        val titleRes: Int,
        val summary: String,
        val items: List<String>,
        val selectedIndex: Int,
        val onSelectedIndexChange: (Int) -> Unit,
        val enabled: Boolean = true
    ) : DialogField()

    data class CustomContent(
        val content: @Composable ColumnScope.() -> Unit
    ) : DialogField()
}

/**
 * 通用多功能对话框组件
 * 
 * @param showDialog 是否显示对话框
 * @param onDismiss 关闭对话框回调
 * @param onConfirm 确认回调，返回是否应该关闭对话框
 * @param titleRes 标题资源ID
 * @param isLoading 是否正在加载
 * @param fields 对话框字段列表
 * @param confirmTextRes 确认按钮文本资源ID，默认为"添加"
 * @param cancelTextRes 取消按钮文本资源ID，默认为"取消"
 * @param isConfirmEnabled 确认按钮是否启用，默认为true
 * @param scrollable 内容是否可滚动，默认为false
 * @param onReset 重置回调，用于清空字段
 */
@Composable
fun UniversalDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Boolean,
    titleRes: Int,
    isLoading: Boolean = false,
    fields: List<DialogField>,
    confirmTextRes: Int = R.string.add,
    cancelTextRes: Int = R.string.cancel,
    isConfirmEnabled: Boolean = true,
    scrollable: Boolean = false,
    onReset: (() -> Unit)? = null
) {
    val showDialogState = remember { mutableStateOf(showDialog) }
    
    LaunchedEffect(showDialog) {
        showDialogState.value = showDialog
    }

    if (showDialogState.value) {
        SuperDialog(
            show = showDialogState,
            title = stringResource(titleRes),
            onDismissRequest = {
                onDismiss()
                onReset?.invoke()
            },
            content = {
                val contentModifier = if (scrollable) {
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                } else {
                    Modifier.padding(horizontal = 24.dp)
                }

                Column(
                    modifier = contentModifier,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    fields.forEach { field ->
                        when (field) {
                            is DialogField.TextField -> {
                                TextField(
                                    value = field.value,
                                    onValueChange = field.onValueChange,
                                    label = stringResource(field.labelRes),
                                    useLabelAsPlaceholder = true,
                                    modifier = field.modifier,
                                    enabled = field.enabled && !isLoading
                                )
                            }
                            is DialogField.Dropdown -> {
                                SuperDropdown(
                                    title = stringResource(field.titleRes),
                                    summary = field.summary,
                                    items = field.items,
                                    selectedIndex = field.selectedIndex,
                                    onSelectedIndexChange = field.onSelectedIndexChange,
                                    enabled = field.enabled && !isLoading
                                )
                            }
                            is DialogField.CustomContent -> {
                                field.content.invoke(this)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                onDismiss()
                                onReset?.invoke()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .padding(vertical = 8.dp),
                            cornerRadius = 8.dp
                        ) {
                            Text(
                                text = stringResource(cancelTextRes)
                            )
                        }
                        Button(
                            onClick = {
                                if (onConfirm()) {
                                    onDismiss()
                                    onReset?.invoke()
                                }
                            },
                            enabled = isConfirmEnabled && !isLoading,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .padding(vertical = 8.dp),
                            cornerRadius = 8.dp
                        ) {
                            Text(
                                text = stringResource(confirmTextRes)
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun DescriptionCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    warning: String? = null,
    additionalInfo: String? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        cornerRadius = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Medium,
                color = colorScheme.primary
            )
            Text(
                text = description,
                style = MiuixTheme.textStyles.body2,
                color = colorScheme.onSurfaceVariantSummary,
                lineHeight = 16.sp
            )
            warning?.let {
                Text(
                    text = it,
                    style = MiuixTheme.textStyles.body2,
                    color = colorScheme.secondary,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 16.sp
                )
            }
            additionalInfo?.let {
                Text(
                    text = it,
                    style = MiuixTheme.textStyles.body2,
                    color = colorScheme.onSurfaceVariantSummary.copy(alpha = 0.8f),
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    titleRes: Int,
    messageRes: Int,
    isLoading: Boolean = false
) {
    UniversalDialog(
        showDialog = showDialog,
        onDismiss = onDismiss,
        onConfirm = {
            onConfirm()
            true
        },
        titleRes = titleRes,
        isLoading = isLoading,
        fields = listOf(
            DialogField.CustomContent {
                Text(
                    text = stringResource(messageRes),
                    style = MiuixTheme.textStyles.body2
                )
            }
        ),
        confirmTextRes = R.string.confirm,
        cancelTextRes = R.string.cancel,
        isConfirmEnabled = !isLoading
    )
}

@Composable
fun EmptyStateCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = colorScheme.surfaceVariant.copy(alpha = 0.15f)
        ),
        cornerRadius = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MiuixTheme.textStyles.body2,
                color = colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    count: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        cornerRadius = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface
                )
                subtitle?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = it,
                        style = MiuixTheme.textStyles.body2,
                        color = colorScheme.onSurfaceVariantSummary
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorScheme.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = count.toString(),
                    style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp),
                    color = colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
