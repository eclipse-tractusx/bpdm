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
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.AddressContentChangeParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.businesspartner.FieldUpdateRaw
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.LogisticAddressParsed
import org.springframework.stereotype.Component

/**
 * Builds an [AddressContentChangeParsed] that fully replaces a logistic address's content from parsed content — every field is
 * [FieldUpdateRaw.Set]. Site membership is not content, so the caller states it: [FieldUpdateRaw.NoOp] leaves it as it stands,
 * [FieldUpdateRaw.Set] replaces it with the stated sites.
 */
@Component
class AddressChangeMapper {

    fun toFullUpdate(content: LogisticAddressParsed, sites: FieldUpdateRaw<List<SiteDb>>) = AddressContentChangeParsed(
        name = FieldUpdateRaw.Set(content.name),
        physicalPostalAddress = FieldUpdateRaw.Set(content.physicalPostalAddress),
        alternativePostalAddress = FieldUpdateRaw.Set(content.alternativePostalAddress),
        confidenceCriteria = FieldUpdateRaw.Set(content.confidenceCriteria),
        identifiers = FieldUpdateRaw.Set(content.identifiers),
        states = FieldUpdateRaw.Set(content.states),
        scriptVariants = FieldUpdateRaw.Set(content.scriptVariants),
        sites = sites
    )
}
