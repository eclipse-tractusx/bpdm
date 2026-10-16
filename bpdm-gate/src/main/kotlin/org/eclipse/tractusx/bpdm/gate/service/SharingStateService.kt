/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeLogger
import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeType
import org.eclipse.tractusx.bpdm.gate.api.exception.BusinessPartnerSharingError
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.config.GoldenRecordTaskConfigProperties
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.model.businesspartner.BusinessPartnerChange
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime


@Service
class SharingStateService(
    private val stateRepository: SharingStateRepository,
    private val goldenRecordTaskConfigProperties: GoldenRecordTaskConfigProperties,
    private val changeLogger: PersistedChangeLogger
) {

    fun setInitial(sharingState: SharingStateDb): SharingStateDb {
        sharingState.sharingStateType =
                //If new business partner data should be immediately ready to be shared our initial state is ready instead
            if (goldenRecordTaskConfigProperties.creation.fromSharingMember.startsAsReady)
                SharingStateType.Ready
            else
                SharingStateType.Initial
        sharingState.sharingErrorCode = null
        sharingState.sharingErrorMessage = null
        sharingState.sharingProcessStarted = null
        sharingState.taskId = null

        return stateRepository.save(sharingState)
            .also { logChange(it, PersistedChangeType.Updated) }
    }

    fun setSuccess(sharingState: SharingStateDb, startTimeOverwrite: LocalDateTime? = null): SharingStateDb {

        sharingState.sharingStateType = SharingStateType.Success
        sharingState.sharingErrorCode = null
        sharingState.sharingErrorMessage = null
        sharingState.sharingProcessStarted = startTimeOverwrite ?: sharingState.sharingProcessStarted ?: LocalDateTime.now()

        return stateRepository.save(sharingState)
    }

    fun setPending(sharingState: SharingStateDb, taskId: String, startTimeOverwrite: LocalDateTime? = null): SharingStateDb {
        sharingState.sharingStateType = SharingStateType.Pending
        sharingState.sharingErrorCode = null
        sharingState.sharingErrorMessage = null
        sharingState.sharingProcessStarted = startTimeOverwrite ?: sharingState.sharingProcessStarted ?: LocalDateTime.now()
        sharingState.taskId = taskId

        return stateRepository.save(sharingState)
    }

    fun setError(
        sharingState: SharingStateDb,
        sharingErrorCode: BusinessPartnerSharingError,
        sharingErrorMessage: String? = null,
        startTimeOverwrite: LocalDateTime? = null
    ): SharingStateDb {
        sharingState.sharingStateType = SharingStateType.Error
        sharingState.sharingErrorCode = sharingErrorCode
        sharingState.sharingErrorMessage = sharingErrorMessage
        sharingState.sharingProcessStarted = startTimeOverwrite ?: sharingState.sharingProcessStarted ?: LocalDateTime.now()

        return stateRepository.save(sharingState)
    }


    /**
     * Returns the sharing states of [externalIds] for [ownerBpnl] in the same order, creating each missing one in its
     * initial state.
     */
    fun getOrCreateStates(externalIds: List<String>, ownerBpnl: String?): List<SharingStateDb> {
        val sharingStates = stateRepository.findByExternalIdInAndTenantBpnl(externalIds, ownerBpnl)
        val sharingStatesByExternalId = sharingStates.associateBy { it.externalId }

        return externalIds.map { externalId ->
            sharingStatesByExternalId[externalId]
                ?: createInitial(externalId, ownerBpnl)
        }
    }

    private fun createInitial(externalId: String, ownerBpnl: String?): SharingStateDb {
        val sharingState = SharingStateDb(
            externalId,
            sharingStateType = SharingStateType.Ready,
            orchestratorRecordId = null,
            sharingErrorCode = null,
            sharingErrorMessage = null,
            sharingProcessStarted = null,
            tenantBpnl = ownerBpnl,
            isGoldenRecordCounted = null,
            syncedIsGoldenRecordCounted = null
        )
        return setInitial(sharingState)
            .also { logChange(it, PersistedChangeType.Created) }
    }

    private fun logChange(sharingState: SharingStateDb, type: PersistedChangeType) {
        changeLogger.log(BusinessPartnerChange(type, sharingState.externalId, sharingState.tenantBpnl, BusinessPartnerChange.Aspect.SharingState))
    }
}
