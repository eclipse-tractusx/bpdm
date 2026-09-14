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


package org.eclipse.tractusx.bpdm.pool.service.parser.task

import org.eclipse.tractusx.bpdm.pool.model.error.GoldenRecordUpsertParseError
import org.eclipse.tractusx.bpdm.pool.model.error.SiteScriptCodeNotStatedByLegalEntity
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityUpsertRequest
import org.eclipse.tractusx.bpdm.pool.model.request.SiteUpsertRequest
import org.springframework.stereotype.Service

/**
 * The rule that a site sharing the legal address is named only in scripts the legal entity is named in.
 *
 * One address carries one set of script variants, and the legal entity's are the ones written to it, so a script the
 * legal entity does not state is a script that address will never cover. A request states both partners at once and
 * is held to that, rather than having the two sets merged for it.
 */
@Service
class SharedLegalAddressScriptCodeValidator {

    /**
     * Reports every script code the site claims that its shared legal address will not carry.
     */
    fun validate(legalEntity: LegalEntityUpsertRequest, site: SiteUpsertRequest?): List<GoldenRecordUpsertParseError> {
        if (site !is SiteUpsertRequest.WithLegalAddressAsMain) return emptyList()

        val statedByLegalEntity = legalEntity.header.scriptVariants.map { it.scriptCode }.toSet()

        return site.header.scriptVariants
            .map { it.scriptCode }
            .distinct()
            .filterNot { it in statedByLegalEntity }
            .map { SiteScriptCodeNotStatedByLegalEntity(it) }
    }
}
