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

package org.eclipse.tractusx.bpdm.pool.util

import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult


inline fun <reified T : Any> WebTestClient.invokeGetEndpoint(path: String, vararg params: Pair<String, String>): T {
    return get().uri { builder ->
        var b = builder.path(path)
        params.forEach { p -> b = b.queryParam(p.first, p.second) }
        b.build()
    }
        .exchange()
        .expectStatus().is2xxSuccessful
        .returnResult<T>()
        .responseBody
        .blockFirst()!!
}

