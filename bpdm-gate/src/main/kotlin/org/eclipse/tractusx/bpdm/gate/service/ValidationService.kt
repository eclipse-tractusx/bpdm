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
import org.eclipse.tractusx.bpdm.common.dto.cdq.ValidationRequestCdq
import org.eclipse.tractusx.bpdm.common.dto.cdq.ValidationResponseCdq
import org.eclipse.tractusx.bpdm.common.dto.cdq.ViolationLevel
import org.eclipse.tractusx.bpdm.common.service.ValidationMapper
import org.eclipse.tractusx.bpdm.gate.dto.AddressGateInput
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityGateInput
import org.eclipse.tractusx.bpdm.gate.dto.SiteGateInput
import org.eclipse.tractusx.bpdm.gate.dto.response.ValidationResponse
import org.eclipse.tractusx.bpdm.gate.dto.response.ValidationStatus
import org.springframework.stereotype.Service

/**
 * Service giving feedback about whether a business partner record is valid for entering the sharing process
 */
@Service
class ValidationService(
    private val cdqClient: CdqClient,
    private val siteService: SiteService,
    private val addressService: AddressService,
    private val validationMapper: ValidationMapper,
    private val cdqRequestMappingService: CdqRequestMappingService
) {

    /**
     * Validates a legal entity, listing all errors which will keep it from entering the sharing process
     */
    fun validate(legalEntityInput: LegalEntityGateInput): ValidationResponse {
        val partnerModel = cdqRequestMappingService.toCdqModel(legalEntityInput)
        return validate(partnerModel)
    }

    /**
     * Validates a lsite, listing all errors which will keep it from entering the sharing process
     */
    fun validate(siteInput: SiteGateInput): ValidationResponse {
        val partnerModel = siteService.toCdqModels(listOf(siteInput)).first()
        return validate(partnerModel)
    }

    /**
     * Validates an address, listing all errors which will keep it from entering the sharing process
     */
    fun validate(addressInput: AddressGateInput): ValidationResponse {
        val partnerModel = addressService.toCdqModels(listOf(addressInput)).first()
        return validate(partnerModel)
    }

    private fun validate(partner: BusinessPartnerCdq): ValidationResponse {
        val validationModel = validationMapper.toValidation(partner)

        val validationRequest = ValidationRequestCdq(validationModel)
        val validationResponse = cdqClient.validateBusinessPartner(validationRequest)

        return toGateResponse(validationResponse)
    }


    private fun toGateResponse(validationResponse: ValidationResponseCdq): ValidationResponse {
        val errors = validationResponse.dataDefects
            .filter { it.violationLevel == ViolationLevel.ERROR }
            .map { it.violationMessage }

        return ValidationResponse(
            status = if (errors.isNotEmpty()) ValidationStatus.ERROR else ValidationStatus.OK,
            errors = errors
        )
    }
}