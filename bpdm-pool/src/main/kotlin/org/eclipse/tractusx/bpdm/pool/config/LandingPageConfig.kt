package org.eclipse.tractusx.bpdm.pool.config

import mu.KotlinLogging
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@Configuration
class LandingPageConfig : WebMvcConfigurer {

    private val logger = KotlinLogging.logger { }

    override fun addViewControllers(registry: ViewControllerRegistry) {
        val redirectUri = "/api/swagger-ui/index.html"
        logger.info { "Set landing page to path '$redirectUri'" }
        registry.addRedirectViewController("/", redirectUri)
    }
}