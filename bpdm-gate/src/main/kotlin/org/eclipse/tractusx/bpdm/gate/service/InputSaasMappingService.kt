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

import org.eclipse.tractusx.bpdm.common.dto.saas.BusinessPartnerSaas
import org.eclipse.tractusx.bpdm.common.service.SaasMappings
import org.eclipse.tractusx.bpdm.common.service.SaasMappings.toDto
import org.eclipse.tractusx.bpdm.common.service.SaasMappings.toLegalEntityDto
import org.eclipse.tractusx.bpdm.common.service.SaasMappings.toSiteDto
import org.eclipse.tractusx.bpdm.gate.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.gate.config.SaasConfigProperties
import org.eclipse.tractusx.bpdm.gate.dto.AddressGateInputResponse
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityGateInputResponse
import org.eclipse.tractusx.bpdm.gate.dto.SiteGateInputResponse
import org.springframework.stereotype.Service

@Service
class InputSaasMappingService(
    private val saasConfigProperties: SaasConfigProperties,
    private val bpnConfigProperties: BpnConfigProperties
) {

    fun toInputLegalEntity(businessPartner: BusinessPartnerSaas): LegalEntityGateInputResponse {
        return LegalEntityGateInputResponse(
            legalEntity = businessPartner.toLegalEntityDto(),
            externalId = businessPartner.externalId!!,
            bpn = businessPartner.identifiers.find { it.type?.technicalKey == SaasMappings.BPN_TECHNICAL_KEY }?.value,
            processStartedAt = businessPartner.lastModifiedAt,
        )
    }

    fun toInputAddress(businessPartner: BusinessPartnerSaas, legalEntityExternalId: String?, siteExternalId: String?): AddressGateInputResponse {
        return AddressGateInputResponse(
            address = toDto(businessPartner.addresses.first()),
            externalId = businessPartner.externalId!!,
            legalEntityExternalId = legalEntityExternalId,
            siteExternalId = siteExternalId,
            bpn = businessPartner.identifiers.find { it.type?.technicalKey == bpnConfigProperties.id }?.value,
            processStartedAt = businessPartner.lastModifiedAt,
        )
    }

    fun toInputSite(businessPartner: BusinessPartnerSaas): SiteGateInputResponse {
        return SiteGateInputResponse(
            site = businessPartner.toSiteDto(),
            externalId = businessPartner.externalId!!,
            legalEntityExternalId = toParentLegalEntityExternalId(businessPartner)!!,
            bpn = businessPartner.identifiers.find { it.type?.technicalKey == bpnConfigProperties.id }?.value,
            processStartedAt = businessPartner.lastModifiedAt,
        )
    }

    fun toParentLegalEntityExternalId(businessPartner: BusinessPartnerSaas): String? {
        return toParentLegalEntityExternalIds(businessPartner).firstOrNull()
    }


    fun toParentLegalEntityExternalIds(businessPartner: BusinessPartnerSaas): Collection<String> {
        return businessPartner.relations
            .filter { it.startNodeDataSource == saasConfigProperties.datasource }
            .filter { it.type?.technicalKey == PARENT_RELATION_TYPE_KEY }
            .filter { it.endNode == businessPartner.externalId }
            .map { it.startNode }
    }
}
