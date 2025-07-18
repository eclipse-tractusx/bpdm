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

package org.eclipse.tractusx.bpdm.pool.api.model.request

import io.swagger.v3.oas.annotations.media.Schema

data class SiteBpnSearchRequest(

    @Schema(description = "Filter sites that should belong to legal entities (specified by BPNL)", required = false)
    val legalEntities: Collection<String> = emptyList(),

    @Schema(description = "Filter sites by BPNS of sites")
    val sites: Collection<String> = emptyList(),
)
