package org.example.project.core.network

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ErrorMessageTest {

    @Test
    fun `401 returns auth expired message`() {
        val message = ErrorMessage.classify(statusCode = 401)
        assertContains(message, "再ログイン")
    }

    @Test
    fun `403 returns permission denied message`() {
        val message = ErrorMessage.classify(statusCode = 403)
        assertContains(message, "権限")
    }

    @Test
    fun `500 returns server error message`() {
        val message = ErrorMessage.classify(statusCode = 500)
        assertContains(message, "サーバー")
    }

    @Test
    fun `429 returns rate limit message`() {
        val message = ErrorMessage.classify(statusCode = 429)
        assertContains(message, "操作が多すぎ")
    }

    @Test
    fun `offline returns offline message`() {
        val message = ErrorMessage.classify(isOnline = false)
        assertContains(message, "オフライン")
    }

    @Test
    fun `timeout exception returns network error message`() {
        val message = ErrorMessage.classify(exception = TestTimeoutException("timeout"))
        assertContains(message, "通信環境")
    }

    @Test
    fun `validation status returns validation message`() {
        val message = ErrorMessage.classify(statusCode = 422)
        assertContains(message, "内容を確認")
    }

    @Test
    fun `unknown exception returns generic retry message`() {
        val message = ErrorMessage.classify(exception = RuntimeException("unknown"))
        assertEquals("通信中にエラーが発生しました。再試行してください。", message)
    }

    @Test
    fun `no inputs returns generic error message`() {
        val message = ErrorMessage.classify()
        assertEquals("エラーが発生しました。再試行してください。", message)
    }

    private class TestTimeoutException(message: String) : Exception(message)
}
