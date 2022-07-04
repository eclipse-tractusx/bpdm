package org.eclipse.tractusx.bpdm.gate.dto

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.request.*
import org.eclipse.tractusx.bpdm.common.model.BusinessPartnerType
import javax.validation.constraints.NotEmpty

data class LegalEntityRequest(
    val externalId: String,
    @Schema(description = "Business Partner Number")
    val bpn: String?,
    @ArraySchema(arraySchema = Schema(description = "Additional identifiers (except BPN)", required = false))
    val identifiers: Collection<IdentifierRequest> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Names the partner goes by"), minItems = 1)
    @field:NotEmpty
    val names: Collection<NameRequest>,
    @Schema(description = "Technical key of the legal form")
    val legalForm: String?,
    @Schema(description = "Current business status")
    val status: BusinessStatusRequest?,
    @ArraySchema(arraySchema = Schema(description = "Profile classifications", required = false))
    val profileClassifications: Collection<ClassificationRequest> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "The type of partner", required = false, defaultValue = "UNKNOWN"))
    val types: Collection<BusinessPartnerType> = listOf(BusinessPartnerType.UNKNOWN),
    @ArraySchema(arraySchema = Schema(description = "Bank accounts of this partner", required = false))
    val bankAccounts: Collection<BankAccountRequest> = emptyList()
)
