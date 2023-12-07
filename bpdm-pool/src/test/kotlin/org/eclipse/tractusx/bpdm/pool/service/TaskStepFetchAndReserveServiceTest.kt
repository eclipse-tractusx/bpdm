package org.eclipse.tractusx.bpdm.pool.service

import com.neovisionaries.i18n.CountryCode
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.dto.IBaseSiteStateDto
import org.eclipse.tractusx.bpdm.common.dto.ILegalEntityStateDto
import org.eclipse.tractusx.bpdm.common.dto.response.*
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.ClassificationType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolClientImpl
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityClassificationVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityIdentifierVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityStateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SitePoolVerboseDto
import org.eclipse.tractusx.bpdm.pool.repository.BpnRequestIdentifierRepository
import org.eclipse.tractusx.bpdm.pool.service.TaskStepBuildService.CleaningError
import org.eclipse.tractusx.bpdm.pool.util.BusinessPartnerVerboseValues
import org.eclipse.tractusx.bpdm.pool.util.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.pool.util.TestHelpers
import org.eclipse.tractusx.bpdm.pool.util.BusinessPartnerNonVerboseValues.addressIdentifierTypeDto1
import org.eclipse.tractusx.bpdm.pool.util.BusinessPartnerNonVerboseValues.addressIdentifierTypeDto2
import org.eclipse.tractusx.orchestrator.api.model.*
import org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType.BpnRequestIdentifier
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.function.BiPredicate


@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class]
)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class TaskStepFetchAndReserveServiceTest @Autowired constructor(
    val cleaningStepService: TaskStepFetchAndReserveService,
    val bpnRequestIdentifierRepository: BpnRequestIdentifierRepository,
    val testHelpers: TestHelpers,
    val poolClient: PoolClientImpl
) {


    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        testHelpers.createTestMetadata()
    }

    @Test
    fun `create empty legal entity`() {

        val fullBpWithLegalEntity = minFullBusinessPartner().copy(
            legalEntity = emptyLegalEntity()
        )

        val result = cleanStep(taskId = "TASK_1", businessPartner = fullBpWithLegalEntity)
        assertTaskError(result[0], "TASK_1", CleaningError.LEGAL_ENTITY_IS_NULL)
    }

    @Test
    fun `create legal entity without legal name`() {

        val fullBpWithLegalEntity = minFullBusinessPartner().copy(
            legalEntity = emptyLegalEntity().copy(
                bpnLReference = BpnReferenceDto(referenceValue = "123", referenceType = BpnRequestIdentifier),
                legalAddress = LogisticAddressDto()
            )
        )

        val result = cleanStep(taskId = "TASK_1", businessPartner = fullBpWithLegalEntity)
        assertTaskError(result[0], "TASK_1", CleaningError.LEGAL_NAME_IS_NULL)
    }

    @Test
    fun `create min legal entity`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val fullBpWithLegalEntity = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = leRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = leAddressRefValue, referenceType = BpnRequestIdentifier)
            )
        )
        val resultSteps = cleanStep(taskId = "TASK_1", businessPartner = fullBpWithLegalEntity)
        assertThat(resultSteps[0].taskId).isEqualTo("TASK_1")
        assertThat(resultSteps[0].errors.size).isEqualTo(0)

        val bpnMappings = bpnRequestIdentifierRepository.findDistinctByRequestIdentifierIn(listOf(leRefValue, leAddressRefValue))
        assertThat(bpnMappings.size).isEqualTo(2)

        val createdLegalEntity = poolClient.legalEntities.getLegalEntity(resultSteps[0].businessPartner?.legalEntity?.bpnLReference?.referenceValue!!)
        assertThat(createdLegalEntity.legalAddress.bpnLegalEntity).isNotNull()
        assertThat(resultSteps[0].businessPartner?.generic?.legalEntityBpn).isEqualTo(createdLegalEntity.legalEntity.bpnl)
        compareLegalEntity(createdLegalEntity, resultSteps[0].businessPartner?.legalEntity)
    }

    @Test
    fun `create legal entity with all fields`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val fullBpWithLegalEntity = minFullBusinessPartner().copy(
            legalEntity = fullValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = leRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = leAddressRefValue, referenceType = BpnRequestIdentifier)
            )
        )
        val resultSteps = cleanStep(taskId = "TASK_1", businessPartner = fullBpWithLegalEntity)
        assertThat(resultSteps[0].taskId).isEqualTo("TASK_1")
        assertThat(resultSteps[0].errors.size).isEqualTo(0)

        val bpnMappings = bpnRequestIdentifierRepository.findDistinctByRequestIdentifierIn(listOf(leRefValue, leAddressRefValue))
        assertThat(bpnMappings.size).isEqualTo(2)

        val createdLegalEntity = poolClient.legalEntities.getLegalEntity(resultSteps[0].businessPartner?.legalEntity?.bpnLReference?.referenceValue!!)
        assertThat(createdLegalEntity.legalAddress.bpnLegalEntity).isNotNull()
        assertThat(resultSteps[0].businessPartner?.generic?.legalEntityBpn).isEqualTo(createdLegalEntity.legalEntity.bpnl)
        compareLegalEntity(createdLegalEntity, resultSteps[0].businessPartner?.legalEntity)
    }

    @Test
    fun `check that requests with same referenceValue don't create a new legal entity`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val fullBpWithLegalEntity = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = leRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = leAddressRefValue, referenceType = BpnRequestIdentifier)
            )
        )
        val resultSteps1 = cleanStep(taskId = "TASK_1", businessPartner = fullBpWithLegalEntity)
        assertThat(resultSteps1[0].taskId).isEqualTo("TASK_1")
        assertThat(resultSteps1[0].errors.size).isEqualTo(0)
        val createdLegalEntity1 = poolClient.legalEntities.getLegalEntity(resultSteps1[0].businessPartner?.legalEntity?.bpnLReference?.referenceValue!!)

        val resultSteps2 = cleanStep(taskId = "TASK_2", businessPartner = fullBpWithLegalEntity)
        assertThat(resultSteps2[0].taskId).isEqualTo("TASK_2")
        assertThat(resultSteps2[0].errors.size).isEqualTo(0)
        assertThat(createdLegalEntity1.legalEntity.bpnl).isEqualTo(resultSteps2[0].businessPartner?.legalEntity?.bpnLReference?.referenceValue!!)
    }

    @Test
    fun `create legal entity with different referenceValues `() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val fullBpWithLegalEntity = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = leRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = leAddressRefValue, referenceType = BpnRequestIdentifier)
            )
        )
        val resultSteps1 = cleanStep(taskId = "TASK_1", businessPartner = fullBpWithLegalEntity)
        assertThat(resultSteps1[0].taskId).isEqualTo("TASK_1")
        assertThat(resultSteps1[0].errors.size).isEqualTo(0)
        val createdLegalEntity1 = poolClient.legalEntities.getLegalEntity(resultSteps1[0].businessPartner?.legalEntity?.bpnLReference?.referenceValue!!)

        val leRefValue2 = "diffenrentBpnL"
        val leAddressRefValue2 = "diffenrentBpnA"
        val fullBpWithLegalEntity2 = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = leRefValue2, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = leAddressRefValue2, referenceType = BpnRequestIdentifier)
            )
        )

        val resultSteps2 = cleanStep(taskId = "TASK_2", businessPartner = fullBpWithLegalEntity2)
        val bpnMappings =
            bpnRequestIdentifierRepository.findDistinctByRequestIdentifierIn(listOf(leRefValue, leAddressRefValue, leRefValue2, leAddressRefValue2))
        assertThat(bpnMappings.size).isEqualTo(4)

        assertThat(resultSteps2[0].taskId).isEqualTo("TASK_2")
        assertThat(resultSteps2[0].errors.size).isEqualTo(0)
        assertThat(createdLegalEntity1.legalEntity.bpnl).isNotEqualTo(resultSteps2[0].businessPartner?.legalEntity?.bpnLReference?.referenceValue!!)
        val createdLegalEntity2 = poolClient.legalEntities.getLegalEntity(resultSteps2[0].businessPartner?.legalEntity?.bpnLReference?.referenceValue!!)
        assertThat(resultSteps2[0].businessPartner?.generic?.legalEntityBpn).isEqualTo(createdLegalEntity2.legalEntity.bpnl)
    }


    @Test
    fun `create Site with minimal fields`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val siteRefValue = "siteRefValue"
        val mainAddressRefValue = "mainAddressRefValue"
        val fullBpWithSite = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = leRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = leAddressRefValue, referenceType = BpnRequestIdentifier)
            ),
            site = minValidSite(
                bpnSReference = BpnReferenceDto(referenceValue = siteRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = mainAddressRefValue, referenceType = BpnRequestIdentifier)
            )
        )

        val result = cleanStep(taskId = "TASK_1", businessPartner = fullBpWithSite)
        val createdSite = poolClient.sites.getSite(result[0].businessPartner?.site?.bpnSReference?.referenceValue!!)
        assertThat(result[0].taskId).isEqualTo("TASK_1")
        assertThat(result[0].errors).hasSize(0)
        assertThat(createdSite.site.name).isEqualTo(fullBpWithSite.site?.name)
    }

    @Test
    fun `create Site with all fields`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val siteRefValue = "siteRefValue"
        val mainAddressRefValue = "mainAddressRefValue"
        val fullBpWithSite = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = leRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = leAddressRefValue, referenceType = BpnRequestIdentifier)
            ),
            site = fullValidSite(
                bpnSReference = BpnReferenceDto(referenceValue = siteRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = mainAddressRefValue, referenceType = BpnRequestIdentifier)
            )
        )

        val result = cleanStep(taskId = "TASK_1", businessPartner = fullBpWithSite)
        val createdSite = poolClient.sites.getSite(result[0].businessPartner?.site?.bpnSReference?.referenceValue!!)
        assertThat(result[0].taskId).isEqualTo("TASK_1")
        assertThat(result[0].errors).hasSize(0)
        assertThat(createdSite.site.name).isEqualTo(fullBpWithSite.site?.name)
        compareSite(createdSite, result[0].businessPartner?.site)
    }

    @Test
    fun `update Site from orchestrator`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val siteRefValue = "siteRefValue"
        val mainAddressRefValue = "mainAddressRefValue"
        val fullBpWithSite = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = leRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = leAddressRefValue, referenceType = BpnRequestIdentifier)
            ),
            site = minValidSite(
                bpnSReference = BpnReferenceDto(referenceValue = siteRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = mainAddressRefValue, referenceType = BpnRequestIdentifier)
            )
        )

        val result = cleanStep(taskId = "TASK_1", businessPartner = fullBpWithSite)
        val createdSite = poolClient.sites.getSite(result[0].businessPartner?.site?.bpnSReference?.referenceValue!!)
        assertThat(createdSite.site.name).isEqualTo(fullBpWithSite.site?.name)

        val updateCopy = fullBpWithSite.copy(
            site = fullBpWithSite.site?.copy(name = "ChangedName", hasChanged = true)
        )
        val result2 = cleanStep(taskId = "TASK_1", businessPartner = updateCopy)
        val updatedSite = poolClient.sites.getSite(result2[0].businessPartner?.site?.bpnSReference?.referenceValue!!)
        compareSite(updatedSite, result2[0].businessPartner?.site)
    }

    @Test
    fun `create site address with minimal fields`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val siteRefValue = "siteRefValue"
        val mainAddressRefValue = "mainAddressRefValue"
        val additionalAddressRefValue = "77"
        val fullBpWithAddress = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = leRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = leAddressRefValue, referenceType = BpnRequestIdentifier)
            ),
            site = minValidSite(
                bpnSReference = BpnReferenceDto(referenceValue = siteRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = mainAddressRefValue, referenceType = BpnRequestIdentifier)
            ),
            address = minLogisticAddress(
                bpnAReference = BpnReferenceDto(referenceValue = additionalAddressRefValue, referenceType = BpnRequestIdentifier),
            )
        )

        val result = cleanStep(taskId = "TASK_1", businessPartner = fullBpWithAddress)
        val createdLeAddress = poolClient.addresses.getAddress(result[0].businessPartner?.legalEntity?.legalAddress?.bpnAReference?.referenceValue!!)
        val createdAdditionalAddress = poolClient.addresses.getAddress(result[0].businessPartner?.address?.bpnAReference?.referenceValue!!)
        assertThat(result[0].taskId).isEqualTo("TASK_1")
        assertThat(result[0].errors).hasSize(0)
        assertThat(createdLeAddress.name).isEqualTo(fullBpWithAddress.address?.name)
        compareLogisticAddress(createdAdditionalAddress, result[0].businessPartner?.address)
        assertThat(createdAdditionalAddress.bpnLegalEntity).isNull()
        assertThat(createdAdditionalAddress.bpnSite).isEqualTo(result[0].businessPartner?.site?.bpnSReference?.referenceValue)
        assertThat(createdAdditionalAddress.isLegalAddress).isFalse()
        assertThat(createdAdditionalAddress.isMainAddress).isFalse()
    }

    @Test
    fun `create address with all fields`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val additionalAddressRefValue = "77"
        val fullBpWithAddress = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = leRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = leAddressRefValue, referenceType = BpnRequestIdentifier)
            ),
            address = fullLogisticAddressDto(
                bpnAReference = BpnReferenceDto(referenceValue = additionalAddressRefValue, referenceType = BpnRequestIdentifier)
            )
        )

        val result = cleanStep(taskId = "TASK_1", businessPartner = fullBpWithAddress)
        val createdLeAddress = poolClient.addresses.getAddress(result[0].businessPartner?.legalEntity?.legalAddress?.bpnAReference?.referenceValue!!)
        val createdAdditionalAddress = poolClient.addresses.getAddress(result[0].businessPartner?.address?.bpnAReference?.referenceValue!!)
        assertThat(result[0].taskId).isEqualTo("TASK_1")
        assertThat(result[0].errors).hasSize(0)
        assertThat(createdLeAddress.isLegalAddress).isTrue()
        assertThat(createdAdditionalAddress.isMainAddress).isFalse()
        assertThat(createdAdditionalAddress.isLegalAddress).isFalse()
        compareLogisticAddress(createdAdditionalAddress, result[0].businessPartner?.address)
    }

    fun cleanStep(taskId: String, businessPartner: BusinessPartnerFullDto): List<TaskStepResultEntryDto> {

        val steps = singleTaskStep(taskId = taskId, businessPartner = businessPartner)
        return cleaningStepService.upsertGoldenRecordIntoPool(steps)
    }

    fun singleTaskStep(taskId: String, businessPartner: BusinessPartnerFullDto): List<TaskStepReservationEntryDto> {

        return listOf(
            TaskStepReservationEntryDto(
                taskId = taskId,
                businessPartner = businessPartner
            )
        )
    }

    fun minFullBusinessPartner(): BusinessPartnerFullDto {

        return BusinessPartnerFullDto(generic = BusinessPartnerGenericDto())
    }

    fun emptyLegalEntity(): LegalEntityDto {

        return LegalEntityDto()
    }

    fun minValidLegalEntity(bpnLReference: BpnReferenceDto, bpnAReference: BpnReferenceDto): LegalEntityDto {

        return LegalEntityDto(
            bpnLReference = bpnLReference,
            legalName = "legalName_" + bpnLReference.referenceValue,
            legalAddress = minLogisticAddress(bpnAReference = bpnAReference)
        )
    }

    fun fullValidLegalEntity(bpnLReference: BpnReferenceDto, bpnAReference: BpnReferenceDto): LegalEntityDto {

        return LegalEntityDto(
            bpnLReference = bpnLReference,
            legalName = "legalName_" + bpnLReference.referenceValue,
            legalShortName = "shortName_" + bpnLReference.referenceValue,
            legalForm = BusinessPartnerVerboseValues.legalForm1.technicalKey,
            identifiers = listOf(
                legalEntityIdentifierDto(bpnLReference.referenceValue, 1L, BusinessPartnerVerboseValues.identifierType1),
                legalEntityIdentifierDto(bpnLReference.referenceValue, 2L, BusinessPartnerVerboseValues.identifierType2)
            ),
            states = listOf(
                legalEntityState(bpnLReference.referenceValue, 1L, BusinessStateType.ACTIVE),
                legalEntityState(bpnLReference.referenceValue, 2L, BusinessStateType.INACTIVE)
            ),
            classifications = listOf(
                classificationDto(bpnLReference.referenceValue, 1L, ClassificationType.NACE),
                classificationDto(bpnLReference.referenceValue, 2L, ClassificationType.NAICS)
            ),
            legalAddress = fullLogisticAddressDto(bpnAReference)
        )
    }

    fun legalEntityIdentifierDto(name: String, id: Long, type: TypeKeyNameVerboseDto<String>): LegalEntityIdentifierDto {

        return LegalEntityIdentifierDto(
            value = "value_" + name + "_" + id,
            issuingBody = "issuingBody_" + name + "_" + id,
            type = type.technicalKey
        )
    }

    fun addressIdentifierDto(name: String, id: Long, type: TypeKeyNameVerboseDto<String>): AddressIdentifierDto {

        return AddressIdentifierDto(
            value = "value_" + name + "_" + id,
            type = type.technicalKey
        )
    }

    fun legalEntityState(name: String, id: Long, type: BusinessStateType): LegalEntityStateDto {

        return LegalEntityStateDto(
            description = "description_" + name + "_" + id,
            validFrom = LocalDateTime.now().plusDays(id),
            validTo = LocalDateTime.now().plusDays(id + 2),
            type = type
        )
    }

    fun siteState(name: String, id: Long, type: BusinessStateType): SiteStateDto {

        return SiteStateDto(
            description = "description_" + name + "_" + id,
            validFrom = LocalDateTime.now().plusDays(id),
            validTo = LocalDateTime.now().plusDays(id + 2),
            type = type
        )
    }

    fun addressState(name: String, id: Long, type: BusinessStateType): AddressStateDto {

        return AddressStateDto(
            description = "description_" + name + "_" + id,
            validFrom = LocalDateTime.now().plusDays(id),
            validTo = LocalDateTime.now().plusDays(id + 2),
            type = type
        )
    }


    fun classificationDto(name: String, id: Long, type: ClassificationType): LegalEntityClassificationDto {

        return LegalEntityClassificationDto(
            code = "code_" + name + "_" + id,
            value = "value_" + name + "_" + id,
            type = type
        )
    }

    fun minLogisticAddress(bpnAReference: BpnReferenceDto): LogisticAddressDto {

        return LogisticAddressDto(
            bpnAReference = bpnAReference,
            physicalPostalAddress = minPhysicalPostalAddressDto(bpnAReference)
        )
    }

    private fun minPhysicalPostalAddressDto(bpnAReference: BpnReferenceDto) = PhysicalPostalAddressDto(
        country = CountryCode.DE,
        city = "City_" + bpnAReference.referenceValue
    )

    fun fullLogisticAddressDto(bpnAReference: BpnReferenceDto): LogisticAddressDto {

        return LogisticAddressDto(
            bpnAReference = bpnAReference,
            name = "name_" + bpnAReference.referenceValue,
            identifiers = listOf(
                addressIdentifierDto(bpnAReference.referenceValue, 1L, TypeKeyNameVerboseDto(addressIdentifierTypeDto1.technicalKey, "")),
                addressIdentifierDto(bpnAReference.referenceValue, 2L, TypeKeyNameVerboseDto(addressIdentifierTypeDto2.technicalKey, ""))
            ),
            states = listOf(
                addressState(bpnAReference.referenceValue, 1L, BusinessStateType.ACTIVE),
                addressState(bpnAReference.referenceValue, 2L, BusinessStateType.INACTIVE)
            ),
            physicalPostalAddress = PhysicalPostalAddressDto(
                geographicCoordinates = GeoCoordinateDto(longitude = 1.1f, latitude = 2.2f, altitude = 3.3f),
                country = CountryCode.DE,
                administrativeAreaLevel1 = "AD-07",
                administrativeAreaLevel2 = "adminArea2_" + bpnAReference.referenceValue,
                administrativeAreaLevel3 = "adminArea3_" + bpnAReference.referenceValue,
                postalCode = "postalCode_" + bpnAReference.referenceValue,
                city = "city_" + bpnAReference.referenceValue,
                street = StreetDto(
                    name = "name_" + bpnAReference.referenceValue,
                    houseNumber = "houseNumber_" + bpnAReference.referenceValue,
                    milestone = "milestone_" + bpnAReference.referenceValue,
                    direction = "direction_" + bpnAReference.referenceValue,
                    namePrefix = "namePrefix_" + bpnAReference.referenceValue,
                    additionalNamePrefix = "additionalNamePrefix_" + bpnAReference.referenceValue,
                    nameSuffix = "nameSuffix_" + bpnAReference.referenceValue,
                    additionalNameSuffix = "additionalNameSuffix_" + bpnAReference.referenceValue,
                ),
                district = "district_" + bpnAReference.referenceValue,
                companyPostalCode = "companyPostalCode_" + bpnAReference.referenceValue,
                industrialZone = "industrialZone_" + bpnAReference.referenceValue,
                building = "building_" + bpnAReference.referenceValue,
                floor = "floor_" + bpnAReference.referenceValue,
                door = "door_" + bpnAReference.referenceValue,
            ),
            alternativePostalAddress = AlternativePostalAddressDto(
                geographicCoordinates = GeoCoordinateDto(longitude = 12.3f, latitude = 4.56f, altitude = 7.89f),
                country = CountryCode.DE,
                administrativeAreaLevel1 = "DE-BW",
                postalCode = "alternate_postalCode_" + bpnAReference.referenceValue,
                city = "alternate_city_" + bpnAReference.referenceValue,
                deliveryServiceType = DeliveryServiceType.PO_BOX,
                deliveryServiceQualifier = "deliveryServiceQualifier_" + bpnAReference.referenceValue,
                deliveryServiceNumber = "deliveryServiceNumber_" + bpnAReference.referenceValue,
            )
        )
    }

    fun minValidSite(bpnSReference: BpnReferenceDto, bpnAReference: BpnReferenceDto): SiteDto {

        return SiteDto(
            bpnSReference = bpnSReference,
            name = "siteName_" + bpnSReference.referenceValue,
            mainAddress = minLogisticAddress(bpnAReference = bpnAReference)
        )
    }

    fun fullValidSite(bpnSReference: BpnReferenceDto, bpnAReference: BpnReferenceDto): SiteDto {

        return SiteDto(
            bpnSReference = bpnSReference,
            name = "siteName_" + bpnSReference.referenceValue,
            states = listOf(
                siteState(bpnSReference.referenceValue, 1L, BusinessStateType.ACTIVE), siteState(bpnSReference.referenceValue, 2L, BusinessStateType.INACTIVE)
            ),
            mainAddress = fullLogisticAddressDto(bpnAReference)
        )
    }

    fun assertTaskError(step: TaskStepResultEntryDto, taskId: String, error: CleaningError) {

        assertThat(step.taskId).isEqualTo(taskId)
        assertThat(step.errors.size).isEqualTo(1)
        assertThat(step.errors[0].description).isEqualTo(error.message)

    }

    fun compareLegalEntity(verboseRequest: LegalEntityWithLegalAddressVerboseDto, legalEntity: LegalEntityDto?) {

        val verboseLegalEntity = verboseRequest.legalEntity

        assertThat(verboseLegalEntity.legalShortName).isEqualTo(legalEntity?.legalShortName)
        assertThat(verboseLegalEntity.legalForm?.technicalKey).isEqualTo(legalEntity?.legalForm)
        compareStates(verboseLegalEntity.states, legalEntity?.states)
        compareClassifications(verboseLegalEntity.classifications, legalEntity?.classifications)
        compareIdentifiers(verboseLegalEntity.identifiers, legalEntity?.identifiers)

        val verboseLegalAddress = verboseRequest.legalAddress
        assertThat(verboseLegalAddress.bpnLegalEntity).isEqualTo(legalEntity?.bpnLReference?.referenceValue)
        assertThat(verboseLegalAddress.isLegalAddress).isTrue()
        compareLogisticAddress(verboseLegalAddress, legalEntity?.legalAddress)
    }

    fun compareSite(verboseRequest: SitePoolVerboseDto, site: SiteDto?) {

        val verboseSite = verboseRequest.site

        assertThat(verboseSite.name).isEqualTo(site?.name)
        assertThat(verboseSite.bpns).isEqualTo(site?.bpnSReference?.referenceValue)
        compareSiteStates(verboseSite.states, site?.states)

        val verboseMainAddress = verboseRequest.mainAddress
        assertThat(verboseMainAddress.bpnSite).isEqualTo(site?.bpnSReference?.referenceValue)
        val mainAddress = site?.mainAddress
        assertThat(verboseMainAddress.isMainAddress).isTrue()
        compareLogisticAddress(verboseMainAddress, mainAddress)
    }

    private fun compareLogisticAddress(verboseAddress: LogisticAddressVerboseDto, address: LogisticAddressDto?) {

        assertThat(verboseAddress.name).isEqualTo(address?.name)
        compareAddressStates(verboseAddress.states, address?.states)
        compareAddressIdentifiers(verboseAddress.identifiers, address?.identifiers)


        val verbosePhysicalAddress = verboseAddress.physicalPostalAddress
        val physicalAddress = address?.physicalPostalAddress
        assertThat(verbosePhysicalAddress).usingRecursiveComparison()
            .ignoringFields("country", "administrativeAreaLevel1")
            .isEqualTo(physicalAddress)
        assertThat(verbosePhysicalAddress.country.technicalKey.name).isEqualTo(physicalAddress?.country?.name)
        assertThat(verbosePhysicalAddress.administrativeAreaLevel1?.regionCode).isEqualTo(physicalAddress?.administrativeAreaLevel1)
        val verboseAlternAddress = verboseAddress.alternativePostalAddress
        val alternAddress = address?.alternativePostalAddress
        assertThat(verboseAlternAddress).usingRecursiveComparison()
            .ignoringFields("country", "administrativeAreaLevel1")
            .isEqualTo(alternAddress)
        assertThat(verboseAlternAddress?.country?.technicalKey?.name).isEqualTo(alternAddress?.country?.name)
        assertThat(verboseAlternAddress?.administrativeAreaLevel1?.regionCode).isEqualTo(alternAddress?.administrativeAreaLevel1)
    }

    fun compareAddressStates(statesVerbose: Collection<AddressStateVerboseDto>, states: Collection<AddressStateDto>?) {

        assertThat(statesVerbose.size).isEqualTo(states?.size ?: 0)
        val sortedVerboseStates = statesVerbose.sortedBy { it.description }
        val sortedStates = states?.sortedBy { it.description }
        sortedVerboseStates.indices.forEach {
            assertThat(sortedVerboseStates[it].type.technicalKey.name).isEqualTo(sortedStates!![it].type.name)
            assertThat(sortedVerboseStates[it]).usingRecursiveComparison()
                .withEqualsForFields(isEqualToIgnoringMilliseconds(), "validTo")
                .withEqualsForFields(isEqualToIgnoringMilliseconds(), "validFrom")
                .ignoringFields("type").isEqualTo(sortedStates[it])
        }
    }

    fun compareAddressIdentifiers(identifiersVerbose: Collection<AddressIdentifierVerboseDto>, identifiers: Collection<AddressIdentifierDto>?) {

        assertThat(identifiersVerbose.size).isEqualTo(identifiers?.size ?: 0)
        val sortedVerboseIdentifiers = identifiersVerbose.sortedBy { it.type.name }
        val sortedIdentifiers = identifiers!!.sortedBy { it.type }
        sortedVerboseIdentifiers.indices.forEach {
            assertThat(sortedVerboseIdentifiers[it].type.technicalKey).isEqualTo(sortedIdentifiers[it].type)
            assertThat(sortedVerboseIdentifiers[it]).usingRecursiveComparison()
                .ignoringFields("type")
                .isEqualTo(sortedIdentifiers[it])
        }
    }

    fun compareStates(statesVerbose: Collection<LegalEntityStateVerboseDto>, states: Collection<ILegalEntityStateDto>?) {

        assertThat(statesVerbose.size).isEqualTo(states?.size ?: 0)
        val sortedVerboseStates = statesVerbose.sortedBy { it.description }
        val sortedStates = states!!.sortedBy { it.description }
        sortedVerboseStates.indices.forEach {
            assertThat(sortedVerboseStates[it].type.technicalKey.name).isEqualTo(sortedStates[it].type.name)
            assertThat(sortedVerboseStates[it]).usingRecursiveComparison()
                .withEqualsForFields(isEqualToIgnoringMilliseconds(), "validTo")
                .withEqualsForFields(isEqualToIgnoringMilliseconds(), "validFrom")
                .ignoringFields("type")
                .isEqualTo(sortedStates[it])
        }
    }

    fun compareSiteStates(statesVerbose: Collection<SiteStateVerboseDto>, states: Collection<IBaseSiteStateDto>?) {

        assertThat(statesVerbose.size).isEqualTo(states?.size ?: 0)
        val sortedVerboseStates = statesVerbose.sortedBy { it.description }
        val sortedStates = states!!.sortedBy { it.description }
        sortedVerboseStates.indices.forEach {
            assertThat(sortedVerboseStates[it].type.technicalKey.name).isEqualTo(sortedStates[it].type.name)
            assertThat(sortedVerboseStates[it]).usingRecursiveComparison()
                .withEqualsForFields(isEqualToIgnoringMilliseconds(), "validTo")
                .withEqualsForFields(isEqualToIgnoringMilliseconds(), "validFrom")
                .ignoringFields("type")
                .isEqualTo(sortedStates[it])
        }
    }

    fun isEqualToIgnoringMilliseconds(): BiPredicate<LocalDateTime?, LocalDateTime?> {


        return BiPredicate<LocalDateTime?, LocalDateTime?> { d1, d2 ->
            (d1 == null && d2 == null)
                    || d1.truncatedTo(ChronoUnit.SECONDS).equals(d2.truncatedTo(ChronoUnit.SECONDS))
        }
    }

    fun compareClassifications(
        classificationsVerbose: Collection<LegalEntityClassificationVerboseDto>,
        classifications: Collection<LegalEntityClassificationDto>?
    ) {

        assertThat(classificationsVerbose.size).isEqualTo(classifications?.size ?: 0)
        val sortedVerboseClassifications = classificationsVerbose.sortedBy { it.type.name }
        val sortedClassifications = classifications!!.sortedBy { it.type.name }
        sortedVerboseClassifications.indices.forEach {
            assertThat(sortedVerboseClassifications[it].type.technicalKey.name).isEqualTo(sortedClassifications[it].type.name)
            assertThat(sortedVerboseClassifications[it]).usingRecursiveComparison()
                .ignoringFields("type")
                .isEqualTo(sortedClassifications[it])
        }
    }

    fun compareIdentifiers(identifiersVerbose: Collection<LegalEntityIdentifierVerboseDto>, identifiers: Collection<LegalEntityIdentifierDto>?) {

        assertThat(identifiersVerbose.size).isEqualTo(identifiers?.size ?: 0)
        val sortedVerboseIdentifiers = identifiersVerbose.sortedBy { it.type.name }
        val sortedIdentifiers = identifiers!!.sortedBy { it.type }
        sortedVerboseIdentifiers.indices.forEach {
            assertThat(sortedVerboseIdentifiers[it].type.technicalKey).isEqualTo(sortedIdentifiers[it].type)
            assertThat(sortedVerboseIdentifiers[it]).usingRecursiveComparison()
                .ignoringFields("type").isEqualTo(sortedIdentifiers[it])
        }
    }


}