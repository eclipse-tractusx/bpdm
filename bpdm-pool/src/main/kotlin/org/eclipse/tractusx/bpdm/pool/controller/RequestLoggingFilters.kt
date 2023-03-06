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

package org.eclipse.tractusx.bpdm.pool.controller

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.CommonsRequestLoggingFilter

@Component
class RequestLoggingFilters : CommonsRequestLoggingFilter() {

    private val logger = LoggerFactory.getLogger(RequestLoggingFilters::class.java)

    init {
        setIncludeQueryString(true)
        setIncludePayload(true)
        setMaxPayloadLength(10000)
        setIncludeHeaders(false)
        setBeforeMessagePrefix("Request: ")
    }

    override fun beforeRequest(request: HttpServletRequest, message: String) {
        logger.info(message)
        val urlWithParams = request.requestURL.toString() + "?" + request.queryString
        logger.info("URL: $urlWithParams")
        logger.info("Headers: ${request.getHeaderNames().toList().joinToString(", ")}")
        super.beforeRequest(request, message)
    }

    override fun afterRequest(request: HttpServletRequest, message: String) {
        // Do nothing
    }


}
