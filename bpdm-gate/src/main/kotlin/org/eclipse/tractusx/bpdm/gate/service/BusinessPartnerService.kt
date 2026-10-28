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

import org.eclipse.tractusx.bpdm.gate.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.gate.entity.ChangelogEntryDb
import org.eclipse.tractusx.bpdm.gate.entity.GoldenRecordType
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.OutputUpsertData
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.bpdm.gate.util.BusinessPartnerComparisonUtil
import org.eclipse.tractusx.bpdm.gate.util.BusinessPartnerCopyUtil
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BusinessPartnerService(
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val changelogCreateService: ChangelogCreateService,
    private val copyUtil: BusinessPartnerCopyUtil,
    private val compareUtil: BusinessPartnerComparisonUtil,
    private val outputUpsertMappings: OutputUpsertMappings
) {

    /**
     * Creates the output of [sharingState] with [upsertData], or updates [storedOutput] to it when one exists.
     */
    @Transactional
    fun upsertBusinessPartnerOutput(storedOutput: BusinessPartnerDb?, sharingState: SharingStateDb, upsertData: OutputUpsertData): UpsertResult =
        upsertFromEntity(storedOutput, outputUpsertMappings.toEntity(upsertData, sharingState))

    private fun upsertFromEntity(existingPartner: BusinessPartnerDb?, upsertData: BusinessPartnerDb): UpsertResult {
        val sharingState = upsertData.sharingState
        val stage = upsertData.stage
        val changeType = if (existingPartner == null) ChangelogType.CREATE else ChangelogType.UPDATE
        val partnerToUpsert = existingPartner ?: BusinessPartnerDb.createEmpty(upsertData.sharingState, upsertData.stage)

        val hasChanges = changeType == ChangelogType.CREATE || compareUtil.hasChanges(upsertData, partnerToUpsert)
        val shouldUpdate = when {
            upsertData.externalSequenceTimestamp == null -> true
            existingPartner?.externalSequenceTimestamp == null -> true
            else -> upsertData.externalSequenceTimestamp!!.isAfter(existingPartner.externalSequenceTimestamp)
        }

        if (hasChanges && shouldUpdate) {
                changelogCreateService.record(
                    ChangelogEntryDb(sharingState.externalId, sharingState.tenantBpnl, changeType, stage, GoldenRecordType.BusinessPartner)
                )

                copyUtil.copyValues(upsertData, partnerToUpsert)
                businessPartnerRepository.save(partnerToUpsert)
        }

        return UpsertResult(hasChanges, shouldUpdate, changeType, partnerToUpsert)
    }

    data class UpsertResult(
        val hadChanges: Boolean,
        val shouldUpdate: Boolean,
        val type: ChangelogType,
        val businessPartner: BusinessPartnerDb
    )
}
