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


package org.eclipse.tractusx.bpdm.pool.model.parsed

import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb

/**
 * Everything a golden record upsert will do, decided: which records it writes and how, and which it only reads.
 *
 * The variant is the kind of business partner the task is about, and with it the address the record shares and
 * whether further sites can sit on that address. Each partner appears once and a variant maps to exactly one
 * sequence of operation service calls, so no combination of fields can describe a write that cannot be carried out.
 * Parents are not stated on the children: a request has one legal entity and at most one site, so whoever executes
 * the plan already holds them.
 */
sealed interface GoldenRecordUpsertParsed {
    val sharingMemberRecordId: String
    val legalEntity: LegalEntityUpsertPlan

    /** A task about a legal entity: the record address is its legal address, and no site sits on that address. */
    data class LegalEntityRecord(
        override val sharingMemberRecordId: String,
        override val legalEntity: LegalEntityUpsertPlan
    ) : GoldenRecordUpsertParsed

    /** A task about a site: the record address is the site's main address, shared with [additionalSites]. */
    data class SiteRecord(
        override val sharingMemberRecordId: String,
        override val legalEntity: LegalEntityUpsertPlan,
        val site: SiteUpsertPlan,
        val additionalSites: AdditionalSitesPlan
    ) : GoldenRecordUpsertParsed

    /** A task about an address of a legal entity: the record address is that address, and no site sits on it. */
    data class LegalEntityAddressRecord(
        override val sharingMemberRecordId: String,
        override val legalEntity: LegalEntityUpsertPlan,
        val address: AddressUpsertPlan
    ) : GoldenRecordUpsertParsed

    /** A task about an address of a site: the record address is that address, shared with [additionalSites]. */
    data class SiteAddressRecord(
        override val sharingMemberRecordId: String,
        override val legalEntity: LegalEntityUpsertPlan,
        val site: SiteUpsertPlan,
        val address: AddressUpsertPlan,
        val additionalSites: AdditionalSitesPlan
    ) : GoldenRecordUpsertParsed
}

sealed interface LegalEntityUpsertPlan {
    val reference: BpnReferenceParsed
    val legalAddressReference: BpnReferenceParsed

    data class Unchanged(
        override val reference: BpnReferenceParsed,
        override val legalAddressReference: BpnReferenceParsed,
        val target: LegalEntityDb
    ) : LegalEntityUpsertPlan

    data class Create(
        override val reference: BpnReferenceParsed,
        override val legalAddressReference: BpnReferenceParsed,
        val content: LegalEntityContentParsed
    ) : LegalEntityUpsertPlan

    data class Update(
        override val reference: BpnReferenceParsed,
        override val legalAddressReference: BpnReferenceParsed,
        val target: LegalEntityDb,
        val content: LegalEntityContentParsed
    ) : LegalEntityUpsertPlan
}

sealed interface SiteUpsertPlan {
    val reference: BpnReferenceParsed

    data class Unchanged(
        override val reference: BpnReferenceParsed,
        val target: SiteDb
    ) : SiteUpsertPlan

    data class CreateWithOwnMainAddress(
        override val reference: BpnReferenceParsed,
        val mainAddressReference: BpnReferenceParsed,
        val content: SiteContentParsed
    ) : SiteUpsertPlan

    data class CreateOnLegalAddress(
        override val reference: BpnReferenceParsed,
        val header: SiteHeaderParsed
    ) : SiteUpsertPlan

    data class CreateOnExistingAddress(
        override val reference: BpnReferenceParsed,
        val mainAddressReference: BpnReferenceParsed,
        val creation: SiteCreateWithReferencedAddressAsMainParsed
    ) : SiteUpsertPlan

    data class UpdateWithOwnMainAddress(
        override val reference: BpnReferenceParsed,
        val mainAddressReference: BpnReferenceParsed,
        val target: SiteDb,
        val content: SiteContentParsed
    ) : SiteUpsertPlan

    data class UpdateOnLegalAddress(
        override val reference: BpnReferenceParsed,
        val update: SiteUpdateOnLegalAddressParsed
    ) : SiteUpsertPlan
}

sealed interface AddressUpsertPlan {
    val reference: BpnReferenceParsed

    data class Create(
        override val reference: BpnReferenceParsed,
        val content: LogisticAddressParsed
    ) : AddressUpsertPlan

    data class Update(
        override val reference: BpnReferenceParsed,
        val target: LogisticAddressDb,
        val content: LogisticAddressParsed
    ) : AddressUpsertPlan
}

/**
 * The site a golden record upsert writes, together with the further sites its record address ends up shared with.
 *
 * [additionalSites] starts as what the request stated and is completed with the sites already bound to that address
 * by their own main-address relation, so it is broader than the list the request carried.
 */
data class RecordSitePlan(
    val site: SiteUpsertPlan,
    val additionalSites: AdditionalSitesPlan
)

data class AdditionalSitesPlan(
    val existingSites: List<SiteDb>,
    val newSites: List<AdditionalSiteCreatePlan>
)

data class AdditionalSiteCreatePlan(
    val reference: BpnReferenceParsed,
    val header: SiteHeaderParsed
)
