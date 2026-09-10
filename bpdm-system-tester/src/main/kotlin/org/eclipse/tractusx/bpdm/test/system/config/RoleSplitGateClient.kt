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
import org.eclipse.tractusx.bpdm.gate.api.client.BusinessPartnerApiClient
import org.eclipse.tractusx.bpdm.gate.api.client.ChangelogApiClient
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.client.PartnerUploadApiClient
import org.eclipse.tractusx.bpdm.gate.api.client.RelationApiClient
import org.eclipse.tractusx.bpdm.gate.api.client.RelationChangelogApiClient
import org.eclipse.tractusx.bpdm.gate.api.client.RelationOutputApiClient
import org.eclipse.tractusx.bpdm.gate.api.client.RelationSharingStateApiClient
import org.eclipse.tractusx.bpdm.gate.api.client.SharingStateApiClient
import org.eclipse.tractusx.bpdm.gate.api.client.StatsApiClient
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.ChangelogSearchRequest

/**
 * Sends every Gate call with whichever of two configured credentials is permitted to make it.
 *
 * A Gate whose technical users the Portal manages grants each of them one role only, so input data and the
 * golden record output cannot be reached with the same credential; both credentials have to belong to the same
 * company, as the Gate scopes what a read returns by the BPNL of the token that made it.
 */
class RoleSplitGateClient(
    private val inputCredential: GateClient,
    private val outputCredential: GateClient
) : GateClient {

    override val businessParters: BusinessPartnerApiClient = SplitBusinessPartnerApiClient(inputCredential, outputCredential)

    override val changelog: ChangelogApiClient = SplitChangelogApiClient(inputCredential, outputCredential)

    override val relationChangelog: RelationChangelogApiClient = SplitRelationChangelogApiClient(inputCredential, outputCredential)

    override val sharingState: SharingStateApiClient get() = inputCredential.sharingState

    override val stats: StatsApiClient get() = inputCredential.stats

    override val partnerUpload: PartnerUploadApiClient get() = inputCredential.partnerUpload

    override val relation: RelationApiClient get() = inputCredential.relation

    override val relationSharingState: RelationSharingStateApiClient get() = inputCredential.relationSharingState

    override val relationOutput: RelationOutputApiClient get() = outputCredential.relationOutput
}

/** Splits the business partner API, whose input and output endpoints fall to different credentials. */
private class SplitBusinessPartnerApiClient(
    private val inputCredential: GateClient,
    private val outputCredential: GateClient
) : BusinessPartnerApiClient {

    override fun upsertBusinessPartnersInput(businessPartners: Collection<BusinessPartnerInputRequest>) =
        inputCredential.businessParters.upsertBusinessPartnersInput(businessPartners)

    override fun getBusinessPartnersInput(externalIds: Collection<String>?, paginationRequest: PaginationRequest) =
        inputCredential.businessParters.getBusinessPartnersInput(externalIds, paginationRequest)

    override fun getBusinessPartnersOutput(externalIds: Collection<String>?, paginationRequest: PaginationRequest) =
        outputCredential.businessParters.getBusinessPartnersOutput(externalIds, paginationRequest)
}

/** Splits the business partner changelog API, whose input and output searches fall to different credentials. */
private class SplitChangelogApiClient(
    private val inputCredential: GateClient,
    private val outputCredential: GateClient
) : ChangelogApiClient {

    override fun getInputChangelog(paginationRequest: PaginationRequest, searchRequest: ChangelogSearchRequest) =
        inputCredential.changelog.getInputChangelog(paginationRequest, searchRequest)

    override fun getOutputChangelog(paginationRequest: PaginationRequest, searchRequest: ChangelogSearchRequest) =
        outputCredential.changelog.getOutputChangelog(paginationRequest, searchRequest)
}

/** Splits the relation changelog API, whose input and output searches fall to different credentials. */
private class SplitRelationChangelogApiClient(
    private val inputCredential: GateClient,
    private val outputCredential: GateClient
) : RelationChangelogApiClient {

    override fun getInputChangelog(paginationRequest: PaginationRequest, searchRequest: ChangelogSearchRequest) =
        inputCredential.relationChangelog.getInputChangelog(paginationRequest, searchRequest)

    override fun getOutputChangelog(paginationRequest: PaginationRequest, searchRequest: ChangelogSearchRequest) =
        outputCredential.relationChangelog.getOutputChangelog(paginationRequest, searchRequest)
}
