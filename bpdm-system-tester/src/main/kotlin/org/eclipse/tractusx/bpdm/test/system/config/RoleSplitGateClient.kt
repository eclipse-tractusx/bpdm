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

package org.eclipse.tractusx.bpdm.test.system.config

import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.v7.client.BusinessPartnerV7ApiClient
import org.eclipse.tractusx.bpdm.gate.api.v7.client.ChangelogV7ApiClient
import org.eclipse.tractusx.bpdm.gate.api.v7.client.GateV7Client
import org.eclipse.tractusx.bpdm.gate.api.v7.client.PartnerUploadV7ApiClient
import org.eclipse.tractusx.bpdm.gate.api.v7.client.RelationChangelogV7ApiClient
import org.eclipse.tractusx.bpdm.gate.api.v7.client.RelationOutputV7ApiClient
import org.eclipse.tractusx.bpdm.gate.api.v7.client.RelationSharingStateV7ApiClient
import org.eclipse.tractusx.bpdm.gate.api.v7.client.RelationV7ApiClient
import org.eclipse.tractusx.bpdm.gate.api.v7.client.SharingStateV7ApiClient
import org.eclipse.tractusx.bpdm.gate.api.v7.client.StatsV7ApiClient
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.input.BusinessPartnerInputV7Request
import org.eclipse.tractusx.bpdm.gate.api.v7.model.changelog.ChangelogSearchV7Request

/**
 * Sends every Gate call with whichever of two configured credentials is permitted to make it.
 *
 * A Gate whose technical users the Portal manages grants each of them one role only, so input data and the
 * golden record output cannot be reached with the same credential; both credentials have to belong to the same
 * company, as the Gate scopes what a read returns by the BPNL of the token that made it.
 */
class RoleSplitGateClient(
    private val inputCredential: GateV7Client,
    private val outputCredential: GateV7Client
) : GateV7Client {

    override val businessParters: BusinessPartnerV7ApiClient = SplitBusinessPartnerApiClient(inputCredential, outputCredential)

    override val changelog: ChangelogV7ApiClient = SplitChangelogApiClient(inputCredential, outputCredential)

    override val relationChangelog: RelationChangelogV7ApiClient = SplitRelationChangelogApiClient(inputCredential, outputCredential)

    override val sharingState: SharingStateV7ApiClient get() = inputCredential.sharingState

    override val stats: StatsV7ApiClient get() = inputCredential.stats

    override val partnerUpload: PartnerUploadV7ApiClient get() = inputCredential.partnerUpload

    override val relation: RelationV7ApiClient get() = inputCredential.relation

    override val relationSharingState: RelationSharingStateV7ApiClient get() = inputCredential.relationSharingState

    override val relationOutput: RelationOutputV7ApiClient get() = outputCredential.relationOutput
}

/** Splits the business partner API, whose input and output endpoints fall to different credentials. */
private class SplitBusinessPartnerApiClient(
    private val inputCredential: GateV7Client,
    private val outputCredential: GateV7Client
) : BusinessPartnerV7ApiClient {

    override fun upsertBusinessPartnersInput(businessPartners: Collection<BusinessPartnerInputV7Request>) =
        inputCredential.businessParters.upsertBusinessPartnersInput(businessPartners)

    override fun getBusinessPartnersInput(externalIds: Collection<String>?, paginationRequest: PaginationRequest) =
        inputCredential.businessParters.getBusinessPartnersInput(externalIds, paginationRequest)

    override fun getBusinessPartnersOutput(externalIds: Collection<String>?, paginationRequest: PaginationRequest) =
        outputCredential.businessParters.getBusinessPartnersOutput(externalIds, paginationRequest)
}

/** Splits the business partner changelog API, whose input and output searches fall to different credentials. */
private class SplitChangelogApiClient(
    private val inputCredential: GateV7Client,
    private val outputCredential: GateV7Client
) : ChangelogV7ApiClient {

    override fun getInputChangelog(paginationRequest: PaginationRequest, searchRequest: ChangelogSearchV7Request) =
        inputCredential.changelog.getInputChangelog(paginationRequest, searchRequest)

    override fun getOutputChangelog(paginationRequest: PaginationRequest, searchRequest: ChangelogSearchV7Request) =
        outputCredential.changelog.getOutputChangelog(paginationRequest, searchRequest)
}

/** Splits the relation changelog API, whose input and output searches fall to different credentials. */
private class SplitRelationChangelogApiClient(
    private val inputCredential: GateV7Client,
    private val outputCredential: GateV7Client
) : RelationChangelogV7ApiClient {

    override fun getInputChangelog(paginationRequest: PaginationRequest, searchRequest: ChangelogSearchV7Request) =
        inputCredential.relationChangelog.getInputChangelog(paginationRequest, searchRequest)

    override fun getOutputChangelog(paginationRequest: PaginationRequest, searchRequest: ChangelogSearchV7Request) =
        outputCredential.relationChangelog.getOutputChangelog(paginationRequest, searchRequest)
}
