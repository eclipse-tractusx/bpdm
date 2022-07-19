package org.eclipse.tractusx.bpdm.common.config

import mu.KotlinLogging
import org.springdoc.core.SwaggerUiConfigProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@Configuration
class LandingPageConfig(
    val swaggerProperties: SwaggerUiConfigProperties
) : WebMvcConfigurer{

    private val logger = KotlinLogging.logger { }

    override fun addViewControllers(registry: ViewControllerRegistry) {
        val redirectUri = swaggerProperties.path
        logger.info { "Set landing page to path '$redirectUri'" }
        registry.addRedirectViewController("/", redirectUri)
    }
}