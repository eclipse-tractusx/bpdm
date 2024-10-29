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
import org.springdoc.core.properties.SwaggerUiConfigProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@Configuration
class LandingPageConfig(
    val swaggerProperties: SwaggerUiConfigProperties?
) : WebMvcConfigurer{

    private val logger = KotlinLogging.logger { }

    override fun addViewControllers(registry: ViewControllerRegistry) {
        if(swaggerProperties == null) return
        val redirectUri = swaggerProperties!!.path
        logger.info { "Set landing page to path '$redirectUri'" }
        registry.addRedirectViewController("/", redirectUri)
    }
}