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

package org.eclipse.tractusx.bpdm.gate.util

import org.eclipse.tractusx.bpdm.common.dto.AddressIdentifierDto
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGateOutputChildRequest
import org.eclipse.tractusx.bpdm.gate.api.model.BusinessPartnerPostalAddressDto
import org.eclipse.tractusx.bpdm.gate.api.model.request.*

object BusinessPartnerNonVerboseValues {

    val physicalAddressMinimal = BusinessPartnerVerboseValues.physicalAddressMinimal

    val physicalAddressChina = BusinessPartnerVerboseValues.physicalAddressChina

    val bpPostalAddressInputDtoMinimal = BusinessPartnerPostalAddressDto(
        addressType = null,
        physicalPostalAddress = physicalAddressMinimal
    )

    val bpInputRequestMinimal = BusinessPartnerInputRequest(
        externalId = BusinessPartnerVerboseValues.externalId2, postalAddress = bpPostalAddressInputDtoMinimal
    )

    val bpInputRequestFull = BusinessPartnerVerboseValues.bpInputRequestFull

    val bpInputRequestChina = BusinessPartnerVerboseValues.bpInputRequestChina

    val legalEntityGateInputRequest1 = LegalEntityGateInputRequest(
        legalEntity = BusinessPartnerVerboseValues.legalEntity1,
        legalAddress = BusinessPartnerVerboseValues.physicalAddress1,
        legalNameParts = BusinessPartnerVerboseValues.legalEntityGateInputResponse1.legalNameParts,
        externalId = BusinessPartnerVerboseValues.externalId1,
    )

    val legalEntityGateInputRequest2 = LegalEntityGateInputRequest(
        legalEntity = BusinessPartnerVerboseValues.legalEntity2,
        legalAddress = BusinessPartnerVerboseValues.physicalAddress2,
        legalNameParts = BusinessPartnerVerboseValues.legalEntityGateInputResponse2.legalNameParts,
        externalId = BusinessPartnerVerboseValues.externalId2,
    )

    val legalEntityGateInputRequest3 = LegalEntityGateInputRequest(
        legalEntity = BusinessPartnerVerboseValues.legalEntity3,
        legalAddress = BusinessPartnerVerboseValues.physicalAddress3,
        legalNameParts = BusinessPartnerVerboseValues.logisticAddressGateInputResponse1.address.nameParts,
        externalId = BusinessPartnerVerboseValues.externalId3,
    )


    val siteGateInputRequest1 = SiteGateInputRequest(
        site = BusinessPartnerVerboseValues.site1,
        externalId = BusinessPartnerVerboseValues.externalIdSite1,
        legalEntityExternalId = BusinessPartnerVerboseValues.externalId1,
        mainAddress = BusinessPartnerVerboseValues.physicalAddress1
    )

    val siteGateInputRequest2 = SiteGateInputRequest(
        site = BusinessPartnerVerboseValues.site2,
        externalId = BusinessPartnerVerboseValues.externalIdSite2,
        legalEntityExternalId = BusinessPartnerVerboseValues.externalId2,
        mainAddress = BusinessPartnerVerboseValues.physicalAddress2
    )

    //Output values for sites
    val siteGateOutputRequest1 = SiteGateOutputRequest(
        site = BusinessPartnerVerboseValues.site1,
        externalId = BusinessPartnerVerboseValues.externalIdSite1,
        legalEntityExternalId = BusinessPartnerVerboseValues.externalId1,
        mainAddress = AddressGateOutputChildRequest(BusinessPartnerVerboseValues.physicalAddress1, BusinessPartnerVerboseValues.persistencesiteGateOutputResponse1.mainAddress.bpna),
        bpn = BusinessPartnerVerboseValues.persistencesiteGateOutputResponse1.bpns
    )

    val siteGateOutputRequest2 = SiteGateOutputRequest(
        site = BusinessPartnerVerboseValues.site2,
        externalId = BusinessPartnerVerboseValues.externalIdSite2,
        legalEntityExternalId = BusinessPartnerVerboseValues.externalId2,
        mainAddress = AddressGateOutputChildRequest(BusinessPartnerVerboseValues.physicalAddress2, BusinessPartnerVerboseValues.persistencesiteGateOutputResponse2.mainAddress.bpna),
        bpn = BusinessPartnerVerboseValues.persistencesiteGateOutputResponse2.bpns
    )

    val addressGateInputRequest1 = AddressGateInputRequest(
        address = BusinessPartnerVerboseValues.physicalAddress1.copy(
            nameParts = BusinessPartnerVerboseValues.logisticAddressGateInputResponse1.address.nameParts,
            identifiers = listOf(
                AddressIdentifierDto(BusinessPartnerVerboseValues.identifierValue1!!, BusinessPartnerVerboseValues.identifierTypeTechnicalKey1!!)
            )
        ),
        externalId = BusinessPartnerVerboseValues.externalIdAddress1,
        legalEntityExternalId = BusinessPartnerVerboseValues.externalId1,
    )

    val addressGateInputRequest2 = AddressGateInputRequest(
        address = BusinessPartnerVerboseValues.physicalAddress2.copy(
            nameParts = BusinessPartnerVerboseValues.logisticAddressGateInputResponse2.address.nameParts,
            identifiers = listOf(
                AddressIdentifierDto(BusinessPartnerVerboseValues.identifierValue1!!, BusinessPartnerVerboseValues.identifierTypeTechnicalKey1!!)
            )
        ),
        externalId = BusinessPartnerVerboseValues.externalIdAddress2,
        siteExternalId = BusinessPartnerVerboseValues.externalIdSite1,
    )

    //Output Endpoint Values
    val addressGateOutputRequest1 = AddressGateOutputRequest(
        address = BusinessPartnerVerboseValues.physicalAddress1.copy(
            nameParts = BusinessPartnerVerboseValues.logisticAddressGateOutputResponse1.address.nameParts,
            identifiers = listOf(
                AddressIdentifierDto(BusinessPartnerVerboseValues.identifierValue1!!, BusinessPartnerVerboseValues.identifierTypeTechnicalKey1!!)
            )
        ),
        externalId = BusinessPartnerVerboseValues.externalIdAddress1,
        legalEntityExternalId = BusinessPartnerVerboseValues.externalId1,
        bpn = BusinessPartnerVerboseValues.logisticAddressGateOutputResponse1.bpna
    )

    val addressGateOutputRequest2 = AddressGateOutputRequest(
        address = BusinessPartnerVerboseValues.physicalAddress2.copy(
            nameParts = BusinessPartnerVerboseValues.logisticAddressGateOutputResponse2.address.nameParts,
            identifiers = listOf(
                AddressIdentifierDto(BusinessPartnerVerboseValues.identifierValue1!!, BusinessPartnerVerboseValues.identifierTypeTechnicalKey1!!)
            )
        ),
        externalId = BusinessPartnerVerboseValues.externalIdAddress2,
        siteExternalId = BusinessPartnerVerboseValues.externalIdSite1,
        bpn = BusinessPartnerVerboseValues.logisticAddressGateOutputResponse2.bpna
    )

    //Output Values
    val legalEntityGateOutputRequest1 = LegalEntityGateOutputRequest(
        legalEntity = BusinessPartnerVerboseValues.legalEntity1,
        legalAddress = AddressGateOutputChildRequest(BusinessPartnerVerboseValues.physicalAddress1, BusinessPartnerVerboseValues.legalEntityGateOutputResponse1.legalAddress.bpna),
        legalNameParts = BusinessPartnerVerboseValues.legalEntityGateOutputResponse1.legalNameParts,
        externalId = BusinessPartnerVerboseValues.externalId1,
        bpn = BusinessPartnerVerboseValues.legalEntityGateOutputResponse1.bpnl
    )

    val legalEntityGateOutputRequest2 = LegalEntityGateOutputRequest(
        legalEntity = BusinessPartnerVerboseValues.legalEntity2,
        legalAddress = AddressGateOutputChildRequest(BusinessPartnerVerboseValues.physicalAddress2, BusinessPartnerVerboseValues.legalEntityGateOutputResponse2.legalAddress.bpna),
        legalNameParts = BusinessPartnerVerboseValues.legalEntityGateOutputResponse2.legalNameParts,
        externalId = BusinessPartnerVerboseValues.externalId2,
        bpn = BusinessPartnerVerboseValues.legalEntityGateOutputResponse2.bpnl
    )
}