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

package org.eclipse.tractusx.bpdm.pool.service

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.ILegalEntityState
import org.eclipse.tractusx.bpdm.common.dto.ISiteState
import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseResponse
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteWithMainAddressVerboseResponse
import org.eclipse.tractusx.orchestrator.api.model.AddressIdentifier
import org.eclipse.tractusx.orchestrator.api.model.AddressState
import org.eclipse.tractusx.orchestrator.api.model.LegalEntity
import org.eclipse.tractusx.orchestrator.api.model.LegalEntityClassification
import org.eclipse.tractusx.orchestrator.api.model.LegalEntityIdentifier
import org.eclipse.tractusx.orchestrator.api.model.LogisticAddress
import org.eclipse.tractusx.orchestrator.api.model.Site
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.function.BiPredicate

fun compareLegalEntity(verboseRequest: LegalEntityWithLegalAddressVerboseResponse, legalEntity: LegalEntity?) {

    val verboseLegalEntity = verboseRequest.legalEntity

    Assertions.assertThat(verboseLegalEntity.legalShortName).isEqualTo(legalEntity?.legalShortName)
    Assertions.assertThat(verboseLegalEntity.legalFormVerbose?.technicalKey).isEqualTo(legalEntity?.legalForm)
    compareStates(verboseLegalEntity.states, legalEntity?.states)
    compareClassifications(verboseLegalEntity.classifications, legalEntity?.classifications)
    compareIdentifiers(verboseLegalEntity.identifiers, legalEntity?.identifiers)

    val verboseLegalAddress = verboseRequest.legalAddress
    Assertions.assertThat(verboseLegalAddress.bpnLegalEntity).isEqualTo(legalEntity?.bpnLReference?.referenceValue)
    Assertions.assertThat(verboseLegalAddress.isLegalAddress).isTrue()
    compareLogisticAddress(verboseLegalAddress, legalEntity?.legalAddress)
}

fun compareSite(verboseRequest: SiteWithMainAddressVerboseResponse, site: Site?) {

    val verboseSite = verboseRequest.site

    Assertions.assertThat(verboseSite.name).isEqualTo(site?.name)
    Assertions.assertThat(verboseSite.bpns).isEqualTo(site?.bpnSReference?.referenceValue)
    compareSiteStates(verboseSite.states, site?.states)

    val verboseMainAddress = verboseRequest.mainAddress
    Assertions.assertThat(verboseMainAddress.bpnSite).isEqualTo(site?.bpnSReference?.referenceValue)
    val mainAddress = site?.mainAddress
    Assertions.assertThat(verboseMainAddress.isMainAddress).isTrue()
    compareLogisticAddress(verboseMainAddress, mainAddress)
}

fun compareLogisticAddress(verboseAddress: LogisticAddressVerbose, address: LogisticAddress?) {

    Assertions.assertThat(verboseAddress.name).isEqualTo(address?.name)
    compareAddressStates(verboseAddress.states, address?.states)
    compareAddressIdentifiers(verboseAddress.identifiers, address?.identifiers)


    val verbosePhysicalAddress = verboseAddress.physicalPostalAddress
    val physicalAddress = address?.physicalPostalAddress
    Assertions.assertThat(verbosePhysicalAddress).usingRecursiveComparison()
        .ignoringFields(PhysicalPostalAddressVerbose::countryVerbose.name, PhysicalPostalAddressVerbose::administrativeAreaLevel1Verbose.name)
        .isEqualTo(physicalAddress)
    Assertions.assertThat(verbosePhysicalAddress.country.name).isEqualTo(physicalAddress?.country?.name)
    Assertions.assertThat(verbosePhysicalAddress.administrativeAreaLevel1).isEqualTo(physicalAddress?.administrativeAreaLevel1)
    val verboseAlternAddress = verboseAddress.alternativePostalAddress
    val alternAddress = address?.alternativePostalAddress
    Assertions.assertThat(verboseAlternAddress).usingRecursiveComparison()
        .ignoringFields(AlternativePostalAddressVerboseDto::countryVerbose.name, AlternativePostalAddressVerboseDto::administrativeAreaLevel1Verbose.name)
        .isEqualTo(alternAddress)
    Assertions.assertThat(verboseAlternAddress?.country?.name).isEqualTo(alternAddress?.country?.name)
    Assertions.assertThat(verboseAlternAddress?.administrativeAreaLevel1).isEqualTo(alternAddress?.administrativeAreaLevel1)
}

fun compareAddressStates(statesVerbose: Collection<AddressStateVerbose>, states: Collection<AddressState>?) {

    Assertions.assertThat(statesVerbose.size).isEqualTo(states?.size ?: 0)
    val sortedVerboseStates = statesVerbose.sortedBy { it.validFrom }
    val sortedStates = states?.sortedBy { it.validFrom }
    sortedVerboseStates.indices.forEach {
        Assertions.assertThat(sortedVerboseStates[it].typeVerbose.technicalKey.name).isEqualTo(sortedStates!![it].type.name)
        Assertions.assertThat(sortedVerboseStates[it]).usingRecursiveComparison()
            .withEqualsForFields(isEqualToIgnoringMilliseconds(), AddressStateVerbose::validTo.name)
            .withEqualsForFields(isEqualToIgnoringMilliseconds(), AddressStateVerbose::validFrom.name)
            .ignoringFields(AddressStateVerbose::typeVerbose.name).isEqualTo(sortedStates[it])
    }
}

fun compareAddressIdentifiers(identifiersVerbose: Collection<AddressIdentifierVerbose>, identifiers: Collection<AddressIdentifier>?) {

    Assertions.assertThat(identifiersVerbose.size).isEqualTo(identifiers?.size ?: 0)
    val sortedVerboseIdentifiers = identifiersVerbose.sortedBy { it.typeVerbose.name }
    val sortedIdentifiers = identifiers!!.sortedBy { it.type }
    sortedVerboseIdentifiers.indices.forEach {
        Assertions.assertThat(sortedVerboseIdentifiers[it].typeVerbose.technicalKey).isEqualTo(sortedIdentifiers[it].type)
        Assertions.assertThat(sortedVerboseIdentifiers[it]).usingRecursiveComparison()
            .ignoringFields(AddressIdentifierVerbose::typeVerbose.name)
            .isEqualTo(sortedIdentifiers[it])
    }
}

fun compareStates(statesVerbose: Collection<LegalEntityStateVerbose>, states: Collection<ILegalEntityState>?) {

    Assertions.assertThat(statesVerbose.size).isEqualTo(states?.size ?: 0)
    val sortedVerboseStates = statesVerbose.sortedBy { it.validFrom }
    val sortedStates = states!!.sortedBy { it.validFrom }
    sortedVerboseStates.indices.forEach {
        Assertions.assertThat(sortedVerboseStates[it].typeVerbose.technicalKey.name).isEqualTo(sortedStates[it].type.name)
        Assertions.assertThat(sortedVerboseStates[it]).usingRecursiveComparison()
            .withEqualsForFields(isEqualToIgnoringMilliseconds(), LegalEntityStateVerbose::validTo.name)
            .withEqualsForFields(isEqualToIgnoringMilliseconds(), LegalEntityStateVerbose::validFrom.name)
            .ignoringFields(LegalEntityStateVerbose::typeVerbose.name)
            .isEqualTo(sortedStates[it])
    }
}

fun compareSiteStates(statesVerbose: Collection<SiteStateVerbose>, states: Collection<ISiteState>?) {

    Assertions.assertThat(statesVerbose.size).isEqualTo(states?.size ?: 0)
    val sortedVerboseStates = statesVerbose.sortedBy { it.validFrom }
    val sortedStates = states!!.sortedBy { it.validFrom }
    sortedVerboseStates.indices.forEach {
        Assertions.assertThat(sortedVerboseStates[it].typeVerbose.technicalKey.name).isEqualTo(sortedStates[it].type.name)
        Assertions.assertThat(sortedVerboseStates[it]).usingRecursiveComparison()
            .withEqualsForFields(isEqualToIgnoringMilliseconds(), SiteStateVerbose::validTo.name)
            .withEqualsForFields(isEqualToIgnoringMilliseconds(), SiteStateVerbose::validFrom.name)
            .ignoringFields(SiteStateVerbose::typeVerbose.name)
            .isEqualTo(sortedStates[it])
    }
}

fun isEqualToIgnoringMilliseconds(): BiPredicate<LocalDateTime?, LocalDateTime?> {
    return BiPredicate<LocalDateTime?, LocalDateTime?> { d1, d2 ->
        (d1 == null && d2 == null)
                || d1.truncatedTo(ChronoUnit.SECONDS).equals(d2.truncatedTo(ChronoUnit.SECONDS))
    }
}

fun compareClassifications(
    classificationsVerbose: Collection<LegalEntityClassificationVerbose>,
    classifications: Collection<LegalEntityClassification>?
) {

    Assertions.assertThat(classificationsVerbose.size).isEqualTo(classifications?.size ?: 0)
    val sortedVerboseClassifications = classificationsVerbose.sortedBy { it.typeVerbose.name }
    val sortedClassifications = classifications!!.sortedBy { it.type.name }
    sortedVerboseClassifications.indices.forEach {
        Assertions.assertThat(sortedVerboseClassifications[it].typeVerbose.technicalKey.name).isEqualTo(sortedClassifications[it].type.name)
        Assertions.assertThat(sortedVerboseClassifications[it]).usingRecursiveComparison()
            .ignoringFields(LegalEntityClassificationVerbose::typeVerbose.name)
            .isEqualTo(sortedClassifications[it])
    }
}

fun compareIdentifiers(identifiersVerbose: Collection<LegalEntityIdentifierVerbose>, identifiers: Collection<LegalEntityIdentifier>?) {

    Assertions.assertThat(identifiersVerbose.size).isEqualTo(identifiers?.size ?: 0)
    val sortedVerboseIdentifiers = identifiersVerbose.sortedBy { it.typeVerbose.name }
    val sortedIdentifiers = identifiers!!.sortedBy { it.type }
    sortedVerboseIdentifiers.indices.forEach {
        Assertions.assertThat(sortedVerboseIdentifiers[it].typeVerbose.technicalKey).isEqualTo(sortedIdentifiers[it].type)
        Assertions.assertThat(sortedVerboseIdentifiers[it]).usingRecursiveComparison()
            .ignoringFields(LegalEntityIdentifierVerbose::typeVerbose.name).isEqualTo(sortedIdentifiers[it])
    }
}
