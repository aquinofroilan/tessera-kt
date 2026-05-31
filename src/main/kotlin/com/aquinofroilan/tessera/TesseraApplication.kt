package com.aquinofroilan.tessera

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableAspectJAutoProxy
@EnableScheduling
@ConfigurationPropertiesScan
class TesseraApplication

fun main(args: Array<String>) {
    runApplication<TesseraApplication>(*args)
}
