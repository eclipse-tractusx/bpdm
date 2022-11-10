/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.common.exception

import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import kotlin.reflect.full.findAnnotations


open class BpdmExceptionHandler : ResponseEntityExceptionHandler() {

    private val kotlinLogger = KotlinLogging.logger { }

    @ExceptionHandler(value = [Exception::class])
    protected fun logException(
        ex: Exception, request: WebRequest
    ): ResponseEntity<Any>? {
        logException(ex)
        return handleException(ex, request)
    }

    private fun logException(ex: Throwable) {
        val annotations = ex::class.findAnnotations(ResponseStatus::class)
        if (annotations.isEmpty() || annotations.any { it.value == HttpStatus.INTERNAL_SERVER_ERROR }) {
            //Prints with stack trace
            kotlinLogger.error(ex) { "Caught internal server error" }
        } else {
            //Prints without stack trace
            logger.debug(ex)
        }

    }

}