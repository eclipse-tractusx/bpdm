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

package org.eclipse.tractusx.bpdm.common.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import mu.KotlinLogging
import org.springdoc.core.GroupedOpenApi
import org.springdoc.core.customizers.OpenApiCustomiser
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component


@Configuration
class OpenApiConfig(
    val securityProperties: SecurityConfigProperties,
    val infoProperties: AppInfoProperties
) {
    private val logger = KotlinLogging.logger { }

    @Bean
    fun openApiDefinition(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .addOpenApiCustomiser(bpdmOpenApiCustomiser())
            .group("docs")
            .pathsToMatch("/**")
            .displayName("Docs")
            .build()
    }

    fun bpdmOpenApiCustomiser(): OpenApiCustomiser {
        return OpenApiCustomiser { openApi: OpenAPI ->
            openApi.info(Info().title(infoProperties.name).description(infoProperties.description).version(infoProperties.version))
                .components(with(openApi.components) { schemas(schemas.values.sortedBy { it.name }.associateBy { it.name }) })
                .also { if (securityProperties.enabled) it.withSecurity() }
        }
    }

    private fun OpenAPI.withSecurity(): OpenAPI {
        logger.info { "Add security schemes to OpenAPI definition" }
        return this.components(
            Components()
                .addSecuritySchemes(
                    "open_id_scheme",
                    SecurityScheme().type(SecurityScheme.Type.OAUTH2).flows(
                        OAuthFlows().authorizationCode(
                            OAuthFlow().authorizationUrl(securityProperties.authUrl)
                                .tokenUrl(securityProperties.tokenUrl)
                                .refreshUrl(securityProperties.refreshUrl)
                        )
                    )
                )
        )
            .addSecurityItem(SecurityRequirement().addList("open_id_scheme", emptyList()))
    }


}

@Component
class SortSchemasCustomiser : OpenApiCustomiser {
    override fun customise(openApi: OpenAPI) {
        val sortedSchemas = openApi.components.schemas.values.sortedBy { it.name }
        openApi.components.schemas = sortedSchemas.associateBy { it.name }
    }
}

