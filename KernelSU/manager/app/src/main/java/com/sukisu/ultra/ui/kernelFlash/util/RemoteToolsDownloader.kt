package com.sukisu.ultra.ui.kernelFlash.util

import android.content.Context
import android.util.Log
import com.sukisu.ultra.ui.util.getRootShell
import com.topjohnwu.superuser.ShellUtils
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.TimeUnit

class RemoteToolsDownloader(
    private val context: Context,
    private val workDir: String
) {
    companion object {
        private const val TAG = "RemoteToolsDownloader"

        // 远程下载URL配置
        private const val KPTOOLS_REMOTE_URL = "https://raw.githubusercontent.com/ShirkNeko/SukiSU_patch/refs/heads/main/kpm/kptools"
        private const val KPIMG_REMOTE_URL = "https://raw.githubusercontent.com/ShirkNeko/SukiSU_patch/refs/heads/main/kpm/kpimg"

        // 网络超时配置（毫秒）
        private const val CONNECTION_TIMEOUT = 10000
        private const val READ_TIMEOUT = 20000

        // 最大重试次数
        private const val MAX_RETRY_COUNT = 3

        // 文件校验相关
        private const val MIN_FILE_SIZE = 1024
    }

    interface DownloadProgressListener {
        fun onProgress(fileName: String, progress: Int, total: Int)
        fun onLog(message: String)
        fun onError(fileName: String, error: String)
        fun onSuccess(fileName: String, isRemote: Boolean)
    }

    data class DownloadResult(
        val success: Boolean,
        val isRemoteSource: Boolean,
        val errorMessage: String? = null
    )


    suspend fun downloadToolsAsync(listener: DownloadProgressListener?): Map<String, DownloadResult> = withContext(Dispatchers.IO) {
        listener?.onLog("Starting to prepare KPM tool files...")
        File(workDir).mkdirs()

        // 并行下载两个工具文件
        val results = mapOf(
            "kptools" to async { downloadSingleTool("kptools", KPTOOLS_REMOTE_URL, listener) },
            "kpimg" to async { downloadSingleTool("kpimg", KPIMG_REMOTE_URL, listener) }
        ).mapValues { it.value.await() }

        // 设置 kptools 执行权限
        File(workDir, "kptools").takeIf { it.exists() }?.let { file ->
            setExecutablePermission(file.absolutePath)
            listener?.onLog("Set kptools execution permission")
        }

        val successCount = results.values.count { it.success }
        val remoteCount = results.values.count { it.success && it.isRemoteSource }
        listener?.onLog("KPM tools preparation completed: Success $successCount/2, Remote downloaded $remoteCount")

        results
    }

    private suspend fun downloadSingleTool(
        fileName: String,
        remoteUrl: String?,
        listener: DownloadProgressListener?
    ): DownloadResult = withContext(Dispatchers.IO) {
        val targetFile = File(workDir, fileName)
        
        if (remoteUrl == null) {
            return@withContext useLocalVersion(fileName, targetFile, listener)
        }

        listener?.onLog("Downloading $fileName from remote repository...")

        // 重试机制
        var lastError = ""
        repeat(MAX_RETRY_COUNT) { attempt ->
            try {
                val result = downloadFromRemote(fileName, remoteUrl, targetFile, listener)
                if (result.success) {
                    listener?.onSuccess(fileName, true)
                    return@withContext result
                } else {
                    lastError = result.errorMessage ?: "Unknown error"
                }
            } catch (e: Exception) {
                lastError = "Network exception"
                Log.w(TAG, "$fileName download attempt ${attempt + 1} failed", e)
            }

            if (attempt < MAX_RETRY_COUNT - 1) {
                listener?.onLog("$fileName download failed, retrying in ${(attempt + 1) * 2} seconds...")
                delay(TimeUnit.SECONDS.toMillis((attempt + 1) * 2L))
            }
        }

        listener?.onError(fileName, "Remote download failed: $lastError")
        listener?.onLog("$fileName remote download failed, falling back to local version...")
        useLocalVersion(fileName, targetFile, listener)
    }

    private suspend fun downloadFromRemote(
        fileName: String,
        remoteUrl: String,
        targetFile: File,
        listener: DownloadProgressListener?
    ): DownloadResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null

        try {
            connection = (URL(remoteUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECTION_TIMEOUT
                readTimeout = READ_TIMEOUT
                requestMethod = "GET"
                setRequestProperty("User-Agent", "SukiSU-KPM-Downloader/1.0")
                setRequestProperty("Accept", "*/*")
                setRequestProperty("Connection", "close")
            }

            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext DownloadResult(
                    false,
                    isRemoteSource = false,
                    errorMessage = "HTTP error code: ${connection.responseCode}"
                )
            }

            val fileLength = connection.contentLength
            val tempFile = File(targetFile.absolutePath + ".tmp")

            // 下载文件
            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var totalBytes = 0

                    while (true) {
                        ensureActive()
                        val bytesRead = input.read(buffer)
                        if (bytesRead == -1) break

                        output.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead

                        if (fileLength > 0) {
                            listener?.onProgress(fileName, totalBytes, fileLength)
                        }
                    }
                    output.flush()
                }
            }

            // 验证并移动文件
            if (!validateDownloadedFile(tempFile, fileName)) {
                tempFile.delete()
                return@withContext DownloadResult(
                    false,
                    isRemoteSource = false,
                    errorMessage = "File verification failed"
                )
            }

            targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                tempFile.delete()
                return@withContext DownloadResult(
                    false,
                    isRemoteSource = false,
                    errorMessage = "Failed to move file"
                )
            }

            Log.i(TAG, "$fileName remote download successful, file size: ${targetFile.length()} bytes")
            listener?.onLog("$fileName remote download successful")
            DownloadResult(true, isRemoteSource = true)

        } catch (e: SocketTimeoutException) {
            Log.w(TAG, "$fileName download timeout", e)
            DownloadResult(false, isRemoteSource = false, errorMessage = "Connection timeout")
        } catch (e: IOException) {
            Log.w(TAG, "$fileName network IO exception", e)
            DownloadResult(false, isRemoteSource = false, errorMessage = "Network exception: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "$fileName exception occurred during download", e)
            DownloadResult(false, isRemoteSource = false, errorMessage = "Download exception: ${e.message}")
        } finally {
            connection?.disconnect()
        }
    }

    private suspend fun useLocalVersion(
        fileName: String,
        targetFile: File,
        listener: DownloadProgressListener?
    ): DownloadResult = withContext(Dispatchers.IO) {
        try {
            AssetsUtil.exportFiles(context, fileName, targetFile.absolutePath)

            if (!targetFile.exists() || !validateDownloadedFile(targetFile, fileName)) {
                val errorMsg = if (!targetFile.exists()) {
                    "Local $fileName file extraction failed"
                } else {
                    "Local $fileName file verification failed"
                }
                listener?.onError(fileName, errorMsg)
                return@withContext DownloadResult(false, isRemoteSource = false, errorMessage = errorMsg)
            }

            Log.i(TAG, "$fileName local version loaded successfully, file size: ${targetFile.length()} bytes")
            listener?.onLog("$fileName local version loaded successfully")
            listener?.onSuccess(fileName, false)
            DownloadResult(true, isRemoteSource = false)

        } catch (e: Exception) {
            Log.e(TAG, "$fileName local version loading failed", e)
            val errorMsg = "Local version loading failed: ${e.message}"
            listener?.onError(fileName, errorMsg)
            DownloadResult(false, isRemoteSource = false, errorMessage = errorMsg)
        }
    }

    private fun validateDownloadedFile(file: File, fileName: String): Boolean {
        if (!file.exists() || file.length() < MIN_FILE_SIZE) {
            Log.w(TAG, "$fileName file validation failed: exists=${file.exists()}, size=${file.length()}")
            return false
        }

        return try {
            file.inputStream().use { input ->
                val header = ByteArray(4)
                if (input.read(header) < 4) {
                    Log.w(TAG, "$fileName file header read incomplete")
                    return false
                }

                val isELF = header[0] == 0x7F.toByte() &&
                        header[1] == 'E'.code.toByte() &&
                        header[2] == 'L'.code.toByte() &&
                        header[3] == 'F'.code.toByte()

                if (fileName == "kptools" && !isELF) {
                    Log.w(TAG, "kptools file format is invalid, not ELF format")
                    return false
                }

                Log.d(TAG, "$fileName file verification passed, size: ${file.length()} bytes, ELF: $isELF")
                true
            }
        } catch (e: Exception) {
            Log.w(TAG, "$fileName file verification exception", e)
            false
        }
    }

    private fun setExecutablePermission(filePath: String) {
        try {
            val shell = getRootShell()
            if (ShellUtils.fastCmdResult(shell, "chmod a+rx $filePath")) {
                Log.d(TAG, "Set execution permission for $filePath")
            } else {
                File(filePath).setExecutable(true, false)
                Log.d(TAG, "Set execution permission using Java method for $filePath")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set execution permission: $filePath", e)
            try {
                File(filePath).setExecutable(true, false)
            } catch (ex: Exception) {
                Log.w(TAG, "Java method to set permissions also failed", ex)
            }
        }
    }


    fun cleanup() {
        try {
            File(workDir).listFiles()?.filter { it.name.endsWith(".tmp") }?.forEach { file ->
                file.delete()
                Log.d(TAG, "Cleaned temporary file: ${file.name}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clean temporary files", e)
        }
    }
}
