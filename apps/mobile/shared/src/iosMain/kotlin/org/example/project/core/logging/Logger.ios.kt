package org.example.project.core.logging

actual fun platformLogger(): AppLogger = object : AppLogger {
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        println("[$level] $tag: $message")
    }
}
