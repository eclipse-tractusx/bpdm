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

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.dto.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.ClassificationType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.pool.util.BusinessPartnerNonVerboseValues
import org.eclipse.tractusx.bpdm.pool.util.BusinessPartnerVerboseValues
import org.eclipse.tractusx.orchestrator.api.model.*
import java.time.LocalDateTime

fun minFullBusinessPartner(): BusinessPartnerFullDto {

    return BusinessPartnerFullDto(generic = BusinessPartnerGenericDto())
}

fun emptyLegalEntity(): LegalEntityDto {

    return LegalEntityDto()
}

fun minValidLegalEntity(bpnLReference: BpnReferenceDto, bpnAReference: BpnReferenceDto): LegalEntityDto {

    return LegalEntityDto(
        bpnLReference = bpnLReference,
        legalName = "legalName_" + bpnLReference.referenceValue,
        legalAddress = minLogisticAddress(bpnAReference = bpnAReference)
    )
}

fun fullValidLegalEntity(bpnLReference: BpnReferenceDto, bpnAReference: BpnReferenceDto): LegalEntityDto {

    return LegalEntityDto(
        bpnLReference = bpnLReference,
        legalName = "legalName_" + bpnLReference.referenceValue,
        legalShortName = "shortName_" + bpnLReference.referenceValue,
        legalForm = BusinessPartnerVerboseValues.legalForm1.technicalKey,
        identifiers = listOf(
            legalEntityIdentifierDto(bpnLReference.referenceValue, 1L, BusinessPartnerVerboseValues.identifierType1),
            legalEntityIdentifierDto(bpnLReference.referenceValue, 2L, BusinessPartnerVerboseValues.identifierType2)
        ),
        states = listOf(
            legalEntityState(bpnLReference.referenceValue, 1L, BusinessStateType.ACTIVE),
            legalEntityState(bpnLReference.referenceValue, 2L, BusinessStateType.INACTIVE)
        ),
        classifications = listOf(
            classificationDto(bpnLReference.referenceValue, 1L, ClassificationType.NACE),
            classificationDto(bpnLReference.referenceValue, 2L, ClassificationType.NAICS)
        ),
        legalAddress = fullLogisticAddressDto(bpnAReference)
    )
}

fun legalEntityIdentifierDto(name: String, id: Long, type: TypeKeyNameVerboseDto<String>): LegalEntityIdentifierDto {

    return LegalEntityIdentifierDto(
        value = "value_" + name + "_" + id,
        issuingBody = "issuingBody_" + name + "_" + id,
        type = type.technicalKey
    )
}

fun addressIdentifierDto(name: String, id: Long, type: TypeKeyNameVerboseDto<String>): AddressIdentifierDto {

    return AddressIdentifierDto(
        value = "value_" + name + "_" + id,
        type = type.technicalKey
    )
}

fun legalEntityState(name: String, id: Long, type: BusinessStateType): LegalEntityStateDto {

    return LegalEntityStateDto(
        validFrom = LocalDateTime.now().plusDays(id),
        validTo = LocalDateTime.now().plusDays(id + 2),
        type = type
    )
}

fun siteState(name: String, id: Long, type: BusinessStateType): SiteStateDto {

    return SiteStateDto(
        validFrom = LocalDateTime.now().plusDays(id),
        validTo = LocalDateTime.now().plusDays(id + 2),
        type = type
    )
}

fun addressState(name: String, id: Long, type: BusinessStateType): AddressStateDto {

    return AddressStateDto(
        validFrom = LocalDateTime.now().plusDays(id),
        validTo = LocalDateTime.now().plusDays(id + 2),
        type = type
    )
}


fun classificationDto(name: String, id: Long, type: ClassificationType): LegalEntityClassificationDto {

    return LegalEntityClassificationDto(
        code = "code_" + name + "_" + id,
        value = "value_" + name + "_" + id,
        type = type
    )
}

fun minLogisticAddress(bpnAReference: BpnReferenceDto): LogisticAddressDto {

    return LogisticAddressDto(
        bpnAReference = bpnAReference,
        physicalPostalAddress = minPhysicalPostalAddressDto(bpnAReference)
    )
}

fun minPhysicalPostalAddressDto(bpnAReference: BpnReferenceDto) = PhysicalPostalAddressDto(
    country = CountryCode.DE,
    city = "City_" + bpnAReference.referenceValue
)

fun fullLogisticAddressDto(bpnAReference: BpnReferenceDto): LogisticAddressDto {

    return LogisticAddressDto(
        bpnAReference = bpnAReference,
        name = "name_" + bpnAReference.referenceValue,
        identifiers = listOf(
            addressIdentifierDto(bpnAReference.referenceValue, 1L, TypeKeyNameVerboseDto(BusinessPartnerNonVerboseValues.addressIdentifierTypeDto1.technicalKey, "")),
            addressIdentifierDto(bpnAReference.referenceValue, 2L, TypeKeyNameVerboseDto(BusinessPartnerNonVerboseValues.addressIdentifierTypeDto2.technicalKey, ""))
        ),
        states = listOf(
            addressState(bpnAReference.referenceValue, 1L, BusinessStateType.ACTIVE),
            addressState(bpnAReference.referenceValue, 2L, BusinessStateType.INACTIVE)
        ),
        physicalPostalAddress = PhysicalPostalAddressDto(
            geographicCoordinates = GeoCoordinateDto(longitude = 1.1f, latitude = 2.2f, altitude = 3.3f),
            country = CountryCode.DE,
            administrativeAreaLevel1 = "AD-07",
            administrativeAreaLevel2 = "adminArea2_" + bpnAReference.referenceValue,
            administrativeAreaLevel3 = "adminArea3_" + bpnAReference.referenceValue,
            postalCode = "postalCode_" + bpnAReference.referenceValue,
            city = "city_" + bpnAReference.referenceValue,
            street = StreetDto(
                name = "name_" + bpnAReference.referenceValue,
                houseNumber = "houseNumber_" + bpnAReference.referenceValue,
                milestone = "milestone_" + bpnAReference.referenceValue,
                direction = "direction_" + bpnAReference.referenceValue,
                namePrefix = "namePrefix_" + bpnAReference.referenceValue,
                additionalNamePrefix = "additionalNamePrefix_" + bpnAReference.referenceValue,
                nameSuffix = "nameSuffix_" + bpnAReference.referenceValue,
                additionalNameSuffix = "additionalNameSuffix_" + bpnAReference.referenceValue,
            ),
            district = "district_" + bpnAReference.referenceValue,
            companyPostalCode = "companyPostalCode_" + bpnAReference.referenceValue,
            industrialZone = "industrialZone_" + bpnAReference.referenceValue,
            building = "building_" + bpnAReference.referenceValue,
            floor = "floor_" + bpnAReference.referenceValue,
            door = "door_" + bpnAReference.referenceValue,
        ),
        alternativePostalAddress = AlternativePostalAddressDto(
            geographicCoordinates = GeoCoordinateDto(longitude = 12.3f, latitude = 4.56f, altitude = 7.89f),
            country = CountryCode.DE,
            administrativeAreaLevel1 = "DE-BW",
            postalCode = "alternate_postalCode_" + bpnAReference.referenceValue,
            city = "alternate_city_" + bpnAReference.referenceValue,
            deliveryServiceType = DeliveryServiceType.PO_BOX,
            deliveryServiceQualifier = "deliveryServiceQualifier_" + bpnAReference.referenceValue,
            deliveryServiceNumber = "deliveryServiceNumber_" + bpnAReference.referenceValue,
        )
    )
}

fun minValidSite(bpnSReference: BpnReferenceDto, bpnAReference: BpnReferenceDto): SiteDto {

    return SiteDto(
        bpnSReference = bpnSReference,
        name = "siteName_" + bpnSReference.referenceValue,
        mainAddress = minLogisticAddress(bpnAReference = bpnAReference)
    )
}

fun fullValidSite(bpnSReference: BpnReferenceDto, bpnAReference: BpnReferenceDto): SiteDto {

    return SiteDto(
        bpnSReference = bpnSReference,
        name = "siteName_" + bpnSReference.referenceValue,
        states = listOf(
            siteState(bpnSReference.referenceValue, 1L, BusinessStateType.ACTIVE), siteState(bpnSReference.referenceValue, 2L, BusinessStateType.INACTIVE)
        ),
        mainAddress = fullLogisticAddressDto(bpnAReference)
    )
}
