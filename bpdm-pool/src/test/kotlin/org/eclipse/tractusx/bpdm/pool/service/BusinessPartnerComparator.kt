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
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.pool.service

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressIdentifierVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressStateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AlternativePostalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressInvariantVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PhysicalPostalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityIdentifierVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityStateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityWithLegalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteStateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteWithMainAddressVerboseV7Dto
import org.eclipse.tractusx.orchestrator.api.model.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.function.BiPredicate

fun compareLegalEntity(verboseRequest: LegalEntityWithLegalAddressVerboseV7Dto, legalEntity: LegalEntity?) {

    val verboseLegalEntity = verboseRequest.header

    assertThat(verboseLegalEntity.legalShortName).isEqualTo(legalEntity?.legalShortName)
    assertThat(verboseLegalEntity.legalFormVerbose?.technicalKey).isEqualTo(legalEntity?.legalForm)
    compareStates(verboseLegalEntity.states, legalEntity?.states)
    compareIdentifiers(verboseLegalEntity.identifiers, legalEntity?.identifiers)

    val verboseLegalAddress = verboseRequest.legalAddress
    assertThat(verboseLegalAddress.bpnLegalEntity).isEqualTo(legalEntity?.bpnReference?.referenceValue)
    assertThat(verboseLegalAddress.addressType == AddressType.LegalAddress).isTrue()

    compareLogisticAddress(verboseLegalAddress, legalEntity?.legalAddress)
}

fun compareSite(verboseRequest: SiteWithMainAddressVerboseV7Dto, site: Site?) {

    val verboseSite = verboseRequest.site

    assertThat(verboseSite.name).isEqualTo(site?.siteName)
    assertThat(verboseSite.bpns).isEqualTo(site?.bpnReference?.referenceValue)
    compareSiteStates(verboseSite.states, site?.states)

    val verboseMainAddress = verboseRequest.mainAddress
    assertThat(verboseMainAddress.bpnSite).isEqualTo(site?.bpnReference?.referenceValue)
    val mainAddress = site?.siteMainAddress
    assertThat(verboseMainAddress.addressType == AddressType.SiteMainAddress).isTrue()
    compareLogisticAddress(verboseMainAddress, mainAddress)
}

fun compareLogisticAddress(verboseAddress: LogisticAddressInvariantVerboseV7Dto, address: PostalAddress?) {

    assertThat(verboseAddress.name).isEqualTo(address?.addressName)
    compareAddressStates(verboseAddress.states, address?.states)
    compareAddressIdentifiers(verboseAddress.identifiers, address?.identifiers)


    val verbosePhysicalAddress = verboseAddress.physicalPostalAddress
    val physicalAddress = address?.physicalAddress
    assertThat(verbosePhysicalAddress).usingRecursiveComparison()
        .ignoringFields(
            PhysicalPostalAddressVerboseV7Dto::countryVerbose.name,
            PhysicalPostalAddressVerboseV7Dto::administrativeAreaLevel1Verbose.name,
            PhysicalPostalAddressVerboseV7Dto::geographicCoordinates.name,
            PhysicalPostalAddressVerboseV7Dto::street.name
        )
        .isEqualTo(physicalAddress)


    if(verbosePhysicalAddress.geographicCoordinates == null) {
        assertThat(address?.physicalAddress?.geographicCoordinates).isEqualTo(GeoCoordinate.empty)
    }else{
        assertThat(verbosePhysicalAddress.geographicCoordinates).usingRecursiveComparison().isEqualTo(address?.physicalAddress?.geographicCoordinates)
    }

    if(verbosePhysicalAddress.street == null) {
        assertThat(address?.physicalAddress?.street).isEqualTo(Street.empty)
    }else{
        assertThat(verbosePhysicalAddress.street).usingRecursiveComparison().isEqualTo(address?.physicalAddress?.street)
    }

    assertThat(verbosePhysicalAddress.country).isEqualTo(physicalAddress?.country)
    assertThat(verbosePhysicalAddress.administrativeAreaLevel1).isEqualTo(physicalAddress?.administrativeAreaLevel1)
    val verboseAlternAddress = verboseAddress.alternativePostalAddress
    val alternAddress = address?.alternativeAddress
    assertThat(verboseAlternAddress).usingRecursiveComparison()
        .ignoringFields(AlternativePostalAddressVerboseV7Dto::countryVerbose.name, AlternativePostalAddressVerboseV7Dto::administrativeAreaLevel1Verbose.name)
        .isEqualTo(alternAddress)
    assertThat(verboseAlternAddress?.country).isEqualTo(alternAddress?.country)
    assertThat(verboseAlternAddress?.administrativeAreaLevel1).isEqualTo(alternAddress?.administrativeAreaLevel1)
}

fun compareAddressStates(statesVerbose: Collection<AddressStateVerboseV7Dto>, states: Collection<BusinessState>?) {

    assertThat(statesVerbose.size).isEqualTo(states?.size ?: 0)
    val sortedVerboseStates = statesVerbose.sortedBy { it.validFrom }
    val sortedStates = states?.sortedBy { it.validFrom }
    sortedVerboseStates.indices.forEach {
        assertThat(sortedVerboseStates[it].typeVerbose.technicalKey.name).isEqualTo(sortedStates!![it].type?.name)
        assertThat(sortedVerboseStates[it]).usingRecursiveComparison()
            .withEqualsForFields(isEqualToIgnoringMilliseconds(), AddressStateVerboseV7Dto::validTo.name)
            .withEqualsForFields(isEqualToIgnoringMilliseconds(), AddressStateVerboseV7Dto::validFrom.name)
            .ignoringFields(AddressStateVerboseV7Dto::typeVerbose.name).isEqualTo(sortedStates[it])
    }
}

fun compareAddressIdentifiers(identifiersVerbose: Collection<AddressIdentifierVerboseV7Dto>, identifiers: Collection<Identifier>?) {

    assertThat(identifiersVerbose.size).isEqualTo(identifiers?.size ?: 0)
    val sortedVerboseIdentifiers = identifiersVerbose.sortedWith( compareBy( {it.value}, {it.typeVerbose.name }) )
    val sortedIdentifiers = identifiers!!.sortedWith(compareBy({it.value}, { it.type }))
    sortedVerboseIdentifiers.indices.forEach {
        assertThat(sortedVerboseIdentifiers[it].typeVerbose.technicalKey).isEqualTo(sortedIdentifiers[it].type)
        assertThat(sortedVerboseIdentifiers[it]).usingRecursiveComparison()
            .ignoringFields(AddressIdentifierVerboseV7Dto::typeVerbose.name)
            .isEqualTo(sortedIdentifiers[it])
    }
}

fun compareStates(statesVerbose: Collection<LegalEntityStateVerboseV7Dto>,  states: Collection<BusinessState>?) {

    assertThat(statesVerbose.size).isEqualTo(states?.size ?: 0)
    val sortedVerboseStates = statesVerbose.sortedBy { it.validFrom }
    val sortedStates = states!!.sortedBy { it.validFrom }
    sortedVerboseStates.indices.forEach {
        assertThat(sortedVerboseStates[it].typeVerbose.technicalKey.name).isEqualTo(sortedStates[it].type?.name)
        assertThat(sortedVerboseStates[it]).usingRecursiveComparison()
            .withEqualsForFields(isEqualToIgnoringMilliseconds(), LegalEntityStateVerboseV7Dto::validTo.name )
            .withEqualsForFields(isEqualToIgnoringMilliseconds(), LegalEntityStateVerboseV7Dto::validFrom.name)
            .ignoringFields(LegalEntityStateVerboseV7Dto::typeVerbose.name)
            .isEqualTo(sortedStates[it])
    }
}

fun isEqualToIgnoringMilliseconds(): BiPredicate<LocalDateTime?, Instant?> {
    return BiPredicate<LocalDateTime?, Instant?> { d1, d2 ->
         (d1 == null && d2 == null)
                 || d1?.let { notNullD1 ->
                     d2?.let { notNullD2 ->
                         notNullD1.toInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).equals(notNullD2.truncatedTo(ChronoUnit.SECONDS))
                     }
                 } ?: false
    }
}

fun compareIdentifiers(identifiersVerbose: Collection<LegalEntityIdentifierVerboseV7Dto>, identifiers: Collection<Identifier>?) {

    assertThat(identifiersVerbose.size).isEqualTo(identifiers?.size ?: 0)

    val sortedVerboseIdentifiers = identifiersVerbose.sortedWith( compareBy( {it.value}, {it.typeVerbose.name }) )
    val sortedIdentifiers = identifiers!!.sortedWith(compareBy({it.value}, { it.type }))
    sortedVerboseIdentifiers.indices.forEach {
        assertThat(sortedVerboseIdentifiers[it].typeVerbose.technicalKey).isEqualTo(sortedIdentifiers[it].type)
        assertThat(sortedVerboseIdentifiers[it]).usingRecursiveComparison()
            .ignoringFields(LegalEntityIdentifierVerboseV7Dto::typeVerbose.name).isEqualTo(sortedIdentifiers[it])
    }
}

fun compareSiteStates(statesVerbose: Collection<SiteStateVerboseV7Dto>, states: Collection<BusinessState>?) {

    Assertions.assertThat(statesVerbose.size).isEqualTo(states?.size ?: 0)
    val sortedVerboseStates = statesVerbose.sortedBy { it.validFrom }
    val sortedStates = states!!.sortedBy { it.validFrom }
    sortedVerboseStates.indices.forEach {
        Assertions.assertThat(sortedVerboseStates[it].typeVerbose.technicalKey.name).isEqualTo(sortedStates[it].type?.name)
        assertThat(sortedVerboseStates[it]).usingRecursiveComparison()
            .withEqualsForFields(isEqualToIgnoringMilliseconds(), SiteStateVerboseV7Dto::validTo.name)
            .withEqualsForFields(isEqualToIgnoringMilliseconds(), SiteStateVerboseV7Dto::validFrom.name)
            .ignoringFields(SiteStateVerboseV7Dto::typeVerbose.name)
            .isEqualTo(sortedStates[it])
    }
}

