/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource


@EnableWebSecurity
@ConditionalOnProperty(
    value = ["bpdm.security.enabled"],
    havingValue = "false",
    matchIfMissing = true
)
@Configuration
class NoAuthenticationConfig {

    private val logger = KotlinLogging.logger { }

    @Bean
    fun webSecurityCustomizer(): WebSecurityCustomizer {
        logger.info { "Disabling security for any endpoints" }
        return WebSecurityCustomizer { web -> web.ignoring().requestMatchers(AntPathRequestMatcher("**")) }
    }
}

@EnableWebSecurity
@ConditionalOnProperty(
    value = ["bpdm.security.enabled"],
    havingValue = "true"
)
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
class OAuthSecurityConfig(
    val configProperties: SecurityConfigProperties,
    val bpdmSecurityConfigurerAdapter: BpdmSecurityConfigurerAdapter
) {
    private val logger = KotlinLogging.logger { }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        logger.info { "Security active, securing endpoint" }
        bpdmSecurityConfigurerAdapter.configure(http)
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
}