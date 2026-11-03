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

package org.eclipse.tractusx.bpdm.pool.api.v7.model.error

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.pool.api.v7.exception.ErrorCodeV7

open class EntitiesWithErrorsV7<ENTITY, out ERROR : ErrorCodeV7>(

    @Schema(description = "Successfully created entities")
    open val entities: Collection<ENTITY>,
    @Schema(description = "Errors for not created entities")
    open val errors: Collection<ErrorInfoV7<ERROR>>
) {
    val entityCount: Int
        get() = entities.size
    val errorCount: Int
        get() = errors.size
}
