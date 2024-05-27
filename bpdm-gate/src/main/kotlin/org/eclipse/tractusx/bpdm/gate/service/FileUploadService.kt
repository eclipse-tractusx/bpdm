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

package org.eclipse.tractusx.bpdm.gate.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.bpdm.gate.util.CsvFileUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class FileUploadService(
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val businessPartnerMappings: BusinessPartnerMappings
) {

    private val logger = KotlinLogging.logger { }

    fun processFile(file: MultipartFile, ownerBpnl: String?): ResponseEntity<Collection<BusinessPartnerInputDto>> {

        val csvData: List<Map<String, String>> = CsvFileUtils.parseCsv(file)
        // Validate the CSV data
        for (rowData in csvData) {
            if (rowData["externalId"].isNullOrBlank() || rowData["legalEntity.legalEntityBpn"].isNullOrBlank()) {
                return ResponseEntity(HttpStatus.BAD_REQUEST)
            }
        }

        val businessPartnerDtos = CsvFileUtils.mapToBusinessPartnerRequests(csvData)
        logger.debug { "Executing processFile() with parameters $businessPartnerDtos" }
        val businessPartnerEntities = businessPartnerDtos.map { dto -> businessPartnerMappings.toBusinessPartnerInput(dto, ownerBpnl) }
        businessPartnerRepository.saveAll(businessPartnerEntities)
        return ResponseEntity.ok(businessPartnerEntities.map(businessPartnerMappings::toBusinessPartnerInputDto))
    }

}