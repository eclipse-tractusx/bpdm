package com.catenax.gpdm

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRestClientAutoConfiguration
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication


@SpringBootApplication(
    exclude = [
        ElasticsearchRestClientAutoConfiguration::class,
        ElasticsearchDataAutoConfiguration::class,
        ElasticsearchRepositoriesAutoConfiguration::class,
        ReactiveElasticsearchRestClientAutoConfiguration::class],
    scanBasePackages = [
        "com.catenax.gpdm.config",
        "com.catenax.gpdm.controller",
        "com.catenax.gpdm.repository",
        "com.catenax.gpdm.service"
    ]
)
@ConfigurationPropertiesScan
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}