package com.froilan.synectix.aspect

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlin.system.measureTimeMillis

@Aspect
@Component
class PerformanceMonitoringAspect {
    private val logger: Logger = LoggerFactory.getLogger(PerformanceMonitoringAspect::class.java)

    @Around("execution(* com.froilan.synectix.service..*(..))")
    fun monitorServicePerformance(joinPoint: ProceedingJoinPoint): Any? {
        val methodName = "${joinPoint.target.javaClass.simpleName}.${joinPoint.signature.name}"

        var result: Any?
        val executionTime =
            measureTimeMillis {
                result = joinPoint.proceed()
            }

        if (executionTime > 1000) {
            logger.warn("SLOW OPERATION: $methodName took ${executionTime}ms")
        } else if (executionTime > 500) {
            logger.info("$methodName took ${executionTime}ms")
        }

        return result
    }

    @Around("execution(* com.froilan.synectix.controller..*(..))")
    fun monitorControllerPerformance(joinPoint: ProceedingJoinPoint): Any? {
        val methodName = "${joinPoint.target.javaClass.simpleName}.${joinPoint.signature.name}"

        var result: Any?
        val executionTime =
            measureTimeMillis {
                result = joinPoint.proceed()
            }

        logger.info("Controller $methodName executed in ${executionTime}ms")

        return result
    }

    @Around("execution(* com.froilan.synectix.repository..*(..))")
    fun monitorRepositoryPerformance(joinPoint: ProceedingJoinPoint): Any? {
        val methodName = "${joinPoint.target.javaClass.simpleName}.${joinPoint.signature.name}"

        var result: Any?
        val executionTime =
            measureTimeMillis {
                result = joinPoint.proceed()
            }

        if (executionTime > 200) {
            logger.warn("SLOW DB QUERY: $methodName took ${executionTime}ms")
        }

        return result
    }
}
