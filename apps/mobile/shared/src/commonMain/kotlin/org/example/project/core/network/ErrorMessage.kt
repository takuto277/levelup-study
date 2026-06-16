package org.example.project.core.network

object ErrorMessage {
    fun classify(
        statusCode: Int? = null,
        exception: Throwable? = null,
        isOnline: Boolean = true
    ): String {
        return when {
            !isOnline ->
                "オフラインです。通信環境を確認してから再試行してください。"

            isNetworkTimeoutError(exception) ->
                "ネットワークに接続できません。通信環境を確認してください。"

            statusCode == 401 ->
                "認証が切れました。再ログインしてください。"

            statusCode == 403 ->
                "アクセス権限がありません。"

            statusCode == 429 ->
                "操作が多すぎます。少し待ってから再試行してください。"

            statusCode in 500..599 ->
                "サーバーでエラーが発生しました。しばらく待ってから再試行してください。"

            statusCode != null ->
                "リクエストを処理できませんでした。内容を確認してから再試行してください。"

            exception != null ->
                "通信中にエラーが発生しました。再試行してください。"

            else -> "エラーが発生しました。再試行してください。"
        }
    }

    private fun isNetworkTimeoutError(e: Throwable?): Boolean {
        if (e == null) return false
        val name = e::class.simpleName ?: return false
        return name.contains("Timeout", ignoreCase = true) ||
                name.contains("ConnectException", ignoreCase = true) ||
                name.contains("IOException", ignoreCase = true) ||
                name.contains("SocketException", ignoreCase = true)
    }
}
