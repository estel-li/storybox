package net.lijue.storybox.core.common

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

fun Throwable.toFriendlyMessage(): String {
    val raw = message.orEmpty()
    return when {
        raw.contains("服务器地址未配置") -> "请先填写家庭故事盒后端地址。"
        raw.contains("Unable to resolve", ignoreCase = true) -> "无法找到服务器，请检查地址和局域网连接。"
        raw.contains("Failed to connect", ignoreCase = true) -> "无法连接家庭故事盒，请检查手机和 NAS 是否在同一局域网。"
        raw.contains("timeout", ignoreCase = true) -> "连接超时，请检查服务器是否正在运行。"
        raw.contains("HTTP 404", ignoreCase = true) -> "故事文件暂时无法播放，请稍后再试。"
        raw.contains("HTTP", ignoreCase = true) -> "服务器返回异常，请稍后再试。"
        else -> "操作失败，请检查网络后重试。"
    }
}
