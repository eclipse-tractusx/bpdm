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

package org.eclipse.tractusx.orchestrator.api.client

import org.eclipse.tractusx.orchestrator.api.OriginatorRegistrarApi
import org.eclipse.tractusx.orchestrator.api.model.UpsertOriginRequest
import org.eclipse.tractusx.orchestrator.api.model.UpsertOriginResponse
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.annotation.PutExchange

@HttpExchange(OriginatorRegistrarApi.PRIORITY_INDICATOR_PATH)
interface OriginatorRegistrarApiClient: OriginatorRegistrarApi{

    @PostExchange
    override fun registerOrigin(@RequestBody request: UpsertOriginRequest): UpsertOriginResponse

    @GetExchange("/{originId}")
    override fun fetchOrigin(@PathVariable("originId") originId: String): UpsertOriginResponse

    @PutExchange("/{originId}")
    override fun updateOrigin(@PathVariable("originId") originId: String,
                              @RequestBody request: UpsertOriginRequest): UpsertOriginResponse
}