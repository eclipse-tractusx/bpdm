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

package org.eclipse.tractusx.bpdm.pool.model.parsed.task

import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.parsed.LogisticAddressParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteContentParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteHeaderParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.AddressUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.legalentity.LegalEntityCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.legalentity.LegalEntityUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.SiteCreateWithReferencedAddressAsMainParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.SiteUpdateParsed

/**
 * Everything a golden record task writes, decided against the Pool's state before the task's first write.
 *
 * An existing site and an existing additional address belong to the task's legal entity, no business partner outside
 * the task loses the script variant coverage it is named in, and additional sites are stated only next to a site. A
 * node created by the task has no parent yet, so its parents are the task's own legal entity and site, supplied when the
 * plan is carried out.
 */
data class GoldenRecordTaskParsed(
    val sharingMemberRecordId: String,
    val legalEntity: LegalEntityUpsertParsed,
    val site: SiteUpsertParsed?,
    val additionalAddress: AdditionalAddressUpsertParsed?,
    val additionalSites: AdditionalSitesParsed?
)

sealed interface LegalEntityUpsertParsed {
    val reference: BpnReferenceParsed
    val legalAddressReference: BpnReferenceParsed

    /**
     * The legal entity as it stands before the task, or null when the task creates it.
     */
    val existingLegalEntity: LegalEntityDb?
        get() = when (this) {
            is Unchanged -> legalEntity
            is Update -> update.target
            is Create -> null
        }

    data class Unchanged(
        override val reference: BpnReferenceParsed,
        override val legalAddressReference: BpnReferenceParsed,
        val legalEntity: LegalEntityDb
    ) : LegalEntityUpsertParsed

    data class Create(
        override val reference: BpnReferenceParsed,
        override val legalAddressReference: BpnReferenceParsed,
        val create: LegalEntityCreateParsed
    ) : LegalEntityUpsertParsed

    data class Update(
        override val reference: BpnReferenceParsed,
        override val legalAddressReference: BpnReferenceParsed,
        val update: LegalEntityUpdateParsed
    ) : LegalEntityUpsertParsed
}

sealed interface SiteUpsertParsed {
    val reference: BpnReferenceParsed

    /**
     * The site as it stands before the task, or null when the task creates it.
     */
    val existingSite: SiteDb?
        get() = when (this) {
            is Unchanged -> site
            is Update -> update.target
            is CreateWithOwnMainAddress, is CreateOnLegalAddress, is CreateOnExistingAddress -> null
        }

    data class Unchanged(
        override val reference: BpnReferenceParsed,
        val site: SiteDb
    ) : SiteUpsertParsed

    data class CreateWithOwnMainAddress(
        override val reference: BpnReferenceParsed,
        val mainAddressReference: BpnReferenceParsed,
        val content: SiteContentParsed
    ) : SiteUpsertParsed

    data class CreateOnLegalAddress(
        override val reference: BpnReferenceParsed,
        val header: SiteHeaderParsed
    ) : SiteUpsertParsed

    data class CreateOnExistingAddress(
        override val reference: BpnReferenceParsed,
        val mainAddressReference: BpnReferenceParsed,
        val create: SiteCreateWithReferencedAddressAsMainParsed
    ) : SiteUpsertParsed

    data class Update(
        override val reference: BpnReferenceParsed,
        val mainAddressReference: BpnReferenceParsed,
        val update: SiteUpdateParsed
    ) : SiteUpsertParsed
}

sealed interface AdditionalAddressUpsertParsed {
    val reference: BpnReferenceParsed

    /**
     * The additional address as it stands before the task, or null when the task creates it.
     */
    val existingAddress: LogisticAddressDb?
        get() = when (this) {
            is Update -> update.target
            is Create -> null
        }

    data class Create(
        override val reference: BpnReferenceParsed,
        val content: LogisticAddressParsed
    ) : AdditionalAddressUpsertParsed

    data class Update(
        override val reference: BpnReferenceParsed,
        val update: AddressUpdateParsed
    ) : AdditionalAddressUpsertParsed
}

/**
 * The further sites a golden record task states on its record address, next to the task's own site.
 *
 * Every existing site belongs to the legal entity of the record address, and every site the record address already is
 * the main address of is among them or is the task's own site.
 */
data class AdditionalSitesParsed(
    val existingSites: List<SiteDb>,
    val newSites: List<AdditionalSiteCreateParsed>
)

data class AdditionalSiteCreateParsed(
    val reference: BpnReferenceParsed,
    val header: SiteHeaderParsed
)

/**
 * What a BPN reference of a golden record task names: a BPN, a request identifier no BPN is mapped to yet, or nothing.
 */
sealed interface BpnReferenceParsed {

    data class Existing(val bpn: String) : BpnReferenceParsed

    data class Pending(val requestIdentifier: String) : BpnReferenceParsed

    data object Unstated : BpnReferenceParsed
}
