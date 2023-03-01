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
import org.eclipse.tractusx.bpdm.common.dto.saas.ValidationRequestSaas
import org.eclipse.tractusx.bpdm.common.dto.saas.ValidationResponseSaas
import org.eclipse.tractusx.bpdm.common.dto.saas.ViolationLevel
import org.eclipse.tractusx.bpdm.common.service.ValidationMapper
import org.eclipse.tractusx.bpdm.gate.dto.AddressGateInputRequest
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityGateInputRequest
import org.eclipse.tractusx.bpdm.gate.dto.SiteGateInputRequest
import org.eclipse.tractusx.bpdm.gate.dto.response.ValidationResponse
import org.eclipse.tractusx.bpdm.gate.dto.response.ValidationStatus
import org.springframework.stereotype.Service

/**
 * Service giving feedback about whether a business partner record is valid for entering the sharing process
 */
@Service
class ValidationService(
    private val saasClient: SaasClient,
    private val siteService: SiteService,
    private val addressService: AddressService,
    private val validationMapper: ValidationMapper,
    private val saasRequestMappingService: SaasRequestMappingService
) {

    /**
     * Validates a legal entity, listing all errors which will keep it from entering the sharing process
     */
    fun validate(legalEntityInput: LegalEntityGateInputRequest): ValidationResponse {
        val partnerModel = saasRequestMappingService.toSaasModel(legalEntityInput)
        return validate(partnerModel)
    }

    /**
     * Validates a lsite, listing all errors which will keep it from entering the sharing process
     */
    fun validate(siteInput: SiteGateInputRequest): ValidationResponse {
        val partnerModel = siteService.toSaasModels(listOf(siteInput)).first()
        return validate(partnerModel)
    }

    /**
     * Validates an address, listing all errors which will keep it from entering the sharing process
     */
    fun validate(addressInput: AddressGateInputRequest): ValidationResponse {
        val partnerModel = addressService.toSaasModels(listOf(addressInput)).first()
        return validate(partnerModel)
    }

    private fun validate(partner: BusinessPartnerSaas): ValidationResponse {
        val validationModel = validationMapper.toValidation(partner)

        val validationRequest = ValidationRequestSaas(validationModel)
        val validationResponse = saasClient.validateBusinessPartner(validationRequest)

        return toGateResponse(validationResponse)
    }


    private fun toGateResponse(validationResponse: ValidationResponseSaas): ValidationResponse {
        val errors = validationResponse.dataDefects
            .filter { it.violationLevel == ViolationLevel.ERROR }
            .map { it.violationMessage }

        return ValidationResponse(
            status = if (errors.isNotEmpty()) ValidationStatus.ERROR else ValidationStatus.OK,
            errors = errors
        )
    }
}