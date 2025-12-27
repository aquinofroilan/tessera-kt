package com.froilan.synectix.util

import com.froilan.synectix.annotation.Loggable
import com.froilan.synectix.annotation.LogLevel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Utility class to demonstrate different logging scenarios with AOP
 */
@Component
class LoggingExampleService {

    private val logger: Logger = LoggerFactory.getLogger(LoggingExampleService::class.java)

    @Loggable(logParameters = true, logReturnValue = true, level = LogLevel.DEBUG)
    fun exampleMethodWithFullLogging(param1: String, param2: Int): String {
        // This method will log parameters, return value, and execution time
        Thread.sleep(100) // Simulate some work
        return "Result for $param1 with $param2"
    }

    @Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
    fun exampleMethodWithMinimalLogging() {
        // This method will only log entry and exit without parameters or return values
        logger.info("Doing some internal work...")
    }

    @Loggable(logExecutionTime = true, level = LogLevel.WARN)
    fun slowMethod() {
        // This method will be flagged for slow execution
        Thread.sleep(600) // Simulate slow operation
    }

    @Loggable(level = LogLevel.ERROR)
    fun methodThatThrows() {
        throw RuntimeException("This is a test exception for logging")
    }

    // Method without @Loggable annotation - will still be monitored by PerformanceMonitoringAspect
    fun regularMethod() {
        logger.info("This is a regular method without custom logging annotation")
    }
}
