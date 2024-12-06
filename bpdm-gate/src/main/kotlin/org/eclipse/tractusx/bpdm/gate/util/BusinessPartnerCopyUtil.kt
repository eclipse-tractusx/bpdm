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

package org.eclipse.tractusx.bpdm.gate.util

import org.eclipse.tractusx.bpdm.common.util.copyAndSync
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.gate.entity.generic.*
import org.springframework.stereotype.Component

@Component
class BusinessPartnerCopyUtil {

    fun copyValues(fromPartner: BusinessPartnerDb, toPartner: BusinessPartnerDb): BusinessPartnerDb {
        return toPartner.apply {
            stage = fromPartner.stage
            shortName = fromPartner.shortName
            legalName = fromPartner.legalName
            siteName = fromPartner.siteName
            addressName = fromPartner.addressName
            legalForm = fromPartner.legalForm
            isOwnCompanyData = fromPartner.isOwnCompanyData
            bpnL = fromPartner.bpnL
            bpnS = fromPartner.bpnS
            bpnA = fromPartner.bpnA
            legalEntityConfidence = fromPartner.legalEntityConfidence
            siteConfidence = fromPartner.siteConfidence
            addressConfidence = fromPartner.addressConfidence
            externalSequenceTimestamp = fromPartner.externalSequenceTimestamp

            nameParts.replace(fromPartner.nameParts)
            roles.replace(fromPartner.roles)

            states.copyAndSync(fromPartner.states, ::copyValues)
            classifications.copyAndSync(fromPartner.classifications, ::copyValues)
            identifiers.copyAndSync(fromPartner.identifiers, ::copyValues)

            copyValues(fromPartner.postalAddress, postalAddress)
        }
    }

    private fun copyValues(fromState: StateDb, toState: StateDb) =
        toState.apply {
            validFrom = fromState.validFrom
            validTo = fromState.validTo
            type = fromState.type
            businessPartnerTyp = fromState.businessPartnerTyp
        }

    private fun copyValues(fromClassification: ClassificationDb, toClassification: ClassificationDb) =
        toClassification.apply {
            value = fromClassification.value
            type = fromClassification.type
            code = fromClassification.code
        }

    private fun copyValues(fromIdentifier: IdentifierDb, toIdentifier: IdentifierDb) =
        toIdentifier.apply {
            type = fromIdentifier.type
            value = fromIdentifier.value
            issuingBody = fromIdentifier.issuingBody
            businessPartnerType = fromIdentifier.businessPartnerType
        }

    private fun copyValues(fromPostalAddress: PostalAddressDb, toPostalAddress: PostalAddressDb) =
        toPostalAddress.apply {
            addressType = fromPostalAddress.addressType
            physicalPostalAddress = fromPostalAddress.physicalPostalAddress
            alternativePostalAddress = fromPostalAddress.alternativePostalAddress
        }

}