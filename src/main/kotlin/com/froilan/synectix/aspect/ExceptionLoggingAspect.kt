package com.froilan.synectix.aspect

import org.aspectj.lang.annotation.AfterThrowing
import org.aspectj.lang.annotation.Aspect
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Aspect
@Component
class ExceptionLoggingAspect {
    private val logger: Logger = LoggerFactory.getLogger(ExceptionLoggingAspect::class.java)

    @AfterThrowing(pointcut = "execution(* com.froilan.synectix.service..*(..))", throwing = "exception")
    fun logServiceException(exception: Throwable) {
        logger.error("Service layer exception: ${exception.javaClass.simpleName} - ${exception.message}", exception)
    }

    @AfterThrowing(pointcut = "execution(* com.froilan.synectix.controller..*(..))", throwing = "exception")
    fun logControllerException(exception: Throwable) {
        logger.error("Controller layer exception: ${exception.javaClass.simpleName} - ${exception.message}", exception)
    }

    @AfterThrowing(pointcut = "execution(* com.froilan.synectix.repository..*(..))", throwing = "exception")
    fun logRepositoryException(exception: Throwable) {
        logger.error("Repository layer exception: ${exception.javaClass.simpleName} - ${exception.message}", exception)
    }
}
