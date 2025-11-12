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

package org.eclipse.tractusx.bpdm.gate.controller.v6

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.gate.service.BusinessPartnerService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class BusinessPartnerLegacyMapper(
    private val businessPartnerService: BusinessPartnerService
) {

    fun getBusinessPartnersOutput(pageRequest: PageRequest, externalIds: Collection<String>?, tenantBpnl: String?): PageDto<BusinessPartnerOutputDto> {
        return businessPartnerService
            .getBusinessPartnersOutput(pageRequest, externalIds, tenantBpnl)
            .toV6PageDto()
    }

    fun PageDto<org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto>.toV6PageDto():
            PageDto<BusinessPartnerOutputDto> {

        return PageDto(
            totalElements = this.totalElements,
            totalPages = this.totalPages,
            page = this.page,
            contentSize = this.contentSize,
            content = this.content.map { it.toV6() }
        )
    }

    fun org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto.toV6(): BusinessPartnerOutputDto {

        return BusinessPartnerOutputDto(
            externalId = this.externalId,
            nameParts = this.nameParts,
            identifiers = this.identifiers,
            states = this.states,
            roles = this.roles,
            isOwnCompanyData = this.isOwnCompanyData,
            legalEntity = org.eclipse.tractusx.bpdm.gate.api.v6.model.response.LegalEntityRepresentationOutputDto(
                legalEntityBpn = this.legalEntity.legalEntityBpn,
                legalName = this.legalEntity.legalName,
                shortName = this.legalEntity.shortName,
                legalForm = this.legalEntity.legalForm,
                confidenceCriteria = this.legalEntity.confidenceCriteria,
                states = this.legalEntity.states
            ),
            site = this.site?.let {
                org.eclipse.tractusx.bpdm.gate.api.v6.model.response.SiteRepresentationOutputDto(
                    siteBpn = it.siteBpn,
                    name = it.name,
                    confidenceCriteria = it.confidenceCriteria,
                    states = it.states
                )
            },
            address = org.eclipse.tractusx.bpdm.gate.api.v6.model.response.AddressComponentOutputDto(
                addressBpn = this.address.addressBpn,
                name = this.address.name,
                addressType = this.address.addressType,
                physicalPostalAddress = this.address.physicalPostalAddress,
                alternativePostalAddress = this.address.alternativePostalAddress,
                confidenceCriteria = this.address.confidenceCriteria,
                states = this.address.states
            ),
            externalSequenceTimestamp = this.externalSequenceTimestamp,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}
