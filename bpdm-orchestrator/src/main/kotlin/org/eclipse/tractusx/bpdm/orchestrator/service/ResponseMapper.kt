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

package org.eclipse.tractusx.bpdm.orchestrator.service

import org.eclipse.tractusx.bpdm.orchestrator.entity.*
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ResponseMapper {


    fun toClientState(task: GoldenRecordTaskDb, timeout: Instant) =
        with(task) {
            TaskClientStateDto(
                taskId = task.uuid.toString(),
                recordId = task.gateRecord.privateId.toString(),
                businessPartnerResult = toBusinessPartnerResult(businessPartner),
                processingState = toProcessingState(task, timeout)
            )
        }

    fun toProcessingState(task: GoldenRecordTaskDb, timeout: Instant) =
        with(task.processingState) {
            TaskProcessingStateDto(
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
            TaskErrorDto(type = type, description = description)
        }


    fun toBusinessPartnerResult(businessPartner: GoldenRecordTaskDb.BusinessPartner) =
        with(businessPartner) {
            BusinessPartner(
                nameParts = toCategorizedNameParts(nameParts),
                owningCompany = owningCompany,
                uncategorized = toUncategorizedProperties(businessPartner),
                legalEntity = toLegalEntity(businessPartner),
                site = toSite(businessPartner),
                additionalAddress = toAdditionalAddress(businessPartner)
            )
        }


    fun toUncategorizedProperties(businessPartner: GoldenRecordTaskDb.BusinessPartner) =
        UncategorizedProperties(
            nameParts = toUncategorizedNameParts(businessPartner.nameParts),
            identifiers = toIdentifiers(businessPartner, IdentifierDb.Scope.Uncategorized),
            states = toStates(businessPartner, BusinessStateDb.Scope.Uncategorized),
            address = toUncategorizedAddress(businessPartner)
        )

    fun toLegalEntity(businessPartner: GoldenRecordTaskDb.BusinessPartner) =
        with(businessPartner) {
            LegalEntity(
                bpnReference = toBpnReference(businessPartner, BpnReferenceDb.Scope.LegalEntity),
                legalName = legalName,
                legalShortName = legalShortName,
                legalForm = legalForm,
                identifiers = toIdentifiers(businessPartner, IdentifierDb.Scope.LegalEntity),
                states = toStates(businessPartner, BusinessStateDb.Scope.LegalEntity),
                confidenceCriteria = toConfidence(businessPartner, ConfidenceCriteriaDb.Scope.LegalEntity),
                isParticipantData = isCatenaXMemberData,
                hasChanged = legalEntityHasChanged,
                legalAddress = toPostalAddressOrEmpty(businessPartner, PostalAddressDb.Scope.LegalAddress)!!,
                scriptVariants = toLegalEntityScriptVariants(businessPartner),
                goldenRecordRelations = toLegalEntityGoldenRecordRelations(businessPartner)
            )
        }

    fun toSite(businessPartner: GoldenRecordTaskDb.BusinessPartner) =
        businessPartner.takeIf { it.siteExists }?.let {
            with(businessPartner) {
                Site(
                    bpnReference = toBpnReference(businessPartner, BpnReferenceDb.Scope.Site),
                    siteName = siteName,
                    states = toStates(businessPartner, BusinessStateDb.Scope.Site),
                    confidenceCriteria = toConfidence(businessPartner, ConfidenceCriteriaDb.Scope.Site),
                    hasChanged = siteHasChanged,
                    siteMainAddress = toPostalAddress(businessPartner, PostalAddressDb.Scope.SiteMainAddress),
                    scriptVariants = toSiteScriptVariants(businessPartner)
                )
            }
        }


    fun toCategorizedNameParts(nameParts: List<NamePartDb>) =
        nameParts.filter { it.type != null }.map { NamePart(it.name, it.type!!) }

    fun toUncategorizedNameParts(nameParts: List<NamePartDb>) =
        nameParts.filter { it.type == null }.map { it.name }

    fun toIdentifiers(businessPartner: GoldenRecordTaskDb.BusinessPartner, scope: IdentifierDb.Scope) =
        businessPartner.identifiers.filter { it.scope == scope }.map { Identifier(it.value, it.type, it.issuingBody) }

    fun toStates(businessPartner: GoldenRecordTaskDb.BusinessPartner, scope: BusinessStateDb.Scope) =
        businessPartner.businessStates.filter { it.scope == scope }.map { BusinessState(it.validFrom?.instant, it.validTo?.instant, it.type) }

    fun toConfidence(businessPartner: GoldenRecordTaskDb.BusinessPartner, scope: ConfidenceCriteriaDb.Scope) =
        businessPartner.confidences[scope]?.let { toConfidence(it) } ?: ConfidenceCriteria.empty

    fun toConfidence(confidenceCriteria: ConfidenceCriteriaDb) =
        with(confidenceCriteria) {
            ConfidenceCriteria(
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
            BpnReference(referenceValue = referenceValue, desiredBpn = desiredBpn, referenceType = referenceType)
        }

    fun toBpnReference(businessPartner: GoldenRecordTaskDb.BusinessPartner, scope: BpnReferenceDb.Scope) =
        businessPartner.bpnReferences[scope]?.let { toBpnReference(it) } ?: BpnReference.empty

    fun toPostalAddressOrEmpty(businessPartner: GoldenRecordTaskDb.BusinessPartner, scope: PostalAddressDb.Scope) =
        toPostalAddress(businessPartner, scope)

    fun toAdditionalAddress(businessPartner: GoldenRecordTaskDb.BusinessPartner) =
        toPostalAddress(businessPartner, PostalAddressDb.Scope.AdditionalAddress)?.let {
            PostalAddressWithScriptVariants(it, toAdditionalAddressScriptVariant(businessPartner))
        }

    fun toUncategorizedAddress(businessPartner: GoldenRecordTaskDb.BusinessPartner) =
        toPostalAddress(businessPartner, PostalAddressDb.Scope.UncategorizedAddress)?.let {
            PostalAddressWithScriptVariants(it, toUncategorizedAddressScriptVariant(businessPartner))
        }

    fun toPostalAddress(businessPartner: GoldenRecordTaskDb.BusinessPartner, scope: PostalAddressDb.Scope) =
        businessPartner.addresses[scope]?.let { postalAddress ->
            with(postalAddress) {
                PostalAddress(
                    bpnReference = toBpnReference(businessPartner, scope.bpnReference),
                    addressName = addressName,
                    identifiers = toIdentifiers(businessPartner, scope.identifier),
                    states = toStates(businessPartner, scope.state),
                    confidenceCriteria = toConfidence(businessPartner, scope.confidence),
                    physicalAddress = toPhysicalAddress(physicalAddress),
                    alternativeAddress = toAlternativeAddress(alternativeAddress),
                    hasChanged = hasChanged,
                    goldenRecordRelations = scope.addressGoldenRecordRelation?.let { toAddressGoldenRecordRelations(businessPartner, it) } ?: emptyList()
                )
            }
        }

    fun toPhysicalAddress(physicalAddress: PostalAddressDb.PhysicalAddressDb) =
        with(physicalAddress) {
            PhysicalAddress(
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
                AlternativeAddress(
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
            GeoCoordinate(longitude = longitude, latitude = latitude, altitude = altitude)
        }

    fun toStreet(street: PostalAddressDb.Street) =
        with(street) {
            Street(
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
            GoldenRecordTaskDb.ResultState.Pending -> ResultState.Pending
            GoldenRecordTaskDb.ResultState.Success ->  ResultState.Success
            GoldenRecordTaskDb.ResultState.Error ->  ResultState.Error
            GoldenRecordTaskDb.ResultState.Aborted ->  ResultState.Error
        }

    fun toStepState(stepState: GoldenRecordTaskDb.StepState) =
        when(stepState){
            GoldenRecordTaskDb.StepState.Queued -> StepState.Queued
            GoldenRecordTaskDb.StepState.Reserved -> StepState.Reserved
            GoldenRecordTaskDb.StepState.Success -> StepState.Success
            GoldenRecordTaskDb.StepState.Error -> StepState.Error
            GoldenRecordTaskDb.StepState.Aborted -> StepState.Error
        }


    fun toAdditionalAddressScriptVariant(businessPartner: GoldenRecordTaskDb.BusinessPartner) =
        businessPartner.addressScriptVariants.filter { it.scope == PostalAddressDb.Scope.AdditionalAddress }.map{
            PostalAddressScriptVariantWithScriptCode(it.scriptCode, toPostalAddressScriptVariant(it))
        }

    fun toUncategorizedAddressScriptVariant(businessPartner: GoldenRecordTaskDb.BusinessPartner) =
        businessPartner.addressScriptVariants.filter { it.scope == PostalAddressDb.Scope.UncategorizedAddress }.map{
            PostalAddressScriptVariantWithScriptCode(it.scriptCode, toPostalAddressScriptVariant(it))
        }

    fun toPostalAddressScriptVariant(addressScriptVariant: PostalAddressScriptVariantDb) =
        PostalAddressScriptVariant(
            addressName = addressScriptVariant.addressName,
            physicalAddress = toPhysicalAddressScriptVariant(addressScriptVariant.physicalAddress),
            alternativeAddress = addressScriptVariant.alternativeAddress?.let { toAlternativeAddressScriptVariant(it) }
        )

    fun toPhysicalAddressScriptVariant(physicalAddressScriptVariant: PhysicalAddressScriptVariantDb) =
        with(physicalAddressScriptVariant){
            PhysicalAddressScriptVariant(
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

    fun toAlternativeAddressScriptVariant(alternativeAddressScriptVariant: AlternativeAddressScriptVariantDb) =
        with(alternativeAddressScriptVariant){
            AlternativeAddressScriptVariant(
                postalCode = postalCode,
                city = city,
                deliveryServiceQualifier = deliveryServiceQualifier,
                deliveryServiceNumber = deliveryServiceNumber
            )
        }

    fun toLegalEntityScriptVariants(businessPartner: GoldenRecordTaskDb.BusinessPartner): List<LegalEntityScriptVariant>{
        val legalAddressVariantsByScriptCode = businessPartner.addressScriptVariants.filter { it.scope == PostalAddressDb.Scope.LegalAddress }.associateBy { it.scriptCode }

        return businessPartner.legalEntityHeaderScriptVariants.map { headerVariant ->
            val legalAddressVariant = legalAddressVariantsByScriptCode[headerVariant.scriptCode]

            LegalEntityScriptVariant(headerVariant.scriptCode, headerVariant.legalName, headerVariant.legalShortName, legalAddressVariant?.let { toPostalAddressScriptVariant(it) } ?: PostalAddressScriptVariant.empty )
        }
    }

    fun toLegalEntityGoldenRecordRelations(businessPartner: GoldenRecordTaskDb.BusinessPartner) =
        businessPartner.legalEntityGoldenRecordRelations
            .map { LegalEntityGoldenRecordRelation(it.relationType, it.sourceBpn, it.targetBpn) }

    fun toAddressGoldenRecordRelations(businessPartner: GoldenRecordTaskDb.BusinessPartner, scope: AddressGoldenRecordRelationDb.Scope) =
        businessPartner.addressGoldenRecordRelations
            .filter { it.scope == scope }
            .map { AddressGoldenRecordRelation(it.relationType, it.sourceBpn, it.targetBpn) }

    fun toSiteScriptVariants(businessPartner: GoldenRecordTaskDb.BusinessPartner): List<SiteScriptVariant>{
        val mainAddressVariantsByScriptCode = businessPartner.addressScriptVariants.filter { it.scope == PostalAddressDb.Scope.SiteMainAddress }.associateBy { it.scriptCode }

        return businessPartner.siteHeaderScriptVariants.map { headerVariant ->
            val mainAddressVariant = mainAddressVariantsByScriptCode[headerVariant.scriptCode]

            SiteScriptVariant(headerVariant.scriptCode, headerVariant.siteName ?: "", mainAddressVariant?.let { toPostalAddressScriptVariant(it) } ?: PostalAddressScriptVariant.empty )
        }
    }

}
