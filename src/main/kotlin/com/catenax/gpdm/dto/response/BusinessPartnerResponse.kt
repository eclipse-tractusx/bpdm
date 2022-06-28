package com.catenax.gpdm.dto.response


import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import com.catenax.gpdm.entity.BusinessPartnerType
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(name = "Business Partner Response", description = "Business partner record")
data class BusinessPartnerResponse (
    @Schema(description = "Business Partner Number, main identifier value for business partners")
    val bpn: String,
    @ArraySchema(arraySchema = Schema(description = "All identifiers of the business partner, including BPN information"))
    val identifiers: Collection<IdentifierResponse> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Names the partner goes by"))
    val names: Collection<NameResponse> = emptyList(),
    @Schema(description = "Legal form of the business partner")
    val legalForm: LegalFormResponse? = null,
    @Schema(description = "Current business status")
    val status: BusinessStatusResponse? = null,
    @ArraySchema(arraySchema = Schema(description = "Profile classifications"))
    val profileClassifications: Collection<ClassificationResponse> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "The partner types"))
    val types: Collection<TypeKeyNameUrlDto<BusinessPartnerType>> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Bank accounts of this partner"))
    val bankAccounts: Collection<BankAccountResponse> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Roles the partner takes in the Catena network"))
    val roles: Collection<TypeKeyNameDto<String>> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Relations to other business partners"))
    val relations: Collection<RelationResponse> = emptyList(),
    @Schema(description = "The timestamp the business partner data was last indicated to be still current")
    val currentness: Instant
)
