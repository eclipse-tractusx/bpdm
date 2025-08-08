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

package org.eclipse.tractusx.bpdm.pool.adapter.api.v7

import org.eclipse.tractusx.bpdm.pool.adapter.common.ErrorDescriptionMapper
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressCreateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.dto.input.AddressCreate
import org.eclipse.tractusx.bpdm.pool.dto.validation.error.*
import org.eclipse.tractusx.bpdm.pool.service.entry.AdditionalAddressCreationService
import org.eclipse.tractusx.bpdm.pool.service.toCreateResponse
import org.eclipse.tractusx.bpdm.pool.util.mapToNullUnless
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdditionalAddressCreationAdapter(
    private val creationService: AdditionalAddressCreationService,
    private val unformattedDataMapper: UnformattedDataMapper,
    private val errorDescriptionMapper: ErrorDescriptionMapper
) {

    @Transactional
    fun create(requests: List<AddressPartnerCreateRequest>): AddressPartnerCreateResponseWrapper {
        val validationRequests = requests.map { AddressCreate(unformattedDataMapper.toAddressContent(it.address), it.bpnParent) }
        val creationResults =  creationService.create(validationRequests)

        val createdAddresses = creationResults
            .zip(requests){ result, request -> result.validatedValue?.toCreateResponse(request.index) }
            .filterNotNull()

        val errorInfos = creationResults
            .mapToNullUnless { it.errors.isNotEmpty() }
            .zip(requests) { validationResult, request -> validationResult?.errors?.map { ErrorInfo(toErrorCode(it), errorDescriptionMapper.getDescription("", it), request.index) } ?: emptyList() }
            .flatten()

        return AddressPartnerCreateResponseWrapper(createdAddresses, errorInfos)
    }

    private fun toErrorCode(errorCode: AddressCreateContentError): AddressCreateError{
       return when(errorCode){
           is AddressCreateContentError.AddressContent -> with(errorCode.addressContent) {
               when (this) {
                   is AddressContentError.AlternativeAddress -> with(alternativeAddress) {
                       when (this) {
                           is AlternativeAddressError.PostalAddress -> with(postalAddress) {
                               when (this) {
                                   is PostalAddressError.AdminArea -> AddressCreateError.RegionNotFound
                                   else -> null
                               }
                           }
                           else -> null
                       }
                   }
                   is AddressContentError.Identifier -> with(identifierError){ when(this){
                       is IdentifierError.Duplicate -> AddressCreateError.AddressDuplicateIdentifier
                       is IdentifierError.Type -> AddressCreateError.IdentifierNotFound
                       else -> null
                   }
                   }
                   is AddressContentError.PhysicalAddress -> with(physicalAddressError){ when(this){
                       is PhysicalAddressError.PostalAddress -> with(postalAddress){ when(this){
                           is PostalAddressError.AdminArea -> AddressCreateError.RegionNotFound
                           else -> null
                       }
                       }
                       else -> null
                   }
                   }
                   is AddressContentError.IdentifierList -> AddressCreateError.IdentifiersTooMany
                   else -> null
               }
           }
           is AddressCreateContentError.Parent -> with(errorCode.parent){ when(this){
               is AddressParentError.Invalid -> AddressCreateError.BpnNotValid
               is AddressParentError.IsMissing -> AddressCreateError.BpnNotValid
               is AddressParentError.LegalEntity -> AddressCreateError.LegalEntityNotFound
               is AddressParentError.Site -> AddressCreateError.SiteNotFound
           }
           }
       } ?: AddressCreateError.BpnNotValid
    }

}