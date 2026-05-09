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

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.*
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.pool.entity.*
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class BusinessPartnerEquivalenceMapper {

    fun toEquivalenceDto(legalEntity: LegalEntityDb) =
        with(legalEntity) {
            LegalEntityEquivalenceDto(
                legalForm = legalForm?.technicalKey,
                legalName = legalName.value,
                legalShortName = legalName.shortName,
                identifiers = identifiers.map { IdentifierEquivalenceDto(it.value, it.type.technicalKey) }.toSortedSet(compareBy { it.value }),
                states = states.map { StateEquivalenceDto(it.validFrom, it.validTo, it.type) }.toSortedSet(compareBy { it.validFrom }),
                confidenceCriteria = toEquivalenceDto(confidenceCriteria),
                isCatenaXMemberData = isCatenaXMemberData,
                legalAddress = toEquivalenceDto(legalEntity.legalAddress),
                scriptVariants = scriptVariants.map { toEquivalenceDto(it) }.toSortedSet(compareBy { it.scriptCode })
            )
        }

    fun toEquivalenceDto(site: SiteDb) =
        with(site) {
            SiteEquivalenceDto(
                name = name,
                states = states.map { StateEquivalenceDto(it.validFrom, it.validTo, it.type) }.toSortedSet(compareBy { it.validFrom }),
                confidenceCriteria = toEquivalenceDto(confidenceCriteria),
                mainAddress = toEquivalenceDto(mainAddress),
                scriptVariants = scriptVariants.map { SiteHeaderScriptVariantEquivalenceDto(it.scriptCode.technicalKey, it.name) }.toSortedSet(compareBy { it.scriptCode })
            )
        }

    fun toEquivalenceDto(logisticAddress: LogisticAddressDb) =
        LogisticAddressEquivalenceDto(
            name = logisticAddress.name,
            states = logisticAddress.states.map { StateEquivalenceDto(it.validFrom, it.validTo, it.type) }.toSortedSet(compareBy { it.validFrom }),
            identifiers = logisticAddress.identifiers.map { IdentifierEquivalenceDto(it.value, it.type.technicalKey) }.toSortedSet(compareBy { it.value }),
            physicalPostalAddress = with(logisticAddress.physicalPostalAddress) {
                PhysicalAddressEquivalenceDto(
                    geographicCoordinates = with(geographicCoordinates) { this?.let { GeoCoordinateDto(longitude, latitude, altitude) } },
                    country = country,
                    administrativeAreaLevel1 = administrativeAreaLevel1?.regionCode,
                    administrativeAreaLevel2 = administrativeAreaLevel2,
                    administrativeAreaLevel3 = administrativeAreaLevel3,
                    postalCode = postCode,
                    city = city,
                    district = districtLevel1,
                    companyPostalCode = companyPostCode,
                    industrialZone = industrialZone,
                    building = building,
                    floor = floor,
                    door = door,
                    street = with(street) {
                        this?.let {
                            StreetEquivalenceDto(
                                name,
                                houseNumber,
                                houseNumberSupplement,
                                milestone,
                                direction,
                                namePrefix,
                                additionalNamePrefix,
                                nameSuffix,
                                additionalNameSuffix
                            )
                        }
                    },
                    taxJurisdictionCode = taxJurisdictionCode
                )
            },
            alternativePostalAddress = with(logisticAddress.alternativePostalAddress) {
                this?.let {
                    AlternativeEquivalenceDto(
                        geographicCoordinates = geographicCoordinates?.let { with(geographicCoordinates) { GeoCoordinateDto(longitude, latitude, altitude) } },
                        country = country,
                        administrativeAreaLevel1 = administrativeAreaLevel1?.regionCode,
                        postalCode = postCode,
                        city = city,
                        deliveryServiceType = deliveryServiceType,
                        deliveryServiceQualifier = deliveryServiceQualifier,
                        deliveryServiceNumber = deliveryServiceNumber
                    )
                }
            },
            confidenceCriteria = toEquivalenceDto(logisticAddress.confidenceCriteria),
            scriptVariants = logisticAddress.scriptVariants.map { toEquivalenceDto(it) }.toSortedSet(compareBy { it.scriptCode })
        )

    fun toEquivalenceDto(logisticAddressScriptVariant: LogisticAddressScriptVariantDb ) =
        with(logisticAddressScriptVariant){
            LogisticAddressScriptVariantEquivalenceDto(
                scriptCode = scriptCode.technicalKey,
                name = name,
                physicalAddress = with(physicalAddress){
                    PhysicalAddressScriptVariantEquivalenceDto(
                        postalCode = postalCode,
                        city = city,
                        district = district,
                        street = street?.let {
                            with(it) {
                                StreetEquivalenceDto(
                                    name = name,
                                    houseNumber = houseNumber,
                                    houseNumberSupplement = houseNumberSupplement,
                                    milestone = milestone,
                                    direction = direction,
                                    namePrefix = namePrefix,
                                    additionalNamePrefix = additionalNamePrefix,
                                    nameSuffix = nameSuffix,
                                    additionalNameSuffix = additionalNamePrefix
                                )
                            }
                        },
                        companyPostalCode = companyPostalCode,
                        industrialZone = industrialZone,
                        building = building,
                        floor = floor,
                        door = door,
                        taxJurisdictionCode = taxJurisdictionCode
                    )
                },
                alternativeAddress = alternativeAddress?.let {
                    with(it){
                        AlternativeAddressScriptVariantEquivalenceDto(
                            postalCode = postalCode,
                            city = city,
                            deliveryServiceQualifier = deliveryServiceQualifier,
                            deliveryServiceNumber = deliveryServiceNumber
                        )
                    }
                }
            )
        }


    private fun toEquivalenceDto(confidenceCriteria: ConfidenceCriteriaDb) =
        with(confidenceCriteria) {
            ConfidenceCriteriaEquivalenceDto(
                sharedByOwner,
                checkedByExternalDataSource,
                numberOfSharingMembers,
                lastConfidenceCheckAt,
                nextConfidenceCheckAt,
                confidenceLevel
            )
        }

    private fun toEquivalenceDto(legalEntityScriptVariantDb: LegalEntityScriptVariantDb) =
        with(legalEntityScriptVariantDb) { LegalEntityScriptVariantEquivalenceDto(scriptCode.technicalKey, legalName, shortName) }

    data class LegalEntityEquivalenceDto(
        override val legalForm: String?,
        val legalName: String?,
        override val legalShortName: String?,
        override val identifiers: SortedSet<IdentifierEquivalenceDto>,
        override val states: SortedSet<StateEquivalenceDto>,
        override val confidenceCriteria: ConfidenceCriteriaEquivalenceDto?,
        val legalAddress: LogisticAddressEquivalenceDto?,
        val isCatenaXMemberData: Boolean,
        val scriptVariants: SortedSet<LegalEntityScriptVariantEquivalenceDto>,
    ) : IBaseLegalEntityDto

    data class LegalEntityScriptVariantEquivalenceDto(
        val scriptCode: String,
        val legalName: String?,
        val legalShortName: String?
    )

    data class SiteEquivalenceDto(
        override val name: String?,
        override val states: Collection<StateEquivalenceDto>,
        override val confidenceCriteria: ConfidenceCriteriaEquivalenceDto?,
        val mainAddress: LogisticAddressEquivalenceDto,
        val scriptVariants: SortedSet<SiteHeaderScriptVariantEquivalenceDto>,
    ) : IBaseSiteDto

    data class SiteHeaderScriptVariantEquivalenceDto(
        val scriptCode: String,
        val name: String
    )

    data class LogisticAddressEquivalenceDto(
        val name: String?,
        override val states: Collection<StateEquivalenceDto>,
        override val identifiers: Collection<IdentifierEquivalenceDto>,
        override val physicalPostalAddress: PhysicalAddressEquivalenceDto?,
        override val alternativePostalAddress: AlternativeEquivalenceDto?,
        override val confidenceCriteria: ConfidenceCriteriaEquivalenceDto?,
        val scriptVariants: SortedSet<LogisticAddressScriptVariantEquivalenceDto>
    ) : IBaseLogisticAddressDto

    data class IdentifierEquivalenceDto(
        override val value: String,
        override val type: String,
    ) : IAddressIdentifierDto, ILegalEntityIdentifierDto {
        override val issuingBody: String? = null
    }

    data class StateEquivalenceDto(
        override val validFrom: LocalDateTime?,
        override val validTo: LocalDateTime?,
        override val type: BusinessStateType,
    ) : IAddressStateDto, ILegalEntityStateDto, ISiteStateDto

    data class PhysicalAddressEquivalenceDto(
        override val geographicCoordinates: GeoCoordinateDto?,
        override val country: CountryCode?,
        override val administrativeAreaLevel1: String?,
        override val administrativeAreaLevel2: String?,
        override val administrativeAreaLevel3: String?,
        override val postalCode: String?,
        override val city: String?,
        override val district: String?,
        override val street: StreetEquivalenceDto?,
        override val companyPostalCode: String?,
        override val industrialZone: String?,
        override val building: String?,
        override val floor: String?,
        override val door: String?,
        override val taxJurisdictionCode: String?
    ) : IBasePhysicalPostalAddressDto

    data class AlternativeEquivalenceDto(
        override val geographicCoordinates: GeoCoordinateDto?,
        override val country: CountryCode?,
        override val administrativeAreaLevel1: String?,
        override val postalCode: String?,
        override val city: String?,
        override val deliveryServiceType: DeliveryServiceType?,
        override val deliveryServiceQualifier: String?,
        override val deliveryServiceNumber: String?
    ) : IBaseAlternativePostalAddressDto

    data class StreetEquivalenceDto(
        override val name: String?,
        override val houseNumber: String?,
        override val houseNumberSupplement: String?,
        override val milestone: String?,
        override val direction: String?,
        override val namePrefix: String?,
        override val additionalNamePrefix: String?,
        override val nameSuffix: String?,
        override val additionalNameSuffix: String?
    ) : IStreetDetailedDto

    data class ConfidenceCriteriaEquivalenceDto(
        override val sharedByOwner: Boolean?,
        override val checkedByExternalDataSource: Boolean?,
        override val numberOfSharingMembers: Int?,
        override val lastConfidenceCheckAt: LocalDateTime?,
        override val nextConfidenceCheckAt: LocalDateTime?,
        override val confidenceLevel: Int?
    ) : IConfidenceCriteriaDto

    data class LogisticAddressScriptVariantEquivalenceDto(
        val scriptCode: String,
        val name: String?,
        val physicalAddress: PhysicalAddressScriptVariantEquivalenceDto?,
        val alternativeAddress: AlternativeAddressScriptVariantEquivalenceDto?
    )

    data class PhysicalAddressScriptVariantEquivalenceDto(
        val postalCode: String?,
        val city: String?,
        val district: String?,
        val street: StreetEquivalenceDto?,
        val companyPostalCode: String?,
        val industrialZone: String?,
        val building: String?,
        val floor: String?,
        val door: String?,
        val taxJurisdictionCode: String?
    )

    data class AlternativeAddressScriptVariantEquivalenceDto(
        val postalCode: String?,
        val city: String?,
        val deliveryServiceQualifier: String?,
        val deliveryServiceNumber: String?
    )
}