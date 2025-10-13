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

package org.eclipse.tractusx.bpdm.gate.controller

import org.eclipse.tractusx.bpdm.gate.api.GatePartnerUploadApi
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.gate.service.PartnerUploadService
import org.eclipse.tractusx.bpdm.gate.util.getCurrentUserBpn
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class PartnerUploadController(
    val partnerUploadService: PartnerUploadService
) : GatePartnerUploadApi {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.UPLOAD_INPUT_PARTNER})")
    override fun uploadPartnerCsvFile(
        file: MultipartFile
    ): ResponseEntity<Collection<BusinessPartnerInputDto>> {
        return when {
            file.isEmpty -> ResponseEntity(HttpStatus.BAD_REQUEST)
            !file.contentType.equals("text/csv", ignoreCase = true) -> ResponseEntity(HttpStatus.BAD_REQUEST)
            else -> partnerUploadService.processFile(file, getCurrentUserBpn())
        }
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.UPLOAD_INPUT_PARTNER})")
    override fun getPartnerCsvTemplate(): ResponseEntity<ByteArrayResource> {
        val resource = partnerUploadService.generatePartnerCsvTemplate()
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=partner-upload-template.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(resource)
    }

}