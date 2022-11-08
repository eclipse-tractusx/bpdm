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

package org.eclipse.tractusx.bpdm.pool.component.cdq.service

import org.eclipse.tractusx.bpdm.common.dto.cdq.BusinessPartnerCdq
import org.eclipse.tractusx.bpdm.common.dto.cdq.LegalFormCdq
import org.eclipse.tractusx.bpdm.common.dto.cdq.TypeKeyNameCdq
import org.eclipse.tractusx.bpdm.common.dto.cdq.TypeKeyNameUrlCdq
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeNameUrlDto
import org.eclipse.tractusx.bpdm.common.service.CdqMappings
import org.eclipse.tractusx.bpdm.common.service.CdqMappings.toLegalEntityDto
import org.eclipse.tractusx.bpdm.pool.dto.request.LegalEntityPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.LegalEntityPartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.LegalFormRequest
import org.springframework.stereotype.Service

@Service
class CdqToRequestMapper {
    fun toUpdateRequest(partner: BusinessPartnerCdq): LegalEntityPartnerUpdateRequest {
        return LegalEntityPartnerUpdateRequest(
            partner.identifiers.find { it.type?.technicalKey == CdqMappings.BPN_TECHNICAL_KEY }?.value!!,
            partner.toLegalEntityDto()
        )
    }

    fun toCreateRequest(partner: BusinessPartnerCdq, index: String? = null): LegalEntityPartnerCreateRequest {
        return LegalEntityPartnerCreateRequest(
            partner.toLegalEntityDto(),
            index
        )
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