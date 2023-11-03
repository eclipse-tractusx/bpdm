package org.eclipse.tractusx.bpdm.pool.service

import com.neovisionaries.i18n.CountryCode
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolClientImpl
import org.eclipse.tractusx.bpdm.pool.repository.BpnRequestIdentifierRepository
import org.eclipse.tractusx.bpdm.pool.service.TaskStepBuildService.CleaningError
import org.eclipse.tractusx.bpdm.pool.util.OpenSearchContextInitializer
import org.eclipse.tractusx.bpdm.pool.util.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.pool.util.TestHelpers
import org.eclipse.tractusx.orchestrator.api.model.*
import org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType.BpnRequestIdentifier
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class]
)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class, OpenSearchContextInitializer::class])
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
    fun `upsert Golden Record into pool with empty legal entity`() {

        val fullBpWithLegalEntity = minFullBusinessPartner().copy(
            legalEntity = emptyLegalEntity()
        )

        val result = cleanStep(taskId = "TASK_1", businessPartner = fullBpWithLegalEntity)
        assertTaskError(result[0], "TASK_1", CleaningError.LEGAL_ENTITY_IS_NULL)
    }

    @Test
    fun `upsert Golden Record into pool with legal entity without legal name to create`() {

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
    fun `upsert Golden Record into pool with legal entity to create`() {

        val fullBpWithLegalEntity = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                BpnReferenceDto(referenceValue = "123", referenceType = BpnRequestIdentifier),
                BpnReferenceDto(referenceValue = "222", referenceType = BpnRequestIdentifier)
            )
        )
        val resultSteps = cleanStep(taskId = "TASK_1", businessPartner = fullBpWithLegalEntity)
        assertThat(resultSteps[0].taskId).isEqualTo("TASK_1")
        assertThat(resultSteps[0].errors.size).isEqualTo(0)

        val bpnMappings = bpnRequestIdentifierRepository.findDistinctByRequestIdentifierIn(listOf("123","222" ))
        val createdLegalEntity = poolClient.legalEntities.getLegalEntity(resultSteps[0].businessPartner?.legalEntity?.bpnLReference?.referenceValue!!)
        assertThat(createdLegalEntity.legalAddress.bpnLegalEntity).isNotNull()
        assertThat(bpnMappings.size).isEqualTo(2)
        assertThat(resultSteps[0].businessPartner?.generic?.legalEntityBpn).isEqualTo(createdLegalEntity.legalEntity.bpnl)
    }

    @Test
    fun `upsert Golden Record into pool with same legal entity referenceValue to create`() {

        val fullBpWithLegalEntity = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = "123", referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = "222", referenceType = BpnRequestIdentifier)
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
    fun `upsert Golden Record into pool with differrent legal entity referenceValue to create`() {

        val fullBpWithLegalEntity = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = "123", referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = "222", referenceType = BpnRequestIdentifier)
            )
        )
        val resultSteps1 = cleanStep(taskId = "TASK_1", businessPartner = fullBpWithLegalEntity)
        assertThat(resultSteps1[0].taskId).isEqualTo("TASK_1")
        assertThat(resultSteps1[0].errors.size).isEqualTo(0)
        val createdLegalEntity1 = poolClient.legalEntities.getLegalEntity(resultSteps1[0].businessPartner?.legalEntity?.bpnLReference?.referenceValue!!)

        val fullBpWithLegalEntity2 = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = "diffenrentBpnL", referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = "diffenrentBpnA", referenceType = BpnRequestIdentifier)
            )
        )
        val resultSteps2 = cleanStep(taskId = "TASK_2", businessPartner = fullBpWithLegalEntity2)
        assertThat(resultSteps2[0].taskId).isEqualTo("TASK_2")
        assertThat(resultSteps2[0].errors.size).isEqualTo(0)
        assertThat(createdLegalEntity1.legalEntity.bpnl).isNotEqualTo(resultSteps2[0].businessPartner?.legalEntity?.bpnLReference?.referenceValue!!)
        val createdLegalEntity2 = poolClient.legalEntities.getLegalEntity(resultSteps2[0].businessPartner?.legalEntity?.bpnLReference?.referenceValue!!)
        assertThat(resultSteps2[0].businessPartner?.generic?.legalEntityBpn).isEqualTo(createdLegalEntity2.legalEntity.bpnl)

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
            legalAddress = LogisticAddressDto(
                bpnAReference = bpnAReference,
                physicalPostalAddress = PhysicalPostalAddressDto(
                    country = CountryCode.DE,
                    city = "City" + bpnLReference.referenceValue
                )
            )
        )
    }

    fun assertTaskError(step: TaskStepResultEntryDto, taskId: String, error: CleaningError) {

        assertThat(step.taskId).isEqualTo(taskId)
        assertThat(step.errors.size).isEqualTo(1)
        assertThat(step.errors[0].description).isEqualTo(error.message)

    }
}