package com.froilan.synectix.annotation

/**
 * Annotation to mark methods that should be logged.
 * When applied to a method, it will log:
 * - Method entry with parameters
 * - Method exit with return value
 * - Method execution time
 * - Exceptions if they occur
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Loggable(
    /**
     * Whether to log method parameters
     */
    val logParameters: Boolean = true,
    /**
     * Whether to log return values
     */
    val logReturnValue: Boolean = true,
    /**
     * Whether to log execution time
     */
    val logExecutionTime: Boolean = true,
    /**
     * Log level to use (INFO, DEBUG, WARN, ERROR)
     */
    val level: LogLevel = LogLevel.INFO,
)

enum class LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
}
