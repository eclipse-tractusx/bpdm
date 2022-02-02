package com.catenax.gpdm

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}