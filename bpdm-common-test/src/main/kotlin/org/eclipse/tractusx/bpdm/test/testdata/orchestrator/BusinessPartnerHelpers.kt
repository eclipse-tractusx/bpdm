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
 * SPDX-License-IdentifierV7Dto: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.test.testdata.orchestrator
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BpnReferenceV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BpnReferenceTypeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BusinessPartnerV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.ConfidenceCriteriaV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.IdentifierV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressWithScriptVariantsV7Dto


fun BusinessPartnerV7Dto.copyWithLegalEntityIdentifiers(identifiers: List<IdentifierV7Dto>) =
    copy(
        legalEntity = legalEntity.copy(
            identifiers = identifiers
        )
    )

fun BusinessPartnerV7Dto.copyWithBpnReferences(bpnReference: BpnReferenceV7Dto) =
    copy(
        legalEntity = legalEntity.copy(bpnReference = bpnReference),
        site = site?.copy(bpnReference = bpnReference),
        additionalAddress = additionalAddress?.copyAsPostalAddress{ it.copy(bpnReference = bpnReference) }
    )

fun BusinessPartnerV7Dto.copyWithConfidenceCriteria(confidenceCriteria: ConfidenceCriteriaV7Dto) =
    copy(
        legalEntity = legalEntity.copy(
            confidenceCriteria = confidenceCriteria,
            legalAddress = legalEntity.legalAddress.copy(confidenceCriteria = confidenceCriteria)
        ),
        site = site?.let {
            it.copy(
                confidenceCriteria = confidenceCriteria,
                siteMainAddress = it.siteMainAddress?.copy(confidenceCriteria = confidenceCriteria)
            )
        } ,
        additionalAddress = additionalAddress?.copyAsPostalAddress{ it.copy(confidenceCriteria = confidenceCriteria) }
    )

fun BusinessPartnerV7Dto.copyWithLegalAddress(postalAddress: PostalAddressWithScriptVariantsV7Dto) =
    copyWithLegalAddress(postalAddress.postalProperties)

fun BusinessPartnerV7Dto.copyWithLegalAddress(postalAddress: PostalAddressV7Dto) =
    copy(
        legalEntity = legalEntity.copy(
            legalAddress = postalAddress
        )
    )


fun BusinessPartnerV7Dto.copyWithSiteMainAddress(postalAddress: PostalAddressV7Dto?) =
    copy(
        site = site?.copy(
            siteMainAddress = postalAddress
        )
    )

fun BusinessPartnerV7Dto.copyWithHasChanged(legalEntityChanged: Boolean = false, siteChanged: Boolean = false, additionalAddressChanged: Boolean = false) =
    copy(
        legalEntity = legalEntity.copy(hasChanged = legalEntityChanged),
        site = site?.copy(hasChanged = siteChanged),
        additionalAddress = additionalAddress?.copyAsPostalAddress{ it.copy(hasChanged = additionalAddressChanged) }
    )

fun BusinessPartnerV7Dto.copyWithBpnRequests() = copyWithBpnReferenceType(BpnReferenceTypeV7.BpnRequestIdentifier)

fun BusinessPartnerV7Dto.copyWithBpnReferenceType(bpnReferenceType: BpnReferenceTypeV7) =
    copy(
        legalEntity = legalEntity.copy(bpnReference = legalEntity.bpnReference.copy(referenceType = bpnReferenceType), legalAddress = legalEntity.legalAddress.copyWithBpnReferenceType(bpnReferenceType)),
        site = site?.copy(bpnReference = site!!.bpnReference.copy(referenceType = bpnReferenceType), siteMainAddress = site?.siteMainAddress?.copyWithBpnReferenceType(bpnReferenceType)),
        additionalAddress = additionalAddress?.copyAsPostalAddress{ it.copyWithBpnReferenceType(bpnReferenceType) }
    )

fun PostalAddressV7Dto.copyWithBpnReferenceType(bpnReferenceType: BpnReferenceTypeV7) =
    copy(bpnReference = bpnReference.copy(referenceType = bpnReferenceType))

fun BusinessPartnerV7Dto.copyAsCxMemberData() =
    copy(
        legalEntity = legalEntity.copy(isParticipantData = true)
    )

