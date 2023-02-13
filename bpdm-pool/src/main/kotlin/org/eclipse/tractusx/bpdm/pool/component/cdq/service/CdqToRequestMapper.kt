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

package org.eclipse.tractusx.bpdm.pool.component.cdq.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.cdq.BusinessPartnerCdq
import org.eclipse.tractusx.bpdm.common.dto.cdq.LegalFormCdq
import org.eclipse.tractusx.bpdm.common.dto.cdq.TypeKeyNameCdq
import org.eclipse.tractusx.bpdm.common.dto.cdq.TypeKeyNameUrlCdq
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeNameUrlDto
import org.eclipse.tractusx.bpdm.common.service.CdqMappings
import org.eclipse.tractusx.bpdm.common.service.CdqMappings.toDto
import org.eclipse.tractusx.bpdm.common.service.CdqMappings.toLegalEntityDto
import org.eclipse.tractusx.bpdm.common.service.CdqMappings.toSiteDto
import org.eclipse.tractusx.bpdm.pool.component.cdq.dto.BusinessPartnerWithBpn
import org.eclipse.tractusx.bpdm.pool.component.cdq.dto.BusinessPartnerWithParentBpn
import org.eclipse.tractusx.bpdm.pool.dto.request.*
import org.springframework.stereotype.Service

@Service
class CdqToRequestMapper {
    private val logger = KotlinLogging.logger { }

    fun toLegalEntityCreateRequest(partnerWithImportId: BusinessPartnerCdq): LegalEntityPartnerCreateRequest {
        return LegalEntityPartnerCreateRequest(
            partnerWithImportId.toLegalEntityDto(),
            partnerWithImportId.externalId
        )
    }

    fun toLegalEntityCreateRequestOrNull(partnerWithImportId: BusinessPartnerCdq): LegalEntityPartnerCreateRequest? {
        return try {
            toLegalEntityCreateRequest(partnerWithImportId)
        } catch (_: Throwable) {
            logger.warn { "Business Partner with ID ${partnerWithImportId.externalId} could not be mapped to ${LegalEntityPartnerCreateRequest::class.simpleName}" }
            null
        }
    }

    fun toLegalEntityUpdateRequest(partnerWithBpn: BusinessPartnerWithBpn): LegalEntityPartnerUpdateRequest {
        return LegalEntityPartnerUpdateRequest(
            partnerWithBpn.bpn,
            partnerWithBpn.partner.toLegalEntityDto()
        )
    }

    fun toLegalEntityUpdateRequestOrNull(partnerWithBpn: BusinessPartnerWithBpn): LegalEntityPartnerUpdateRequest? {
        return try {
            toLegalEntityUpdateRequest(partnerWithBpn)
        } catch (_: Throwable) {
            logger.warn { "Business Partner with ID ${partnerWithBpn.partner.externalId} could not be mapped to ${LegalEntityPartnerUpdateRequest::class.simpleName}" }
            null
        }
    }

    fun toSiteCreateRequest(partnerWithParent: BusinessPartnerWithParentBpn): SitePartnerCreateRequest {
        return SitePartnerCreateRequest(
            site = partnerWithParent.partner.toSiteDto(),
            legalEntity = partnerWithParent.parentBpn,
            index = partnerWithParent.partner.externalId
        )
    }


    fun toSiteCreateRequestOrNull(partnerWithImportId: BusinessPartnerWithParentBpn): SitePartnerCreateRequest? {
        return try {
            toSiteCreateRequest(partnerWithImportId)
        } catch (_: Throwable) {
            logger.warn { "Business Partner with ID ${partnerWithImportId.partner.externalId} could not be mapped to ${SitePartnerCreateRequest::class.simpleName}" }
            null
        }
    }

    fun toSiteUpdateRequest(partnerWithBpn: BusinessPartnerWithBpn): SitePartnerUpdateRequest {
        return SitePartnerUpdateRequest(
            partnerWithBpn.bpn,
            partnerWithBpn.partner.toSiteDto()
        )
    }

    fun toSiteUpdateRequestOrNull(partnerWithBpn: BusinessPartnerWithBpn): SitePartnerUpdateRequest? {
        return try {
            toSiteUpdateRequest(partnerWithBpn)
        } catch (_: Throwable) {
            logger.warn { "Business Partner with ID ${partnerWithBpn.partner.externalId} could not be mapped to ${SitePartnerUpdateRequest::class.simpleName}" }
            null
        }
    }

    fun toAddressCreateRequest(partnerWithParent: BusinessPartnerWithParentBpn): AddressPartnerCreateRequest {
        return AddressPartnerCreateRequest(
            properties = toDto(partnerWithParent.partner.addresses.first()),
            parent = partnerWithParent.parentBpn,
            index = partnerWithParent.partner.externalId
        )
    }

    fun toAddressCreateRequestOrNull(partnerWithParent: BusinessPartnerWithParentBpn): AddressPartnerCreateRequest? {
        return try {
            toAddressCreateRequest(partnerWithParent)
        } catch (_: Throwable) {
            logger.warn { "Business Partner with ID ${partnerWithParent.partner.externalId} could not be mapped to ${AddressPartnerCreateRequest::class.simpleName}" }
            null
        }
    }

    fun toAddressUpdateRequest(partnerWithBpn: BusinessPartnerWithBpn): AddressPartnerUpdateRequest {
        return AddressPartnerUpdateRequest(
            partnerWithBpn.bpn,
            properties = toDto(partnerWithBpn.partner.addresses.first())
        )
    }

    fun toAddressUpdateRequestOrNull(partnerWithBpn: BusinessPartnerWithBpn): AddressPartnerUpdateRequest? {
        return try {
            toAddressUpdateRequest(partnerWithBpn)
        } catch (_: Throwable) {
            logger.warn { "Business Partner with ID ${partnerWithBpn.partner.externalId} could not be mapped to ${AddressPartnerUpdateRequest::class.simpleName}" }
            null
        }
    }


    fun toRequest(idType: TypeKeyNameUrlCdq): TypeKeyNameUrlDto<String> {
        return TypeKeyNameUrlDto(idType.technicalKey!!, idType.name ?: "", idType.url)
    }

    fun toRequest(idStatus: TypeKeyNameCdq): TypeKeyNameDto<String> {
        return TypeKeyNameDto(idStatus.technicalKey!!, idStatus.name ?: "")
    }

    fun toRequest(legalForm: LegalFormCdq, partner: BusinessPartnerCdq): LegalFormRequest {
        return LegalFormRequest(
            legalForm.technicalKey!!,
            legalForm.name,
            legalForm.url,
            legalForm.mainAbbreviation,
            CdqMappings.toLanguageCode(legalForm.language),
            partner.categories.map { toCategoryRequest(it) }
        )
    }

    fun toCategoryRequest(category: TypeKeyNameUrlCdq): TypeNameUrlDto {
        return TypeNameUrlDto(category.name!!, category.url)
    }

}