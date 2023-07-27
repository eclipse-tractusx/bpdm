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

package com.catenax.bpdm.bridge.dummy.service

import com.catenax.bpdm.bridge.dummy.dto.*
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.response.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.exception.BusinessPartnerSharingError
import org.eclipse.tractusx.bpdm.gate.api.model.LsaType
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.request.AddressGateOutputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.LegalEntityGateOutputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.SiteGateOutputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.SharingStateDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class GateUpdateService(
    val gateClient: GateClient
) {

    private val logger = KotlinLogging.logger { }

    fun handleLegalEntityCreateResponse(
        responseWrapper: LegalEntityPartnerCreateResponseWrapper,
        requestEntryByExternalId: Map<String, GateLegalEntityInfo>
    ) {
        for (entity in responseWrapper.entities) {
            val externalId = entity.index
            val requestEntry = requestEntryByExternalId[externalId]
            // TODO It's confusing that both the PUT methods for the output and sharing state take a BPN independently.
            putLegalEntityOutput(
                buildLegalEntityGateOutputRequest(requestEntry, entity)
            )
        }
        for (errorInfo in responseWrapper.errors) {
            val externalId = errorInfo.entityKey
            upsertSharingState(
                buildErrorSharingStateDto(LsaType.LEGAL_ENTITY, externalId, null, errorInfo, true)
            )

        }
        logger.info { "Sharing states for ${responseWrapper.entityCount} valid and ${responseWrapper.errorCount} invalid new legal entities were updated in the Gate" }
    }

    fun handleLegalEntityUpdateResponse(
        responseWrapper: LegalEntityPartnerUpdateResponseWrapper,
        externalIdByBpn: Map<String, String>,
        requestEntryByExternalId: Map<String, GateLegalEntityInfo>
    ) {
        for (entity in responseWrapper.entities) {
            val bpn = entity.legalEntity.bpnl
            val externalId = externalIdByBpn[bpn]
            val requestEntry = requestEntryByExternalId[externalId]
            putLegalEntityOutput(
                buildLegalEntityGateOutputRequest(requestEntry, entity)
            )
        }
        for (errorInfo in responseWrapper.errors) {
            val bpn = errorInfo.entityKey
            val externalId = externalIdByBpn[bpn]
            upsertSharingState(
                buildErrorSharingStateDto(LsaType.LEGAL_ENTITY, externalId, bpn, errorInfo, false)
            )
        }
        logger.info { "Sharing states for ${responseWrapper.entityCount} valid and ${responseWrapper.errorCount} invalid modified legal entities were updated in the Gate" }
    }

    fun handleSiteCreateResponse(
        responseWrapper: SitePartnerCreateResponseWrapper,
        requestEntryByExternalId: Map<String, GateSiteInfo>
    ) {
        for (entity in responseWrapper.entities) {
            val externalId = entity.index
            val requestEntry = requestEntryByExternalId[externalId]
            putSiteOutput(
                buildSiteGateOutputRequest(requestEntry, entity)
            )
        }
        for (errorInfo in responseWrapper.errors) {
            val externalId = errorInfo.entityKey
            upsertSharingState(
                buildErrorSharingStateDto(LsaType.SITE, externalId, null, errorInfo, true)
            )
        }
        logger.info { "Sharing states for ${responseWrapper.entityCount} valid and ${responseWrapper.errorCount} invalid new sites were updated in the Gate" }
    }

    fun handleSiteUpdateResponse(
        responseWrapper: SitePartnerUpdateResponseWrapper,
        externalIdByBpn: Map<String, String>,
        requestEntryByExternalId: Map<String, GateSiteInfo>
    ) {
        for (entity in responseWrapper.entities) {
            val bpn = entity.site.bpns
            val externalId = externalIdByBpn[bpn]
            val requestEntry = requestEntryByExternalId[externalId]
            putSiteOutput(
                buildSiteGateOutputRequest(requestEntry, entity)
            )
        }
        for (errorInfo in responseWrapper.errors) {
            val bpn = errorInfo.entityKey
            val externalId = externalIdByBpn[bpn]
            upsertSharingState(
                buildErrorSharingStateDto(LsaType.SITE, externalId, bpn, errorInfo, false)
            )
        }
        logger.info { "Sharing states for ${responseWrapper.entityCount} valid and ${responseWrapper.errorCount} invalid modified sites were updated in the Gate" }
    }

    fun handleAddressCreateResponse(
        responseWrapper: AddressPartnerCreateResponseWrapper,
        requestEntryByExternalId: Map<String, GateAddressInfo>
    ) {
        for (entity in responseWrapper.entities) {
            val externalId = entity.index
            val requestEntry = requestEntryByExternalId[externalId]
            putAddressOutput(
                buildAddressGateOutputRequest(requestEntry, entity.address)
            )
        }
        for (errorInfo in responseWrapper.errors) {
            val externalId = errorInfo.entityKey
            upsertSharingState(
                buildErrorSharingStateDto(LsaType.ADDRESS, externalId, null, errorInfo, true)
            )
        }
        logger.info { "Sharing states for ${responseWrapper.entityCount} valid and ${responseWrapper.errorCount} invalid new addresses were updated in the Gate" }
    }

    fun handleAddressUpdateResponse(
        responseWrapper: AddressPartnerUpdateResponseWrapper,
        externalIdByBpn: Map<String, String>,
        requestEntryByExternalId: Map<String, GateAddressInfo>
    ) {
        for (entity in responseWrapper.entities) {
            val bpn = entity.bpna
            val externalId = externalIdByBpn[bpn]
            val requestEntry = requestEntryByExternalId[externalId]
            putAddressOutput(
                buildAddressGateOutputRequest(requestEntry, entity)
            )
        }
        for (errorInfo in responseWrapper.errors) {
            val bpn = errorInfo.entityKey
            val externalId = externalIdByBpn[bpn]
            upsertSharingState(
                buildErrorSharingStateDto(LsaType.ADDRESS, externalId, bpn, errorInfo, false)
            )
        }
        logger.info { "Sharing states for ${responseWrapper.entityCount} valid and ${responseWrapper.errorCount} invalid modified addresses were updated in the Gate" }
    }

    private fun buildLegalEntityGateOutputRequest(
        requestEntry: GateLegalEntityInfo?,
        poolResponse: LegalEntityPartnerCreateVerboseDto
    ): LegalEntityGateOutputRequest? {
        if (requestEntry == null) {
            logger.warn { "No matching request for Pool response for ${poolResponse.legalEntity.bpnl} found, can't update the Gate output state" }
            return null
        }
        return LegalEntityGateOutputRequest(
            legalNameParts = listOfNotNull(poolResponse.legalName),
            legalEntity = poolToGateLegalEntity(poolResponse.legalEntity),
            legalAddress = poolToGateAddressChild(poolResponse.legalAddress),
            externalId = requestEntry.externalId,
            bpn = poolResponse.legalEntity.bpnl
        )
    }

    private fun buildSiteGateOutputRequest(requestEntry: GateSiteInfo?, poolResponse: SitePartnerCreateVerboseDto): SiteGateOutputRequest? {
        if (requestEntry == null) {
            logger.warn { "No matching request for Pool response for ${poolResponse.site.bpns} found, can't update the Gate output state" }
            return null
        }
        return SiteGateOutputRequest(
            site = poolToGateSite(poolResponse.site),
            mainAddress = poolToGateAddressChild(poolResponse.mainAddress),
            externalId = requestEntry.externalId,
            bpn = poolResponse.site.bpns,
            legalEntityExternalId = requestEntry.legalEntityExternalId
        )
    }

    private fun buildAddressGateOutputRequest(requestEntry: GateAddressInfo?, poolResponse: LogisticAddressVerboseDto): AddressGateOutputRequest? {
        if (requestEntry == null) {
            logger.warn { "No matching request for Pool response for ${poolResponse.bpna} found, can't update the Gate output state" }
            return null
        }
        return AddressGateOutputRequest(
            address = poolToGateLogisticAddress(poolResponse),
            externalId = requestEntry.externalId,
            bpn = poolResponse.bpna,
            legalEntityExternalId = requestEntry.legalEntityExternalId,
            siteExternalId = requestEntry.siteExternalId
        )
    }

    private fun buildSuccessSharingStateDto(lsaType: LsaType, externalId: String?, bpn: String, processStarted: Boolean): SharingStateDto? {
        if (externalId == null) {
            logger.warn { "Encountered externalId=null in Pool response for $bpn, can't update the Gate sharing state" }
            return null
        }
        return SharingStateDto(
            lsaType = lsaType,
            externalId = externalId,
            sharingStateType = SharingStateType.Success,
            bpn = bpn,
            sharingProcessStarted = if (processStarted) LocalDateTime.now() else null
        )
    }

    private fun buildErrorSharingStateDto(
        lsaType: LsaType,
        externalId: String?,
        bpn: String?,
        errorInfo: ErrorInfo<*>,
        processStarted: Boolean
    ): SharingStateDto? {
        if (externalId == null) {
            logger.warn { "Couldn't determine externalId for $errorInfo, can't update the Gate sharing state" }
            return null
        }
        return SharingStateDto(
            lsaType = lsaType,
            externalId = externalId,
            sharingStateType = SharingStateType.Error,
            bpn = bpn,
            sharingErrorCode = BusinessPartnerSharingError.SharingProcessError,
            sharingErrorMessage = "${errorInfo.message} (${errorInfo.errorCode})",
            sharingProcessStarted = if (processStarted) LocalDateTime.now() else null
        )
    }

    private fun putLegalEntityOutput(request: LegalEntityGateOutputRequest?) =
        request?.let {
            gateClient.legalEntities().upsertLegalEntitiesOutput(listOf(it))
        }

    private fun putSiteOutput(request: SiteGateOutputRequest?) =
        request?.let {
            gateClient.sites().upsertSitesOutput(listOf(it))
        }

    private fun putAddressOutput(request: AddressGateOutputRequest?) =
        request?.let {
            gateClient.addresses().putAddressesOutput(listOf(it))
        }

    private fun upsertSharingState(request: SharingStateDto?) =
        request?.let {
            gateClient.sharingState().upsertSharingState(it)
        }

}
