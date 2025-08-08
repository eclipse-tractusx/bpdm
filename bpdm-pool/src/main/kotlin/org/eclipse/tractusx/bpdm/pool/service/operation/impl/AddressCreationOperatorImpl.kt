/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.service.operation.impl

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.dto.valid.NewAddress
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.service.BpnIssuingService
import org.eclipse.tractusx.bpdm.pool.service.PartnerChangelogService
import org.eclipse.tractusx.bpdm.pool.service.operation.AddressCreationOperator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AddressCreationOperatorImpl(
    private val bpnIssuingService: BpnIssuingService,
    private val upsertDataMapper: UpsertDataMapper,
    private val logisticAddressRepository: LogisticAddressRepository,
    private val partnerChangelogService: PartnerChangelogService
): AddressCreationOperator {

    @Transactional
    override fun create(requests: List<NewAddress>): List<LogisticAddressDb> {
        val bpns = bpnIssuingService.issueAddressBpns(requests.size)
        return bpns.zip(requests, ::create)
    }

    private fun create(bpn: String, request: NewAddress): LogisticAddressDb{

        val newAddress = LogisticAddressDb(
            bpn = bpn,
            legalEntity = request.parent.legalEntity,
            site = request.parent.site,
            name = request.content.name,
            physicalPostalAddress = request.content.physicalAddress.let(upsertDataMapper::toPhysicalAddress),
            alternativePostalAddress = request.content.alternativeAddress?.let(upsertDataMapper::toAlternativeAddress),
            confidenceCriteria = request.content.confidenceCriteria.let(upsertDataMapper::toConfidence)
        )
        newAddress.identifiers.addAll(request.content.identifiers.map{ upsertDataMapper.toAddressIdentifier(newAddress, it) })
        newAddress.states.addAll(request.content.businessStates.map { upsertDataMapper.toAddressState(newAddress, it) })

        logisticAddressRepository.save(newAddress)
        partnerChangelogService.createChangelogEntry(ChangelogEntryCreateRequest(bpn, ChangelogType.CREATE, BusinessPartnerType.ADDRESS))

        return newAddress
    }
}