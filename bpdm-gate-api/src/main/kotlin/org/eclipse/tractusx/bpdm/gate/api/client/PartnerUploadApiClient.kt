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

package org.eclipse.tractusx.bpdm.gate.api.client

import org.eclipse.tractusx.bpdm.gate.api.ApiCommons
import org.eclipse.tractusx.bpdm.gate.api.GatePartnerUploadApi
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

@HttpExchange
interface PartnerUploadApiClient : GatePartnerUploadApi {

    @PostExchange(
        url = "${ApiCommons.BASE_PATH_V7}/input/partner-upload-process",
        contentType = MediaType.MULTIPART_FORM_DATA_VALUE,
        accept = ["application/json"]
    )
    override fun uploadPartnerCsvFile(
        @RequestPart("file") file: MultipartFile
    ): ResponseEntity<Collection<BusinessPartnerInputDto>>

    @GetExchange(
        url = "${ApiCommons.BASE_PATH_V7}/input/partner-upload-template",
    )
    override fun getPartnerCsvTemplate(): ResponseEntity<ByteArrayResource>

}