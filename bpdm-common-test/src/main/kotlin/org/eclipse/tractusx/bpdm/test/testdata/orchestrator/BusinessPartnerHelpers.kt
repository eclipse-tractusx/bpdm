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

package org.eclipse.tractusx.bpdm.test.testdata.orchestrator

import org.eclipse.tractusx.orchestrator.api.model.*

fun BusinessPartner.copyWithLegalEntityIdentifiers(identifiers: List<Identifier>) =
    copy(
        legalEntity = legalEntity.copy(
            identifiers = identifiers
        )
    )

fun BusinessPartner.copyWithBpnReferences(bpnReference: BpnReference) =
    copy(
        legalEntity = legalEntity.copy(bpnReference = bpnReference),
        site = site?.copy(bpnReference = bpnReference),
        additionalAddress = additionalAddress?.copy(bpnReference = bpnReference)
    )

fun BusinessPartner.copyWithConfidenceCriteria(confidenceCriteria: ConfidenceCriteria) =
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
        additionalAddress = additionalAddress?.copy(confidenceCriteria = confidenceCriteria)
    )

fun BusinessPartner.copyWithAddress(overWriteAddress: PostalAddress = PostalAddress.empty, applyToLegalAddress: Boolean, applyToSiteMainAddress: Boolean, applyToAdditionalAddress: Boolean) =
    copy(
        legalEntity = legalEntity.copy(
            legalAddress = if(applyToLegalAddress) overWriteAddress else legalEntity.legalAddress
        ),
        site = site?.copy(
            siteMainAddress = if(applyToSiteMainAddress) overWriteAddress else site?.siteMainAddress
        ),
        additionalAddress = if(applyToAdditionalAddress) overWriteAddress else additionalAddress
    )

fun BusinessPartner.copyWithLegalAddress(postalAddress: PostalAddress) =
    copy(
        legalEntity = legalEntity.copy(
            legalAddress = postalAddress
        )
    )

fun BusinessPartner.copyWithSiteMainAddress(postalAddress: PostalAddress?) =
    copy(
        site = site?.copy(
            siteMainAddress = postalAddress
        )
    )

fun BusinessPartner.copyWithHasChanged(legalEntityChanged: Boolean = false, siteChanged: Boolean = false, additionalAddressChanged: Boolean = false) =
    copy(
        legalEntity = legalEntity.copy(hasChanged = legalEntityChanged),
        site = site?.copy(hasChanged = siteChanged),
        additionalAddress = additionalAddress?.copy(hasChanged = additionalAddressChanged)
    )

fun BusinessPartner.copyWithBpnRequests() =
    copy(
        legalEntity = legalEntity.copy(bpnReference = legalEntity.bpnReference.copy(referenceType = BpnReferenceType.BpnRequestIdentifier)),
        site = site?.copy(bpnReference = site!!.bpnReference.copy(referenceType = BpnReferenceType.BpnRequestIdentifier)),
        additionalAddress = additionalAddress?.copy(bpnReference = additionalAddress!!.bpnReference.copy(referenceType = BpnReferenceType.BpnRequestIdentifier))
    )

fun BusinessPartner.copyAsCxMemberData() =
    copy(
        legalEntity = legalEntity.copy(isParticipantData = true)
    )