package com.froilan.synectix

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.EnableAspectJAutoProxy

@SpringBootApplication
@EnableAspectJAutoProxy
@ConfigurationPropertiesScan
class SynectixApplication

fun main(args: Array<String>) {
    runApplication<SynectixApplication>(*args)
}
