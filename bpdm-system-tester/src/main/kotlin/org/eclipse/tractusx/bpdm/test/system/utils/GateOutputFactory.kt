/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.test.system.utils

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.gate.api.model.ConfidenceCriteriaDto
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressComponentOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityRepresentationOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteRepresentationOutputDto
import org.eclipse.tractusx.bpdm.test.testdata.gate.GateInputFactory
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class GateOutputFactory(
    private val gateInputTestDataFactory: GateInputFactory
) {

    private val dummyConfidence = ConfidenceCriteriaDto(
        sharedByOwner = false,
        numberOfSharingMembers = 1,
        checkedByExternalDataSource = false,
        lastConfidenceCheckAt = LocalDateTime.now(),
        nextConfidenceCheckAt = LocalDateTime.now().plus (5, ChronoUnit.DAYS),
        confidenceLevel = 0
    )

    fun createOutput(fromSeed: String, externalId: String = fromSeed): BusinessPartnerOutputDto{
        return createOutput(gateInputTestDataFactory.createFullValid(fromSeed, externalId))
    }

    fun createOutput(
        fromInput: BusinessPartnerInputRequest
    ): BusinessPartnerOutputDto{
        return BusinessPartnerOutputDto(
            externalId = fromInput.externalId,
            nameParts = fromInput.nameParts,
            identifiers = fromInput.identifiers,
            states = fromInput.states,
            roles = fromInput.roles,
            isOwnCompanyData = fromInput.isOwnCompanyData,
            legalEntity = with(fromInput.legalEntity){
                LegalEntityRepresentationOutputDto(
                    legalEntityBpn = legalEntityBpn ?: "INVALID",
                    legalName = legalName,
                    shortName = shortName,
                    legalForm = legalForm,
                    confidenceCriteria = dummyConfidence,
                    states = states
                )
            },
            site = with(fromInput.site){
                SiteRepresentationOutputDto(
                    siteBpn = siteBpn ?: "INVALID",
                    name = name,
                    confidenceCriteria = dummyConfidence,
                    states = states
                )
            },
            address = with(fromInput.address){
                AddressComponentOutputDto(
                    addressBpn = addressBpn ?: "INVALID",
                    name = name,
                    addressType = addressType,
                    physicalPostalAddress = physicalPostalAddress,
                    alternativePostalAddress = alternativePostalAddress,
                    confidenceCriteria = dummyConfidence,
                    states = states
                )
            },
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}

fun BusinessPartnerOutputDto.withAddressType(addressType: AddressType) = copy(address = address.copy(addressType = addressType))