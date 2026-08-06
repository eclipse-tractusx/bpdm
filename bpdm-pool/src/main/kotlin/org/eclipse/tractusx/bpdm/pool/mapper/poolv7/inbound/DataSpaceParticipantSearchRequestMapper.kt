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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound

import org.eclipse.tractusx.bpdm.pool.model.request.DataSpaceParticipantSearchRequest
import org.springframework.stereotype.Component
import org.eclipse.tractusx.bpdm.pool.api.model.request.DataSpaceParticipantSearchRequest as DataSpaceParticipantSearchRequestDto

/**
 * Creates data space participant search requests from the v7 API participant search DTO.
 */
@Component
class DataSpaceParticipantSearchRequestMapper {

    /**
     * Returns the search request holding the criteria a client sent.
     */
    fun toSearchRequest(searchRequest: DataSpaceParticipantSearchRequestDto): DataSpaceParticipantSearchRequest =
        DataSpaceParticipantSearchRequest(
            legalEntityBpns = searchRequest.bpnLs,
            isDataSpaceParticipant = searchRequest.isDataSpaceParticipant
        )
}
