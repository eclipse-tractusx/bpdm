/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

import org.eclipse.tractusx.bpdm.common.dto.cdq.BusinessPartnerCdq
import org.eclipse.tractusx.bpdm.common.dto.cdq.RelationCdq
import org.eclipse.tractusx.bpdm.common.service.CdqMappings
import org.eclipse.tractusx.bpdm.common.service.CdqMappings.toDto
import org.eclipse.tractusx.bpdm.common.service.CdqMappings.toLegalEntityDto
import org.eclipse.tractusx.bpdm.common.service.CdqMappings.toSiteDto
import org.eclipse.tractusx.bpdm.gate.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.gate.config.CdqConfigProperties
import org.eclipse.tractusx.bpdm.gate.dto.AddressGateInput
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityGateInput
import org.eclipse.tractusx.bpdm.gate.dto.SiteGateInput
import org.springframework.stereotype.Service

@Service
class InputCdqMappingService(
    private val cdqConfigProperties: CdqConfigProperties,
    private val bpnConfigProperties: BpnConfigProperties
) {

    fun toInputLegalEntity(businessPartner: BusinessPartnerCdq): LegalEntityGateInput {
        return LegalEntityGateInput(
            businessPartner.externalId!!,
            businessPartner.identifiers.find { it.type?.technicalKey == CdqMappings.BPN_TECHNICAL_KEY }?.value,
            businessPartner.toLegalEntityDto()
        )
    }

    fun toInputAddress(businessPartner: BusinessPartnerCdq): AddressGateInput {
        return AddressGateInput(
            bpn = businessPartner.identifiers.find { it.type?.technicalKey == bpnConfigProperties.id }?.value,
            address = toDto(businessPartner.addresses.single()),
            externalId = businessPartner.externalId!!,
            legalEntityExternalId = toParentLegalEntityExternalId(businessPartner.relations),
            siteExternalId = toParentSiteExternalId(businessPartner.relations)
        )
    }

    fun toInputSite(businessPartner: BusinessPartnerCdq): SiteGateInput {
        return SiteGateInput(
            bpn = businessPartner.identifiers.find { it.type?.technicalKey == bpnConfigProperties.id }?.value,
            site = businessPartner.toSiteDto(),
            externalId = businessPartner.externalId!!,
            legalEntityExternalId = toParentLegalEntityExternalId(businessPartner.relations)!!
        )
    }

    fun toParentLegalEntityExternalId(relations: Collection<RelationCdq>): String? {
        return toParentLegalEntityExternalIds(relations).firstOrNull()
    }

    fun toParentSiteExternalId(relations: Collection<RelationCdq>): String? {
        return toParentSiteExternalIds(relations).firstOrNull()
    }

    fun toParentLegalEntityExternalIds(relations: Collection<RelationCdq>): Collection<String> {
        return relations.filter { it.startNodeDataSource == cdqConfigProperties.datasourceLegalEntity }
            .filter { it.type.technicalKey == "PARENT" }
            .map { it.startNode }
    }

    fun toParentSiteExternalIds(relations: Collection<RelationCdq>): Collection<String> {
        return relations.filter { it.startNodeDataSource == cdqConfigProperties.datasourceSite }
            .filter { it.type.technicalKey == "PARENT" }
            .map { it.startNode }
    }
}

