package com.sukisu.ultra.ui.susfs.content

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.R
import com.sukisu.ultra.ui.susfs.component.ResetButton
import top.yukonga.miuix.kmp.basic.*

@SuppressLint("SdCardPath")
@Composable
fun PathSettingsContent(
    androidDataPath: String,
    onAndroidDataPathChange: (String) -> Unit,
    sdcardPath: String,
    onSdcardPathChange: (String) -> Unit,
    isLoading: Boolean,
    onSetAndroidDataPath: () -> Unit,
    onSetSdcardPath: () -> Unit,
    onReset: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextField(
                    value = androidDataPath,
                    onValueChange = onAndroidDataPathChange,
                    label = stringResource(R.string.susfs_android_data_path_label),
                    useLabelAsPlaceholder = true,
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    enabled = !isLoading
                )

                TextField(
                    value = sdcardPath,
                    onValueChange = onSdcardPathChange,
                    label = stringResource(R.string.susfs_sdcard_path_label),
                    useLabelAsPlaceholder = true,
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    enabled = !isLoading
                )

                Button(
                    onClick = {
                        onSetAndroidDataPath()
                        onSetSdcardPath()
                    },
                    enabled = !isLoading && androidDataPath.isNotBlank() && sdcardPath.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp
                ) {
                    Text(
                        text = stringResource(R.string.susfs_apply)
                    )
                }
            }
        }
    }

    if (onReset != null) {
        ResetButton(
            title = stringResource(R.string.susfs_reset_path_title),
            onClick = onReset
        )
    }
}

