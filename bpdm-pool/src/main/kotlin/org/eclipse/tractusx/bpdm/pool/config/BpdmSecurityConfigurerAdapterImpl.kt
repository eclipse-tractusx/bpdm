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

package org.eclipse.tractusx.bpdm.pool.config

import org.eclipse.tractusx.bpdm.common.config.BpdmSecurityConfigurerAdapter
import org.eclipse.tractusx.bpdm.common.config.CustomJwtAuthenticationConverter
import org.eclipse.tractusx.bpdm.common.config.SecurityConfigProperties
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.util.matcher.AntPathRequestMatcher

@Configuration
class BpdmSecurityConfigurerAdapterImpl(
    val securityConfigProperties: SecurityConfigProperties
) : BpdmSecurityConfigurerAdapter {

    companion object {
        const val ALL_API_PATHS = "/api/**"
    }

    override fun configure(http: HttpSecurity) {
        http.csrf().disable()
            .cors()
            .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and().authorizeHttpRequests()
            .requestMatchers(AntPathRequestMatcher(ALL_API_PATHS, HttpMethod.OPTIONS.name())).permitAll()
            .requestMatchers(AntPathRequestMatcher("/")).permitAll() // forwards to swagger
            .requestMatchers(AntPathRequestMatcher("/docs/api-docs/**")).permitAll()
            .requestMatchers(AntPathRequestMatcher("/ui/swagger-ui/**")).permitAll()
            .requestMatchers(AntPathRequestMatcher("/actuator/health/**")).permitAll()
            .requestMatchers(AntPathRequestMatcher(ALL_API_PATHS, HttpMethod.GET.name())).authenticated()
            .requestMatchers(AntPathRequestMatcher("/api/catena/**/search", HttpMethod.POST.name())).authenticated()
            .requestMatchers(AntPathRequestMatcher("/api/catena/**/filter", HttpMethod.POST.name())).authenticated()
            .requestMatchers(AntPathRequestMatcher(ALL_API_PATHS)).hasRole("add_company_data")
            .and().oauth2ResourceServer()
            .jwt()
            .jwtAuthenticationConverter(CustomJwtAuthenticationConverter(securityConfigProperties.clientId))
    }
}