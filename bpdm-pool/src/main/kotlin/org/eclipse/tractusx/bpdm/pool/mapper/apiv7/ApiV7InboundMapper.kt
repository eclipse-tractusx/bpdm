/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.mapper.apiv7

import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.IdentifierBusinessPartnerTypeV7
import org.eclipse.tractusx.bpdm.pool.model.raw.metadata.FieldQualityRuleSearchRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.metadata.IdentifierTypeSearchRaw
import org.springframework.stereotype.Component

/**
 * Maps API v7 requests to their raw model.
 */
@Component
class ApiV7InboundMapper {

    /**
     * Returns the raw identifier type search criteria a client sent.
     */
    fun toIdentifierTypeSearchRaw(businessPartnerType: IdentifierBusinessPartnerTypeV7, country: String?): IdentifierTypeSearchRaw =
        IdentifierTypeSearchRaw(businessPartnerType = businessPartnerType, country = country)

    /**
     * Returns the raw field quality rule search for [country].
     */
    fun toFieldQualityRuleSearchRaw(country: String): FieldQualityRuleSearchRaw =
        FieldQualityRuleSearchRaw(country = country)
}
