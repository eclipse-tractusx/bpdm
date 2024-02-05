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

import org.eclipse.tractusx.bpdm.common.exception.BpdmExceptionHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver

@Configuration
@EnableWebMvc
class ExceptionHandlerConfig : WebMvcConfigurer {

    @Bean
    fun bpdmExceptionHandler(): BpdmExceptionHandler {
        return BpdmExceptionHandler()
    }

    @Bean
    fun handlerExceptionResolver(): HandlerExceptionResolver {
        val resolver = ExceptionHandlerExceptionResolver()
        resolver.order = 0 // Set the order to ensure it's checked before other resolvers
        return resolver
    }
}
