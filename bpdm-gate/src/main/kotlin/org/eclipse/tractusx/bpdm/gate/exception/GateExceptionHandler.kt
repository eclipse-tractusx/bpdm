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
import org.eclipse.tractusx.bpdm.gate.api.model.response.GateErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import java.time.Instant

@ControllerAdvice
class GateExceptionHandler : BpdmExceptionHandler() {

    @ExceptionHandler(BpdmInvalidPartnerUploadException::class)
    fun handleInvalidPartnerUploadException(ex:BpdmInvalidPartnerUploadException, request: WebRequest): ResponseEntity<GateErrorResponse> {
        val errorResponse = createResponse(ex.errors, request)
        return ResponseEntity.status(errorResponse.status).body(errorResponse)
    }

    @ExceptionHandler(BpdmRelationAlreadyExistsException::class)
    fun handleMissingRelationshipException(ex: BpdmRelationAlreadyExistsException, request: WebRequest): ResponseEntity<GateErrorResponse> {
        val errorResponse = createResponse(ex.message ?: "", request, HttpStatus.CONFLICT)
        return ResponseEntity.status(errorResponse.status).body(errorResponse)
    }

    @ExceptionHandler(BpdmInvalidRelationException::class)
    fun handleInvalidRelationException(ex: BpdmInvalidRelationException, request: WebRequest): ResponseEntity<GateErrorResponse> {
        val errorResponse = createResponse(ex.message ?: "", request, HttpStatus.BAD_REQUEST)
        return ResponseEntity.status(errorResponse.status).body(errorResponse)
    }

    @ExceptionHandler(
        BpdmRelationTargetNotFoundException::class,
        BpdmRelationTargetNotFoundException::class,
        BpdmRelationSourceNotFoundException::class,
        BpdmMissingSharingStateException::class,
        BpdmTenantResolutionException::class,
        BpdmMissingRelationException::class,
        BpdmInvalidTenantBpnlException::class
    )
    fun handleGenericBadRequestException(ex: RuntimeException, request: WebRequest): ResponseEntity<GateErrorResponse>{
        val errorResponse = createResponse(ex, request)
        return ResponseEntity.status(errorResponse.status).body(errorResponse)
    }

    private fun createResponse(exception: RuntimeException, request: WebRequest, status: HttpStatus = HttpStatus.BAD_REQUEST): GateErrorResponse{
        return createResponse(listOf(exception.message ?: ""), request, status)
    }

    private fun createResponse(error: String, request: WebRequest, status: HttpStatus = HttpStatus.BAD_REQUEST): GateErrorResponse{
        return createResponse(listOf(error), request, status)
    }

    private fun createResponse(errors: List<String>, request: WebRequest, status: HttpStatus = HttpStatus.BAD_REQUEST): GateErrorResponse{
        return GateErrorResponse(
            timestamp = Instant.now(),
            status = status,
            error = errors,
            path = request.getDescription(false)
        )
    }
}
