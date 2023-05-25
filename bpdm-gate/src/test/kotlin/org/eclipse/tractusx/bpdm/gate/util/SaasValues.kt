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

import org.eclipse.tractusx.bpdm.common.dto.saas.*
import org.eclipse.tractusx.bpdm.common.model.*
import org.eclipse.tractusx.bpdm.common.model.SaasThoroughfareType.INDUSTRIAL_ZONE
import org.eclipse.tractusx.bpdm.common.model.SaasThoroughfareType.STREET
import org.eclipse.tractusx.bpdm.common.service.SaasMappings
import java.time.LocalDateTime

/**
 * Test values for SaaS DTOs
 * Numbered values should match with RequestValues numbered values for easier testing
 */
object SaasValues {
    private const val testDatasource = "test-saas-datasource"

    private val idTypeBpn = TypeKeyNameUrlSaas(SaasMappings.BPN_TECHNICAL_KEY, "Business Partner Number")
    private val issuerBpn = TypeKeyNameUrlSaas("CATENAX", "Catena-X")

    private val language1 = LanguageSaas(technicalKey = CommonValues.language1)
    private val language2 = LanguageSaas(technicalKey = CommonValues.language2)

    private val characterSet1 = TypeKeyNameSaas(CommonValues.characterSet1.name)
    private val characterSet2 = TypeKeyNameSaas(CommonValues.characterSet2.name)

    val modificationTime1 = LocalDateTime.of(2020, 1, 1, 2, 1)
    val modificationTime2 = LocalDateTime.of(2020, 1, 1, 3, 1)

    val identifier1 = IdentifierSaas(
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.identifierTypeTechnicalKey1),
        value = CommonValues.identifierValue1,
        issuingBody = TypeKeyNameUrlSaas(name = CommonValues.identifierIssuingBodyName1),
        status = TypeKeyNameSaas(technicalKey = CommonValues.identifierStatusTechnicalKey1)
    )

    private val identifier1Response = identifier1.copy(
        type = identifier1.type!!.copy(
            name = CommonValues.identifierTypeName1,
            url = CommonValues.identifierTypeUrl1
        ),
        issuingBody = identifier1.issuingBody!!.copy(
            name = CommonValues.identifierIssuingBodyName1,
            url = CommonValues.identifierIssuingBodyUrl1
        ),
        status = identifier1.status!!.copy(
            name = CommonValues.identifierStatusName1
        )
    )

    val identifier2 = IdentifierSaas(
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.identifierTypeTechnicalKey2),
        value = CommonValues.identifierValue2,
        issuingBody = TypeKeyNameUrlSaas(name = CommonValues.identifierIssuingBodyName2),
        status = TypeKeyNameSaas(technicalKey = CommonValues.identifierStatusTechnicalKey2)
    )

    private val identifier2Response = identifier2.copy(
        type = identifier2.type!!.copy(
            name = CommonValues.identifierTypeName2,
            url = CommonValues.identifierTypeUrl2
        ),
        issuingBody = identifier2.issuingBody!!.copy(
            name = CommonValues.identifierIssuingBodyName2,
            url = CommonValues.identifierIssuingBodyUrl2
        ),
        status = identifier2.status!!.copy(
            name = CommonValues.identifierStatusName2
        )
    )

    private val identifier3 = IdentifierSaas(
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.identifierTypeTechnicalKey3),
        value = CommonValues.identifierValue3,
        issuingBody = TypeKeyNameUrlSaas(name = CommonValues.identifierIssuingBodyName3),
        status = TypeKeyNameSaas(technicalKey = CommonValues.identifierStatusTechnicalKey3)
    )

    private val identifier3Response = identifier3.copy(
        type = identifier3.type!!.copy(
            name = CommonValues.identifierTypeName3,
            url = CommonValues.identifierTypeUrl3
        ),
        issuingBody = identifier3.issuingBody!!.copy(
            name = CommonValues.identifierIssuingBodyName3,
            url = CommonValues.identifierIssuingBodyUrl3
        ),
        status = identifier3.status!!.copy(
            name = CommonValues.identifierStatusName3
        )
    )

    private val identifier4 = IdentifierSaas(
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.identifierTypeTechnicalKey4),
        value = CommonValues.identifierValue4,
        issuingBody = TypeKeyNameUrlSaas(name = CommonValues.identifierIssuingBodyName4),
        status = TypeKeyNameSaas(technicalKey = CommonValues.identifierStatusTechnicalKey4)
    )

    private val identifier4Response = identifier4.copy(
        type = identifier4.type!!.copy(
            name = CommonValues.identifierTypeName4,
            url = CommonValues.identifierTypeUrl4
        ),
        issuingBody = identifier4.issuingBody!!.copy(
            name = CommonValues.identifierIssuingBodyName4,
            url = CommonValues.identifierIssuingBodyUrl4
        ),
        status = identifier4.status!!.copy(
            name = CommonValues.identifierStatusName4
        )
    )

    private val identifierBpn1 = IdentifierSaas(
        type = idTypeBpn,
        value = CommonValues.bpn1,
        issuingBody = issuerBpn
    )

    private val identifierBpn2 = IdentifierSaas(
        type = idTypeBpn,
        value = CommonValues.bpn2,
        issuingBody = issuerBpn
    )

    private val identifierBpn3 = IdentifierSaas(
        type = idTypeBpn,
        value = CommonValues.bpn3,
        issuingBody = issuerBpn
    )

    private val identifierBpnSite1 = IdentifierSaas(
        type = idTypeBpn,
        value = CommonValues.bpnSite1,
        issuingBody = issuerBpn
    )

    private val identifierBpnSite2 = IdentifierSaas(
        type = idTypeBpn,
        value = CommonValues.bpnSite2,
        issuingBody = issuerBpn
    )

    private val identifierBpnSite3 = IdentifierSaas(
        type = idTypeBpn,
        value = CommonValues.bpnSite3,
        issuingBody = issuerBpn
    )

    private val identifierBpnAddress1 = IdentifierSaas(
        type = idTypeBpn,
        value = CommonValues.bpnAddress1,
        issuingBody = issuerBpn
    )

    private val identifierBpnAddress2 = IdentifierSaas(
        type = idTypeBpn,
        value = CommonValues.bpnAddress2,
        issuingBody = issuerBpn
    )

    private val identifierBpnAddress3 = IdentifierSaas(
        type = idTypeBpn,
        value = CommonValues.bpnAddress3,
        issuingBody = issuerBpn
    )

    private val name1 = NameSaas(
        value = CommonValues.name1,
        shortName = CommonValues.shortName1,
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.nameType1.name),
        language = language1
    )

    private val name2 = NameSaas(
        value = CommonValues.name2,
        shortName = CommonValues.shortName2,
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.nameType1.name),
        language = language1
    )

    private val name3 = NameSaas(
        value = CommonValues.name3,
        shortName = CommonValues.shortName3,
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.nameType1.name),
        language = language1
    )

    private val name4 = NameSaas(
        value = CommonValues.name4,
        shortName = CommonValues.shortName4,
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.nameType1.name),
        language = language1
    )

    val nameSite1 = NameSaas(value = CommonValues.nameSite1)
    val nameSite2 = NameSaas(value = CommonValues.nameSite2)

    private val legalForm1 = LegalFormSaas(technicalKey = CommonValues.legalFormTechnicalKey1)

    private val legalForm1Response = legalForm1.copy(
        name = CommonValues.legalFormName1,
        mainAbbreviation = CommonValues.legalFormAbbreviation1,
    )

    private val legalForm2 = LegalFormSaas(technicalKey = CommonValues.legalFormTechnicalKey2)

    private val legalForm2Response = legalForm2.copy(
        name = CommonValues.legalFormName2,
        mainAbbreviation = CommonValues.legalFormAbbreviation2,
    )

    private val businessStatus1 = BusinessPartnerStatusSaas(
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.businessStateType1.name),
        officialDenotation = CommonValues.businessStatusOfficialDenotation1,
        validFrom = CommonValues.businessStatusValidFrom1,
        validUntil = CommonValues.businessStatusValidUntil1
    )

    private val businessStatus2 = BusinessPartnerStatusSaas(
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.businessStateType2.name),
        officialDenotation = CommonValues.businessStatusOfficialDenotation2,
        validFrom = CommonValues.businessStatusValidFrom2,
        validUntil = CommonValues.businessStatusValidUntil2
    )

    private val classification1 = ClassificationSaas(
        value = CommonValues.classificationValue1,
        code = CommonValues.classificationCode1,
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.classificationType.name)
    )

    private val classification2 = ClassificationSaas(
        value = CommonValues.classificationValue2,
        code = CommonValues.classificationCode2,
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.classificationType.name)
    )

    private val classification3 = ClassificationSaas(
        value = CommonValues.classificationValue3,
        code = CommonValues.classificationCode3,
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.classificationType.name)
    )

    private val classification4 = ClassificationSaas(
        value = CommonValues.classificationValue4,
        code = CommonValues.classificationCode4,
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.classificationType.name)
    )

    private val profile1 = PartnerProfileSaas(
        classifications = listOf(classification1, classification2)
    )

    private val profile2 = PartnerProfileSaas(
        classifications = listOf(classification3, classification4)
    )

    private val adminAreaRegion1 = AdministrativeAreaSaas(CommonValues.adminAreaLevel1RegionCode_1, SaasAdministrativeAreaType.REGION, language1)
    private val adminAreaCounty1 = AdministrativeAreaSaas(CommonValues.county1, SaasAdministrativeAreaType.COUNTY, language1)

    private val adminAreaRegion2 = AdministrativeAreaSaas(CommonValues.adminAreaLevel1RegionCode_2, SaasAdministrativeAreaType.REGION, language2)
    private val adminAreaCounty2 = AdministrativeAreaSaas(CommonValues.county2, SaasAdministrativeAreaType.COUNTY, language2)

    private val postCodeRegular1 = PostCodeSaas(CommonValues.postCode1, SaasPostCodeType.REGULAR)
    private val postCodeRegular2 = PostCodeSaas(CommonValues.postCode2, SaasPostCodeType.REGULAR)

    private val localityCity1 = LocalitySaas(CommonValues.city1, SaasLocalityType.CITY, language1)
    private val localityDistrict1 = LocalitySaas(CommonValues.district1, SaasLocalityType.DISTRICT, language1)
    private val localityQuarter1 = LocalitySaas("Quarter1", SaasLocalityType.QUARTER, language1)

    private val localityCity2 = LocalitySaas(CommonValues.city2, SaasLocalityType.CITY, language2)
    private val localityDistrict2 = LocalitySaas(CommonValues.district2, SaasLocalityType.DISTRICT, language2)
    private val localityQuarter2 = LocalitySaas("Quarter2", SaasLocalityType.QUARTER, language2)

    private val thoroughfareZone1 = ThoroughfareSaas(name = CommonValues.industrialZone1, saasType = INDUSTRIAL_ZONE, language = language1)
    private val thoroughfareStreet1 = ThoroughfareSaas(
        name = CommonValues.street1, direction = CommonValues.direction1, number = CommonValues.houseNumber1, saasType = STREET, language = language1
    )
    private val thoroughfareZone2 = ThoroughfareSaas(name = CommonValues.industrialZone2, saasType = INDUSTRIAL_ZONE, language = language2)
    private val thoroughfareStreet2 = ThoroughfareSaas(
        name = CommonValues.street2, direction = CommonValues.direction2, number = CommonValues.houseNumber2, saasType = STREET, language = language2
    )


    private val premiseBuilding1 = PremiseSaas(CommonValues.building1, SaasPremiseType.BUILDING, language1)
    private val premiseRoom1 = PremiseSaas(CommonValues.door1, SaasPremiseType.ROOM, language1)
    private val premiseLevel1 = PremiseSaas(CommonValues.floor1, SaasPremiseType.LEVEL, language1)

    private val premiseBuilding2 = PremiseSaas(CommonValues.building2, SaasPremiseType.BUILDING, language2)
    private val premiseRoom2 = PremiseSaas(CommonValues.door2, SaasPremiseType.ROOM, language2)
    private val premiseLevel2 = PremiseSaas(CommonValues.floor2, SaasPremiseType.LEVEL, language2)

    private val postalDeliveryPoint1 = PostalDeliveryPointSaas("Postal Delivery point", SaasPostalDeliveryPointType.MAILBOX, language1)
    private val postalDeliveryPoint2 = PostalDeliveryPointSaas("Mailbox Premise Street", SaasPostalDeliveryPointType.POST_OFFICE_BOX, language2)

    private val geoCoordinate1 = GeoCoordinatesSaas(CommonValues.geoCoordinates1.first, CommonValues.geoCoordinates1.second)
    private val geoCoordinate2 = GeoCoordinatesSaas(CommonValues.geoCoordinates2.first, CommonValues.geoCoordinates2.second)

    val address1 = AddressSaas(
        //version = AddressVersionSaas(language1, characterSet1),
        country = CountrySaas(shortName = CommonValues.country1),
        //careOf = WrappedValueSaas(CommonValues.careOf1),
        //contexts = listOf( WrappedValueSaas(CommonValues.context1)),
        administrativeAreas = listOf(adminAreaRegion1, adminAreaCounty1),
        postCodes = listOf(postCodeRegular1),
        localities = listOf(localityCity1, localityDistrict1, localityQuarter1),
        thoroughfares = listOf(thoroughfareZone1, thoroughfareStreet1),
        premises = listOf(premiseBuilding1, premiseRoom1, premiseLevel1),
        postalDeliveryPoints = listOf(postalDeliveryPoint1),
        types = listOf(SaasAddressType.LEGAL.toSaasTypeDto()),
        geographicCoordinates = geoCoordinate1
    )

    private val address2 = AddressSaas(
        version = AddressVersionSaas(language2, characterSet2),
        country = CountrySaas(shortName = CommonValues.country2),
        //careOf = WrappedValueSaas(CommonValues.careOf2),
        //contexts = listOf( WrappedValueSaas(CommonValues.context2)),
        administrativeAreas = listOf(adminAreaRegion2, adminAreaCounty2),
        postCodes = listOf(postCodeRegular2),
        localities = listOf(localityCity2, localityDistrict2, localityQuarter2),
        thoroughfares = listOf(thoroughfareZone2, thoroughfareStreet2),
        premises = listOf(premiseBuilding2, premiseRoom2, premiseLevel2),
        postalDeliveryPoints = listOf(postalDeliveryPoint2),
        types = listOf(SaasAddressType.LEGAL.toSaasTypeDto()),
        geographicCoordinates = geoCoordinate2
    )

    val legalEntityResponse1 = BusinessPartnerSaas(
        externalId = CommonValues.externalId1,
        identifiers = listOf(identifier1, identifier2, identifierBpn1),
        names = listOf(name1, name2),
        legalForm = legalForm1,
        status = businessStatus1,
        profile = profile1,
        types = listOf(TypeKeyNameUrlSaas(technicalKey = BusinessPartnerTypeSaas.LEGAL_ENTITY.name)),
        addresses = listOf(address1),
        dataSource = testDatasource,
        lastModifiedAt = modificationTime1,
    )

    // identical but without lastModifiedAt
    val legalEntityRequest1 = legalEntityResponse1.copy(
        lastModifiedAt = null,
    )

    val legalEntityResponse2 = BusinessPartnerSaas(
        externalId = CommonValues.externalId2,
        identifiers = listOf(identifier3, identifier4, identifierBpn2),
        names = listOf(name3, name4),
        legalForm = legalForm2,
        status = businessStatus2,
        profile = profile2,
        types = listOf(TypeKeyNameUrlSaas(technicalKey = BusinessPartnerTypeSaas.LEGAL_ENTITY.name)),
        addresses = listOf(address2),
        dataSource = testDatasource,
        lastModifiedAt = modificationTime2,
    )

    // identical but without lastModifiedAt
    val legalEntityRequest2 = legalEntityResponse2.copy(
        lastModifiedAt = null,
    )

    val siteBusinessPartner1 = BusinessPartnerSaas(
        status = BusinessPartnerStatusSaas(
            type = TypeKeyNameUrlSaas(CommonValues.businessStateType1.name),
            validFrom = CommonValues.businessStatusValidFrom1,
            validUntil = CommonValues.businessStatusValidUntil1,
            officialDenotation = CommonValues.businessStatusOfficialDenotation1
        ),
        externalId = CommonValues.externalIdSite1,
        identifiers = listOf(identifierBpnSite1, identifier1, identifier2), // identifiers copied from legal entity
        names = listOf(nameSite1),
        addresses = listOf(address1),
        dataSource = testDatasource,
        types = listOf(TypeKeyNameUrlSaas(BusinessPartnerTypeSaas.ORGANIZATIONAL_UNIT.name)),
        lastModifiedAt = modificationTime1,
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.SHARED_WITH_CONFIDENT_MATCH, "OK")
        ),
    )
    val siteBusinessPartnerRequest1 = siteBusinessPartner1.copy(
        lastModifiedAt = null,
        metadata = null,
    )

    val siteBusinessPartner2 = BusinessPartnerSaas(
        status = BusinessPartnerStatusSaas(
            type = TypeKeyNameUrlSaas(CommonValues.businessStateType2.name),
            validFrom = CommonValues.businessStatusValidFrom2,
            validUntil = CommonValues.businessStatusValidUntil2,
            officialDenotation = CommonValues.businessStatusOfficialDenotation2
        ),
        externalId = CommonValues.externalIdSite2,
        identifiers = listOf(identifierBpnSite2, identifier3, identifier4), // identifiers copied from legal entity
        names = listOf(nameSite2),
        addresses = listOf(address2),
        dataSource = testDatasource,
        types = listOf(TypeKeyNameUrlSaas(BusinessPartnerTypeSaas.ORGANIZATIONAL_UNIT.name)),
        lastModifiedAt = modificationTime2,
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.SHARED_WITH_NO_MATCH, "OK")
        ),
    )
    val siteBusinessPartnerRequest2 = siteBusinessPartner2.copy(
        lastModifiedAt = null,
        metadata = null,
    )

    val siteNotInPoolResponse = siteBusinessPartner2.copy(
        externalId = CommonValues.externalId3,
        identifiers = listOf(identifier3Response, identifier4Response, identifierBpnSite3),
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.SHARED_WITH_NO_MATCH, "OK")
        ),
    )

    val siteSharingErrorResponse = siteBusinessPartner2.copy(
        externalId = CommonValues.externalId4,
        identifiers = listOf(),
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.ERRONEOUS_RECORD, "Error message")
        ),
    )

    val sitePendingResponse = siteBusinessPartner2.copy(
        externalId = CommonValues.externalId5,
        identifiers = listOf(),
        lastModifiedAt = LocalDateTime.now().minusMinutes(1),
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.SHARED_BY_REVIEW, "OK")
        ),
    )

    private val relationType = TypeKeyNameSaas(technicalKey = "PARENT")

    val relationSite1ToLegalEntity = RelationSaas(
        startNode = legalEntityRequest1.externalId!!,
        startNodeDataSource = testDatasource,
        endNode = siteBusinessPartner1.externalId!!,
        endNodeDataSource = testDatasource,
        type = relationType
    )

    val relationSite2ToLegalEntity = RelationSaas(
        startNode = legalEntityRequest2.externalId!!,
        startNodeDataSource = testDatasource,
        endNode = siteBusinessPartner2.externalId!!,
        endNodeDataSource = testDatasource,
        type = relationType
    )

    val addressBusinessPartner1 = BusinessPartnerSaas(
        externalId = CommonValues.externalIdAddress1,
        names = legalEntityRequest1.names,
        identifiers = listOf(identifierBpnAddress1, identifier1, identifier2), // identifiers copied from legal entity
        addresses = listOf(address1),
        dataSource = testDatasource,
        types = listOf(TypeKeyNameUrlSaas(BusinessPartnerTypeSaas.BP_ADDRESS.name)),
        lastModifiedAt = modificationTime1,
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.SHARED_WITH_CONFIDENT_MATCH, "OK")
        ),
    )

    val addressBusinessPartnerRequest1 = addressBusinessPartner1.copy(
        lastModifiedAt = null,
        metadata = null,
    )

    val addressBusinessPartner2 = BusinessPartnerSaas(
        externalId = CommonValues.externalIdAddress2,
        names = siteBusinessPartner1.names,
        identifiers = listOf(identifierBpnAddress2, identifier1, identifier2), // identifiers copied from site
        addresses = listOf(address2),
        dataSource = testDatasource,
        types = listOf(TypeKeyNameUrlSaas(BusinessPartnerTypeSaas.BP_ADDRESS.name)),
        lastModifiedAt = modificationTime2,
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.SHARED_WITH_NO_MATCH, "OK")
        ),
    )

    val addressBusinessPartnerRequest2 = addressBusinessPartner2.copy(
        lastModifiedAt = null,
        metadata = null,
    )

    val addressNotInPoolResponse = addressBusinessPartner2.copy(
        externalId = CommonValues.externalId3,
        identifiers = listOf(identifierBpnAddress3, identifier1, identifier2), // identifiers copied from site
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.SHARED_WITH_NO_MATCH, "OK")
        ),
    )

    val addressSharingErrorResponse = addressBusinessPartner2.copy(
        externalId = CommonValues.externalId4,
        identifiers = listOf(),
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.ERRONEOUS_RECORD, "Error message")
        ),
    )

    val addressPendingResponse = addressBusinessPartner2.copy(
        externalId = CommonValues.externalId5,
        identifiers = listOf(),
        lastModifiedAt = LocalDateTime.now().minusMinutes(1),
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.SHARED_BY_REVIEW, "OK")
        ),
    )

    val relationAddress1ToLegalEntity = RelationSaas(
        startNode = legalEntityRequest1.externalId!!,
        startNodeDataSource = testDatasource,
        endNode = addressBusinessPartner1.externalId!!,
        endNodeDataSource = testDatasource,
        type = relationType
    )

    val relationAddress2ToSite = RelationSaas(
        startNode = siteBusinessPartner1.externalId!!,
        startNodeDataSource = testDatasource,
        endNode = addressBusinessPartner2.externalId!!,
        endNodeDataSource = testDatasource,
        type = relationType
    )

    val addressBusinessPartnerWithRelations1 = addressBusinessPartner1.copy(
        relations = listOf(relationAddress1ToLegalEntity)
    )

    val addressBusinessPartnerWithRelations2 = addressBusinessPartner2.copy(
        relations = listOf(relationAddress2ToSite)
    )

    val legalEntityAugmented1 = legalEntityRequest1.copy(
        identifiers = listOf(identifier1Response, identifier2Response, identifierBpn1),
        legalForm = legalForm1Response,
        lastModifiedAt = modificationTime1,
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.SHARED_WITH_CONFIDENT_MATCH, "OK")
        ),
    )

    val legalEntityAugmented2 = legalEntityRequest2.copy(
        identifiers = listOf(identifier3Response, identifier4Response, identifierBpn2),
        legalForm = legalForm2Response,
        lastModifiedAt = modificationTime2,
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.SHARED_WITH_NO_MATCH, "OK")
        ),
    )

    val legalEntityAugmentedNotInPoolResponse = legalEntityAugmented2.copy(
        externalId = CommonValues.externalId3,
        identifiers = listOf(identifier3Response, identifier4Response, identifierBpn3),
        legalForm = legalForm2Response,
        lastModifiedAt = modificationTime2,
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.SHARED_WITH_NO_MATCH, "OK")
        ),
    )

    val legalEntityAugmentedSharingErrorResponse = legalEntityRequest2.copy(
        externalId = CommonValues.externalId4,
        identifiers = listOf(),
        legalForm = legalForm2Response,
        lastModifiedAt = modificationTime2,
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.ERRONEOUS_RECORD, "Error message")
        ),
    )

    val legalEntityAugmentedPendingResponse = legalEntityRequest2.copy(
        externalId = CommonValues.externalId5,
        identifiers = listOf(),
        legalForm = legalForm2Response,
        lastModifiedAt = LocalDateTime.now().minusMinutes(1),
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.SHARED_BY_REVIEW, "OK")
        ),
    )

    val siteBusinessPartnerWithRelations1 = siteBusinessPartner1.copy(
        relations = listOf(relationAddress2ToSite, relationSite1ToLegalEntity)      // relations to both child and parent
    )

    val siteBusinessPartnerWithRelations2 = siteBusinessPartner2.copy(
        relations = listOf(relationSite2ToLegalEntity)
    )
}