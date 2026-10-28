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

package org.eclipse.tractusx.bpdm.gate.service


import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.exception.BpdmNullMappingException
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.response.*
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.eclipse.tractusx.bpdm.gate.entity.generic.*
import org.eclipse.tractusx.bpdm.gate.exception.BpdmInvalidPartnerException
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class BusinessPartnerMappings {

    fun toScriptVariantDto(entity: BusinessPartnerScriptVariantDb): BusinessPartnerScriptVariantDto{
        return BusinessPartnerScriptVariantDto(
            scriptCode = entity.scriptCode,
            nameParts = entity.nameParts,
            legalEntity = toLegalEntityScriptVariantDto(entity),
            site = toSiteScriptVariantDto(entity),
            address = toAddressScriptVariantDto(entity)
        )
    }

    private fun toLegalEntityScriptVariantDto(entity: BusinessPartnerScriptVariantDb): LegalEntityScriptVariantDto{
        return LegalEntityScriptVariantDto(entity.legalName, entity.shortName)
    }

    private fun toSiteScriptVariantDto(entity: BusinessPartnerScriptVariantDb): SiteScriptVariantDto{
        return SiteScriptVariantDto(entity.siteName)
    }

    private fun toAddressScriptVariantDto(entity: BusinessPartnerScriptVariantDb): AddressScriptVariantDto{
        return AddressScriptVariantDto(
            name = entity.addressName,
            physicalAddress = entity.physicalAddress?.let {  toPhysicalAddressScriptVariantDto(it) } ?: PhysicalAddressScriptVariantDto(),
            alternativeAddress = entity.alternativeAddress?.let { toAlternativeAddressScriptVariantDto(it) })
    }

    private fun toPhysicalAddressScriptVariantDto(entity: PhysicalPostalAddressScriptVariantDb): PhysicalAddressScriptVariantDto{
        return with(entity) {
            PhysicalAddressScriptVariantDto(
                city = city,
                district = district,
                street = street?.toStreetScriptVariantDto() ?: StreetScriptVariantDto(),
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door
            )
        }
    }

    private fun toAlternativeAddressScriptVariantDto(entity: AlternativePostalAddressScriptVariantDb): AlternativeAddressScriptVariantDto{
        return with(entity){
            AlternativeAddressScriptVariantDto(
                city = city
            )
        }
    }

    fun toScriptVariantDb(businessPartner: BusinessPartnerDb, dto: BusinessPartnerScriptVariantDto): BusinessPartnerScriptVariantDb{
        return with(dto){
            BusinessPartnerScriptVariantDb(
                scriptCode = dto.scriptCode,
                businessPartner = businessPartner,
                nameParts = nameParts.toMutableList(),
                shortName = legalEntity.shortName,
                legalName = legalEntity.legalName,
                siteName = site.name,
                addressName = address.name,
                physicalAddress = toPhysicalAddressScriptVariantDb(address.physicalAddress),
                alternativeAddress = toAlternativeAddressScriptVariantDb(address.alternativeAddress))
        }
    }

    private fun toPhysicalAddressScriptVariantDb(dto: PhysicalAddressScriptVariantDto): PhysicalPostalAddressScriptVariantDb{
        return with(dto){
            PhysicalPostalAddressScriptVariantDb(
                city = city,
                district = district,
                street = toStreetScriptVariant(street),
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door
            )
        }
    }

    private fun toAlternativeAddressScriptVariantDb(dto: AlternativeAddressScriptVariantDto?): AlternativePostalAddressScriptVariantDb?{
        return dto?.city?.let { AlternativePostalAddressScriptVariantDb(city = it) }
    }

    private fun toStreetScriptVariant(dto: StreetScriptVariantDto): StreetScriptVariantDb{
        return with(dto){
            StreetScriptVariantDb(
                name = name,
                direction = direction,
                namePrefix = namePrefix,
                additionalNamePrefix = additionalNamePrefix,
                nameSuffix = nameSuffix,
                additionalNameSuffix = additionalNameSuffix
            )
        }
    }

    private fun StreetScriptVariantDb.toStreetScriptVariantDto(): StreetScriptVariantDto{
        return StreetScriptVariantDto(
            name = name,
            direction = direction,
            namePrefix = namePrefix,
            additionalNamePrefix = additionalNamePrefix,
            nameSuffix = nameSuffix,
            additionalNameSuffix = additionalNameSuffix
        )
    }
}
