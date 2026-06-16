package org.example.project.core.logging

enum class LogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR }

interface AppLogger {
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null)
}

fun AppLogger.v(tag: String, msg: String) = log(LogLevel.VERBOSE, tag, msg)
fun AppLogger.d(tag: String, msg: String) = log(LogLevel.DEBUG, tag, msg)
fun AppLogger.i(tag: String, msg: String) = log(LogLevel.INFO, tag, msg)
fun AppLogger.w(tag: String, msg: String, t: Throwable? = null) = log(LogLevel.WARN, tag, msg, t)
fun AppLogger.e(tag: String, msg: String, t: Throwable? = null) = log(LogLevel.ERROR, tag, msg, t)
