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

package org.eclipse.tractusx.bpdm.pool.api.model.request

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogSubject
import java.time.Instant


@Schema(name = "ChangeLogSearchRequest", description = "Request for searching and filtering the business partner changelog")
data class ChangelogSearchRequest(
    @Schema(description = "Changelog entries should be created after this time", example = "2023-03-20T10:23:28.194Z")
    val fromTime: Instant? = null,
    @Schema(description = "Only show changelog entries for business partners with the given BPNs. Empty means no restriction.")
    val bpns: Set<String>? = null,
    @Schema(description = "Only show changelog entries for business partners with the given LSA types. Empty means no restriction.")
    val lsaTypes: Set<ChangelogSubject>? = null
)
