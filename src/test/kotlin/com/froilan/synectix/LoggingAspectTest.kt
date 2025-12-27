package com.froilan.synectix

import com.froilan.synectix.util.LoggingExampleService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class LoggingAspectTest {

    @Autowired
    private lateinit var loggingExampleService: LoggingExampleService

    @Test
    fun testLoggingWithParameters() {
        // This test demonstrates logging with parameters and return values
        val result = loggingExampleService.exampleMethodWithFullLogging("test", 42)
        println("Result: $result")
    }

    @Test
    fun testMinimalLogging() {
        // This test demonstrates minimal logging
        loggingExampleService.exampleMethodWithMinimalLogging()
    }

    @Test
    fun testSlowMethod() {
        // This test will trigger slow operation warning
        loggingExampleService.slowMethod()
    }

    @Test
    fun testExceptionLogging() {
        try {
            loggingExampleService.methodThatThrows()
        } catch (e: RuntimeException) {
            // Exception will be logged by aspects
            println("Caught expected exception: ${e.message}")
        }
    }

    @Test
    fun testRegularMethod() {
        // This method will be monitored by performance aspect only
        loggingExampleService.regularMethod()
    }
}
