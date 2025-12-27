package com.froilan.synectix

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.EnableAspectJAutoProxy

@SpringBootApplication
@EnableAspectJAutoProxy
class SynectixApplication

fun main(args: Array<String>) {
	runApplication<SynectixApplication>(*args)
}
