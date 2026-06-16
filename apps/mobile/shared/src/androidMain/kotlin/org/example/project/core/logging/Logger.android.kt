package org.example.project.core.logging

actual fun platformLogger(): AppLogger = AndroidLogger

object AndroidLogger : AppLogger {
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val t = "LevelUp/$tag".take(23)
        when (level) {
            LogLevel.ERROR -> android.util.Log.e(t, message, throwable)
            LogLevel.WARN  -> android.util.Log.w(t, message, throwable)
            LogLevel.INFO  -> android.util.Log.i(t, message)
            else           -> android.util.Log.d(t, message)
        }
    }
}
