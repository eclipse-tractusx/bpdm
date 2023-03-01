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
import org.eclipse.tractusx.bpdm.common.service.SaasMappings
import org.eclipse.tractusx.bpdm.gate.config.SaasConfigProperties
import org.eclipse.tractusx.bpdm.gate.dto.response.ErrorInfo
import org.eclipse.tractusx.bpdm.gate.exception.BusinessPartnerOutputError
import org.eclipse.tractusx.bpdm.gate.filterNotNullKeys
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class OutputSaasMappingService(
    private val saasConfigProperties: SaasConfigProperties,
) {

    private val logger = KotlinLogging.logger { }

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
