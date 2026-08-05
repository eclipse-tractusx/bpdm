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

import org.eclipse.tractusx.bpdm.pool.model.request.ChangelogSearchRequest
import org.springframework.stereotype.Component
import org.eclipse.tractusx.bpdm.pool.api.model.request.ChangelogSearchRequest as ChangelogSearchRequestDto

/**
 * Creates changelog search requests from the v7 API changelog search DTO.
 */
@Component
class ChangelogSearchRequestMapper {

    /**
     * Combines the criteria a client sent with the Catena-X member restriction that the endpoint they sent them to
     * imposes.
     */
    fun toSearchRequest(searchRequest: ChangelogSearchRequestDto, isCatenaXMemberData: Boolean?): ChangelogSearchRequest =
        ChangelogSearchRequest(
            bpns = searchRequest.bpns,
            businessPartnerTypes = searchRequest.businessPartnerTypes,
            timestampAfter = searchRequest.timestampAfter,
            isCatenaXMemberData = isCatenaXMemberData
        )
}
