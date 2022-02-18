package com.catenax.gpdm.service

import com.catenax.gpdm.dto.GeoCoordinateDto
import com.catenax.gpdm.dto.request.*
import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import com.catenax.gpdm.entity.*
import com.catenax.gpdm.exception.BpdmMultipleNotfound
import com.catenax.gpdm.repository.entity.IdentifierStatusRepository
import com.catenax.gpdm.repository.entity.IdentifierTypeRepository
import com.catenax.gpdm.repository.entity.IssuingBodyRepository
import com.catenax.gpdm.repository.entity.LegalFormRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RequestConversionService(
    val bpnIssuingService: BpnIssuingService,
    val legalFormRepository: LegalFormRepository,
    val identifierTypeRepository: IdentifierTypeRepository,
    val issuingBodyRepository: IssuingBodyRepository,
    val identifierStatusRepository: IdentifierStatusRepository
) {



    @Transactional
    fun buildBusinessPartners(bpDtos: Collection<BusinessPartnerRequest>): Collection<BusinessPartner>{

        val idTypeMap = mapIdentifierTypes(bpDtos.flatMap { it.identifiers.map { id -> id.type } }.toSet())
        val idStatusMap = mapIdentifierStati(bpDtos.flatMap { it.identifiers.mapNotNull { id -> id.status } }.toSet())
        val issueBodyMap = mapIssuingBodies(bpDtos.flatMap { it.identifiers.mapNotNull{  id -> id.issuingBody } }.toSet())
        val legalFormMap = mapLegalForms(bpDtos.mapNotNull { it.legalForm }.toSet())

       return bpDtos.map { buildBusinessPartner(it, idTypeMap, idStatusMap, issueBodyMap, legalFormMap) }
    }

    fun buildBusinessPartner(
        dto: BusinessPartnerRequest,
        idTypeMap: Map<String, IdentifierType>,
        idStatusMap: Map<String, IdentifierStatus>,
        issueBodyMap: Map<String, IssuingBody>,
        legalFormMap: Map<String, LegalForm>
    ): BusinessPartner{
        val bpn = bpnIssuingService.issueLegalEntity()
        val legalForm = if(dto.legalForm != null) legalFormMap[dto.legalForm]!! else null

        val partner = toEntity(dto, bpn, legalForm)

        partner.stati = if(dto.status != null) setOf(toEntity(dto.status, partner)) else setOf()
        partner.names = dto.names.map { toEntity(it, partner) }.toSet()
        partner.identifiers = dto.identifiers.map { toEntity(it, idTypeMap, idStatusMap, issueBodyMap, partner)}
            .plus(bpnIssuingService.createIdentifier(partner)).toSet()
        partner.addresses = dto.addresses.map { buildAddress(it, partner) }.toSet()
        partner.classification = dto.profileClassifications.map { toEntity(it, partner) }.toSet()
        partner.bankAccounts = dto.bankAccounts.map { toEntity(it, partner) }.toSet()
        partner.startNodeRelations = emptySet()
        partner.endNodeRelations = emptySet()

        return partner
    }


    fun buildAddress(
        dto: AddressRequest,
        partner: BusinessPartner
    ): Address{
        val address = Address(
            dto.careOf,
            dto.contexts.toSet(),
            dto.country,
            dto.types.toSet(),
            toEntity(dto.version),
            dto.geographicCoordinates?.let { toEntity(dto.geographicCoordinates) },
            partner
        )

        address.administrativeAreas = dto.administrativeAreas.map { toEntity(it, address) }.toSet()
        address.postCodes = dto.postCodes.map { toEntity(it, address) }.toSet()
        address.thoroughfares = dto.thoroughfares.map { toEntity(it, address) }.toSet()
        address.localities = dto.localities.map { toEntity(it, address) }.toSet()
        address.premises = dto.premises.map { toEntity(it, address) }.toSet()
        address.postalDeliveryPoints = dto.postalDeliveryPoints.map { toEntity(it, address) }.toSet()

        return address
    }

    fun buildLegalForm(dto: LegalFormRequest): LegalForm{
        val categories = dto.category.map { LegalFormCategory(it.name, it.url) }.toSet()
        return LegalForm(dto.name, dto.url, dto.language, dto.mainAbbreviation, categories, dto.technicalKey)
    }

    fun toEntity(dto: BusinessPartnerRequest, bpn: String, legalForm: LegalForm?): BusinessPartner {
        return BusinessPartner(bpn, legalForm, dto.types.toSet(), emptySet())
    }

    private fun toEntity(dto: BusinessStatusRequest, partner: BusinessPartner): BusinessStatus{
        return BusinessStatus(dto.officialDenotation, dto.validFrom, dto.validUntil, dto.type, partner)
    }

    fun toEntity(dto: BankAccountRequest, partner: BusinessPartner): BankAccount{
        return BankAccount(dto.trustScores.toSet(),
            dto.currency,
            dto.internationalBankAccountIdentifier,
            dto.internationalBankIdentifier,
            dto.nationalBankAccountIdentifier,
            dto.nationalBankIdentifier,
            partner
        )
    }

    fun toEntity(dto: NameRequest, partner: BusinessPartner): Name{
        return Name(dto.value, dto.shortName, dto.type, dto.language, partner)
    }

    fun toEntity(dto: ClassificationRequest, partner: BusinessPartner): Classification{
        return Classification(dto.value,dto.code, dto.type, partner)
    }

    fun toEntity(
        dto: IdentifierRequest,
        idTypeMap: Map<String, IdentifierType>,
        idStatusMap: Map<String, IdentifierStatus>,
        issueBodyMap: Map<String, IssuingBody>,
        partner: BusinessPartner): Identifier{
        return toEntity(dto,
            idTypeMap[dto.type]!!,
            if(dto.status != null) idStatusMap[dto.status]!! else null,
            if(dto.issuingBody != null) issueBodyMap[dto.issuingBody]!! else null,
            partner)
    }

    fun toEntity(dto: IdentifierRequest, type: IdentifierType, status: IdentifierStatus?,  issuingBody: IssuingBody?, partner: BusinessPartner): Identifier{
        return Identifier(dto.value, type, status, issuingBody, partner)
    }

    private fun toEntity(dto: AddressVersionRequest): AddressVersion{
        return AddressVersion(dto.characterSet, dto.language)
    }

    private fun toEntity(dto: GeoCoordinateDto): GeographicCoordinate{
        return GeographicCoordinate(dto.latitude, dto.longitude, dto.altitude)
    }

    fun toEntity(dto: ThoroughfareRequest, address: Address): Thoroughfare{
        return Thoroughfare(dto.value, dto.name, dto.shortName, dto.number, dto.direction, dto.type, address.version.language, address)
    }

    fun toEntity(dto: LocalityRequest, address: Address): Locality{
        return Locality(dto.value, dto.shortName, dto.type, address.version.language, address)
    }

    fun toEntity(dto: PremiseRequest, address: Address): Premise{
        return Premise(dto.value, dto.shortName, dto.number, dto.type, address.version.language, address)
    }

    fun toEntity(dto: PostalDeliveryPointRequest, address: Address): PostalDeliveryPoint{
        return PostalDeliveryPoint(dto.value, dto.shortName, dto.number, dto.type, address.version.language, address)
    }

    fun toEntity(dto: AdministrativeAreaRequest, address: Address): AdministrativeArea {
       return AdministrativeArea(dto.value, dto.shortName, dto.fipsCode, dto.type, address.version.language, address.country, address)
    }

    fun toEntity(dto: PostCodeRequest, address: Address): PostCode {
        return PostCode(dto.value, dto.type, address.country, address)
    }

    fun toIdTypeEntity(dto: TypeKeyNameUrlDto<String>): IdentifierType{
        return IdentifierType(dto.name, dto.url, dto.technicalKey)
    }

    fun toIdStatusEntity(dto: TypeKeyNameDto<String>): IdentifierStatus{
        return IdentifierStatus(dto.name, dto.technicalKey)
    }


    fun toIssuerEntity(dto: TypeKeyNameUrlDto<String>): IssuingBody{
        return IssuingBody(dto.name, dto.url, dto.technicalKey)
    }

    private fun mapIdentifierTypes(keys: Set<String>): Map<String, IdentifierType>{
        val typeMap = identifierTypeRepository.findByTechnicalKeyIn(keys).associateBy { it.technicalKey }
        assertKeysFound(keys, typeMap)
        return typeMap
    }

    private fun mapIdentifierStati(keys: Set<String>): Map<String, IdentifierStatus>{
        val typeMap = identifierStatusRepository.findByTechnicalKeyIn(keys).associateBy { it.technicalKey }
        assertKeysFound(keys, typeMap)
        return typeMap
    }

    private fun mapIssuingBodies(keys: Set<String>): Map<String, IssuingBody>{
        val typeMap = issuingBodyRepository.findByTechnicalKeyIn(keys).associateBy { it.technicalKey }
        assertKeysFound(keys, typeMap)
        return typeMap
    }

    private fun mapLegalForms(keys: Set<String>): Map<String, LegalForm>{
        val typeMap = legalFormRepository.findByTechnicalKeyIn(keys).associateBy { it.technicalKey }
        assertKeysFound(keys, typeMap)
        return typeMap
    }

    private inline fun <reified T> assertKeysFound(keys: Set<String>, typeMap: Map<String, T>){
        val keysNotfound = keys.minus(typeMap.keys)
        if(keysNotfound.isNotEmpty()) throw BpdmMultipleNotfound(T::class.simpleName!!, keysNotfound )
    }
}