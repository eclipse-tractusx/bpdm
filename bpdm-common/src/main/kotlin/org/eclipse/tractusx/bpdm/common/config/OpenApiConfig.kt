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

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.*
import io.swagger.v3.oas.models.servers.Server
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.util.OpenApiCustomizerFactory
import org.springdoc.core.customizers.ParameterCustomizer
import org.springdoc.core.extractor.DelegatingMethodParameter
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class OpenApiConfig(
    private val securityProperties: SecurityConfigProperties,
    private val infoProperties: AppInfoProperties
) {
    private val logger = KotlinLogging.logger { }

    @Bean
    fun customOpenAPI(): OpenAPI? {
        return OpenAPI().info(Info().title(infoProperties.name).description(infoProperties.description).version(infoProperties.version))
            .apply { if(infoProperties.url.isNotBlank()) addServersItem(Server().url(infoProperties.url)) }
            .also { if (securityProperties.enabled) it.withSecurity() }
    }

    @Bean
    fun openApiDefinition(openApiCustomizerFactory: OpenApiCustomizerFactory): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("docs")
            .pathsToMatch("/**")
            .displayName("Docs")
            .addOpenApiCustomizer(openApiCustomizerFactory.sortSchemaCustomiser())
            .build()
    }

    @Bean
    fun version6Group(openApiCustomizerFactory: OpenApiCustomizerFactory): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("v6")
            .pathsToMatch("/v6/**")
            .displayName("V6")
            .addOpenApiCustomizer(openApiCustomizerFactory.sortSchemaCustomiser())
            .addOpenApiCustomizer(openApiCustomizerFactory.versionApiCustomizer("v6"))
            .build()
    }

    @Bean
    fun version7Group(openApiCustomizerFactory: OpenApiCustomizerFactory): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("v7")
            .pathsToMatch("/v7/**")
            .displayName("V7")
            .addOpenApiCustomizer(openApiCustomizerFactory.sortSchemaCustomiser())
            .addOpenApiCustomizer(openApiCustomizerFactory.versionApiCustomizer("v7"))
            .build()
    }

    @Bean
    fun openApiCustomizerFactory(): OpenApiCustomizerFactory{
        return OpenApiCustomizerFactory()
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
                                .scopes(Scopes())
                        ).clientCredentials(
                            OAuthFlow().tokenUrl(securityProperties.tokenUrl).scopes(Scopes())
                        )
                    )
                )
                .addSecuritySchemes(
                    "bearer_scheme",
                    SecurityScheme().type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                )
        )
            .addSecurityItem(SecurityRequirement().addList("open_id_scheme", emptyList()))
            .addSecurityItem(SecurityRequirement().addList("bearer_scheme", emptyList()))
    }


    /**
     * Workaround for existing Spring-Doc bug relating to required parameters: https://github.com/springdoc/springdoc-openapi/issues/2978
     *
     * With this Customizer we can override the "required" and "default" OpenAPI fields of request parameters via @Parameter-Annotation,
     * since it is not correctly deduced from the Object model at the moment
     */
    @Bean
    fun requiredValueCustomizer(): ParameterCustomizer =
        ParameterCustomizer { model, methodParameter ->
            if(methodParameter is DelegatingMethodParameter){
                methodParameter.field?.annotations
                    ?.firstOrNull { annotation -> annotation.annotationClass == Parameter::class }
                    ?.let { it as Parameter }
                    ?.apply {
                        model.required(required)
                    }
            }

            model
        }

}
