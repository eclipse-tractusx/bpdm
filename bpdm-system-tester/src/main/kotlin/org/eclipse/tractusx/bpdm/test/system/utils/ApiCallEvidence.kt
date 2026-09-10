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

package org.eclipse.tractusx.bpdm.test.system.utils

import tools.jackson.databind.json.JsonMapper

/**
 * Attaches the API calls a step makes to the running scenario.
 *
 * Xray turns these attachments into step-level evidence on the Jira Test Execution, so a failed run shows the
 * HTTP exchanges that led to the assertion.
 */
class ApiCallEvidence(
    private val jsonMapper: JsonMapper
) {

    /**
     * Attaches one call as pretty-printed JSON of its uri, request and response, named "<method> <path>".
     */
    fun attach(method: String, path: String, request: Any? = null, response: Any? = null) {
        val content = buildMap {
            put("uri", "$method $path")
            if (request != null) put("request", request)
            if (response != null) put("response", response)
        }
        val context = ScenarioContext.current() ?: error("no active scenario to attach '$method $path' to")
        context.scenario.attach(
            jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(content),
            "application/json",
            "$method $path"
        )
    }
}
