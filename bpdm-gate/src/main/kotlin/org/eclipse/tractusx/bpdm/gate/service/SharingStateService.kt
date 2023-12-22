/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.gate.api.exception.BusinessPartnerSharingError
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.response.SharingStateDto
import org.eclipse.tractusx.bpdm.gate.entity.SharingState
import org.eclipse.tractusx.bpdm.gate.exception.BpdmInvalidStateException
import org.eclipse.tractusx.bpdm.gate.exception.BpdmInvalidStateRequestException
import org.eclipse.tractusx.bpdm.gate.exception.BpdmMissingPartnerException
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository.Specs.byBusinessPartnerType
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository.Specs.byExternalIdsIn
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import java.time.LocalDateTime

const val ERROR_MISSING_CODE = "Request for Error state but no error code specified."
const val ERROR_MISSING_BPN = "Request for Success state but no BPN specified."
const val ERROR_MISSING_TASK = "Request for Pending state but no task-id specified."

@Service
class SharingStateService(private val stateRepository: SharingStateRepository) {

    private val logger = KotlinLogging.logger { }

    /**
     * Upsert fixed sharing state based on given DTO
     */
    fun upsertSharingState(request: SharingStateDto) {
        logger.info { "Executing upsertSharingState() with parameters $request" }
        val sharingState = getOrCreate(request.externalId, request.businessPartnerType)

        when (request.sharingStateType) {
            SharingStateType.Pending -> setPending(
                sharingState,
                request.taskId ?: throw BpdmInvalidStateRequestException(ERROR_MISSING_TASK),
                startTimeOverwrite = request.sharingProcessStarted
            )

            SharingStateType.Success -> setSuccess(
                sharingState = sharingState,
                bpn = request.bpn ?: throw BpdmInvalidStateRequestException(ERROR_MISSING_BPN),
                startTimeOverwrite = request.sharingProcessStarted
            )

            SharingStateType.Error -> setError(
                sharingState = sharingState,
                sharingErrorCode = request.sharingErrorCode ?: throw BpdmInvalidStateRequestException(ERROR_MISSING_CODE),
                sharingErrorMessage = request.sharingErrorMessage,
                startTimeOverwrite = request.sharingProcessStarted
            )

            SharingStateType.Initial -> setInitial(sharingState)

            SharingStateType.Ready -> setReady(sharingState)
        }
    }

    fun findSharingStates(
        paginationRequest: PaginationRequest,
        businessPartnerType: BusinessPartnerType?,
        externalIds: Collection<String>?
    ): PageDto<SharingStateDto> {

        logger.info { "findSharingStates() called with $paginationRequest // $businessPartnerType and $externalIds" }

        val pageRequest = PageRequest.of(paginationRequest.page, paginationRequest.size)
        val spec = Specification.allOf(byExternalIdsIn(externalIds), byBusinessPartnerType(businessPartnerType))
        val sharingStatePage = stateRepository.findAll(spec, pageRequest)

        return sharingStatePage.toPageDto {
            SharingStateDto(
                externalId = it.externalId,
                businessPartnerType = it.businessPartnerType,
                sharingStateType = it.sharingStateType,
                sharingErrorCode = it.sharingErrorCode,
                sharingErrorMessage = it.sharingErrorMessage,
                bpn = it.bpn,
                sharingProcessStarted = it.sharingProcessStarted,
                taskId = it.taskId
            )
        }
    }

    fun setInitial(sharingStateIds: List<SharingStateIdentifierDto>): List<SharingState> {
        val sharingStates = getOrCreate(sharingStateIds)
        return sharingStates.map { setInitial(it) }
    }

    fun setSuccess(successRequests: List<SuccessRequest>): List<SharingState> {
        val sharingStates = getOrCreate(successRequests.map { it.sharingStateId })
        return sharingStates
            .zip(successRequests)
            .map { (sharingState, request) -> setSuccess(sharingState, request.bpn, request.startTimeOverwrite) }
    }

    fun setPending(pendingRequests: List<PendingRequest>): List<SharingState> {
        val sharingStates = getOrCreate(pendingRequests.map { it.sharingStateId })
        return sharingStates
            .zip(pendingRequests)
            .map { (sharingState, request) -> setPending(sharingState, request.taskId, request.startTimeOverwrite) }
    }

    fun setError(errorRequests: List<ErrorRequest>): List<SharingState>{
        val sharingStates = getOrCreate(errorRequests.map { it.sharingStateId })

        return sharingStates
            .zip(errorRequests)
            .map { (sharingState, request) -> setError(sharingState, request.errorCode, request.errorMessage, request.startTimeOverwrite) }
    }

    fun setReady(externalIds: List<String>): List<SharingState> {
        val existingSharingStates = stateRepository.findByExternalIdInAndBusinessPartnerType(externalIds, BusinessPartnerType.GENERIC)
        val existingIds = existingSharingStates.map { it.externalId }.toSet()
        val missingIds = externalIds.minus(existingIds)

        if (missingIds.isNotEmpty())
            throw BpdmMissingPartnerException(missingIds)


        val (correctStates, incorrectStates) = existingSharingStates.partition {
            it.sharingStateType == SharingStateType.Initial
                    || it.sharingStateType == SharingStateType.Error
        }

        if (incorrectStates.isNotEmpty())
            throw BpdmInvalidStateException(incorrectStates.map { BpdmInvalidStateException.InvalidState(it.externalId, it.sharingStateType) })

        return correctStates.map { setReady(it) }
    }

    private fun setInitial(sharingState: SharingState): SharingState {
        sharingState.sharingStateType = SharingStateType.Initial
        sharingState.sharingErrorCode = null
        sharingState.sharingErrorMessage = null
        sharingState.sharingProcessStarted = null
        sharingState.taskId = null

        return stateRepository.save(sharingState)
    }

    private fun setSuccess(sharingState: SharingState, bpn: String, startTimeOverwrite: LocalDateTime? = null): SharingState {

        sharingState.sharingStateType = SharingStateType.Success
        sharingState.sharingErrorCode = null
        sharingState.sharingErrorMessage = null
        sharingState.bpn = bpn
        sharingState.sharingProcessStarted = startTimeOverwrite ?: sharingState.sharingProcessStarted ?: LocalDateTime.now()

        return stateRepository.save(sharingState)
    }

    private fun setPending(sharingState: SharingState, taskId: String, startTimeOverwrite: LocalDateTime? = null): SharingState {
        sharingState.sharingStateType = SharingStateType.Pending
        sharingState.sharingErrorCode = null
        sharingState.sharingErrorMessage = null
        sharingState.sharingProcessStarted = startTimeOverwrite ?: sharingState.sharingProcessStarted ?: LocalDateTime.now()
        sharingState.taskId = taskId

        return stateRepository.save(sharingState)
    }

    private fun setError(
        sharingState: SharingState,
        sharingErrorCode: BusinessPartnerSharingError,
        sharingErrorMessage: String? = null,
        startTimeOverwrite: LocalDateTime? = null
    ): SharingState {
        sharingState.sharingStateType = SharingStateType.Error
        sharingState.sharingErrorCode = sharingErrorCode
        sharingState.sharingErrorMessage = sharingErrorMessage
        sharingState.sharingProcessStarted = startTimeOverwrite ?: sharingState.sharingProcessStarted ?: LocalDateTime.now()

        return stateRepository.save(sharingState)
    }

    private fun setReady(
        sharingState: SharingState
    ): SharingState {
        sharingState.sharingStateType = SharingStateType.Ready
        sharingState.sharingErrorCode = null
        sharingState.sharingErrorMessage = null
        sharingState.sharingProcessStarted = null
        sharingState.taskId = null

        return stateRepository.save(sharingState)
    }



    private fun getOrCreate(sharingStateIdentifiers: List<SharingStateIdentifierDto>): List<SharingState>{
        val identifiersByType = sharingStateIdentifiers.groupBy { it.businessPartnerType }

        val sharingStates = identifiersByType.flatMap { entry -> getOrCreate(entry.value.map { it.externalId }, entry.key) }
        val sharingStatesByExternalId = sharingStates.associateBy { it.externalId }

        return sharingStateIdentifiers.map { sharingStatesByExternalId[it.externalId]!! }
    }


    private fun getOrCreate(externalIds: List<String>, businessPartnerType: BusinessPartnerType): List<SharingState> {
        val sharingStates = stateRepository.findByExternalIdInAndBusinessPartnerType(externalIds, businessPartnerType)
        val sharingStatesByExternalId = sharingStates.associateBy { it.externalId }

        return externalIds.map { externalId ->
            sharingStatesByExternalId[externalId]
                ?: SharingState(
                    externalId,
                    businessPartnerType = businessPartnerType,
                    sharingStateType = SharingStateType.Initial,
                    sharingErrorCode = null,
                    sharingErrorMessage = null,
                    bpn = null,
                    sharingProcessStarted = null
            )
        }
    }


    private fun getOrCreate(externalId: String, businessPartnerType: BusinessPartnerType): SharingState {
        return getOrCreate(listOf(externalId), businessPartnerType).single()
    }

    data class SharingStateIdentifierDto(
        val externalId: String,
        val businessPartnerType: BusinessPartnerType
    )

    data class PendingRequest(
        val sharingStateId: SharingStateIdentifierDto,
        val taskId: String,
        val startTimeOverwrite: LocalDateTime? = null
    )

    data class SuccessRequest(
        val sharingStateId: SharingStateIdentifierDto,
        val bpn: String,
        val startTimeOverwrite: LocalDateTime? = null
    )

    data class ErrorRequest(
        val sharingStateId: SharingStateIdentifierDto,
        val errorCode: BusinessPartnerSharingError,
        val errorMessage: String?,
        val startTimeOverwrite: LocalDateTime? = null
    )
}