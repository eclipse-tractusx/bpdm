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

package org.eclipse.tractusx.bpdm.pool.service.application.v6

import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.CxMembershipUpdateRequestV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.inbound.CxMembershipRequestMapperV6
import org.eclipse.tractusx.bpdm.pool.mapper.shared.outbound.DataSpaceParticipantParseErrorMapper
import org.eclipse.tractusx.bpdm.common.model.parseAndExecuteAllOrNone
import org.eclipse.tractusx.bpdm.pool.service.operation.participation.DataSpaceParticipantUpdateService
import org.eclipse.tractusx.bpdm.pool.service.parser.participation.DataSpaceParticipantUpdateParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The REST-API boundary for the legacy v6 "update Catena-X memberships" operation, using the v6 request shape.
 */
@Service
class CxMembershipUpdateApplicationV6Service(
    private val dataSpaceParticipantUpdateParser: DataSpaceParticipantUpdateParser,
    private val dataSpaceParticipantUpdateService: DataSpaceParticipantUpdateService,
    private val cxMembershipRequestMapperV6: CxMembershipRequestMapperV6,
    private val dataSpaceParticipantParseErrorMapper: DataSpaceParticipantParseErrorMapper
) {

    /**
     * Sets the Catena-X membership of every legal entity the request names, and changes none of them when a single entry
     * cannot be applied.
     */
    @Transactional
    fun updateMemberships(updateRequest: CxMembershipUpdateRequestV6) {
        parseAndExecuteAllOrNone(
            cxMembershipRequestMapperV6.toUpdateRequests(updateRequest),
            dataSpaceParticipantUpdateParser::parse,
            dataSpaceParticipantParseErrorMapper::toUpdateException,
            dataSpaceParticipantUpdateService::update
        )
    }
}
