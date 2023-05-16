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

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.exception.BusinessPartnerSharingError
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateDto
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.response.LsaType
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class GateUpdateService(
    val gateClient: GateClient
) {

    private val logger = KotlinLogging.logger { }

    fun handleLegalEntityCreateResponse(
        responseWrapper: LegalEntityPartnerCreateResponseWrapper
    ) {
        for (entity in responseWrapper.entities) {
            buildSuccessSharingStateDto(LsaType.LegalEntity, entity.index, entity.legalEntity.bpn)
                ?.let { gateClient.sharingState().upsertSharingState(it) }
        }
        for (errorInfo in responseWrapper.errors) {
            // entityKey should be an externalId
            buildErrorSharingStateDto(LsaType.LegalEntity, errorInfo.entityKey, errorInfo, true)
                ?.let { gateClient.sharingState().upsertSharingState(it) }
        }
        logger.info { "Sharing states for ${responseWrapper.entityCount} valid and ${responseWrapper.errorCount} invalid new legal entities were updated in the Gate" }
    }

    fun handleLegalEntityUpdateResponse(
        responseWrapper: LegalEntityPartnerUpdateResponseWrapper,
        externalIdByBpn: Map<String, String>
    ) {
        for (errorInfo in responseWrapper.errors) {
            // entityKey should be a BPN
            val externalId = externalIdByBpn[errorInfo.entityKey]
            buildErrorSharingStateDto(LsaType.LegalEntity, externalId, errorInfo, false)
                ?.let { gateClient.sharingState().upsertSharingState(it) }
        }
        logger.info { "Sharing states for ${responseWrapper.errorCount} invalid modified legal entities were updated in the Gate" }
    }

    fun handleSiteCreateResponse(
        responseWrapper: SitePartnerCreateResponseWrapper
    ) {
        for (entity in responseWrapper.entities) {
            buildSuccessSharingStateDto(LsaType.Site, entity.index, entity.site.bpn)
                ?.let { gateClient.sharingState().upsertSharingState(it) }
        }
        for (errorInfo in responseWrapper.errors) {
            // entityKey should be an externalId
            buildErrorSharingStateDto(LsaType.Site, errorInfo.entityKey, errorInfo, true)
                ?.let { gateClient.sharingState().upsertSharingState(it) }
        }
        logger.info { "Sharing states for ${responseWrapper.entityCount} valid and ${responseWrapper.errorCount} invalid new sites were updated in the Gate" }
    }

    fun handleSiteUpdateResponse(
        responseWrapper: SitePartnerUpdateResponseWrapper,
        externalIdByBpn: Map<String, String>
    ) {
        for (errorInfo in responseWrapper.errors) {
            // entityKey should be a BPN
            val externalId = externalIdByBpn[errorInfo.entityKey]
            buildErrorSharingStateDto(LsaType.Site, externalId, errorInfo, false)
                ?.let { gateClient.sharingState().upsertSharingState(it) }
        }
        logger.info { "Sharing states for ${responseWrapper.errorCount} invalid modified sites were updated in the Gate" }
    }

    fun handleAddressCreateResponse(
        responseWrapper: AddressPartnerCreateResponseWrapper
    ) {
        for (entity in responseWrapper.entities) {
            buildSuccessSharingStateDto(LsaType.Address, entity.index, entity.address.bpn)
                ?.let { gateClient.sharingState().upsertSharingState(it) }
        }
        for (errorInfo in responseWrapper.errors) {
            // entityKey should be an externalId
            buildErrorSharingStateDto(LsaType.Address, errorInfo.entityKey, errorInfo, true)
                ?.let { gateClient.sharingState().upsertSharingState(it) }
        }
        logger.info { "Sharing states for ${responseWrapper.entityCount} valid and ${responseWrapper.errorCount} invalid new addresses were updated in the Gate" }
    }

    fun handleAddressUpdateResponse(
        responseWrapper: AddressPartnerUpdateResponseWrapper,
        externalIdByBpn: Map<String, String>
    ) {
        for (errorInfo in responseWrapper.errors) {
            // entityKey should be a BPN
            val externalId = externalIdByBpn[errorInfo.entityKey]
            buildErrorSharingStateDto(LsaType.Address, externalId, errorInfo, false)
                ?.let { gateClient.sharingState().upsertSharingState(it) }
        }
        logger.info { "Sharing states for ${responseWrapper.errorCount} invalid modified addresses were updated in the Gate" }
    }

    private fun buildSuccessSharingStateDto(lsaType: LsaType, index: String?, bpn: String): SharingStateDto? {
        if (index == null) {
            logger.warn { "Encountered index=null in Pool response for $bpn, can't update the Gate sharing state" }
            return null
        }
        return SharingStateDto(
            lsaType = lsaType,
            externalId = index,
            sharingStateType = SharingStateType.Success,
            bpn = bpn,
            sharingProcessStarted = LocalDateTime.now()
        )
    }

    private fun buildErrorSharingStateDto(lsaType: LsaType, externalId: String?, errorInfo: ErrorInfo<*>, processStarted: Boolean): SharingStateDto? {
        if (externalId == null) {
            logger.warn { "Couldn't determine externalId for $errorInfo, can't update the Gate sharing state" }
            return null
        }
        return SharingStateDto(
            lsaType = lsaType,
            externalId = externalId,
            sharingStateType = SharingStateType.Error,
            sharingErrorCode = BusinessPartnerSharingError.SharingProcessError,
            sharingErrorMessage = "${errorInfo.message} (${errorInfo.errorCode})",
            sharingProcessStarted = if (processStarted) LocalDateTime.now() else null
        )
    }

}
