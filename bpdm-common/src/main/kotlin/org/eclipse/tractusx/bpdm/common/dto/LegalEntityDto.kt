package org.eclipse.tractusx.bpdm.common.dto

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.model.BusinessPartnerType

@Schema(name = "Legal Entity")
data class LegalEntityDto(
    @Schema(description = "ID the record has in the external system where the record originates from", required = true)
    val externalId: String,
    @Schema(description = "Business Partner Number")
    val bpn: String?,
    @ArraySchema(arraySchema = Schema(description = "Additional identifiers (except BPN)", required = false))
    val identifiers: Collection<IdentifierDto> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Names the partner goes by"), minItems = 1)
    val names: Collection<NameDto>,
    @Schema(description = "Technical key of the legal form")
    val legalForm: String?,
    @Schema(description = "Current business status")
    val status: BusinessStatusDto?,
    @ArraySchema(arraySchema = Schema(description = "Profile classifications", required = false))
    val profileClassifications: Collection<ClassificationDto> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "The type of partner", required = false, defaultValue = "[\"UNKNOWN\"]"))
    val types: Collection<BusinessPartnerType> = listOf(BusinessPartnerType.UNKNOWN),
    @ArraySchema(arraySchema = Schema(description = "Bank accounts of this partner", required = false))
    val bankAccounts: Collection<BankAccountDto> = emptyList()
)
