package com.catenax.gpdm.config

import com.catenax.gpdm.dto.response.PageResponse
import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import com.catenax.gpdm.dto.response.type.TypeNameUrlDto
import com.catenax.gpdm.entity.CharacterSet
import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.OpenAPIService
import org.springdoc.core.SpringDocUtils
import org.springdoc.core.customizers.OpenApiBuilderCustomizer
import org.springdoc.core.customizers.OpenApiCustomiser
import org.springdoc.core.customizers.PropertyCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType


@Configuration
class OpenApiConfig(
    val securityProperties: SecurityConfigProperties,
    val infoProperties: AppInfoProperties
) {

    @Bean
    fun bpdmOpenApiDefinition(): OpenAPI{
        val definition = OpenAPI()
            .addServersItem(Server().url("/"))
            .info(Info().title(infoProperties.name).description(infoProperties.description).version(infoProperties.version))

        return if(securityProperties.enabled) addSecurity(definition) else definition
    }

    private fun addSecurity(definition: OpenAPI): OpenAPI{
        return definition
            .components(Components()
                .addSecuritySchemes("open_id_scheme",
                    SecurityScheme().type(SecurityScheme.Type.OAUTH2).flows(
                        OAuthFlows().authorizationCode(
                            OAuthFlow().authorizationUrl(securityProperties.authUrl)
                                .tokenUrl(securityProperties.tokenUrl)
                                .refreshUrl(securityProperties.refreshUrl)))))
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

