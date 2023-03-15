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
import org.eclipse.tractusx.bpdm.common.dto.saas.AugmentedBusinessPartnerResponseSaas
import org.eclipse.tractusx.bpdm.common.dto.saas.BusinessPartnerSaas
import org.eclipse.tractusx.bpdm.common.dto.saas.SharingStatusSaas
import org.eclipse.tractusx.bpdm.common.dto.saas.isError
import org.eclipse.tractusx.bpdm.common.service.SaasMappings
import org.eclipse.tractusx.bpdm.gate.config.SaasConfigProperties
import org.eclipse.tractusx.bpdm.gate.dto.response.ErrorInfo
import org.eclipse.tractusx.bpdm.gate.exception.BusinessPartnerOutputError
import org.eclipse.tractusx.bpdm.gate.filterNotNullKeys
import org.eclipse.tractusx.bpdm.gate.model.BusinessPartnerSaaSWithBpn
import org.eclipse.tractusx.bpdm.gate.model.BusinessPartnerSaaSWithExternalId
import org.eclipse.tractusx.bpdm.gate.model.SharingStatusEvaluationResult
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class OutputSaasMappingService(
    private val saasConfigProperties: SaasConfigProperties
) {

    private val logger = KotlinLogging.logger { }

    fun mapWithLocalBpn(
        partnersWithExternalId: Collection<BusinessPartnerSaaSWithExternalId>,
        augmentedPartners: Collection<AugmentedBusinessPartnerResponseSaas>
    ): Collection<BusinessPartnerSaaSWithBpn> {
        val bpnByExternalIdMap = buildBpnByExternalIdMap(augmentedPartners)

        val partnersWithLocalBpn = partnersWithExternalId
            .filter { partner -> bpnByExternalIdMap[partner.externalId] != null }
            .map { partner -> BusinessPartnerSaaSWithBpn(partner.partner, bpnByExternalIdMap[partner.externalId]!!, partner.externalId) }

        return partnersWithLocalBpn
    }

    fun evaluateSharingStatus(
        partners: Collection<BusinessPartnerSaas>,
        partnersWithLocalBpn: Collection<BusinessPartnerSaaSWithBpn>,
        partnersWithPoolBpn: Collection<BusinessPartnerSaaSWithBpn>
    ): SharingStatusEvaluationResult {

        val partnersWithExternalId = mapWithExternalId(partners)

        val localBpnByExternalId = partnersWithLocalBpn.associateBy { it.externalId }
        val poolBpnByExternalId = partnersWithPoolBpn.associateBy { it.externalId }

        /// We sort all the entries in one of 3 buckets: valid content, errors or still pending
        val validExternalIds = mutableListOf<String>()
        val errors = mutableListOf<ErrorInfo<BusinessPartnerOutputError>>()
        val pendingExternalIds = mutableListOf<String>()

        partnersWithExternalId.forEach { partnerWithId ->
            val partner = partnerWithId.partner
            val sharingStatus = partner.metadata?.sharingStatus
            val sharingStatusType = sharingStatus?.status
            val externalId = partnerWithId.externalId
            val localBpn = localBpnByExternalId[externalId]?.bpn
            val poolBpn = poolBpnByExternalId[externalId]?.bpn

            if (sharingStatusType == null || sharingStatusType.isError()) {
                // ERROR: SharingProcessError
                errors.add(buildErrorInfoSharingProcessError(externalId, sharingStatus))
            } else if (localBpn != null) {
                if (poolBpn != null) {
                    // OKAY: entry found in pool
                    validExternalIds.add(externalId)
                } else {
                    // ERROR: BpnNotInPool
                    errors.add(buildErrorInfoBpnNotInPool(externalId, localBpn))
                }
            } else if (isSharingTimeoutReached(partner)) {
                // ERROR: SharingTimeout
                errors.add(buildErrorInfoSharingTimeout(externalId, partner.lastModifiedAt))
            } else {
                pendingExternalIds.add(externalId)
            }
        }

        return SharingStatusEvaluationResult(validExternalIds, pendingExternalIds, errors)
    }


    fun mapWithExternalId(partners: Collection<BusinessPartnerSaas>): Collection<BusinessPartnerSaaSWithExternalId> {
        val (partnersWithExternalId, partnersWithoutExternalId) = partners.partition { it.externalId != null }
        //Should only happen when paginating without filtering for externalIds
        if (partnersWithoutExternalId.isNotEmpty()) {
            logger.warn { "Encountered ${partnersWithoutExternalId.size} records without an externalId." }
        }

        return partnersWithExternalId.map { BusinessPartnerSaaSWithExternalId(it, it.externalId!!) }
    }

    fun buildBpnByExternalIdMap(augmentedPartnerWrapperCollection: Collection<AugmentedBusinessPartnerResponseSaas>): Map<String, String?> {
        val augmentedPartnerCollection = augmentedPartnerWrapperCollection.mapNotNull { it.augmentedBusinessPartner }
        val missingAugmentedPartnerCount = augmentedPartnerWrapperCollection.size - augmentedPartnerCollection.size
        if (missingAugmentedPartnerCount > 0) {
            logger.warn { "Encountered $missingAugmentedPartnerCount entries of AugmentedBusinessPartnerResponseSaas without any usable data." }
        }

        val bpnByExternalIdMap = augmentedPartnerCollection
            .associateBy({ it.externalId }, { SaasMappings.findBpn(it.identifiers) })
            .filterNotNullKeys()
        val missingExternalIdCount = augmentedPartnerCollection.size - bpnByExternalIdMap.size
        if (missingExternalIdCount > 0) {
            logger.warn { "Encountered $missingExternalIdCount entries without external id in SaaS." }
        }

        return bpnByExternalIdMap
    }

    fun isSharingTimeoutReached(partner: BusinessPartnerSaas) =
        partner.lastModifiedAt?.isBefore(LocalDateTime.now().minus(saasConfigProperties.sharingTimeout)) ?: true

    fun buildErrorInfoSharingProcessError(externalId: String, sharingStatus: SharingStatusSaas?): ErrorInfo<BusinessPartnerOutputError> {
        val message =
            if (sharingStatus == null) "No SaaS sharing status available"
            else "SaaS sharing process error: ${sharingStatus.description}"
        return ErrorInfo(
            errorCode = BusinessPartnerOutputError.SharingProcessError,
            message = message,
            entityKey = externalId
        )
    }

    fun buildErrorInfoBpnNotInPool(externalId: String, bpn: String): ErrorInfo<BusinessPartnerOutputError> =
        ErrorInfo(
            errorCode = BusinessPartnerOutputError.BpnNotInPool,
            message = "$bpn not found in pool",
            entityKey = externalId
        )

    fun buildErrorInfoSharingTimeout(externalId: String, lastModifiedAt: LocalDateTime?): ErrorInfo<BusinessPartnerOutputError> =
        ErrorInfo(
            errorCode = BusinessPartnerOutputError.SharingTimeout,
            message = "SaaS sharing timeout: Last modified ${lastModifiedAt}",
            entityKey = externalId
        )
}
