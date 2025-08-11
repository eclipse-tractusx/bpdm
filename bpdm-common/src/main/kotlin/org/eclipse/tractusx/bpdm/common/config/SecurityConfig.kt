/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.common.config

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.util.ConditionalOnBoundProperty
import org.eclipse.tractusx.bpdm.common.util.HasEnablingProperty
import org.eclipse.tractusx.bpdm.common.util.MethodMatchPointcut
import org.eclipse.tractusx.bpdm.common.util.UnsecuredRestControllerMethodsMatcher
import org.springframework.aop.Advisor
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authorization.SingleResultAuthorizationManager
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@ConfigurationProperties(prefix = SecurityConfigProperties.PREFIX)
data class SecurityConfigProperties(
    override val enabled: Boolean = false,
    val clientId: String = "BPDM_GATE",
    val realm: String = "master",
    val authUrl: String = "http://localhost:8180",
    val tokenUrl: String = "http://localhost:8180/realms/master/protocol/openid-connect/token",
    val refreshUrl: String = "http://localhost:8180/realms/master/protocol/openid-connect/token",
    val corsOrigins: Collection<String> = emptyList()
) : HasEnablingProperty {
    companion object {
        const val PREFIX = "bpdm.security"
    }
}

@EnableWebSecurity
@Configuration
@ConditionalOnBoundProperty(SecurityConfigProperties.PREFIX, SecurityConfigProperties::class, havingValue = false)
class NoAuthenticationConfig {

    private val logger = KotlinLogging.logger { }

    @Bean
    fun webSecurityCustomizer(): WebSecurityCustomizer {
        logger.info { "Disabling security for any endpoints" }
        return WebSecurityCustomizer { web -> web.ignoring().requestMatchers(AntPathRequestMatcher("**")) }
    }
}

@EnableWebSecurity
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@ConditionalOnBoundProperty(SecurityConfigProperties.PREFIX, SecurityConfigProperties::class, havingValue = true)
class OAuthSecurityConfig(
    val configProperties: SecurityConfigProperties
) {
    private val logger = KotlinLogging.logger { }

    @Bean
    @ConditionalOnMissingBean
    fun customJwtAuthenticationConverter(): CustomJwtAuthenticationConverter {
        return CustomJwtAuthenticationConverter(configProperties.clientId)
    }


    @Bean
    fun filterChain(http: HttpSecurity, customJwtAuthenticationConverter: CustomJwtAuthenticationConverter): SecurityFilterChain {
        logger.info { "Security active, securing endpoint" }
        http.csrf { it.disable() }
        http.cors {}
        http.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        http.authorizeHttpRequests {
            it.requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.OPTIONS, "/**")).permitAll()
            it.requestMatchers(AntPathRequestMatcher.antMatcher("/")).permitAll() // forwards to swagger
            it.requestMatchers(AntPathRequestMatcher.antMatcher("/docs/api-docs/**")).permitAll()
            it.requestMatchers(AntPathRequestMatcher.antMatcher("/docs/api-docs.yaml/**")).permitAll()
            it.requestMatchers(AntPathRequestMatcher.antMatcher("/ui/swagger-ui/**")).permitAll()
            it.requestMatchers(AntPathRequestMatcher.antMatcher("/actuator/health/**")).permitAll()
            it.requestMatchers(AntPathRequestMatcher.antMatcher("/error")).permitAll()
            it.requestMatchers(AntPathRequestMatcher.antMatcher("/**")).authenticated()

        }
        http.oauth2ResourceServer {
            it.jwt { jwt ->
                jwt.jwtAuthenticationConverter(customJwtAuthenticationConverter)
            }
        }
        return http.build()
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

    /**
     * This advisor denies all method invocations on RestController methods that are not secured through MethodSecurity-Annotations
     */
    @Bean
    fun denyUnsecuredRestControllerMethodAdvisor(): Advisor{
        return AuthorizationManagerBeforeMethodInterceptor(
            MethodMatchPointcut(UnsecuredRestControllerMethodsMatcher("org.eclipse.tractusx.bpdm")),
            SingleResultAuthorizationManager.denyAll()
        )
    }
}