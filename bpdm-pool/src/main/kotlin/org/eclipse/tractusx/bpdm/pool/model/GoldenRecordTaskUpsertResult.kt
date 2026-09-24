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


package org.eclipse.tractusx.bpdm.pool.model

import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.service.operation.participation.SharingMemberConfidenceService

/**
 * The records a golden record upsert left behind, by the kind of business partner the task was about.
 *
 * The variant matches the parse that produced it, and the record address follows from the records the variant names,
 * so no result can report an address that contradicts the partners beside it.
 */
sealed interface GoldenRecordTaskUpsertResult {
    val legalEntity: UpsertResult<LegalEntityDb>
    val confidenceUpdates: SharingMemberConfidenceService.Result

    /** The address the record shares, and the one its site membership is stated on. */
    val recordAddress: LogisticAddressDb

    /** The site the record is about, where it is about one. */
    val site: UpsertResult<SiteDb>?

    /** The address the record states of its own, beside the legal address and any site main address. */
    val additionalAddress: UpsertResult<LogisticAddressDb>?

    /** The sites that sit on the record address beside the one the record is about, oldest first. */
    val membershipSites: List<SiteDb>
        get() = site?.let { recordSite -> recordAddress.sites.filterNot { it.bpn == recordSite.value.bpn }.sortedBy { it.createdAt } }
            ?: emptyList()

    /** Whether the record's site holds its main address on the legal address, so one address stands for both. */
    val isSiteMainAddressTheLegalAddress: Boolean
        get() = site?.value?.mainAddress?.bpn == legalEntity.value.legalAddress.bpn

    data class LegalEntity(
        override val legalEntity: UpsertResult<LegalEntityDb>,
        override val confidenceUpdates: SharingMemberConfidenceService.Result
    ) : GoldenRecordTaskUpsertResult {
        override val recordAddress: LogisticAddressDb get() = legalEntity.value.legalAddress
        override val site: UpsertResult<SiteDb>? get() = null
        override val additionalAddress: UpsertResult<LogisticAddressDb>? get() = null
    }

    data class Site(
        override val legalEntity: UpsertResult<LegalEntityDb>,
        override val site: UpsertResult<SiteDb>,
        override val confidenceUpdates: SharingMemberConfidenceService.Result
    ) : GoldenRecordTaskUpsertResult {
        override val recordAddress: LogisticAddressDb get() = site.value.mainAddress
        override val additionalAddress: UpsertResult<LogisticAddressDb>? get() = null
    }

    data class LegalEntityAddress(
        override val legalEntity: UpsertResult<LegalEntityDb>,
        override val additionalAddress: UpsertResult<LogisticAddressDb>,
        override val confidenceUpdates: SharingMemberConfidenceService.Result
    ) : GoldenRecordTaskUpsertResult {
        override val recordAddress: LogisticAddressDb get() = additionalAddress.value
        override val site: UpsertResult<SiteDb>? get() = null
    }

    data class SiteAddress(
        override val legalEntity: UpsertResult<LegalEntityDb>,
        override val site: UpsertResult<SiteDb>,
        override val additionalAddress: UpsertResult<LogisticAddressDb>,
        override val confidenceUpdates: SharingMemberConfidenceService.Result
    ) : GoldenRecordTaskUpsertResult {
        override val recordAddress: LogisticAddressDb get() = additionalAddress.value
    }
}
