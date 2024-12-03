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

import com.opencsv.CSVWriter
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.model.PartnerUploadFileHeader
import org.eclipse.tractusx.bpdm.gate.model.PartnerUploadFileRow
import org.eclipse.tractusx.bpdm.gate.util.PartnerFileUtil
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntitySearchRequest
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter

@Service
class PartnerUploadService(
    private val businessPartnerService: BusinessPartnerService,
    private val poolApiClient: PoolApiClient
) {

    private val logger = KotlinLogging.logger { }

    fun processFile(file: MultipartFile, tenantBpnl: String?): ResponseEntity<Collection<BusinessPartnerInputDto>> {
        validateTenantBpnl(tenantBpnl)
        val legalName = poolApiClient.legalEntities
            .getLegalEntities(
                LegalEntitySearchRequest(listOf(tenantBpnl!!)),
                PaginationRequest(page = 0, size = 1)
            ).content.also { entities ->
                require(entities.isNotEmpty()) { "No legal entities found for tenantBpnl: $tenantBpnl" }
                require(entities.size == 1) { "Multiple legal entities found for tenantBpnl: $tenantBpnl" }
            }
            .first().legalEntity.legalName
        val csvData: List<PartnerUploadFileRow> = PartnerFileUtil.parseCsv(file)
        val businessPartnerDtos = PartnerFileUtil.validateAndMapToBusinessPartnerInputRequests(csvData, tenantBpnl, legalName)
        val result = businessPartnerService.upsertBusinessPartnersInput(businessPartnerDtos, tenantBpnl)
        return ResponseEntity.ok(result)
    }

    fun generatePartnerCsvTemplate(): ByteArrayResource {
        val headers = PartnerUploadFileHeader::class.java.declaredFields
            .asSequence()
            .filter { it.name != "INSTANCE" && it.type == String::class.java }
            .map { it.get(null) as String }
            .toList()
            .toTypedArray()

        // Use ByteArrayOutputStream with CSVWriter to create CSV content
        val outputStream = ByteArrayOutputStream().apply {
            OutputStreamWriter(this).use { writer ->
                CSVWriter(writer).use { csvWriter ->
                    csvWriter.writeNext(headers)
                }
            }
        }

        return ByteArrayResource(outputStream.toByteArray())
    }

    private fun validateTenantBpnl(tenantBpnl: String?) {
        if (tenantBpnl.isNullOrEmpty() || !tenantBpnl.startsWith("BPNL")) {
            throw IllegalArgumentException("tenantBpnl must not be null or empty and must start with 'BPNL'")
        }
    }

}