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

package org.eclipse.tractusx.bpdm.gate.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.eclipse.tractusx.bpdm.gate.api.GateBusinessPartnerApi.Companion.BUSINESS_PARTNER_PATH
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.PartnerUploadErrorResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile

@RequestMapping(BUSINESS_PARTNER_PATH, produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE])
interface GatePartnerUploadApi {

    companion object{
        const val BUSINESS_PARTNER_PATH = ApiCommons.BASE_PATH
    }

    @Operation(
        summary = "Create or update business partners from uploaded CSV file",
        description = "Create or update generic business partners. " +
                "Updates instead of creating a new business partner if an already existing external ID is used. " +
                "The same external ID may not occur more than once in a single request. " +
                "For file upload request, the maximum number of business partners in file limited to \${bpdm.api.upsert-limit} entries.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Business partners were successfully updated or created"),
            ApiResponse(responseCode = "400", description = "On malformed Business partner upload request",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = PartnerUploadErrorResponse::class)
                )]),
        ]
    )
    @PostMapping("/input/partner-upload-process", consumes = ["multipart/form-data"])
    fun uploadPartnerCsvFile(
        @RequestPart("file") file: MultipartFile
    ): ResponseEntity<Collection<BusinessPartnerInputDto>>

}