package com.froilan.synectix.aspect

import com.froilan.synectix.annotation.LogLevel
import com.froilan.synectix.annotation.Loggable
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.lang.reflect.Method
import kotlin.system.measureTimeMillis

@Aspect
@Component
class LoggingAspect {
    @Around("@annotation(com.froilan.synectix.annotation.Loggable) || @within(com.froilan.synectix.annotation.Loggable)")
    fun logExecutionTime(joinPoint: ProceedingJoinPoint): Any? {
        val logger = LoggerFactory.getLogger(joinPoint.target.javaClass)
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method

        val loggableAnnotation = getLoggableAnnotation(method, joinPoint.target.javaClass) ?: return joinPoint.proceed()

        val methodName = "${joinPoint.target.javaClass.simpleName}.${method.name}"
        val args = joinPoint.args

        if (loggableAnnotation.logParameters && args.isNotEmpty()) {
            val params =
                args
                    .mapIndexed { index, arg ->
                        "${signature.parameterNames?.getOrNull(index) ?: "arg$index"}=${maskSensitiveData(arg)}"
                    }.joinToString(", ")
            logMessage(logger, loggableAnnotation.level, "→ Entering $methodName with parameters: [$params]")
        } else {
            logMessage(logger, loggableAnnotation.level, "→ Entering $methodName")
        }

        var result: Any?
        var executionTime: Long = 0

        try {
            if (loggableAnnotation.logExecutionTime) {
                executionTime =
                    measureTimeMillis {
                        result = joinPoint.proceed()
                    }
            } else {
                result = joinPoint.proceed()
            }

            val timeMessage = if (loggableAnnotation.logExecutionTime) " (${executionTime}ms)" else ""

            if (loggableAnnotation.logReturnValue && result != null) {
                logMessage(logger, loggableAnnotation.level, "← Exiting $methodName$timeMessage with result: ${maskSensitiveData(result)}")
            } else {
                logMessage(logger, loggableAnnotation.level, "← Exiting $methodName$timeMessage")
            }

            return result
        } catch (exception: Throwable) {
            val timeMessage = if (loggableAnnotation.logExecutionTime && executionTime > 0) " (${executionTime}ms)" else ""
            logMessage(
                logger,
                LogLevel.ERROR,
                "✗ Exception in $methodName$timeMessage: ${exception.javaClass.simpleName} - ${exception.message}",
            )
            throw exception
        }
    }

    private fun getLoggableAnnotation(
        method: Method,
        targetClass: Class<*>,
    ): Loggable? {
        method.getAnnotation(Loggable::class.java)?.let { return it }

        return targetClass.getAnnotation(Loggable::class.java)
    }

    private fun logMessage(
        logger: Logger,
        level: LogLevel,
        message: String,
    ) {
        when (level) {
            LogLevel.TRACE -> logger.trace(message)
            LogLevel.DEBUG -> logger.debug(message)
            LogLevel.INFO -> logger.info(message)
            LogLevel.WARN -> logger.warn(message)
            LogLevel.ERROR -> logger.error(message)
        }
    }

    private fun maskSensitiveData(obj: Any?): String =
        when {
            obj == null -> "null"
            obj.toString().contains("password", ignoreCase = true) -> "***MASKED***"
            obj.toString().contains("token", ignoreCase = true) -> "***MASKED***"
            obj.toString().contains("secret", ignoreCase = true) -> "***MASKED***"
            obj.toString().length > 100 -> obj.toString().take(100) + "... (truncated)"
            else -> obj.toString()
        }
}
