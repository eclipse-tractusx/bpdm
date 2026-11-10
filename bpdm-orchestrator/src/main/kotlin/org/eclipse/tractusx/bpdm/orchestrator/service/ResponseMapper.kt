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

package org.eclipse.tractusx.bpdm.orchestrator.service

import org.eclipse.tractusx.bpdm.orchestrator.entity.*
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.AdditionalSiteV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.AddressGoldenRecordRelationV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.AlternativeAddressV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.AlternativeAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BpnReferenceV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BusinessPartnerV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BusinessStateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.ConfidenceCriteriaV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.GeoCoordinateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.IdentifierV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.LegalEntityV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.LegalEntityGoldenRecordRelationV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.LegalEntityScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.NamePartV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PhysicalAddressV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PhysicalAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressScriptVariantWithScriptCodeV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressWithScriptVariantsV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.ResultStateV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.SiteV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.SiteGoldenRecordRelationV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.SiteScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.StepStateV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.StreetV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.StreetScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskClientStateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskErrorV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskProcessingStateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.UncategorizedPropertiesV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.UncategorizedScriptVariantV7Dto
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ResponseMapper {


    fun toClientState(task: GoldenRecordTaskDb, timeout: Instant) =
        with(task) {
            TaskClientStateV7Dto(
                taskId = task.uuid.toString(),
                recordId = task.gateRecord.privateId.toString(),
                businessPartnerResult = toBusinessPartnerResult(businessPartner),
                processingState = toProcessingState(task, timeout)
            )
        }

    fun toProcessingState(task: GoldenRecordTaskDb, timeout: Instant) =
        with(task.processingState) {
            TaskProcessingStateV7Dto(
                resultState = toResultState(resultState),
                step = step,
                stepState = toStepState(stepState),
                errors = errors.map { toTaskError(it) },
                createdAt = task.createdAt.instant,
                modifiedAt = task.updatedAt.instant,
                timeout = timeout
            )
        }

    fun toTaskError(taskError: TaskErrorDb) =
        with(taskError) {
            TaskErrorV7Dto(type = type, description = description)
        }


    fun toBusinessPartnerResult(businessPartner: GoldenRecordTaskDb.BusinessPartner) =
        with(businessPartner) {
            BusinessPartnerV7Dto(
                nameParts = toCategorizedNameParts(nameParts),
                owningCompany = owningCompany,
                uncategorized = toUncategorizedProperties(businessPartner),
                legalEntity = toLegalEntity(businessPartner),
                site = toSite(businessPartner),
                additionalAddress = toAdditionalAddress(businessPartner),
                additionalSites = toAdditionalSites(businessPartner)
            )
        }

    fun toAdditionalSites(businessPartner: GoldenRecordTaskDb.BusinessPartner) =
        businessPartner.additionalSites.map { AdditionalSiteV7Dto(bpnReference = toBpnReference(it.bpnReference), siteName = it.siteName) }


    fun toUncategorizedProperties(businessPartner: GoldenRecordTaskDb.BusinessPartner) =
        UncategorizedPropertiesV7Dto(
            nameParts = toUncategorizedNameParts(businessPartner.nameParts),
            identifiers = toIdentifiers(businessPartner, IdentifierDb.Scope.Uncategorized),
            states = toStates(businessPartner, BusinessStateDb.Scope.Uncategorized),
            address = toUncategorizedAddress(businessPartner),
            scriptVariants = toUncategorizedScriptVariants(businessPartner)
        )

    fun toUncategorizedScriptVariants(businessPartner: GoldenRecordTaskDb.BusinessPartner) =
        businessPartner.uncategorizedScriptVariants
            .groupBy({ it.scriptCode }, { it.name })
            .map { (scriptCode, nameParts) -> UncategorizedScriptVariantV7Dto(scriptCode, nameParts) }

    fun toLegalEntity(businessPartner: GoldenRecordTaskDb.BusinessPartner) =
        with(businessPartner) {
            LegalEntityV7Dto(
                bpnReference = toBpnReference(businessPartner, BpnReferenceDb.Scope.LegalEntity),
                legalName = legalName,
                legalShortName = legalShortName,
                legalForm = legalForm,
                identifiers = toIdentifiers(businessPartner, IdentifierDb.Scope.LegalEntity),
                states = toStates(businessPartner, BusinessStateDb.Scope.LegalEntity),
                confidenceCriteria = toConfidence(businessPartner, ConfidenceCriteriaDb.Scope.LegalEntity),
                isParticipantData = isCatenaXMemberData,
                hasChanged = legalEntityHasChanged,
                ownershipUltimate = ownershipUltimate,
                ultimateOwnerBpnl = ultimateOwnerBpnl,
                legalAddress = toPostalAddressOrEmpty(businessPartner, PostalAddressDb.Scope.LegalAddress)!!,
                scriptVariants = toLegalEntityScriptVariants(businessPartner),
                goldenRecordRelations = toLegalEntityGoldenRecordRelations(businessPartner),
                updatedAt = legalEntityUpdatedAt?.instant
            )
        }

    fun toSite(businessPartner: GoldenRecordTaskDb.BusinessPartner) =
        businessPartner.takeIf { it.siteExists }?.let {
            with(businessPartner) {
                SiteV7Dto(
                    bpnReference = toBpnReference(businessPartner, BpnReferenceDb.Scope.Site),
                    siteName = siteName,
                    states = toStates(businessPartner, BusinessStateDb.Scope.Site),
                    confidenceCriteria = toConfidence(businessPartner, ConfidenceCriteriaDb.Scope.Site),
                    hasChanged = siteHasChanged,
                    siteMainAddress = toPostalAddress(businessPartner, PostalAddressDb.Scope.SiteMainAddress),
                    scriptVariants = toSiteScriptVariants(businessPartner),
                    goldenRecordRelations = toSiteGoldenRecordRelations(businessPartner),
                    updatedAt = siteUpdatedAt?.instant
                )
            }
        }


    fun toCategorizedNameParts(nameParts: List<NamePartDb>) =
        nameParts.filter { it.type != null }.map { NamePartV7Dto(it.name, it.type!!) }

    fun toUncategorizedNameParts(nameParts: List<NamePartDb>) =
        nameParts.filter { it.type == null }.map { it.name }

    fun toIdentifiers(businessPartner: GoldenRecordTaskDb.BusinessPartner, scope: IdentifierDb.Scope) =
        businessPartner.identifiers.filter { it.scope == scope }.map { IdentifierV7Dto(it.value, it.type, it.issuingBody) }

    fun toStates(businessPartner: GoldenRecordTaskDb.BusinessPartner, scope: BusinessStateDb.Scope) =
        businessPartner.businessStates.filter { it.scope == scope }.map { BusinessStateV7Dto(it.validFrom?.instant, it.validTo?.instant, it.type) }

    fun toConfidence(businessPartner: GoldenRecordTaskDb.BusinessPartner, scope: ConfidenceCriteriaDb.Scope) =
        businessPartner.confidences[scope]?.let { toConfidence(it) } ?: ConfidenceCriteriaV7Dto.empty

    fun toConfidence(confidenceCriteria: ConfidenceCriteriaDb) =
        with(confidenceCriteria) {
            ConfidenceCriteriaV7Dto(
                sharedByOwner = sharedByOwner,
                checkedByExternalDataSource = checkedByExternalDataSource,
                numberOfSharingMembers = numberOfSharingMembers,
                lastConfidenceCheckAt = lastConfidenceCheckAt?.instant,
                nextConfidenceCheckAt = nextConfidenceCheckAt?.instant,
                confidenceLevel = confidenceLevel
            )
        }

    fun toBpnReference(bpnReference: BpnReferenceDb) =
        with(bpnReference) {
            BpnReferenceV7Dto(referenceValue = referenceValue, desiredBpn = desiredBpn, referenceType = referenceType)
        }

    fun toBpnReference(businessPartner: GoldenRecordTaskDb.BusinessPartner, scope: BpnReferenceDb.Scope) =
        businessPartner.bpnReferences[scope]?.let { toBpnReference(it) } ?: BpnReferenceV7Dto.empty

    fun toPostalAddressOrEmpty(businessPartner: GoldenRecordTaskDb.BusinessPartner, scope: PostalAddressDb.Scope) =
        toPostalAddress(businessPartner, scope)

    fun toAdditionalAddress(businessPartner: GoldenRecordTaskDb.BusinessPartner) =
        toPostalAddress(businessPartner, PostalAddressDb.Scope.AdditionalAddress)?.let {
            PostalAddressWithScriptVariantsV7Dto(it, toAdditionalAddressScriptVariant(businessPartner))
        }

    fun toUncategorizedAddress(businessPartner: GoldenRecordTaskDb.BusinessPartner) =
        toPostalAddress(businessPartner, PostalAddressDb.Scope.UncategorizedAddress)?.let {
            PostalAddressWithScriptVariantsV7Dto(it, toUncategorizedAddressScriptVariant(businessPartner))
        }

    fun toPostalAddress(businessPartner: GoldenRecordTaskDb.BusinessPartner, scope: PostalAddressDb.Scope) =
        businessPartner.addresses[scope]?.let { postalAddress ->
            with(postalAddress) {
                PostalAddressV7Dto(
                    bpnReference = toBpnReference(businessPartner, scope.bpnReference),
                    addressName = addressName,
                    identifiers = toIdentifiers(businessPartner, scope.identifier),
                    states = toStates(businessPartner, scope.state),
                    confidenceCriteria = toConfidence(businessPartner, scope.confidence),
                    physicalAddress = toPhysicalAddress(physicalAddress),
                    alternativeAddress = toAlternativeAddress(alternativeAddress),
                    hasChanged = hasChanged,
                    goldenRecordRelations = scope.addressGoldenRecordRelation?.let { toAddressGoldenRecordRelations(businessPartner, it) } ?: emptyList(),
                    updatedAt = updatedAt?.instant
                )
            }
        }

    fun toPhysicalAddress(physicalAddress: PostalAddressDb.PhysicalAddressDb) =
        with(physicalAddress) {
            PhysicalAddressV7Dto(
                geographicCoordinates = toGeoCoordinate(geographicCoordinates),
                country = country,
                administrativeAreaLevel1 = administrativeAreaLevel1,
                administrativeAreaLevel2 = administrativeAreaLevel2,
                administrativeAreaLevel3 = administrativeAreaLevel3,
                postalCode = postalCode,
                city = city,
                district = district,
                street = toStreet(street),
                companyPostalCode = companyPostalCode,
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door,
                taxJurisdictionCode = taxJurisdictionCode
            )
        }

    fun toAlternativeAddress(alternativeAddress: PostalAddressDb.AlternativeAddress) =
        alternativeAddress.takeIf { it.exists }?.let {
            with(alternativeAddress) {
                AlternativeAddressV7Dto(
                    geographicCoordinates = toGeoCoordinate(geographicCoordinates),
                    country = country,
                    administrativeAreaLevel1 = administrativeAreaLevel1,
                    postalCode = postalCode,
                    city = city,
                    deliveryServiceType = deliveryServiceType,
                    deliveryServiceQualifier = deliveryServiceQualifier,
                    deliveryServiceNumber = deliveryServiceNumber
                )
            }
        }


    fun toGeoCoordinate(geoCoordinate: PostalAddressDb.GeoCoordinate) =
        with(geoCoordinate) {
            GeoCoordinateV7Dto(longitude = longitude, latitude = latitude, altitude = altitude)
        }

    fun toStreet(street: PostalAddressDb.Street) =
        with(street) {
            StreetV7Dto(
                name, houseNumber,
                houseNumberSupplement = houseNumberSupplement,
                milestone = milestone,
                direction = direction,
                namePrefix = namePrefix,
                additionalNamePrefix = additionalNamePrefix,
                nameSuffix = nameSuffix,
                additionalNameSuffix = additionalNameSuffix
            )
        }

    fun toResultState(resultState: GoldenRecordTaskDb.ResultState) =
        when(resultState){
            GoldenRecordTaskDb.ResultState.Pending -> ResultStateV7.Pending
            GoldenRecordTaskDb.ResultState.Success ->  ResultStateV7.Success
            GoldenRecordTaskDb.ResultState.Error ->  ResultStateV7.Error
            GoldenRecordTaskDb.ResultState.Aborted ->  ResultStateV7.Error
        }

    fun toStepState(stepState: GoldenRecordTaskDb.StepState) =
        when(stepState){
            GoldenRecordTaskDb.StepState.Queued -> StepStateV7.Queued
            GoldenRecordTaskDb.StepState.Reserved -> StepStateV7.Reserved
            GoldenRecordTaskDb.StepState.Success -> StepStateV7.Success
            GoldenRecordTaskDb.StepState.Error -> StepStateV7.Error
            GoldenRecordTaskDb.StepState.Aborted -> StepStateV7.Error
        }


    fun toAdditionalAddressScriptVariant(businessPartner: GoldenRecordTaskDb.BusinessPartner) =
        businessPartner.addressScriptVariants.filter { it.scope == PostalAddressDb.Scope.AdditionalAddress }.map{
            PostalAddressScriptVariantWithScriptCodeV7Dto(it.scriptCode, toPostalAddressScriptVariant(it))
        }

    fun toUncategorizedAddressScriptVariant(businessPartner: GoldenRecordTaskDb.BusinessPartner) =
        businessPartner.addressScriptVariants.filter { it.scope == PostalAddressDb.Scope.UncategorizedAddress }.map{
            PostalAddressScriptVariantWithScriptCodeV7Dto(it.scriptCode, toPostalAddressScriptVariant(it))
        }

    fun toPostalAddressScriptVariant(addressScriptVariant: PostalAddressScriptVariantDb) =
        PostalAddressScriptVariantV7Dto(
            addressName = addressScriptVariant.addressName,
            physicalAddress = toPhysicalAddressScriptVariant(addressScriptVariant.physicalAddress),
            alternativeAddress = addressScriptVariant.alternativeAddress?.let { toAlternativeAddressScriptVariant(it) }
        )

    fun toPhysicalAddressScriptVariant(physicalAddressScriptVariant: PhysicalAddressScriptVariantDb) =
        with(physicalAddressScriptVariant){
            PhysicalAddressScriptVariantV7Dto(
                city = city,
                district = district,
                street = toScriptVariantStreet(street),
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door
            )
        }

    fun toAlternativeAddressScriptVariant(alternativeAddressScriptVariant: AlternativeAddressScriptVariantDb) =
        with(alternativeAddressScriptVariant){
            AlternativeAddressScriptVariantV7Dto(
                city = city
            )
        }

    fun toScriptVariantStreet(street: StreetScriptVariantDb) =
        with(street){
            StreetScriptVariantV7Dto(
                name = name,
                direction = direction,
                namePrefix = namePrefix,
                additionalNamePrefix = additionalNamePrefix,
                nameSuffix = nameSuffix,
                additionalNameSuffix = additionalNameSuffix
            )
        }

    fun toLegalEntityScriptVariants(businessPartner: GoldenRecordTaskDb.BusinessPartner): List<LegalEntityScriptVariantV7Dto>{
        val legalAddressVariantsByScriptCode = businessPartner.addressScriptVariants.filter { it.scope == PostalAddressDb.Scope.LegalAddress }.associateBy { it.scriptCode }

        return businessPartner.legalEntityHeaderScriptVariants.map { headerVariant ->
            val legalAddressVariant = legalAddressVariantsByScriptCode[headerVariant.scriptCode]

            LegalEntityScriptVariantV7Dto(headerVariant.scriptCode, headerVariant.legalName, headerVariant.legalShortName, legalAddressVariant?.let { toPostalAddressScriptVariant(it) } ?: PostalAddressScriptVariantV7Dto.empty )
        }
    }

    fun toLegalEntityGoldenRecordRelations(businessPartner: GoldenRecordTaskDb.BusinessPartner) =
        businessPartner.legalEntityGoldenRecordRelations
            .map { LegalEntityGoldenRecordRelationV7Dto(it.relationType, it.sourceBpn, it.targetBpn) }

    fun toSiteGoldenRecordRelations(businessPartner: GoldenRecordTaskDb.BusinessPartner) =
        businessPartner.siteGoldenRecordRelations
            .map { SiteGoldenRecordRelationV7Dto(it.relationType, it.sourceBpn, it.targetBpn) }

    fun toAddressGoldenRecordRelations(businessPartner: GoldenRecordTaskDb.BusinessPartner, scope: AddressGoldenRecordRelationDb.Scope) =
        businessPartner.addressGoldenRecordRelations
            .filter { it.scope == scope }
            .map { AddressGoldenRecordRelationV7Dto(it.relationType, it.sourceBpn, it.targetBpn) }

    fun toSiteScriptVariants(businessPartner: GoldenRecordTaskDb.BusinessPartner): List<SiteScriptVariantV7Dto>{
        val mainAddressVariantsByScriptCode = businessPartner.addressScriptVariants.filter { it.scope == PostalAddressDb.Scope.SiteMainAddress }.associateBy { it.scriptCode }

        return businessPartner.siteHeaderScriptVariants.map { headerVariant ->
            val mainAddressVariant = mainAddressVariantsByScriptCode[headerVariant.scriptCode]

            SiteScriptVariantV7Dto(headerVariant.scriptCode, headerVariant.siteName ?: "", mainAddressVariant?.let { toPostalAddressScriptVariant(it) } ?: PostalAddressScriptVariantV7Dto.empty )
        }
    }

}
