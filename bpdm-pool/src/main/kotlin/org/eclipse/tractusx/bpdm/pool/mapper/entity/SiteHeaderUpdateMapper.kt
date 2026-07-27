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

package org.eclipse.tractusx.bpdm.pool.mapper.entity

import org.eclipse.tractusx.bpdm.pool.model.FieldUpdate
import org.eclipse.tractusx.bpdm.pool.model.SiteHeaderUpdate
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteHeaderParsed
import org.springframework.stereotype.Component

/**
 * Builds a [SiteHeaderUpdate] that fully replaces a site's header from parsed content — every field is
 * [FieldUpdate.Set].
 */
@Component
class SiteHeaderUpdateMapper {

    fun toFullUpdate(header: SiteHeaderParsed) = SiteHeaderUpdate(
        name = FieldUpdate.Set(header.name),
        confidenceCriteria = FieldUpdate.Set(header.confidenceCriteria),
        states = FieldUpdate.Set(header.states),
        scriptVariants = FieldUpdate.Set(header.scriptVariants)
    )
}
