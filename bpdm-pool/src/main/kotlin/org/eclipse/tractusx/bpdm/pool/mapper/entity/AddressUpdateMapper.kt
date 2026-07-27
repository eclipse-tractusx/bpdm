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

import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.AddressContentUpdate
import org.eclipse.tractusx.bpdm.pool.model.FieldUpdate
import org.eclipse.tractusx.bpdm.pool.model.parsed.LogisticAddressParsed
import org.springframework.stereotype.Component

/**
 * Builds an [AddressContentUpdate] that fully replaces a logistic address's content from parsed content — every field is
 * [FieldUpdate.Set]. An optional [assignToSite] adds site membership; when absent, membership is left untouched.
 */
@Component
class AddressUpdateMapper {

    fun toFullUpdate(content: LogisticAddressParsed, assignToSite: SiteDb? = null) = AddressContentUpdate(
        name = FieldUpdate.Set(content.name),
        physicalPostalAddress = FieldUpdate.Set(content.physicalPostalAddress),
        alternativePostalAddress = FieldUpdate.Set(content.alternativePostalAddress),
        confidenceCriteria = FieldUpdate.Set(content.confidenceCriteria),
        identifiers = FieldUpdate.Set(content.identifiers),
        states = FieldUpdate.Set(content.states),
        scriptVariants = FieldUpdate.Set(content.scriptVariants),
        assignToSite = assignToSite?.let { FieldUpdate.Set(it) } ?: FieldUpdate.NoOp
    )
}
