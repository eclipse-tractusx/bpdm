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

package org.eclipse.tractusx.bpdm.gate.exception

import org.eclipse.tractusx.bpdm.common.exception.BpdmExceptionHandler
import org.eclipse.tractusx.bpdm.gate.api.model.response.PartnerUploadErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import java.time.Instant

@ControllerAdvice
class GateExceptionHandler : BpdmExceptionHandler() {

    @ExceptionHandler(BpdmInvalidPartnerUploadException::class)
    fun handleInvalidPartnerUploadException(ex:BpdmInvalidPartnerUploadException, request: WebRequest): ResponseEntity<PartnerUploadErrorResponse> {
        val errorResponse = PartnerUploadErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.BAD_REQUEST,
            error = ex.errors,
            path = request.getDescription(false)
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }
}
