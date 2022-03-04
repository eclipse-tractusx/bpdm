package com.catenax.gpdm.config

import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver
import org.keycloak.adapters.springsecurity.KeycloakConfiguration
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy

@EnableWebSecurity
@ConditionalOnProperty(
    value = ["bpdm.security.enabled"],
    havingValue = "false",
    matchIfMissing = true)
class NoAuthenticationConfig: WebSecurityConfigurerAdapter() {

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http
            .csrf().disable()
            .authorizeRequests().anyRequest().permitAll()
    }

}

@EnableWebSecurity
@KeycloakConfiguration
@ConditionalOnProperty(
    value = ["bpdm.security.enabled"],
    havingValue = "true")
class KeycloakSecurityConfig: KeycloakWebSecurityConfigurerAdapter() {

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
        return RegisterSessionAuthenticationStrategy(
            SessionRegistryImpl()
        )
    }

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        super.configure(http)
        http
            .csrf().disable()
            .authorizeRequests()
                .antMatchers("/v3/api-docs/**").permitAll()
                .antMatchers("/api/swagger-ui/**").permitAll()
                .antMatchers(HttpMethod.POST, "/api/**").hasRole("add_company_data")
                .antMatchers("/api/**").authenticated()
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