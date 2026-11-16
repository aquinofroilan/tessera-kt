package com.aquinofroilan.tessera.config

import io.github.cdimascio.dotenv.Dotenv
import org.slf4j.LoggerFactory
import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.core.Ordered
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import java.io.File
import java.io.IOException

/** Loads project .env values early enough for Spring profile-specific configuration. */
class DotenvEnvironmentPostProcessor : EnvironmentPostProcessor, Ordered {
    private val log = LoggerFactory.getLogger(DotenvEnvironmentPostProcessor::class.java)

    override fun postProcessEnvironment(
        environment: ConfigurableEnvironment,
        application: SpringApplication,
    ) {
        val projectRoot = findProjectRoot()
        val envFile = File(projectRoot, ".env")

        if (!envFile.exists() || !envFile.isFile) {
            log.info("No .env file found in project root: {}", envFile.absolutePath)
            return
        }

        try {
            val dotenv =
                Dotenv
                    .configure()
                    .directory(projectRoot.absolutePath)
                    .filename(".env")
                    .ignoreIfMissing()
                    .load()

            val envMap = dotenv.entries().associate { it.key to it.value as Any }.toMutableMap()
            envMap["SPRING_PROFILES_ACTIVE"]?.let { envMap["spring.profiles.active"] = it }

            environment.propertySources.addAfter(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                MapPropertySource("dotenv", envMap),
            )
            log.info("Loaded {} environment variables from .env", envMap.size)
        } catch (e: IOException) {
            log.warn("Could not load .env file: {}", e.message)
        } catch (e: IllegalStateException) {
            log.warn("Could not load .env file: {}", e.message)
        } catch (e: Exception) {
            log.error("Unexpected error loading .env file: {}", e.message)
        }
    }

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

    /** Finds the project root by looking for build.gradle.kts or gradlew. */
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
