package org.eclipse.tractusx.bpdm.common.config

import mu.KotlinLogging
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver
import org.keycloak.adapters.springsecurity.KeycloakConfiguration
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource


@EnableWebSecurity
@ConditionalOnProperty(
    value = ["bpdm.security.enabled"],
    havingValue = "false",
    matchIfMissing = true)
class NoAuthenticationConfig: WebSecurityConfigurerAdapter() {

    private val logger = KotlinLogging.logger { }

    @Throws(Exception::class)
    override fun configure(web: WebSecurity) {
        logger.info { "Disabling security for any endpoints" }
        web
            .ignoring().antMatchers("**")
    }
}

@EnableWebSecurity
@KeycloakConfiguration
@ConditionalOnProperty(
    value = ["bpdm.security.enabled"],
    havingValue = "true")
class KeycloakSecurityConfig(
    val configProperties: SecurityConfigProperties,
    val bpdmSecurityConfigurerAdapter: BpdmSecurityConfigurerAdapter
): KeycloakWebSecurityConfigurerAdapter() {

    private val logger = KotlinLogging.logger { }

    @Autowired
    @Throws(Exception::class)
    fun configureGlobal(
        auth: AuthenticationManagerBuilder
    ) {
        val keycloakAuthenticationProvider = keycloakAuthenticationProvider()
        keycloakAuthenticationProvider.setGrantedAuthoritiesMapper(
            SimpleAuthorityMapper()
        )
        auth.authenticationProvider(keycloakAuthenticationProvider)
    }

    @Bean
    override fun sessionAuthenticationStrategy(): SessionAuthenticationStrategy? {
        return NullAuthenticatedSessionStrategy()
    }

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        logger.info { "Security active, securing endpoint" }
        super.configure(http)
        bpdmSecurityConfigurerAdapter.configure(http)
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins =  configProperties.corsOrigins.toList()
        configuration.allowedMethods = listOf("HEAD", "OPTIONS", "GET", "POST", "PUT", "DELETE")
        configuration.allowedHeaders = listOf("*")
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}

@Configuration
@ConditionalOnProperty(
    value = ["bpdm.security.enabled"],
    havingValue = "true")
class KeyCloakConfiguration{
    @Bean
    fun keycloakConfigResolver(): KeycloakSpringBootConfigResolver {
        return KeycloakSpringBootConfigResolver()
    }

}