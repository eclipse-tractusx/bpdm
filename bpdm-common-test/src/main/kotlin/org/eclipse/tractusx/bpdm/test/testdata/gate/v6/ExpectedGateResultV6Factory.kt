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

package org.eclipse.tractusx.bpdm.test.testdata.gate.v6

import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import java.time.Instant

class ExpectedGateResultV6Factory {

    private val anyTime = Instant.MIN

    fun buildBusinessPartnerInputCreateResult(request: BusinessPartnerInputRequest): BusinessPartnerInputDto{
        return with(request){
            BusinessPartnerInputDto(
                externalId = externalId,
                nameParts = nameParts,
                identifiers = identifiers,
                states = states,
                roles = roles,
                isOwnCompanyData = isOwnCompanyData,
                legalEntity = legalEntity,
                site = site,
                address = address,
                externalSequenceTimestamp = externalSequenceTimestamp,
                createdAt = anyTime,
                updatedAt = anyTime
            )
        }
    }
}