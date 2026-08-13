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

package org.eclipse.tractusx.bpdm.pool.service.parser.site

import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.error.SiteNotInAddressLegalEntity
import org.springframework.stereotype.Service

/**
 * The rule that a parent site may only be assigned to an address under the same legal entity. Pure: given already
 * resolved values it decides the rule and never looks anything up, so it is unaware of resolution failures — callers
 * apply it only to resolved inputs (see `crossValidateParseResults`).
 */
@Service
class SiteLegalEntityConsistencyValidator {

    /**
     * Reports the (at most one) violation of the rule. Empty when there is no site to check, no known address legal
     * entity to check against, or the site already belongs to it.
     */
    fun check(addressLegalEntity: LegalEntityDb?, site: SiteDb?): List<SiteNotInAddressLegalEntity> =
        when {
            addressLegalEntity == null || site == null -> emptyList()
            site.legalEntity.bpn != addressLegalEntity.bpn -> listOf(SiteNotInAddressLegalEntity(site.bpn, addressLegalEntity.bpn))
            else -> emptyList()
        }
}
