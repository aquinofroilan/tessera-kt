package com.aquinofroilan.tessera.config

import io.github.cdimascio.dotenv.Dotenv
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import java.io.File
import java.io.IOException

/**
 * Initializer to load environment variables from .env file in the project root.
 * This runs before the Spring application context is created.
 */
class EnvironmentInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    private val log = LoggerFactory.getLogger(EnvironmentInitializer::class.java)

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val environment: ConfigurableEnvironment = applicationContext.environment

        val projectRoot = findProjectRoot()
        val envFile = File(projectRoot, ".env")

        if (envFile.exists() && envFile.isFile) {
            try {
                val dotenv =
                    Dotenv
                        .configure()
                        .directory(projectRoot.absolutePath)
                        .filename(".env")
                        .ignoreIfMissing()
                        .load()

                val envMap = mutableMapOf<String, Any>()
                dotenv.entries().forEach { entry ->
                    envMap[entry.key] = entry.value
                }

                val propertySource = MapPropertySource("dotenv", envMap)
                environment.propertySources.addFirst(propertySource)

                log.info("Loaded {} environment variables from .env file", envMap.size)
            } catch (e: IOException) {
                log.warn("Could not load .env file: {}", e.message)
            } catch (e: IllegalStateException) {
                log.warn("Could not load .env file: {}", e.message)
            } catch (e: Exception) {
                log.error("Unexpected error loading .env file: {}", e.message)
            }
        } else {
            log.info("No .env file found in project root: {}", envFile.absolutePath)
        }
    }

    /**
     * Find the project root directory by looking for build.gradle.kts or gradlew
     */
    private fun findProjectRoot(): File {
        var currentDir = File(System.getProperty("user.dir"))

        while (currentDir.parent != null) {
            if (File(currentDir, "gradlew").exists() ||
                File(currentDir, "gradlew.bat").exists() ||
                File(currentDir, "build.gradle.kts").exists()
            ) {
                return currentDir
            }
            currentDir = currentDir.parentFile
        }

        return File(System.getProperty("user.dir"))
    }
}
