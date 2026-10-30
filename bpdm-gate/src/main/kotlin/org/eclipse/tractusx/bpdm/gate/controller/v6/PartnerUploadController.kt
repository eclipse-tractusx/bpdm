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

package org.eclipse.tractusx.bpdm.gate.controller.v6

import org.eclipse.tractusx.bpdm.gate.api.v6.GatePartnerUploadApi
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.gate.controller.TenantResolver
import org.eclipse.tractusx.bpdm.gate.exception.BpdmInvalidPartnerUploadException
import org.eclipse.tractusx.bpdm.gate.mapper.apiv6.ApiV6BusinessPartnerMapper
import org.eclipse.tractusx.bpdm.gate.mapper.apiv7.BusinessPartnerCsvReader
import org.eclipse.tractusx.bpdm.gate.mapper.apiv7.BusinessPartnerCsvTemplateWriter
import org.eclipse.tractusx.bpdm.gate.model.output.businesspartner.BusinessPartnerCsvUpsertResult
import org.eclipse.tractusx.bpdm.gate.service.application.BusinessPartnerCsvUpsertApplicationService
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController("PartnerUploadControllerLegacy")
class PartnerUploadController(
    val csvUpsertApplicationService: BusinessPartnerCsvUpsertApplicationService,
    val csvReader: BusinessPartnerCsvReader,
    val csvTemplateWriter: BusinessPartnerCsvTemplateWriter,
    val apiV6BusinessPartnerMapper: ApiV6BusinessPartnerMapper,
    val tenantResolver: TenantResolver,
) : GatePartnerUploadApi {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.UPLOAD_INPUT_PARTNER})")
    override fun uploadPartnerCsvFile(
        file: MultipartFile
    ): ResponseEntity<Collection<BusinessPartnerInputDto>> {
        if (file.isEmpty || !file.contentType.equals("text/csv", ignoreCase = true))
            return ResponseEntity(HttpStatus.BAD_REQUEST)

        return when (val result = csvUpsertApplicationService.upload(csvReader.read(file), tenantResolver.currentUserBpn())) {
            is BusinessPartnerCsvUpsertResult.Written -> ResponseEntity.ok(result.inputs.map(apiV6BusinessPartnerMapper::toDto))
            is BusinessPartnerCsvUpsertResult.Rejected -> throw BpdmInvalidPartnerUploadException(result.errors.map(apiV6BusinessPartnerMapper::toMessage))
        }
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.UPLOAD_INPUT_PARTNER})")
    override fun getPartnerCsvTemplate(): ResponseEntity<ByteArrayResource> {
        val resource = csvTemplateWriter.write()
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=partner-upload-template.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(resource)
    }

}