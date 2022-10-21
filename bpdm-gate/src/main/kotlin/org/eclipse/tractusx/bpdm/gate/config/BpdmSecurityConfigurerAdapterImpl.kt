/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.config

import org.eclipse.tractusx.bpdm.common.config.BpdmSecurityConfigurerAdapter
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy

@Configuration
class BpdmSecurityConfigurerAdapterImpl : BpdmSecurityConfigurerAdapter {
    override fun configure(http: HttpSecurity) {
        http.csrf().disable()
            .cors()
            .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and().authorizeRequests()
            .antMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
            .antMatchers("/v3/api-docs/**").permitAll()
            .antMatchers("/api/swagger-ui/**").permitAll()
            .antMatchers(HttpMethod.GET, "/api/**").authenticated()
            .antMatchers("/api/**").hasRole("update_company_data")
    }
}