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

package org.eclipse.tractusx.bpdm.pool.service.application.v7

import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.AddressResponseMapper
import org.eclipse.tractusx.bpdm.pool.model.request.AddressGetRequest
import org.eclipse.tractusx.bpdm.pool.service.operation.address.AddressGetService
import org.eclipse.tractusx.bpdm.pool.service.parser.AddressGetParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The REST-API boundary for the V7 "get address" operation.
 */
@Service
class AddressGetApplicationV7Service(
    private val addressGetParser: AddressGetParser,
    private val addressGetService: AddressGetService,
    private val addressResponseMapper: AddressResponseMapper
) {

    /**
     * Returns the address with the given BPN and fails with a not-found error when no address carries it.
     */
    @Transactional(readOnly = true)
    fun getAddress(bpna: String): LogisticAddressVerboseDto {
        val criteria = addressGetParser.parse(AddressGetRequest(bpna))
        val address = addressGetService.get(criteria) ?: throw BpdmNotFoundException("Address", criteria.addressBpn)

        return addressResponseMapper.toAddress(address)
    }
}
