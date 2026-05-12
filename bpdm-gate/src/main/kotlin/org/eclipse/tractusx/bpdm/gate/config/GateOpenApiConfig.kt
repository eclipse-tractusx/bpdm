package org.eclipse.tractusx.bpdm.gate.config
import org.eclipse.tractusx.bpdm.common.util.OpenApiExampleCustomizer
import org.eclipse.tractusx.bpdm.gate.util.BusinessPartnerInputRequestValues
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration




@Configuration
class GateOpenApiConfig {

    @Bean
    fun openApiExampleCustomizer(): OpenApiExampleCustomizer {
        return OpenApiExampleCustomizer(
            listOf(BusinessPartnerInputRequestValues.businessPartnerInputRequestExample)
        )
    }
}