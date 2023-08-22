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

import com.neovisionaries.i18n.CountryCode
import com.nimbusds.jose.shaded.gson.reflect.TypeToken
import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.*
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.LogisticAddressDto
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.GenericDescription
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.LogisticAddressDescription
import org.eclipse.tractusx.bpdm.common.dto.response.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameVerboseDto
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import java.lang.reflect.Type


abstract class OpenApiConfig {
    private val logger = KotlinLogging.logger { }

    abstract val securityProperties: SecurityConfigProperties
    abstract val infoProperties: AppInfoProperties

    @Bean
    open fun customOpenAPI(): OpenAPI? {
        val info = Info()
            .title(infoProperties.name)
            .description(infoProperties.description)
            .version(infoProperties.version)
        val components = buildComponents(getSchemaOverrides())
        return OpenAPI()
            .info(info)
            .components(components)
            .also {
                if (securityProperties.enabled)
                    it.withSecurity()
            }
    }

    /**
     * This hooks allows overriding OpenAPI schema definitions.
     * This can be used for two scenarios:
     * - Override the schema description of a generic class with specific type parameter, as the annotated Schema description might not be specific enough.
     * - Clone a schema, overriding its name and description, to use it for some specific field.
     */
    open fun getSchemaOverrides(): Collection<SchemaOverrideInfo> {
        return setOf(
            // Generic types
            schemaOverride<TypeKeyNameVerboseDto<CountryCode>>(GenericDescription.typeKeyNameCountryCode),

            // Schema copies with alternative description
            schemaOverride<LogisticAddressDto>(
                LogisticAddressDescription.legalAddress,
                LogisticAddressDescription.legalAddressAliasForLogisticAddressDto
            ),
            schemaOverride<LogisticAddressVerboseDto>(
                LogisticAddressDescription.legalAddress,
                LogisticAddressDescription.legalAddressAliasForLogisticAddressVerboseDto
            )
        )
    }

    private fun buildComponents(schemaOverrideInfos: Collection<SchemaOverrideInfo>): Components {
        val components = Components()
        schemaOverrideInfos.forEach {
            val schemaCopy = copySchema(it.schemaType)
            schemaCopy.description(it.description)
            it.alternativeSchemaName?.let { altSchemaName ->
                schemaCopy.name(altSchemaName)
            }
            components.addSchemas(schemaCopy.name, schemaCopy)
        }
        return components
    }

    private fun copySchema(schemaType: Type) =
        ModelConverters.getInstance()
            .resolveAsResolvedSchema(
                AnnotatedType(schemaType).resolveAsRef(false)
            )
            .schema

    inline fun <reified T> schemaOverride(description: String, alternativeSchemaName: String? = null) =
        SchemaOverrideInfo(
            object : TypeToken<T>() {}.type,
            description,
            alternativeSchemaName
        )

    data class SchemaOverrideInfo(
        val schemaType: Type,
        val description: String,
        val alternativeSchemaName: String? = null
    )

    @Bean
    open fun openApiDefinition(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("docs")
            .pathsToMatch("/**")
            .displayName("Docs")
            .addOpenApiCustomizer(sortSchemaCustomiser())
            .build()
    }

    fun sortSchemaCustomiser(): OpenApiCustomizer {
        return OpenApiCustomizer { openApi: OpenAPI ->
            with(openApi.components) {
                schemas(schemas.toSortedMap())
            }
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
                                .scopes(Scopes())
                        ).clientCredentials(
                            OAuthFlow().tokenUrl(securityProperties.tokenUrl)
                        )
                    )
                )
        )
            .addSecurityItem(SecurityRequirement().addList("open_id_scheme", emptyList()))
    }
}
