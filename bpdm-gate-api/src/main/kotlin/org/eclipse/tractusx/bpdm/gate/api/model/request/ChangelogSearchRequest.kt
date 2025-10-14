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

package org.eclipse.tractusx.bpdm.gate.api.model.request

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class ChangelogSearchRequest(

    @get:Schema(
        description = "Only changelog entries created after this time. Ignored if empty.",
        example = "2023-03-20T10:23:28.194Z"
    )
    val timestampAfter: Instant? = null,

    @get:ArraySchema(arraySchema = Schema(description = "Only for business partners with the given array of external IDs. Ignored if empty."))
    val externalIds: Set<String>? = emptySet()
)
