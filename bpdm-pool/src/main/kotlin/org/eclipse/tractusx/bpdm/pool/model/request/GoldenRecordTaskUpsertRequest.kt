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


package org.eclipse.tractusx.bpdm.pool.model.request

data class GoldenRecordTaskUpsertRequest(
    val sharingMemberRecordId: String,
    val legalEntity: LegalEntityUpsertRequest,
    val sites: RecordAddressSitesRequest,
    val additionalAddress: AddressUpsertRequest?
)

/**
 * Every site a golden record upsert states on the record address: the site the record is about and the further sites
 * sharing that address.
 *
 * Both halves are stated together because the further sites share the record address *with* this site: a request
 * naming further sites without one of its own is a contradiction, which is only rejectable while both are in view.
 */
data class RecordAddressSitesRequest(
    val recordSite: SiteUpsertRequest?,
    val additionalSites: List<SiteReferenceRequest>
)

enum class UpsertIntent {
    AlwaysWrite,
    WriteOnlyIfAbsent
}

data class LegalEntityUpsertRequest(
    val reference: BpnReferenceRequest,
    val header: LegalEntityHeaderRequest,
    val legalAddress: AddressUpsertRequest,
    val intent: UpsertIntent
)

sealed interface SiteUpsertRequest {
    val reference: BpnReferenceRequest
    val header: SiteHeaderRequest
    val intent: UpsertIntent

    data class WithOwnMainAddress(
        override val reference: BpnReferenceRequest,
        override val header: SiteHeaderRequest,
        override val intent: UpsertIntent,
        val mainAddress: AddressUpsertRequest
    ) : SiteUpsertRequest

    data class WithLegalAddressAsMain(
        override val reference: BpnReferenceRequest,
        override val header: SiteHeaderRequest,
        override val intent: UpsertIntent
    ) : SiteUpsertRequest
}

data class AddressUpsertRequest(
    val reference: BpnReferenceRequest,
    val content: LogisticAddressRequest
)

data class SiteReferenceRequest(
    val reference: BpnReferenceRequest,
    val name: String?
)
