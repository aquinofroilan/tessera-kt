package com.froilan.synectix.config

import io.github.cdimascio.dotenv.Dotenv
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

                println("Loaded ${envMap.size} environment variables from .env file")
            } catch (e: IOException) {
                println("Warning: Could not load .env file: ${e.message}")
            } catch (e: IllegalStateException) {
                println("Warning: Could not load .env file: ${e.message}")
            }
        } else {
            println("No .env file found in project root: ${envFile.absolutePath}")
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
