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
 * the parse already holds them.
 */
sealed interface GoldenRecordTaskUpsertParsed {
    val sharingMemberRecordId: String
    val legalEntity: LegalEntityUpsertParsed

    /** A task about a legal entity: the record address is its legal address, and no site sits on that address. */
    data class LegalEntity(
        override val sharingMemberRecordId: String,
        override val legalEntity: LegalEntityUpsertParsed
    ) : GoldenRecordTaskUpsertParsed

    /** A task about a site: the record address is the site's main address, shared with the further sites on it. */
    data class Site(
        override val sharingMemberRecordId: String,
        override val legalEntity: LegalEntityUpsertParsed,
        val sites: RecordAddressSitesParsed
    ) : GoldenRecordTaskUpsertParsed

    /** A task about an address of a legal entity: the record address is that address, and no site sits on it. */
    data class LegalEntityAddress(
        override val sharingMemberRecordId: String,
        override val legalEntity: LegalEntityUpsertParsed,
        val address: AddressUpsertParsed
    ) : GoldenRecordTaskUpsertParsed

    /** A task about an address of a site: the record address is that address, shared with the further sites on it. */
    data class SiteAddress(
        override val sharingMemberRecordId: String,
        override val legalEntity: LegalEntityUpsertParsed,
        val sites: RecordAddressSitesParsed,
        val address: AddressUpsertParsed
    ) : GoldenRecordTaskUpsertParsed
}

sealed interface LegalEntityUpsertParsed {
    val legalEntityReference: BpnReferenceParsed
    val legalAddressReference: BpnReferenceParsed

    data class Unchanged(
        override val legalEntityReference: BpnReferenceParsed,
        override val legalAddressReference: BpnReferenceParsed,
        val existingLegalEntity: LegalEntityDb
    ) : LegalEntityUpsertParsed

    data class Create(
        override val legalEntityReference: BpnReferenceParsed,
        override val legalAddressReference: BpnReferenceParsed,
        val content: LegalEntityContentParsed
    ) : LegalEntityUpsertParsed

    data class Update(
        override val legalEntityReference: BpnReferenceParsed,
        override val legalAddressReference: BpnReferenceParsed,
        val update: LegalEntityUpdateParsed
    ) : LegalEntityUpsertParsed
}

sealed interface SiteUpsertParsed {
    val siteReference: BpnReferenceParsed

    data class Unchanged(
        override val siteReference: BpnReferenceParsed,
        val existingSite: SiteDb
    ) : SiteUpsertParsed

    data class CreateWithOwnMainAddress(
        override val siteReference: BpnReferenceParsed,
        val mainAddressReference: BpnReferenceParsed,
        val content: SiteContentParsed
    ) : SiteUpsertParsed

    data class CreateOnLegalAddress(
        override val siteReference: BpnReferenceParsed,
        val header: SiteHeaderParsed
    ) : SiteUpsertParsed

    data class CreateOnExistingAddress(
        override val siteReference: BpnReferenceParsed,
        val mainAddressReference: BpnReferenceParsed,
        val existingMainAddress: LogisticAddressDb,
        val header: SiteHeaderParsed,
        val mainAddressContent: LogisticAddressParsed
    ) : SiteUpsertParsed

    data class UpdateWithOwnMainAddress(
        override val siteReference: BpnReferenceParsed,
        val mainAddressReference: BpnReferenceParsed,
        val existingSite: SiteDb,
        val content: SiteContentParsed
    ) : SiteUpsertParsed

    data class UpdateOnLegalAddress(
        override val siteReference: BpnReferenceParsed,
        val existingSite: SiteDb,
        val header: SiteHeaderParsed
    ) : SiteUpsertParsed
}

sealed interface AddressUpsertParsed {
    val addressReference: BpnReferenceParsed

    data class Create(
        override val addressReference: BpnReferenceParsed,
        val content: LogisticAddressParsed
    ) : AddressUpsertParsed

    data class Update(
        override val addressReference: BpnReferenceParsed,
        val existingAddress: LogisticAddressDb,
        val content: LogisticAddressParsed
    ) : AddressUpsertParsed
}

/**
 * Every site that ends up on the record address: the site the golden record upsert is about and the further sites
 * sharing that address.
 *
 * [additionalSites] starts as what the request stated and is completed with the sites already bound to that address
 * by their own main-address relation, so it is broader than the list the request carried.
 */
data class RecordAddressSitesParsed(
    val recordSite: SiteUpsertParsed,
    val additionalSites: AdditionalSitesParsed
)

data class AdditionalSitesParsed(
    val existingSites: List<SiteDb>,
    val newSites: List<AdditionalSiteCreateParsed>
)

data class AdditionalSiteCreateParsed(
    val siteReference: BpnReferenceParsed,
    val header: SiteHeaderParsed
)
