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

package org.eclipse.tractusx.bpdm.test.testdata.pool.v7

import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressIdentifierV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerUpdateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerUpdateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AlternativeAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressInvariantVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PostalAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.ConfidenceCriteriaV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityHeaderV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityHeaderVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityIdentifierV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerCreateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerUpdateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityStateV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityStateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityWithLegalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.relation.RelationValidityPeriodV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.relation.RelationVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteCreateWithLegalAddressAsMainV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SitePartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SitePartnerUpdateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteV7Dto



fun LegalEntityPartnerCreateV7Request.withLegalIdentifiers(identifiers: List<LegalEntityIdentifierV7Dto>) =
    copy(legalEntity = legalEntity.withLegalIdentifiers(identifiers))

fun LegalEntityPartnerCreateV7Request.withLegalIdentifiers(vararg identifiers: LegalEntityIdentifierV7Dto) =
    withLegalIdentifiers(identifiers.toList())

fun LegalEntityV7Dto.withLegalIdentifiers(identifiers: List<LegalEntityIdentifierV7Dto>) =
    copy(header = header.copy(identifiers = identifiers))

fun LegalEntityV7Dto.withLegalIdentifiers(vararg identifiers: LegalEntityIdentifierV7Dto) =
    withLegalIdentifiers(identifiers.toList())

fun LegalEntityV7Dto.withConfidence(givenConfidence: GivenConfidence) =
    copy(header = header.withConfidence(givenConfidence), legalAddress = legalAddress.withConfidence(givenConfidence))

fun LegalEntityPartnerCreateV7Request.withLegalAddressIdentifiers(identifiers: List<AddressIdentifierV7Dto>) =
    copy(legalEntity = legalEntity.withLegalAddressIdentifiers(identifiers))

fun LegalEntityPartnerCreateV7Request.withLegalAddressIdentifiers(vararg identifiers: AddressIdentifierV7Dto) =
    withLegalAddressIdentifiers(identifiers.toList())

fun LegalEntityV7Dto.withLegalAddressIdentifiers(identifiers: List<AddressIdentifierV7Dto>) =
    copy(legalAddress = legalAddress.copy(identifiers = identifiers))

fun LegalEntityV7Dto.withLegalAddressIdentifiers(vararg identifiers: AddressIdentifierV7Dto) =
    withLegalAddressIdentifiers(identifiers.toList())

fun LegalEntityPartnerCreateV7Request.withLegalForm(legalForm: String) =
    copy(legalEntity = legalEntity.withLegalForm(legalForm))

fun LegalEntityV7Dto.withLegalForm(legalForm: String) =
    copy(header = header.copy(legalForm = legalForm))

fun LegalEntityPartnerCreateV7Request.withPhysicalAdminArea(code: String?) =
    copy(legalEntity = legalEntity.withPhysicalAdminArea(code))

fun LegalEntityV7Dto.withPhysicalAdminArea(code: String?) =
    copy(legalAddress = legalAddress.copy(physicalPostalAddress = legalAddress.physicalPostalAddress.copy(administrativeAreaLevel1 = code)))

fun LegalEntityPartnerCreateV7Request.withAlternativeAdminArea(code: String?) =
    copy(legalEntity = legalEntity.withAlternativeAdminArea(code))

fun LegalEntityV7Dto.withAlternativeAdminArea(code: String?) =
    copy(legalAddress = legalAddress.copy(alternativePostalAddress = legalAddress.alternativePostalAddress?.copy(administrativeAreaLevel1 = code)))

fun LegalEntityPartnerCreateV7Request.withParticipantData(isParticipantData: Boolean) =
    copy(legalEntity = legalEntity.withParticipantData(isParticipantData))

fun LegalEntityPartnerCreateV7Request.withStates(vararg states: LegalEntityStateV7Dto) =
    copy(legalEntity = legalEntity.copy(header = legalEntity.header.copy(states = states.toList())))

fun LegalEntityPartnerUpdateV7Request.withLegalIdentifiers(identifiers: List<LegalEntityIdentifierV7Dto>) =
    copy(legalEntity = legalEntity.withLegalIdentifiers(identifiers))

fun LegalEntityPartnerUpdateV7Request.withLegalIdentifiers(vararg identifiers: LegalEntityIdentifierV7Dto) =
    withLegalIdentifiers(identifiers.toList())

fun LegalEntityPartnerUpdateV7Request.withLegalAddressIdentifiers(identifiers: List<AddressIdentifierV7Dto>) =
    copy(legalEntity = legalEntity.withLegalAddressIdentifiers(identifiers))

fun LegalEntityPartnerUpdateV7Request.withLegalAddressIdentifiers(vararg identifiers: AddressIdentifierV7Dto) =
    withLegalAddressIdentifiers(identifiers.toList())

fun LegalEntityPartnerUpdateV7Request.withLegalForm(legalForm: String) =
    copy(legalEntity = legalEntity.withLegalForm(legalForm))

fun LegalEntityPartnerUpdateV7Request.withPhysicalAdminArea(code: String?) =
    copy(legalEntity = legalEntity.withPhysicalAdminArea(code))

fun LegalEntityPartnerUpdateV7Request.withAlternativeAdminArea(code: String?) =
    copy(legalEntity = legalEntity.withAlternativeAdminArea(code))

fun LegalEntityPartnerUpdateV7Request.withParticipantData(isParticipantData: Boolean) =
    copy(legalEntity = legalEntity.withParticipantData(isParticipantData))

fun LegalEntityPartnerUpdateV7Request.withOwnershipUltimate(ownershipUltimate: Boolean?) =
    copy(legalEntity = legalEntity.withOwnershipUltimate(ownershipUltimate))

fun LegalEntityV7Dto.withOwnershipUltimate(ownershipUltimate: Boolean?) =
    copy(header = header.copy(ownershipUltimate = ownershipUltimate))

fun LegalEntityPartnerCreateVerboseV7Dto.withUltimateOwner(ownershipUltimate: Boolean, ultimateOwnerBpnl: String?) =
    copy(legalEntity = legalEntity.withUltimateOwner(ownershipUltimate, ultimateOwnerBpnl))

fun LegalEntityWithLegalAddressVerboseV7Dto.withUltimateOwner(ownershipUltimate: Boolean, ultimateOwnerBpnl: String?) =
    copy(header = header.copy(ownershipUltimate = ownershipUltimate, ultimateOwnerBpnl = ultimateOwnerBpnl))

fun LegalEntityPartnerCreateVerboseV7Dto.withStates(vararg states: LegalEntityStateVerboseV7Dto) =
    copy(legalEntity = legalEntity.copy(header = legalEntity.header.copy(states = states.toList())))

fun LegalEntityPartnerCreateVerboseV7Dto.withIsOwnedByRelation(ownedBpnL: String, owningBpnL: String) =
    copy(legalEntity = legalEntity.withIsOwnedByRelation(ownedBpnL, owningBpnL))

fun LegalEntityWithLegalAddressVerboseV7Dto.withIsOwnedByRelation(ownedBpnL: String, owningBpnL: String) =
    copy(
        header = header.copy(
            relations = header.relations + RelationVerboseV7Dto(
                type = LegalEntityRelationTypeV7.IsOwnedBy,
                businessPartnerSourceBpnl = ownedBpnL,
                businessPartnerTargetBpnl = owningBpnL,
                validityPeriods = listOf(RelationValidityPeriodV7Dto(validFrom = TestDataV7.currentRelationValidFrom, validTo = null)),
                reasonCode = null
            )
        )
    )

fun LegalEntityV7Dto.withParticipantData(isParticipantData: Boolean) =
    copy(header = header.withParticipantData(isParticipantData), legalAddress = legalAddress.withSharedByOwner(isParticipantData))

fun LegalEntityHeaderV7Dto.withParticipantData(isParticipantData: Boolean) =
    copy(isParticipantData = isParticipantData, confidenceCriteria = confidenceCriteria.copy(sharedByOwner = isParticipantData))

fun LegalEntityHeaderV7Dto.withConfidence(givenConfidence: GivenConfidence) =
    copy(confidenceCriteria = confidenceCriteria.withGivenConfidence(givenConfidence))

fun LogisticAddressV7Dto.withSharedByOwner(isSharedByOwner: Boolean)  =
    copy(confidenceCriteria = confidenceCriteria.copy(sharedByOwner = isSharedByOwner))

fun LogisticAddressV7Dto.withConfidence(confidence: GivenConfidence) =
    copy(confidenceCriteria = confidenceCriteria.withGivenConfidence(confidence))

fun AddressPartnerCreateV7Request.withConfidence(givenConfidence: GivenConfidence) =
    copy(address = address.withConfidence(givenConfidence))

fun AddressPartnerUpdateV7Request.withConfidence(givenConfidence: GivenConfidence) =
    copy(address = address.withConfidence(givenConfidence))

fun LegalEntityPartnerCreateVerboseV7Dto.withConfidence(confidence: CalculatedConfidence) =
    copy(legalEntity = legalEntity.withConfidence(confidence))

fun LegalEntityWithLegalAddressVerboseV7Dto.withConfidence(confidence: CalculatedConfidence) =
    copy(header = header.withConfidence(confidence), legalAddress = legalAddress.withConfidence(confidence))

fun LegalEntityHeaderVerboseV7Dto.withConfidence(confidence: CalculatedConfidence) =
    copy(confidenceCriteria = confidenceCriteria.withCalculatedConfidence(confidence))

fun LogisticAddressInvariantVerboseV7Dto.withConfidence(confidence: CalculatedConfidence) =
    copy(confidenceCriteria = confidenceCriteria.withCalculatedConfidence(confidence))

fun LegalEntityWithLegalAddressVerboseV7Dto.withConfidence(confidence: GivenConfidence) =
    copy(header = header.withConfidence(confidence), legalAddress = legalAddress.withConfidence(confidence))

fun LegalEntityHeaderVerboseV7Dto.withConfidence(confidence: GivenConfidence) =
    copy(confidenceCriteria = confidenceCriteria.withGivenConfidence(confidence))

fun LogisticAddressInvariantVerboseV7Dto.withConfidence(confidence: GivenConfidence) =
    copy(confidenceCriteria = confidenceCriteria.withGivenConfidence(confidence))

fun ConfidenceCriteriaV7Dto.withCalculatedConfidence(confidence: CalculatedConfidence): ConfidenceCriteriaV7Dto {
    return copy(
        numberOfSharingMembers = confidence.numberOfSharingMembers,
        confidenceLevel = confidence.confidenceLevel
    )
}

fun ConfidenceCriteriaV7Dto.withGivenConfidence(confidence: GivenConfidence): ConfidenceCriteriaV7Dto {
    return copy(
        sharedByOwner = confidence.sharedByOwner,
        checkedByExternalDataSource = confidence.checkedByExternalDataSource
    )
}

fun SitePartnerCreateV7Request.withMainAddressIdentifiers(identifiers: List<AddressIdentifierV7Dto>) =
    copy(site = site.copy(mainAddress = site.mainAddress.copy(identifiers = identifiers)))

fun SitePartnerCreateV7Request.withMainAddressIdentifiers(vararg identifiers: AddressIdentifierV7Dto) =
    withMainAddressIdentifiers(identifiers.toList())

fun SitePartnerCreateV7Request.withPhysicalAdminArea(code: String?) =
    copy(site = site.copy(mainAddress = site.mainAddress.copy(physicalPostalAddress = site.mainAddress.physicalPostalAddress.copy(administrativeAreaLevel1 = code))))

fun SitePartnerCreateV7Request.withAlternativeAdminArea(code: String?) =
    copy(site = site.copy(mainAddress = site.mainAddress.copy(alternativePostalAddress = site.mainAddress.alternativePostalAddress?.copy(administrativeAreaLevel1 = code))))

fun SitePartnerUpdateV7Request.withMainAddressIdentifiers(identifiers: List<AddressIdentifierV7Dto>) =
    copy(site = site.copy(mainAddress = site.mainAddress.copy(identifiers = identifiers)))

fun SitePartnerUpdateV7Request.withMainAddressIdentifiers(vararg identifiers: AddressIdentifierV7Dto) =
    withMainAddressIdentifiers(identifiers.toList())

fun SitePartnerUpdateV7Request.withPhysicalAdminArea(code: String?) =
    copy(site = site.copy(mainAddress = site.mainAddress.copy(physicalPostalAddress = site.mainAddress.physicalPostalAddress.copy(administrativeAreaLevel1 = code))))

fun SitePartnerUpdateV7Request.withAlternativeAdminArea(code: String?) =
    copy(site = site.copy(mainAddress = site.mainAddress.copy(alternativePostalAddress = site.mainAddress.alternativePostalAddress?.copy(administrativeAreaLevel1 = code))))

fun AddressPartnerCreateV7Request.withIdentifiers(identifiers: List<AddressIdentifierV7Dto>) =
    copy(address = address.copy(identifiers = identifiers))

fun AddressPartnerCreateV7Request.withIdentifiers(vararg identifiers: AddressIdentifierV7Dto) =
    withIdentifiers(identifiers.toList())

fun AddressPartnerCreateV7Request.withPhysicalAdminArea(code: String?) =
    copy(address = address.copy(physicalPostalAddress = address.physicalPostalAddress.copy(administrativeAreaLevel1 = code)))

fun AddressPartnerCreateV7Request.withAlternativeAdminArea(code: String?) =
    copy(address = address.copy(alternativePostalAddress = address.alternativePostalAddress?.copy(administrativeAreaLevel1 = code)))

fun AddressPartnerCreateV7Request.withPhysicalCountry(country: String) =
    copy(address = address.copy(physicalPostalAddress = address.physicalPostalAddress.copy(country = country)))

fun AddressPartnerCreateV7Request.withAlternativeCountry(country: String) =
    copy(address = address.copy(alternativePostalAddress = address.alternativePostalAddress?.copy(country = country)))

fun AddressPartnerUpdateV7Request.withIdentifiers(identifiers: List<AddressIdentifierV7Dto>) =
    copy(address = address.copy(identifiers = identifiers))

fun AddressPartnerUpdateV7Request.withIdentifiers(vararg identifiers: AddressIdentifierV7Dto) =
    withIdentifiers(identifiers.toList())

fun AddressPartnerUpdateV7Request.withPhysicalAdminArea(code: String?) =
    copy(address = address.copy(physicalPostalAddress = address.physicalPostalAddress.copy(administrativeAreaLevel1 = code)))

fun AddressPartnerUpdateV7Request.withAlternativeAdminArea(code: String?) =
    copy(address = address.copy(alternativePostalAddress = address.alternativePostalAddress?.copy(administrativeAreaLevel1 = code)))

fun AddressPartnerCreateVerboseV7Dto.withConfidence(confidence: CalculatedConfidence) =
    copy(address = address.withConfidence(confidence))

fun AddressPartnerUpdateVerboseV7Dto.withConfidence(confidence: CalculatedConfidence) =
    copy(address = address.withConfidence(confidence))

fun PostalAddressScriptVariantV7Dto.withPhysicalCity(city: String) =
    copy(physicalAddress = physicalAddress.copy(city = city))

fun PostalAddressScriptVariantV7Dto.withAlternativeCity(city: String) =
    copy(alternativeAddress = AlternativeAddressScriptVariantV7Dto(city))

fun LegalEntityPartnerCreateV7Request.withScriptVariantLegalName(legalName: String) =
    copy(legalEntity = legalEntity.withScriptVariantLegalName(legalName))

fun LegalEntityPartnerUpdateV7Request.withScriptVariantLegalName(legalName: String) =
    copy(legalEntity = legalEntity.withScriptVariantLegalName(legalName))

fun LegalEntityV7Dto.withScriptVariantLegalName(legalName: String) =
    copy(scriptVariants = scriptVariants.map { it.copy(legalName = legalName) })

fun LegalEntityPartnerCreateV7Request.withScriptVariantPhysicalCity(city: String) =
    copy(legalEntity = legalEntity.withScriptVariantPhysicalCity(city))

fun LegalEntityPartnerUpdateV7Request.withScriptVariantPhysicalCity(city: String) =
    copy(legalEntity = legalEntity.withScriptVariantPhysicalCity(city))

fun LegalEntityV7Dto.withScriptVariantPhysicalCity(city: String) =
    copy(scriptVariants = scriptVariants.map { it.copy(legalAddress = it.legalAddress.withPhysicalCity(city)) })

fun LegalEntityPartnerCreateV7Request.withDuplicateScriptVariants() =
    copy(legalEntity = legalEntity.withDuplicateScriptVariants())

fun LegalEntityPartnerUpdateV7Request.withDuplicateScriptVariants() =
    copy(legalEntity = legalEntity.withDuplicateScriptVariants())

fun LegalEntityV7Dto.withDuplicateScriptVariants() =
    copy(scriptVariants = scriptVariants + scriptVariants)

fun SitePartnerCreateV7Request.withScriptVariantName(name: String) =
    copy(site = site.withScriptVariantName(name))

fun SitePartnerUpdateV7Request.withScriptVariantName(name: String) =
    copy(site = site.withScriptVariantName(name))

fun SiteV7Dto.withScriptVariantName(name: String) =
    copy(scriptVariants = scriptVariants.map { it.copy(name = name) })

fun SitePartnerUpdateV7Request.withScriptVariantScriptCode(scriptCode: String) =
    copy(site = site.withScriptVariantScriptCode(scriptCode))

fun SiteV7Dto.withScriptVariantScriptCode(scriptCode: String) =
    copy(scriptVariants = scriptVariants.map { it.copy(scriptCode = scriptCode) })

fun LegalEntityPartnerUpdateV7Request.withScriptVariantScriptCode(scriptCode: String) =
    copy(legalEntity = legalEntity.withScriptVariantScriptCode(scriptCode))

fun LegalEntityV7Dto.withScriptVariantScriptCode(scriptCode: String) =
    copy(scriptVariants = scriptVariants.map { it.copy(scriptCode = scriptCode) })

fun SitePartnerCreateV7Request.withScriptVariantPhysicalCity(city: String) =
    copy(site = site.withScriptVariantPhysicalCity(city))

fun SitePartnerUpdateV7Request.withScriptVariantPhysicalCity(city: String) =
    copy(site = site.withScriptVariantPhysicalCity(city))

fun SiteV7Dto.withScriptVariantPhysicalCity(city: String) =
    copy(scriptVariants = scriptVariants.map { it.copy(mainAddress = it.mainAddress.withPhysicalCity(city)) })

fun SitePartnerCreateV7Request.withDuplicateScriptVariants() =
    copy(site = site.withDuplicateScriptVariants())

fun SitePartnerUpdateV7Request.withDuplicateScriptVariants() =
    copy(site = site.withDuplicateScriptVariants())

fun SiteV7Dto.withDuplicateScriptVariants() =
    copy(scriptVariants = scriptVariants + scriptVariants)

fun SiteCreateWithLegalAddressAsMainV7Request.withScriptVariantName(name: String) =
    copy(scriptVariants = scriptVariants.map { it.copy(name = name) })

fun SiteCreateWithLegalAddressAsMainV7Request.withDuplicateScriptVariants() =
    copy(scriptVariants = scriptVariants + scriptVariants)

fun AddressPartnerCreateV7Request.withScriptVariantPhysicalCity(city: String) =
    copy(scriptVariants = scriptVariants.map { it.copy(address = it.address.withPhysicalCity(city)) })

fun AddressPartnerUpdateV7Request.withScriptVariantPhysicalCity(city: String) =
    copy(scriptVariants = scriptVariants.map { it.copy(address = it.address.withPhysicalCity(city)) })

fun AddressPartnerCreateV7Request.withScriptVariantAlternativeCity(city: String) =
    copy(scriptVariants = scriptVariants.map { it.copy(address = it.address.withAlternativeCity(city)) })

fun AddressPartnerUpdateV7Request.withScriptVariantAlternativeCity(city: String) =
    copy(scriptVariants = scriptVariants.map { it.copy(address = it.address.withAlternativeCity(city)) })

fun AddressPartnerCreateV7Request.withDuplicateScriptVariants() =
    copy(scriptVariants = scriptVariants + scriptVariants)

fun AddressPartnerUpdateV7Request.withDuplicateScriptVariants() =
    copy(scriptVariants = scriptVariants + scriptVariants)

