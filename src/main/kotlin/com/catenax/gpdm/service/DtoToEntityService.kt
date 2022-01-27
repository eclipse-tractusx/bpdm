package com.catenax.gpdm.service

import com.catenax.gpdm.controller.dto.*
import com.catenax.gpdm.entity.*
import com.catenax.gpdm.repository.*
import com.neovisionaries.i18n.CountryCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class DtoToEntityService(
    val bpnIssuingService: BpdnIssuingService,
    val issuingAgencyRepository: IssuingAgencyRepository,
    val legalFormRepository: LegalFormRepository,
    val administrativeAreaRepository: AdministrativeAreaRepository,
    val postCodeRepository: PostCodeRepository,
    val addressVersionRepository: AddressVersionRepository
) {



    @Transactional
    fun buildBusinessPartners(bpDtos: Collection<BusinessPartnerBaseDto>): Collection<BusinessPartner>{
        val bpAgencyDtos = bpDtos.flatMap { it.identifiers.mapNotNull { it.registration?.issuingAgency  } }
        val addressAgencyDtos = bpDtos.flatMap { it.addresses.flatMap { it.identifiers.mapNotNull { it.registration?.issuingAgency  }  } }
        val agencyMap = mapIssuingAgencies(bpAgencyDtos + addressAgencyDtos)

        val areaDtos = bpDtos.flatMap {
            it.addresses.flatMap {
                    address ->  address.administrativeAreas.map { area -> Pair(area, address.countryCode) }
            }
        }
        val areaMap = mapAdministrativeAreas(areaDtos)

        val postCodeDtos = bpDtos.flatMap {
            it.addresses.flatMap {
                address -> address.postCodes.map { postCode -> Pair(postCode, address.countryCode) }
            }
        }
        val postCodeMap = mapPostCodes(postCodeDtos)

        val versionDtos = bpDtos.flatMap { it.addresses.flatMap { address ->  address.versions } }
        val versionMap = mapAddressVersions(versionDtos)

       return bpDtos.map { buildBusinessPartner(it, areaMap, postCodeMap, versionMap, agencyMap) }
    }

    fun buildBusinessPartner(
        dto: BusinessPartnerBaseDto,
        areaMap: Map<AdministrativeAreaDto, AdministrativeArea>,
        postCodeMap: Map<PostCodeDto, PostCode>,
        versionMap: Map<AddressVersionDto, AddressVersion>,
        agencyMap: Map<BaseNamedDto, IssuingAgency>
    ): BusinessPartner{
        val bpn = bpnIssuingService.issueLegalEntity()

        val legalForm = toEntity(dto.legalForm)
        val partner = toEntity(dto, bpn, legalForm)

        partner.names = dto.names.map { toEntity(it, partner) }.toSet()
        partner.identifiers = dto.identifiers.map { toPartnerIdEntity(it, partner, agencyMap)}.plus(bpnIssuingService.createIdentifier(partner)).toSet()
        partner.addresses = dto.addresses.map { buildAddress(it, partner, areaMap, postCodeMap, versionMap, agencyMap) }.toSet()
        partner.classification = dto.profile?.classifications?.map { toEntity(it, partner) }?.toSet() ?: emptySet()
        partner.bankAccounts = dto.bankAccounts.map { toEntity(it, partner) }.toSet()
        partner.startNodeRelations = emptySet()
        partner.endNodeRelations = emptySet()

        return partner;
    }


    fun buildAddress(
        dto: AddressDto,
        partner: BusinessPartner,
        areaMap: Map<AdministrativeAreaDto, AdministrativeArea>,
        postCodeMap: Map<PostCodeDto, PostCode>,
        versionMap: Map<AddressVersionDto, AddressVersion>,
        agencyMap: Map<BaseNamedDto, IssuingAgency>
    ): Address{
        val bpn = bpnIssuingService.issueAddress()
        val careOf = if(dto.careOf != null) toCareOfEntity(dto.careOf as BaseNamedDto) else null

        val address = Address(bpn,
            careOf,
            dto.countryCode,
            dto.administrativeAreas.map { areaMap[it]!! }.toSet(),
            dto.postCodes.map { postCodeMap[it]!! }.toSet(),
            dto.type,
            dto.versions.map { versionMap[it]!! }.toSet(),
            partner
        )

        address.identifiers = dto.identifiers.map { buildIdentifier(it, address, agencyMap) }.plus(bpnIssuingService.createIdentifier(address)).toSet()
        address.thoroughfares = dto.thoroughfares.map { toEntity(it, address) }.toSet()
        address.localities = dto.localities.map { toEntity(it, address) }.toSet()
        address.premises = dto.premises.map { toEntity(it, address) }.toSet()
        address.postalDeliveryPoints = dto.postalDeliveryPoints.map { toEntity(it, address) }.toSet()

        return address
    }

    fun buildIdentifier(dto: IdentifierDto, address: Address, agencyMap: Map<BaseNamedDto, IssuingAgency>): IdentifierAddress{
        val registration = if(dto.registration != null) toEntity(dto.registration as RegistrationDto, agencyMap) else null
        return IdentifierAddress(dto.nameComponent.value, dto.nameComponent.shortName, dto.nameComponent.number, dto.type, registration, address)
    }

    fun toEntity(dto: BusinessPartnerBaseDto, bpn: String, legalForm: LegalForm): BusinessPartner {
        return BusinessPartner(bpn, legalForm, dto.status, dto.types.toSet(), dto.roles.toSet())
    }

    fun toEntity(dto: BankAccountDto, partner: BusinessPartner): BankAccount{
        return BankAccount(dto.trustScores.toSet(),
            dto.currencyCode,
            dto.internationalBankAccountIdentifier,
            dto.internationalBankIdentifier,
            dto.nationalBankAccountIdentifier,
            dto.nationalBankIdentifier,
            partner
        )
    }

    fun toCareOfEntity(dto: BaseNamedDto): CareOf{
        return CareOf(dto.value, dto.shortName, dto.number)
    }

    fun toEntity(dto: NameDto, partner: BusinessPartner): Name{
        return Name(dto.nameComponent.value, dto.nameComponent.shortName, dto.nameComponent.number, dto.type, partner)
    }

    fun toEntity(dto: ClassificationDto, partner: BusinessPartner): Classification{
        return Classification(dto.nameComponent.value, dto.nameComponent.shortName, dto.nameComponent.number, dto.type, partner)
    }

    fun toPartnerIdEntity(dto: IdentifierDto, partner: BusinessPartner, agencyMap: Map<BaseNamedDto, IssuingAgency>): IdentifierPartner{
        return toPartnerIdEntity(dto, partner, if(dto.registration!= null) toEntity(dto.registration as RegistrationDto, agencyMap) else null)
    }

    fun toPartnerIdEntity(dto: IdentifierDto, partner: BusinessPartner, registration: Registration?): IdentifierPartner{
        return IdentifierPartner(dto.nameComponent.value, dto.nameComponent.shortName, dto.nameComponent.number, dto.type, registration, partner)
    }

    fun toEntity(dto: RegistrationDto,  agencyMap: Map<BaseNamedDto, IssuingAgency>): Registration{
        return toEntity(dto, agencyMap[dto.issuingAgency]!!)
    }

    fun toEntity(dto: RegistrationDto, issuingAgency: IssuingAgency): Registration{
        return Registration(dto.hardeningGrade, issuingAgency, dto.status, dto.initialRegistration, dto.lastUpdate)
    }

    fun toEntity(dto: BaseNamedDto): IssuingAgency{
        return IssuingAgency(dto.value, dto.shortName, dto.number)
    }

    fun toEntity(dto: LegalFormDto): LegalForm{
        return LegalForm(dto.nameComponent.value, dto.nameComponent.shortName, dto.nameComponent.number, dto.type)
    }

    fun toEntity(dto: ThoroughfareDto, address: Address): Thoroughfare{
        return Thoroughfare(dto.nameComponent.value, dto.nameComponent.shortName, dto.nameComponent.number, dto.type, address)
    }

    fun toEntity(dto: LocalityDto, address: Address): Locality{
        return Locality(dto.nameComponent.value, dto.nameComponent.shortName, dto.nameComponent.number, dto.type, address)
    }

    fun toEntity(dto: PremiseDto, address: Address): Premise{
        return Premise(dto.nameComponent.value, dto.nameComponent.shortName, dto.nameComponent.number, dto.type, address)
    }

    fun toEntity(dto: PostalDeliveryPointDto, address: Address): PostalDeliveryPoint{
        return PostalDeliveryPoint(dto.nameComponent.value, dto.nameComponent.shortName, dto.nameComponent.number, dto.type, address)
    }

    fun toEntity(dto: AdministrativeAreaDto, uuid: UUID, countryCode: CountryCode): AdministrativeArea {
        val area = AdministrativeArea(uuid, dto.name, dto.type, countryCode)
        area.codes = dto.codes.map { toEntity(it, area) }.toSet()
        return area
    }

    fun toUUID(dto: AdministrativeAreaDto, countryCode: CountryCode): UUID {
        val uuidName = dto.name+dto.type+countryCode.name
        return uuid3(Namespace.OID, uuidName)
    }

    fun toEntity(dto: PostCodeDto, uuid: UUID, countryCode: CountryCode): PostCode {
        return PostCode(dto.nameComponent.value, dto.nameComponent.shortName, dto.nameComponent.number, uuid, dto.type, countryCode)
    }

    fun toUUID(dto: PostCodeDto, countryCode: CountryCode): UUID {
        val uuidName = dto.nameComponent.value+countryCode.name
        return uuid3(Namespace.OID, uuidName)
    }

    fun toEntity(dto: AddressVersionDto, uuid: UUID): AddressVersion {
        return AddressVersion(uuid, dto.characterSet, dto.languageCode)
    }

    fun toUUID(dto: AddressVersionDto): UUID {
        val uuidName = dto.characterSet.name+dto.languageCode.name
        return uuid3(Namespace.OID, uuidName)
    }

    fun toEntity(dto: AdministrativeAreaCodeDto, area: AdministrativeArea): AdministrativeAreaCode {
        return AdministrativeAreaCode(dto.value, dto.type, area)
    }


    fun mapIssuingAgencies(dtos: Collection<BaseNamedDto>): Map<BaseNamedDto, IssuingAgency>{
        val dtoGroups = dtos.groupBy { it.value }
        val existingEntities = issuingAgencyRepository.findAllByValueIn(dtoGroups.keys)
        val existingKeys = existingEntities.map { it.value }.toSet()
        val notExistingEntities = dtoGroups.minus(existingKeys).values.flatMap { it.map { toEntity(it) } }
        val allEntities = existingEntities + notExistingEntities
        return allEntities.flatMap { entity -> dtoGroups[entity.value]!!.map { dto -> dto to entity } }.toMap()
    }

    fun mapAdministrativeAreas(dtos: Collection<Pair<AdministrativeAreaDto, CountryCode>>): Map<AdministrativeAreaDto, AdministrativeArea>{
        val dtoGroups = dtos.groupBy { toUUID(it.first, it.second) }
        val existingEntities = administrativeAreaRepository.findByUuidIn(dtoGroups.keys)
        val existingKeys = existingEntities.map { it.uuid }.toSet()
        val notExistingEntities = dtoGroups.minus(existingKeys).flatMap { entry -> entry.value.map{ toEntity(it.first, entry.key, it.second) } }
        val allEntities = existingEntities + notExistingEntities
        return allEntities.flatMap { entity -> dtoGroups[entity.uuid]!!.map { pair -> pair.first to entity } }.toMap()
    }

    fun mapPostCodes(dtos: Collection<Pair<PostCodeDto, CountryCode>>): Map<PostCodeDto, PostCode>{
        val dtoGroups = dtos.groupBy { toUUID(it.first, it.second) }
        val existingEntities = postCodeRepository.findByUuidIn(dtoGroups.keys)
        val existingKeys = existingEntities.map { it.uuid }.toSet()
        val notExistingEntities = dtoGroups.minus(existingKeys).flatMap { entry -> entry.value.map{  toEntity(it.first, entry.key, it.second) } }
        val allEntities = existingEntities + notExistingEntities
        return allEntities.flatMap { entity -> dtoGroups[entity.uuid]!!.map { pair -> pair.first to entity } }.toMap()
    }

    fun mapAddressVersions(dtos: Collection<AddressVersionDto>): Map<AddressVersionDto, AddressVersion>{
        val dtoGroups = dtos.groupBy { toUUID(it) }
        val existingEntities = addressVersionRepository.findByUuidIn(dtoGroups.keys)
        val existingKeys = existingEntities.map { it.uuid }.toSet()
        val notExistingEntities = dtoGroups.minus(existingKeys).flatMap { entry -> entry.value.map { toEntity(it, entry.key) } }
        val allEntities = existingEntities + notExistingEntities
        return allEntities.flatMap { entity -> dtoGroups[entity.uuid]!!.map { dto -> dto to entity } }.toMap()
    }

    fun <S, T: BaseNamedEntity> mapNamedEntities(valueDtoMap: Map<String, S>, existingEntities: Set<T>, newEntityFun: (S)->(T)): Map<S, T>{
        val existingKeys = existingEntities.map { it.value }.toSet()
        val notExistingEntities = valueDtoMap.minus(existingKeys).values.map { newEntityFun(it) }
        val allEntities = existingEntities + notExistingEntities
        return allEntities.associateBy { valueDtoMap[it.value]!! }
    }



}