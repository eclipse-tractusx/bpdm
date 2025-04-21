/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.orchestrator.controller.v6

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.orchestrator.config.StateMachineConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.config.TaskConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.entity.*
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmIllegalStateException
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmRecordNotFoundException
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmTaskNotFoundException
import org.eclipse.tractusx.bpdm.orchestrator.repository.GateRecordRepository
import org.eclipse.tractusx.bpdm.orchestrator.repository.GoldenRecordTaskRepository
import org.eclipse.tractusx.bpdm.orchestrator.repository.fetchBusinessPartnerData
import org.eclipse.tractusx.bpdm.orchestrator.service.GoldenRecordTaskStateMachine
import org.eclipse.tractusx.orchestrator.api.model.*
import org.eclipse.tractusx.orchestrator.api.v6.model.BusinessPartner
import org.eclipse.tractusx.orchestrator.api.v6.model.LegalEntity
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskClientStateDto
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskCreateRequestEntry
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskCreateRequest
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskCreateResponse
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStepReservationEntryDto
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStepReservationResponse
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStepResultRequest
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStateResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class GoldenRecordTaskLegacyServiceMapper(
    private val goldenRecordTaskStateMachine: GoldenRecordTaskStateMachine,
    private val taskConfigProperties: TaskConfigProperties,
    private val taskRepository: GoldenRecordTaskRepository,
    private val gateRecordRepository: GateRecordRepository,
    private val stateMachineConfigProperties: StateMachineConfigProperties
) {

    private val logger = KotlinLogging.logger { }

    @Transactional
    fun createTasks(createRequest: TaskCreateRequest): TaskCreateResponse {
        logger.debug { "Creation of new golden record tasks: executing createTasks() with parameters $createRequest" }

        val gateRecords = getOrCreateGateRecords(createRequest.requests)
        abortOutdatedTasks(gateRecords.toSet())

        return createRequest.requests.zip(gateRecords)
            .map { (request, record) -> initTask(createRequest.mode, request.businessPartner, record) }
            .map { task -> responseToClientState(task, calculateTaskRetentionTimeout(task)) }
            .let { TaskCreateResponse(createdTasks = it) }
    }

    private fun getOrCreateGateRecords(requests: List<TaskCreateRequestEntry>): List<GateRecordDb> {
        val privateIds = requests.map { request -> request.recordId?.let { toUUID(it) } }
        val notNullPrivateIds = privateIds.filterNotNull()

        val foundRecords = gateRecordRepository.findByPrivateIdIn(notNullPrivateIds.toSet())
        val foundRecordsByPrivateId = foundRecords.associateBy { it.privateId }
        val requestedNotFoundRecords = notNullPrivateIds.minus(foundRecordsByPrivateId.keys)

        if (requestedNotFoundRecords.isNotEmpty())
            throw BpdmRecordNotFoundException(requestedNotFoundRecords)

        return privateIds.map { privateId ->
            val gateRecord = privateId?.let { foundRecordsByPrivateId[it] } ?: GateRecordDb(publicId = UUID.randomUUID(), privateId = UUID.randomUUID())
            gateRecordRepository.save(gateRecord)
        }
    }

    private fun toUUID(uuidString: String) =
        try {
            UUID.fromString(uuidString)
        } catch (e: IllegalArgumentException) {
            throw BpdmTaskNotFoundException(uuidString)
        }


    private fun abortOutdatedTasks(records: Set<GateRecordDb>){
        return taskRepository.findTasksByGateRecordInAndProcessingStateResultState(records, GoldenRecordTaskDb.ResultState.Pending)
            .forEach { task -> goldenRecordTaskStateMachine.doAbortTask(task) }
    }

    fun initTask(mode: TaskMode, initBusinessPartner: BusinessPartner, record: GateRecordDb): GoldenRecordTaskDb {
        logger.debug { "Executing initProcessingState() with parameters mode: $mode and business partner data: $initBusinessPartner" }

        val initialStep = getInitialStep(mode)
        val initProcessingState = GoldenRecordTaskDb.ProcessingState(
            mode = mode,
            resultState = GoldenRecordTaskDb.ResultState.Pending,
            step = initialStep,
            errors = mutableListOf(),
            stepState = GoldenRecordTaskDb.StepState.Queued,
            pendingTimeout =  Instant.now().plus(taskConfigProperties.taskPendingTimeout).toTimestamp(),
            retentionTimeout = null
        )

        val initialTask = DbTimestamp.now().let { nowTime ->
            GoldenRecordTaskDb(
                gateRecord = record,
                processingState = initProcessingState,
                businessPartner = requestedToBusinessPartner(initBusinessPartner),
                createdAt = nowTime,
                updatedAt = nowTime
            )
        }

        return taskRepository.save(initialTask)
    }

    fun requestedToBusinessPartner(businessPartner: BusinessPartner) =
        with(businessPartner){
            GoldenRecordTaskDb.BusinessPartner(
                nameParts = toNameParts(businessPartner),
                identifiers = toIdentifiers(businessPartner),
                businessStates = toStates(businessPartner),
                confidences = toConfidences(businessPartner),
                addresses = toPostalAddresses(businessPartner),
                bpnReferences = toBpnReferences(businessPartner),
                legalName = legalEntity.legalName,
                legalShortName = legalEntity.legalShortName,
                siteExists = site != null,
                siteName = site?.siteName,
                legalForm = legalEntity.legalForm,
                isCatenaXMemberData = legalEntity.isCatenaXMemberData,
                owningCompany = owningCompany,
                legalEntityHasChanged = legalEntity.hasChanged,
                siteHasChanged = site?.hasChanged
            )
        }

    fun toNameParts(businessPartner: BusinessPartner) =
        mutableListOf(
            businessPartner.uncategorized.nameParts.map { NamePartDb(it, null) },
            businessPartner.nameParts.map { NamePartDb(it.name, it.type) }
        ).flatten().toMutableList()

    fun toIdentifiers(businessPartner: BusinessPartner) =
        IdentifierDb.Scope.entries.mapNotNull { scope ->
            when (scope) {
                IdentifierDb.Scope.LegalEntity -> businessPartner.legalEntity.identifiers
                IdentifierDb.Scope.LegalAddress -> businessPartner.legalEntity.legalAddress.identifiers
                IdentifierDb.Scope.SiteMainAddress -> businessPartner.site?.siteMainAddress?.identifiers
                IdentifierDb.Scope.AdditionalAddress -> businessPartner.additionalAddress?.identifiers
                IdentifierDb.Scope.Uncategorized -> businessPartner.uncategorized.identifiers
                IdentifierDb.Scope.UncategorizedAddress -> businessPartner.uncategorized.address?.identifiers
            }?.map { toIdentifier(it, scope) }
        }.flatten().toMutableList()

    fun toIdentifier(identifier: Identifier, scope: IdentifierDb.Scope) =
        with(identifier) {
            IdentifierDb(value, type, issuingBody, scope)
        }

    fun toStates(businessPartner: BusinessPartner) =
        BusinessStateDb.Scope.entries.mapNotNull { scope ->
            when (scope) {
                BusinessStateDb.Scope.LegalEntity -> businessPartner.legalEntity.states
                BusinessStateDb.Scope.Site -> businessPartner.site?.states
                BusinessStateDb.Scope.LegalAddress -> businessPartner.legalEntity.legalAddress.states
                BusinessStateDb.Scope.SiteMainAddress -> businessPartner.site?.siteMainAddress?.states
                BusinessStateDb.Scope.AdditionalAddress -> businessPartner.additionalAddress?.states
                BusinessStateDb.Scope.Uncategorized -> businessPartner.uncategorized.states
                BusinessStateDb.Scope.UncategorizedAddress -> businessPartner.uncategorized.address?.states
            }?.map { toState(it, scope) }
        }.flatten().toMutableList()

    fun toState(state: BusinessState, scope: BusinessStateDb.Scope) =
        with(state) {
            BusinessStateDb(validFrom?.toTimestamp(), validTo?.toTimestamp(), type, scope)
        }

    fun toConfidences(businessPartner: BusinessPartner) =
        ConfidenceCriteriaDb.Scope.entries.mapNotNull { scope ->
            when (scope) {
                ConfidenceCriteriaDb.Scope.LegalEntity -> businessPartner.legalEntity.confidenceCriteria
                ConfidenceCriteriaDb.Scope.Site -> businessPartner.site?.confidenceCriteria
                ConfidenceCriteriaDb.Scope.LegalAddress -> businessPartner.legalEntity.legalAddress.confidenceCriteria
                ConfidenceCriteriaDb.Scope.SiteMainAddress -> businessPartner.site?.siteMainAddress?.confidenceCriteria
                ConfidenceCriteriaDb.Scope.AdditionalAddress -> businessPartner.additionalAddress?.confidenceCriteria
                ConfidenceCriteriaDb.Scope.UncategorizedAddress -> businessPartner.uncategorized.address?.confidenceCriteria
            }?.let { scope to toConfidence(it) }
        }.toMap().toMutableMap()

    fun toConfidence(confidenceCriteria: ConfidenceCriteria) =
        with(confidenceCriteria) {
            ConfidenceCriteriaDb(
                sharedByOwner,
                checkedByExternalDataSource,
                numberOfSharingMembers,
                lastConfidenceCheckAt?.toTimestamp(),
                nextConfidenceCheckAt?.toTimestamp(),
                confidenceLevel
            )
        }

    fun toPostalAddresses(businessPartner: BusinessPartner) =
        PostalAddressDb.Scope.entries.mapNotNull { scope ->
            when (scope) {
                PostalAddressDb.Scope.LegalAddress -> businessPartner.legalEntity.legalAddress
                PostalAddressDb.Scope.SiteMainAddress -> businessPartner.site?.siteMainAddress
                PostalAddressDb.Scope.AdditionalAddress -> businessPartner.additionalAddress
                PostalAddressDb.Scope.UncategorizedAddress -> businessPartner.uncategorized.address
            }?.let { scope to toPostalAddress(it, scope) }
        }.toMap().toMutableMap()

    fun toPostalAddress(postalAddress: PostalAddress, scope: PostalAddressDb.Scope) =
        with(postalAddress) {
            PostalAddressDb(
                addressName = addressName,
                physicalAddress = toPhysicalAddress(physicalAddress),
                alternativeAddress = toAlternativeAddress(alternativeAddress),
                hasChanged = hasChanged
            )
        }

    fun toPhysicalAddress(physicalAddress: PhysicalAddress) =
        with(physicalAddress) {
            PostalAddressDb.PhysicalAddressDb(
                geographicCoordinates = toGeoCoordinate(geographicCoordinates),
                country = country,
                administrativeAreaLevel1 = administrativeAreaLevel1,
                administrativeAreaLevel2 = administrativeAreaLevel2,
                administrativeAreaLevel3 = administrativeAreaLevel3,
                postalCode = postalCode,
                city = city,
                district = district,
                street = toStreet(street),
                companyPostalCode = companyPostalCode,
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door,
                taxJurisdictionCode = taxJurisdictionCode
            )
        }

    fun toStreet(street: Street) =
        with(street) {
            PostalAddressDb.Street(
                name,
                houseNumber,
                houseNumberSupplement,
                milestone,
                direction,
                namePrefix,
                additionalNamePrefix,
                nameSuffix,
                additionalNameSuffix
            )
        }

    fun toGeoCoordinate(geoCoordinate: GeoCoordinate) =
        with(geoCoordinate) {
            PostalAddressDb.GeoCoordinate(longitude, latitude, altitude)
        }

    fun toAlternativeAddress(alternativeAddress: AlternativeAddress?) =
        alternativeAddress?.let {
            with(alternativeAddress) {
                PostalAddressDb.AlternativeAddress(
                    exists = true,
                    geographicCoordinates = toGeoCoordinate(geographicCoordinates),
                    country = country,
                    administrativeAreaLevel1 = administrativeAreaLevel1,
                    postalCode = postalCode,
                    city = city,
                    deliveryServiceType = deliveryServiceType,
                    deliveryServiceQualifier = deliveryServiceQualifier,
                    deliveryServiceNumber = deliveryServiceNumber
                )
            }
        } ?: PostalAddressDb.AlternativeAddress(
            exists = false,
            geographicCoordinates = PostalAddressDb.GeoCoordinate(
                longitude = null,
                latitude = null,
                altitude = null
            ),
            country = null,
            administrativeAreaLevel1 = null,
            postalCode = null,
            city = null,
            deliveryServiceType = null,
            deliveryServiceQualifier = null,
            deliveryServiceNumber = null
        )

    fun toBpnReferences(businessPartner: BusinessPartner) =
        BpnReferenceDb.Scope.entries.mapNotNull { scope ->
            when (scope) {
                BpnReferenceDb.Scope.LegalEntity -> businessPartner.legalEntity.bpnReference
                BpnReferenceDb.Scope.Site -> businessPartner.site?.bpnReference
                BpnReferenceDb.Scope.LegalAddress -> businessPartner.legalEntity.legalAddress.bpnReference
                BpnReferenceDb.Scope.SiteMainAddress -> businessPartner.site?.siteMainAddress?.bpnReference
                BpnReferenceDb.Scope.AdditionalAddress -> businessPartner.additionalAddress?.bpnReference
                BpnReferenceDb.Scope.UncategorizedAddress ->  businessPartner.uncategorized.address?.bpnReference
            }?.let { scope to toBpnReference(it) }
        }.toMap().toMutableMap()

    fun toBpnReference(bpnReference: BpnReference) =
        with(bpnReference) {
            BpnReferenceDb(
                referenceValue = referenceValue,
                desiredBpn = desiredBpn,
                referenceType = referenceType
            )
        }

    private fun getInitialStep(mode: TaskMode): TaskStep {
        return stateMachineConfigProperties.modeSteps[mode]!!.first()
    }

    private fun calculateTaskRetentionTimeout(task: GoldenRecordTaskDb) =
        task.createdAt.instant.plus(taskConfigProperties.taskRetentionTimeout)

    fun responseToClientState(task: GoldenRecordTaskDb, timeout: Instant) =
        with(task) {
            TaskClientStateDto(
                taskId = task.uuid.toString(),
                recordId = task.gateRecord.privateId.toString(),
                businessPartnerResult = toBusinessPartnerResult(businessPartner),
                processingState = toResponseProcessingState(task, timeout)
            )
        }

    fun toBusinessPartnerResult(businessPartner: GoldenRecordTaskDb.BusinessPartner) =
        with(businessPartner) {
            BusinessPartner(
                nameParts = toResponseCategorizedNameParts(nameParts),
                owningCompany = owningCompany,
                uncategorized = toResponseUncategorizedProperties(businessPartner),
                legalEntity = toResponseLegalEntity(businessPartner),
                site = toResponseSite(businessPartner),
                additionalAddress = toResponsePostalAddress(businessPartner, PostalAddressDb.Scope.AdditionalAddress)
            )
        }

    fun toResponseCategorizedNameParts(nameParts: List<NamePartDb>) =
        nameParts.filter { it.type != null }.map { NamePart(it.name, it.type!!) }

    fun toResponseUncategorizedProperties(businessPartner: GoldenRecordTaskDb.BusinessPartner) =
        UncategorizedProperties(
            nameParts = toResponseUncategorizedNameParts(businessPartner.nameParts),
            identifiers = toResponseIdentifiers(businessPartner, IdentifierDb.Scope.Uncategorized),
            states = toResponseStates(businessPartner, BusinessStateDb.Scope.Uncategorized),
            address = toResponsePostalAddress(businessPartner, PostalAddressDb.Scope.UncategorizedAddress)
        )

    fun toResponseUncategorizedNameParts(nameParts: List<NamePartDb>) =
        nameParts.filter { it.type == null }.map { it.name }

    fun toResponseIdentifiers(businessPartner: GoldenRecordTaskDb.BusinessPartner, scope: IdentifierDb.Scope) =
        businessPartner.identifiers.filter { it.scope == scope }.map { Identifier(it.value, it.type, it.issuingBody) }

    fun toResponseStates(businessPartner: GoldenRecordTaskDb.BusinessPartner, scope: BusinessStateDb.Scope) =
        businessPartner.businessStates.filter { it.scope == scope }.map { BusinessState(it.validFrom?.instant, it.validTo?.instant, it.type) }

    fun toResponsePostalAddress(businessPartner: GoldenRecordTaskDb.BusinessPartner, scope: PostalAddressDb.Scope) =
        businessPartner.addresses[scope]?.let { postalAddress ->
            with(postalAddress) {
                PostalAddress(
                    bpnReference = toResponseBpnReference(businessPartner, scope.bpnReference),
                    addressName = addressName,
                    identifiers = toResponseIdentifiers(businessPartner, scope.identifier),
                    states = toResponseStates(businessPartner, scope.state),
                    confidenceCriteria = toResponseConfidence(businessPartner, scope.confidence),
                    physicalAddress = toResponsePhysicalAddress(physicalAddress),
                    alternativeAddress = toResponseAlternativeAddress(alternativeAddress),
                    hasChanged = hasChanged
                )
            }
        }

    fun toResponseBpnReference(businessPartner: GoldenRecordTaskDb.BusinessPartner, scope: BpnReferenceDb.Scope) =
        businessPartner.bpnReferences[scope]?.let { toResponseBpnReferences(it) } ?: BpnReference.empty

    fun toResponseBpnReferences(bpnReference: BpnReferenceDb) =
        with(bpnReference) {
            BpnReference(referenceValue = referenceValue, desiredBpn = desiredBpn, referenceType = referenceType)
        }

    fun toResponseConfidence(businessPartner: GoldenRecordTaskDb.BusinessPartner, scope: ConfidenceCriteriaDb.Scope) =
        businessPartner.confidences[scope]?.let { toResponseConfidences(it) } ?: ConfidenceCriteria.empty

    fun toResponseConfidences(confidenceCriteria: ConfidenceCriteriaDb) =
        with(confidenceCriteria) {
            ConfidenceCriteria(
                sharedByOwner = sharedByOwner,
                checkedByExternalDataSource = checkedByExternalDataSource,
                numberOfSharingMembers = numberOfSharingMembers,
                lastConfidenceCheckAt = lastConfidenceCheckAt?.instant,
                nextConfidenceCheckAt = nextConfidenceCheckAt?.instant,
                confidenceLevel = confidenceLevel
            )
        }

    fun toResponsePhysicalAddress(physicalAddress: PostalAddressDb.PhysicalAddressDb) =
        with(physicalAddress) {
            PhysicalAddress(
                geographicCoordinates = toResponseGeoCoordinate(geographicCoordinates),
                country = country,
                administrativeAreaLevel1 = administrativeAreaLevel1,
                administrativeAreaLevel2 = administrativeAreaLevel2,
                administrativeAreaLevel3 = administrativeAreaLevel3,
                postalCode = postalCode,
                city = city,
                district = district,
                street = toResponseStreet(street),
                companyPostalCode = companyPostalCode,
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door,
                taxJurisdictionCode = taxJurisdictionCode
            )
        }

    fun toResponseGeoCoordinate(geoCoordinate: PostalAddressDb.GeoCoordinate) =
        with(geoCoordinate) {
            GeoCoordinate(longitude = longitude, latitude = latitude, altitude = altitude)
        }

    fun toResponseStreet(street: PostalAddressDb.Street) =
        with(street) {
            Street(
                name, houseNumber,
                houseNumberSupplement = houseNumberSupplement,
                milestone = milestone,
                direction = direction,
                namePrefix = namePrefix,
                additionalNamePrefix = additionalNamePrefix,
                nameSuffix = nameSuffix,
                additionalNameSuffix = additionalNameSuffix
            )
        }

    fun toResponseAlternativeAddress(alternativeAddress: PostalAddressDb.AlternativeAddress) =
        alternativeAddress.takeIf { it.exists }?.let {
            with(alternativeAddress) {
                AlternativeAddress(
                    geographicCoordinates = toResponseGeoCoordinate(geographicCoordinates),
                    country = country,
                    administrativeAreaLevel1 = administrativeAreaLevel1,
                    postalCode = postalCode,
                    city = city,
                    deliveryServiceType = deliveryServiceType,
                    deliveryServiceQualifier = deliveryServiceQualifier,
                    deliveryServiceNumber = deliveryServiceNumber
                )
            }
        }

    fun toResponseLegalEntity(businessPartner: GoldenRecordTaskDb.BusinessPartner) =
        with(businessPartner) {
            LegalEntity(
                bpnReference = toResponseBpnReference(businessPartner, BpnReferenceDb.Scope.LegalEntity),
                legalName = legalName,
                legalShortName = legalShortName,
                legalForm = legalForm,
                identifiers = toResponseIdentifiers(businessPartner, IdentifierDb.Scope.LegalEntity),
                states = toResponseStates(businessPartner, BusinessStateDb.Scope.LegalEntity),
                confidenceCriteria = toResponseConfidence(businessPartner, ConfidenceCriteriaDb.Scope.LegalEntity),
                isCatenaXMemberData = isCatenaXMemberData,
                hasChanged = legalEntityHasChanged,
                legalAddress = toResponsePostalAddressOrEmpty(businessPartner, PostalAddressDb.Scope.LegalAddress)!!
            )
        }
    fun toResponsePostalAddressOrEmpty(businessPartner: GoldenRecordTaskDb.BusinessPartner, scope: PostalAddressDb.Scope) =
        toResponsePostalAddress(businessPartner, scope)

    fun toResponseProcessingState(task: GoldenRecordTaskDb, timeout: Instant) =
        with(task.processingState) {
            TaskProcessingStateDto(
                resultState = toResponseResultState(resultState),
                step = step,
                stepState = toResponseStepState(stepState),
                errors = errors.map { toResponseTaskError(it) },
                createdAt = task.createdAt.instant,
                modifiedAt = task.updatedAt.instant,
                timeout = timeout
            )
        }

    fun toResponseResultState(resultState: GoldenRecordTaskDb.ResultState) =
        when(resultState){
            GoldenRecordTaskDb.ResultState.Pending -> ResultState.Pending
            GoldenRecordTaskDb.ResultState.Success ->  ResultState.Success
            GoldenRecordTaskDb.ResultState.Error ->  ResultState.Error
            GoldenRecordTaskDb.ResultState.Aborted ->  ResultState.Error
        }

    fun toResponseStepState(stepState: GoldenRecordTaskDb.StepState) =
        when(stepState){
            GoldenRecordTaskDb.StepState.Queued -> StepState.Queued
            GoldenRecordTaskDb.StepState.Reserved -> StepState.Reserved
            GoldenRecordTaskDb.StepState.Success -> StepState.Success
            GoldenRecordTaskDb.StepState.Error -> StepState.Error
            GoldenRecordTaskDb.StepState.Aborted -> StepState.Error
        }

    fun toResponseTaskError(taskError: TaskErrorDb) =
        with(taskError) {
            TaskErrorDto(type = type, description = description)
        }

    fun toResponseSite(businessPartner: GoldenRecordTaskDb.BusinessPartner) =
        businessPartner.takeIf { it.siteExists }?.let {
            with(businessPartner) {
                Site(
                    bpnReference = toResponseBpnReference(businessPartner, BpnReferenceDb.Scope.Site),
                    siteName = siteName,
                    states = toResponseStates(businessPartner, BusinessStateDb.Scope.Site),
                    confidenceCriteria = toResponseConfidence(businessPartner, ConfidenceCriteriaDb.Scope.Site),
                    hasChanged = siteHasChanged,
                    siteMainAddress = toResponsePostalAddress(businessPartner, PostalAddressDb.Scope.SiteMainAddress)
                )
            }
        }

    @Transactional
    fun reserveTasksForStep(reservationRequest: TaskStepReservationRequest): TaskStepReservationResponse {
        logger.debug { "Reservation of next golden record tasks: executing reserveTasksForStep() with parameters $reservationRequest" }
        val now = Instant.now()

        val foundTasks = taskRepository.findByStepAndStepState(reservationRequest.step, GoldenRecordTaskDb.StepState.Queued, Pageable.ofSize(reservationRequest.amount))
            .content.toSet()
            .also { taskRepository.fetchBusinessPartnerData(it) }
        val reservedTasks = foundTasks.map { goldenRecordTaskStateMachine.doReserve(it) }
        val pendingTimeout = reservedTasks.minOfOrNull { calculateTaskPendingTimeout(it) } ?: now

        return reservedTasks
            .map { task ->
                TaskStepReservationEntryDto(
                    task.uuid.toString(),
                    task.gateRecord.publicId.toString(),
                    toBusinessPartnerResult(task.businessPartner)
                )
            }
            .let { reservations -> TaskStepReservationResponse(reservations, pendingTimeout) }
    }

    private fun calculateTaskPendingTimeout(task: GoldenRecordTaskDb) =
        task.createdAt.instant.plus(taskConfigProperties.taskPendingTimeout)

    @Transactional
    fun resolveStepResults(resultRequest: TaskStepResultRequest) {
        logger.debug { "Step results for reserved golden record tasks: executing resolveStepResults() with parameters $resultRequest" }
        val uuids = resultRequest.results.map { toUUID(it.taskId) }
        val foundTasks = taskRepository.findByUuidIn(uuids.toSet()).also { taskRepository.fetchBusinessPartnerData(it) }
        val foundTasksByUuid = foundTasks.associateBy { it.uuid.toString() }

        resultRequest.results
            .map { resultEntry -> Pair(foundTasksByUuid[resultEntry.taskId] ?: throw BpdmTaskNotFoundException(resultEntry.taskId), resultEntry) }
            .filterNot { (task, _) -> task.processingState.resultState == GoldenRecordTaskDb.ResultState.Aborted }
            .forEach { (task, resultEntry) ->
                val step = resultRequest.step
                val errors = resultEntry.errors
                val resultBusinessPartner = resultEntry.businessPartner

                when{
                    errors.isNotEmpty() -> goldenRecordTaskStateMachine.doResolveTaskToError(task, step, errors)
                    else ->  resolveTaskStepToSuccess(task, step, resultBusinessPartner)
                }
            }
    }

    fun resolveTaskStepToSuccess(
        task: GoldenRecordTaskDb,
        step: TaskStep,
        resultBusinessPartner: BusinessPartner
    ): GoldenRecordTaskDb {
        logger.debug { "Executing doResolveTaskToSuccess() with parameters $task // $step and $resultBusinessPartner" }
        val state = task.processingState

        if (!isResolvableForStep(state, step)) {
            if(hasAlreadyResolvedStep(state, step))
            {
                logger.debug { "Task ${task.uuid} has already been processed for step $step. Result is ignored" }
                return task
            }else{
                throw BpdmIllegalStateException(task.uuid, state)
            }
        }

        val nextStep = getNextStep(state.mode, state.step)

        if (nextStep != null) {
            // still steps left to process -> queued for next step
            task.processingState.toStep(nextStep)
        } else {
            // last step finished -> set resultState and stepState to success
            task.processingState.toSuccess()
        }

        task.updateBusinessPartner(requestedToBusinessPartner(resultBusinessPartner))
        task.updatedAt =  DbTimestamp(Instant.now())

        return taskRepository.save(task)
    }

    private fun isResolvableForStep(state: GoldenRecordTaskDb.ProcessingState, step: TaskStep): Boolean{
        return state.resultState == GoldenRecordTaskDb.ResultState.Pending
                && state.stepState == GoldenRecordTaskDb.StepState.Reserved
                && state.step == step
    }

    private fun hasAlreadyResolvedStep(state: GoldenRecordTaskDb.ProcessingState, step: TaskStep): Boolean{
        if(state.step == step) return state.stepState != GoldenRecordTaskDb.StepState.Reserved
        return isStepBefore(step, state.step, state.mode)
    }

    private fun isStepBefore(stepBefore: TaskStep, stepAfter: TaskStep, mode: TaskMode): Boolean{
        val modeSteps = stateMachineConfigProperties.modeSteps[mode]!!
        return modeSteps.contains(stepBefore) && modeSteps.indexOf(stepBefore) <= modeSteps.indexOf(stepAfter)
    }

    private fun getNextStep(mode: TaskMode, currentStep: TaskStep): TaskStep? {
        return stateMachineConfigProperties.modeSteps[mode]!!
            .dropWhile { it != currentStep }        // drop steps before currentStep
            .drop(1)                             // then drop currentStep
            .firstOrNull()                          // return next step
    }

    private fun GoldenRecordTaskDb.ProcessingState.toStep(nextStep: TaskStep) {
        step = nextStep
        stepState = GoldenRecordTaskDb.StepState.Queued
    }

    private fun GoldenRecordTaskDb.ProcessingState.toSuccess() {
        resultState = GoldenRecordTaskDb.ResultState.Success
        stepState = GoldenRecordTaskDb.StepState.Success
        pendingTimeout = null
        retentionTimeout = Instant.now().plus(taskConfigProperties.taskRetentionTimeout).toTimestamp()

    }

    fun searchTaskStates(stateRequest: TaskStateRequest): TaskStateResponse {
        logger.debug { "Search for the state of golden record task: executing searchTaskStates() with parameters $stateRequest" }
        val requestsByTaskId = stateRequest.entries.associateBy { it.taskId }

        return stateRequest.entries.map { toUUID(it.taskId) }
            .let { uuids -> taskRepository.findByUuidIn(uuids.toSet()) }
            .also { tasks -> taskRepository.fetchBusinessPartnerData(tasks) }
            .filter { task -> requestsByTaskId[task.uuid.toString()]?.recordId == task.gateRecord.privateId.toString() }
            .map { task -> responseToClientState(task, calculateTaskRetentionTimeout(task)) }
            .let { TaskStateResponse(tasks = it) }
    }
}
