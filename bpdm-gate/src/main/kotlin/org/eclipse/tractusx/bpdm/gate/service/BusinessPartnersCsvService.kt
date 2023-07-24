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

package org.eclipse.tractusx.bpdm.gate.service

import com.opencsv.CSVWriter
import com.opencsv.bean.StatefulBeanToCsv
import com.opencsv.bean.StatefulBeanToCsvBuilder
import org.eclipse.tractusx.bpdm.common.dto.AddressToCsvDto
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnersToCsv
import org.eclipse.tractusx.bpdm.common.dto.LegalEntityToCsvDto
import org.eclipse.tractusx.bpdm.common.dto.SiteToCsvDto
import org.eclipse.tractusx.bpdm.common.model.OutputInputEnum
import org.eclipse.tractusx.bpdm.gate.entity.LegalEntity
import org.eclipse.tractusx.bpdm.gate.entity.LogisticAddress
import org.eclipse.tractusx.bpdm.gate.entity.Site
import org.eclipse.tractusx.bpdm.gate.repository.GateAddressRepository
import org.eclipse.tractusx.bpdm.gate.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.gate.repository.SiteRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.io.StringWriter
import java.time.format.DateTimeFormatter


@Service
class BusinessPartnersCsvService(
    private val addressRepository: GateAddressRepository,
    private val legalEntityRepository: LegalEntityRepository,
    private val siteRepository: SiteRepository
) {

    fun downloadAddressCsv(page: Int, size: Int): StringWriter {
        val logisticAddressDtoCollection = addressRepository.findByDataType(OutputInputEnum.Input, PageRequest.of(page, size))
            .content.map { it.mapToAddressToCsvDto() }
        val writer = StringWriter()
        val sbc = beanToCsv(writer)

        sbc.write(logisticAddressDtoCollection)
        return writer
    }

    fun downloadLegalEntitiesCsv(page: Int, size: Int): StringWriter {
        val legalEntitiesDtoCollection = legalEntityRepository.findByDataType(OutputInputEnum.Input, PageRequest.of(page, size))
            .content.map { it.mapToLegalEntityToCsvDto() }
        val writer = StringWriter()
        val sbc = beanToCsv(writer)
        sbc.write(legalEntitiesDtoCollection)
        return writer
    }

    fun downloadSitesCsv(page: Int, size: Int): StringWriter {
        val sitesDtoCollection = siteRepository.findByDataType(OutputInputEnum.Input, PageRequest.of(page, size))
            .content.map { it.mapToLegalEntityToCsvDto() }
        val writer = StringWriter()
        val sbc = beanToCsv(writer)
        sbc.write(sitesDtoCollection)
        return writer
    }

    fun LogisticAddress.mapToAddressToCsvDto(): AddressToCsvDto {
        return AddressToCsvDto(
            externalId,
            legalEntity?.externalId,
            site?.externalId,
            physicalPostalAddress.country.name,
            physicalPostalAddress.city,
            physicalPostalAddress.street?.name,
            nameParts.elementAtOrNull(0)?.namePart,
            physicalPostalAddress.postCode,
            physicalPostalAddress.street?.houseNumber,
            physicalPostalAddress.street?.namePrefix,
            physicalPostalAddress.street?.additionalNamePrefix,
            physicalPostalAddress.street?.nameSuffix,
            physicalPostalAddress.street?.additionalNameSuffix,
            physicalPostalAddress.street?.milestone,
            physicalPostalAddress.street?.direction,
            physicalPostalAddress.administrativeAreaLevel1,
            physicalPostalAddress.administrativeAreaLevel2,
            physicalPostalAddress.administrativeAreaLevel3,
            physicalPostalAddress.districtLevel1,
            physicalPostalAddress.companyPostCode,
            physicalPostalAddress.industrialZone,
            physicalPostalAddress.building,
            physicalPostalAddress.floor,
            physicalPostalAddress.door,
            physicalPostalAddress.geographicCoordinates?.longitude?.toString(),
            physicalPostalAddress.geographicCoordinates?.latitude?.toString(),
            physicalPostalAddress.geographicCoordinates?.altitude?.toString(),
            alternativePostalAddress?.country?.name,
            alternativePostalAddress?.postCode,
            alternativePostalAddress?.city,
            alternativePostalAddress?.administrativeAreaLevel1,
            alternativePostalAddress?.deliveryServiceNumber,
            alternativePostalAddress?.deliveryServiceType?.getTypeName(),
            alternativePostalAddress?.deliveryServiceQualifier,
            alternativePostalAddress?.geographicCoordinates?.longitude?.toString(),
            alternativePostalAddress?.geographicCoordinates?.longitude?.toString(),
            alternativePostalAddress?.geographicCoordinates?.altitude?.toString(),
            states.elementAtOrNull(0)?.description,
            states.elementAtOrNull(0)?.validTo?.format(getFormatter()),
            states.elementAtOrNull(0)?.validFrom?.format(getFormatter()),
            states.elementAtOrNull(0)?.type?.getTypeName(),
            identifiers.elementAtOrNull(0)?.value,
            identifiers.elementAtOrNull(0)?.type
        )
    }

    fun LegalEntity.mapToLegalEntityToCsvDto(): LegalEntityToCsvDto {
        return LegalEntityToCsvDto(
            externalId,
            nameParts.elementAtOrNull(0)?.namePart,
            identifiers.elementAtOrNull(0)?.value,
            identifiers.elementAtOrNull(0)?.type,
            nameParts.elementAtOrNull(0)?.namePart,
            legalAddress.physicalPostalAddress.country.name,
            legalAddress.physicalPostalAddress.city,
            legalAddress.physicalPostalAddress.street?.name,
            legalAddress.physicalPostalAddress.postCode,
            legalAddress.physicalPostalAddress.street?.houseNumber,
            legalAddress.physicalPostalAddress.street?.namePrefix,
            legalAddress.physicalPostalAddress.street?.nameSuffix,
            legalAddress.physicalPostalAddress.street?.additionalNamePrefix,
            legalAddress.physicalPostalAddress.street?.additionalNameSuffix,
            legalAddress.physicalPostalAddress.street?.milestone,
            legalAddress.physicalPostalAddress.street?.direction,
            legalAddress.physicalPostalAddress.administrativeAreaLevel1,
            legalAddress.physicalPostalAddress.administrativeAreaLevel2,
            legalAddress.physicalPostalAddress.administrativeAreaLevel3,
            legalAddress.physicalPostalAddress.districtLevel1,
            legalAddress.physicalPostalAddress.companyPostCode,
            legalAddress.physicalPostalAddress.industrialZone,
            legalAddress.physicalPostalAddress.building,
            legalAddress.physicalPostalAddress.floor,
            legalAddress.physicalPostalAddress.door,
            legalAddress.physicalPostalAddress.geographicCoordinates?.longitude.toString(),
            legalAddress.physicalPostalAddress.geographicCoordinates?.latitude.toString(),
            legalAddress.physicalPostalAddress.geographicCoordinates?.altitude.toString(),
            legalAddress.alternativePostalAddress?.country?.name,
            legalAddress.alternativePostalAddress?.postCode,
            legalAddress.alternativePostalAddress?.city,
            legalAddress.alternativePostalAddress?.administrativeAreaLevel1,
            legalAddress.alternativePostalAddress?.deliveryServiceNumber,
            legalAddress.alternativePostalAddress?.deliveryServiceType?.name,
            legalAddress.alternativePostalAddress?.deliveryServiceQualifier,
            legalAddress.alternativePostalAddress?.geographicCoordinates?.longitude.toString(),
            legalAddress.alternativePostalAddress?.geographicCoordinates?.latitude.toString(),
            legalAddress.alternativePostalAddress?.geographicCoordinates?.altitude.toString(),
            legalAddress.states.elementAtOrNull(0)?.description,
            legalAddress.states.elementAtOrNull(0)?.validFrom?.format(getFormatter()),
            legalAddress.states.elementAtOrNull(0)?.validTo?.format(getFormatter()),
            legalAddress.states.elementAtOrNull(0)?.type?.getTypeName(),
            legalAddress.identifiers.elementAtOrNull(0)?.value,
            legalAddress.identifiers.elementAtOrNull(0)?.type,
            shortName,
            legalForm,
            classifications.elementAtOrNull(0)?.value,
            classifications.elementAtOrNull(0)?.code,
            classifications.elementAtOrNull(0)?.type?.getTypeName(),
            states.elementAtOrNull(0)?.description,
            states.elementAtOrNull(0)?.validFrom?.format(getFormatter()),
            states.elementAtOrNull(0)?.validTo?.format(getFormatter()),
            states.elementAtOrNull(0)?.type?.getTypeName(),
            roles.elementAtOrNull(0)?.roleName?.name,
            roles.elementAtOrNull(1)?.roleName?.name,
            identifiers.elementAtOrNull(1)?.value,
            identifiers.elementAtOrNull(1)?.type,
            identifiers.elementAtOrNull(1)?.issuingBody,
            identifiers.elementAtOrNull(2)?.value,
            identifiers.elementAtOrNull(2)?.type,
            identifiers.elementAtOrNull(2)?.issuingBody,
        )
    }

    fun Site.mapToLegalEntityToCsvDto(): SiteToCsvDto {
        return SiteToCsvDto(
            externalId,
            legalEntity.externalId,
            nameParts.elementAtOrNull(0)?.namePart,
            mainAddress.physicalPostalAddress.country.name,
            mainAddress.physicalPostalAddress.city,
            mainAddress.physicalPostalAddress.street?.name,
            mainAddress.nameParts.elementAtOrNull(0)?.namePart,
            mainAddress.physicalPostalAddress.postCode,
            mainAddress.physicalPostalAddress.street?.houseNumber,
            mainAddress.physicalPostalAddress.street?.namePrefix,
            mainAddress.physicalPostalAddress.street?.additionalNamePrefix,
            mainAddress.physicalPostalAddress.street?.nameSuffix,
            mainAddress.physicalPostalAddress.street?.additionalNameSuffix,
            mainAddress.physicalPostalAddress.street?.milestone,
            mainAddress.physicalPostalAddress.street?.direction,
            mainAddress.physicalPostalAddress.administrativeAreaLevel1,
            mainAddress.physicalPostalAddress.administrativeAreaLevel2,
            mainAddress.physicalPostalAddress.administrativeAreaLevel3,
            mainAddress.physicalPostalAddress.districtLevel1,
            mainAddress.physicalPostalAddress.companyPostCode,
            mainAddress.physicalPostalAddress.industrialZone,
            mainAddress.physicalPostalAddress.building,
            mainAddress.physicalPostalAddress.floor,
            mainAddress.physicalPostalAddress.door,
            mainAddress.physicalPostalAddress.geographicCoordinates?.longitude?.toString(),
            mainAddress.physicalPostalAddress.geographicCoordinates?.latitude?.toString(),
            mainAddress.physicalPostalAddress.geographicCoordinates?.altitude?.toString(),
            mainAddress.alternativePostalAddress?.country?.name,
            mainAddress.alternativePostalAddress?.postCode,
            mainAddress.alternativePostalAddress?.city,
            mainAddress.alternativePostalAddress?.administrativeAreaLevel1,
            mainAddress.alternativePostalAddress?.deliveryServiceNumber,
            mainAddress.alternativePostalAddress?.deliveryServiceType?.getTypeName(),
            mainAddress.alternativePostalAddress?.deliveryServiceQualifier,
            mainAddress.alternativePostalAddress?.geographicCoordinates?.longitude?.toString(),
            mainAddress.alternativePostalAddress?.geographicCoordinates?.latitude?.toString(),
            mainAddress.alternativePostalAddress?.geographicCoordinates?.altitude?.toString(),
            mainAddress.states.elementAtOrNull(0)?.description,
            mainAddress.states.elementAtOrNull(0)?.validFrom?.format(getFormatter()),
            mainAddress.states.elementAtOrNull(0)?.validTo?.format(getFormatter()),
            mainAddress.states.elementAtOrNull(0)?.type?.getTypeName(),
            mainAddress.identifiers.elementAtOrNull(0)?.value,
            mainAddress.identifiers.elementAtOrNull(0)?.type,
            states.elementAtOrNull(0)?.description,
            states.elementAtOrNull(0)?.validFrom?.format(getFormatter()),
            states.elementAtOrNull(0)?.validTo?.format(getFormatter()),
            states.elementAtOrNull(0)?.type?.getTypeName(),
        )
    }

    private fun getFormatter(): DateTimeFormatter {
        return DateTimeFormatter.ofPattern("dd-mm-yyyy")
    }

    private fun beanToCsv(writer: StringWriter): StatefulBeanToCsv<BusinessPartnersToCsv> {
        return StatefulBeanToCsvBuilder<BusinessPartnersToCsv>(writer)
            .withQuotechar('"')
            .withSeparator(';')
            .withEscapechar(CSVWriter.DEFAULT_ESCAPE_CHARACTER)
            .withLineEnd(CSVWriter.DEFAULT_LINE_END)

            .build()
    }

}