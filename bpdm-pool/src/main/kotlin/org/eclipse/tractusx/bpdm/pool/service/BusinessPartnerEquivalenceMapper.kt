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
import org.eclipse.tractusx.bpdm.common.model.ClassificationType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.pool.api.model.ConfidenceCriteriaDto
import org.eclipse.tractusx.bpdm.pool.entity.ConfidenceCriteriaDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
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
                legalAddress = toEquivalenceDto(legalEntity.legalAddress)
            )
        }

    fun toEquivalenceDto(site: SiteDb) =
        with(site) {
            SiteEquivalenceDto(
                name = name,
                states = states.map { StateEquivalenceDto(it.validFrom, it.validTo, it.type) }.toSortedSet(compareBy { it.validFrom }),
                confidenceCriteria = toEquivalenceDto(confidenceCriteria),
                mainAddress = toEquivalenceDto(mainAddress)
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
            confidenceCriteria = toEquivalenceDto(logisticAddress.confidenceCriteria)
        )

    private fun toEquivalenceDto(confidenceCriteriaDto: ConfidenceCriteriaDto) =
        with(confidenceCriteriaDto) {
            ConfidenceCriteriaEquivalenceDto(
                sharedByOwner,
                checkedByExternalDataSource,
                numberOfSharingMembers,
                lastConfidenceCheckAt,
                nextConfidenceCheckAt,
                confidenceLevel
            )
        }

    private fun toEquivalenceDto(confidenceCriteria: ConfidenceCriteriaDb) =
        with(confidenceCriteria) {
            ConfidenceCriteriaEquivalenceDto(
                sharedByOwner,
                checkedByExternalDataSource,
                numberOfBusinessPartners,
                lastConfidenceCheckAt,
                nextConfidenceCheckAt,
                confidenceLevel
            )
        }

    data class LegalEntityEquivalenceDto(
        override val legalForm: String?,
        val legalName: String?,
        override val legalShortName: String?,
        override val identifiers: SortedSet<IdentifierEquivalenceDto>,
        override val states: SortedSet<StateEquivalenceDto>,
        override val confidenceCriteria: ConfidenceCriteriaEquivalenceDto?,
        val legalAddress: LogisticAddressEquivalenceDto?,
        val isCatenaXMemberData: Boolean
    ) : IBaseLegalEntityDto

    data class SiteEquivalenceDto(
        override val name: String?,
        override val states: Collection<StateEquivalenceDto>,
        override val confidenceCriteria: ConfidenceCriteriaEquivalenceDto?,
        val mainAddress: LogisticAddressEquivalenceDto
    ) : IBaseSiteDto

    data class LogisticAddressEquivalenceDto(
        val name: String?,
        override val states: Collection<StateEquivalenceDto>,
        override val identifiers: Collection<IdentifierEquivalenceDto>,
        override val physicalPostalAddress: PhysicalAddressEquivalenceDto?,
        override val alternativePostalAddress: AlternativeEquivalenceDto?,
        override val confidenceCriteria: ConfidenceCriteriaEquivalenceDto?
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

    data class ClassificationEquivalenceDto(
        override val code: String?,
        override val value: String?,
        override val type: ClassificationType
    ) : ILegalEntityClassificationDto

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
}