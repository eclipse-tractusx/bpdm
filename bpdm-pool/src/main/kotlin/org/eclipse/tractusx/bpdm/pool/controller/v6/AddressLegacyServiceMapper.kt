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

package org.eclipse.tractusx.bpdm.pool.controller.v6

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.*
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressCreateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressUpdateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressPartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressPartnerUpdateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.AddressDtoRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.AddressParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateUntypedParentRequest
import org.eclipse.tractusx.bpdm.pool.model.AddressUpdateRequest
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.parseAndExecute
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.service.*
import org.eclipse.tractusx.bpdm.pool.service.operation.AddressCreateService
import org.eclipse.tractusx.bpdm.pool.service.operation.AddressUpdateService
import org.eclipse.tractusx.bpdm.pool.service.parser.AddressUpdateParser
import org.eclipse.tractusx.bpdm.pool.service.parser.UntypedParentAddressCreateParser
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AddressLegacyServiceMapper(
    private val logisticAddressRepository: LogisticAddressRepository,
    private val untypedParentAddressCreateParser: UntypedParentAddressCreateParser,
    private val addressCreateService: AddressCreateService,
    private val addressUpdateParser: AddressUpdateParser,
    private val addressUpdateService: AddressUpdateService,
    private val addressDtoRequestMapper: AddressDtoRequestMapper,
    private val addressParseErrorMapper: AddressParseErrorMapper
) {

    private val logger = KotlinLogging.logger { }

    fun findByBpn(bpn: String): LogisticAddressVerboseDto {
        val address = findAddressByBpn(bpn) ?: throw BpdmNotFoundException("Address", bpn)
        return address.toDto()
    }

    fun findAddressByBpn(bpn: String): LogisticAddressDb? {
        logger.debug { "Executing findAddressByBpn() with parameters $bpn" }
        return logisticAddressRepository.findByBpn(bpn)
    }

    fun LogisticAddressDb.toDto(): LogisticAddressVerboseDto {
        return LogisticAddressVerboseDto(
            bpna = bpn,
            bpnLegalEntity = legalEntity?.bpn,
            bpnSite = mainSite?.bpn,
            createdAt = createdAt,
            updatedAt = updatedAt,
            name = name,
            states = states.map { it.toDto() },
            identifiers = identifiers.map { it.toDto() },
            physicalPostalAddress = physicalPostalAddress.toDto(),
            alternativePostalAddress = alternativePostalAddress?.toDto(),
            confidenceCriteria = confidenceCriteria.toDto(),
            isCatenaXMemberData = legalEntity?.isCatenaXMemberData ?: mainSite?.legalEntity?.isCatenaXMemberData ?: false,
            addressType = getAddressType(this)
        )
    }

    /**
     * Search addresses per page for [searchRequest] and [paginationRequest]
     */
    @Transactional
    fun searchAddresses(searchRequest: AddressSearchRequest, paginationRequest: PaginationRequest): PageDto<LogisticAddressVerboseDto> {

        val spec = Specification.allOf(
            LogisticAddressRepository.byBpns(searchRequest.addressBpns),
            LogisticAddressRepository.bySiteBpns(searchRequest.siteBpns),
            LogisticAddressRepository.byLegalEntityBpns(searchRequest.legalEntityBpns),
            LogisticAddressRepository.byName(searchRequest.name),
            LogisticAddressRepository.byIsMember(searchRequest.isCatenaXMemberData)
        )
        val addressPage = logisticAddressRepository.findAll(spec, paginationRequest.toPageRequest())

        return addressPage.toDto { it.toDto() }
    }

    data class AddressSearchRequest(
        val addressBpns: List<String>?,
        val siteBpns: List<String>?,
        val legalEntityBpns: List<String>?,
        val name: String?,
        val isCatenaXMemberData: Boolean?
    )

    /**
     * `@Transactional` so parse and execute share one persistence context: [UntypedParentAddressCreateParser] resolves
     * the single `bpnParent` into the explicit (legalEntity, site) parents and validates content, then
     * [AddressCreateService] persists the addresses. This border method only maps DTOs and verdicts to the v6 shapes.
     */
    @Transactional
    fun createAddresses(requests: Collection<AddressPartnerCreateRequest>): AddressPartnerCreateResponseWrapper {
        logger.info { "Create ${requests.size} new addresses" }

        val requestList = requests.toList()
        val createRequests = requestList.map {
            AddressCreateUntypedParentRequest(it.bpnParent, addressDtoRequestMapper.toContentRequest(it.address, it.scriptVariants))
        }

        val responses = mutableListOf<AddressPartnerCreateVerboseDto>()
        val errors = mutableListOf<ErrorInfo<AddressCreateError>>()
        requestList.zip(parseAndExecute(createRequests, untypedParentAddressCreateParser::parse, addressCreateService::create)).forEach { (request, result) ->
            when (result) {
                is ParseResult.Success -> responses.add(result.parsed.toCreateResponse(request.index))
                is ParseResult.Failure -> errors.addAll(result.errors.map { addressParseErrorMapper.toCreateErrorInfo(it, request.index) })
            }
        }

        return AddressPartnerCreateResponseWrapper(responses, errors)
    }

    fun LogisticAddressDb.toCreateResponse(index: String?): AddressPartnerCreateVerboseDto {
        return AddressPartnerCreateVerboseDto(
            address = toDto(),
            index = index
        )
    }

    /**
     * `@Transactional` so parse and execute share one persistence context: [AddressUpdateParser] resolves the target
     * entities and validates content, then [AddressUpdateService] mutates their lazy collections. There is no parent to
     * resolve on update.
     */
    @Transactional
    fun updateAddresses(requests: Collection<AddressPartnerUpdateRequest>): AddressPartnerUpdateResponseWrapper {
        logger.info { "Update ${requests.size} business partner addresses" }

        val requestList = requests.toList()
        val updateRequests = requestList.map {
            AddressUpdateRequest(addressBpn = it.bpna, siteBpn = null, content = addressDtoRequestMapper.toContentRequest(it.address, it.scriptVariants))
        }

        val responses = mutableListOf<LogisticAddressVerboseDto>()
        val errors = mutableListOf<ErrorInfo<AddressUpdateError>>()
        requestList.zip(parseAndExecute(updateRequests, addressUpdateParser::parse, addressUpdateService::update)).forEach { (request, result) ->
            when (result) {
                is ParseResult.Success -> responses.add(result.parsed.value.toDto())
                is ParseResult.Failure -> errors.addAll(result.errors.map { addressParseErrorMapper.toUpdateErrorInfo(it, request.bpna) })
            }
        }

        return AddressPartnerUpdateResponseWrapper(responses, errors)
    }
}
