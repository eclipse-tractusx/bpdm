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

package org.eclipse.tractusx.bpdm.pool.v6.auth

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.v6.PoolMetadataApi
import org.eclipse.tractusx.bpdm.pool.api.v6.model.IdentifierTypeDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalFormDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalFormRequest
import org.eclipse.tractusx.bpdm.pool.v6.IsPoolV6Test
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType
import org.junit.jupiter.api.Test

interface MetadataApiAuthV6Test: PoolMetadataApi, IsPoolV6Test {

    val expectationCreateIdentifierType: AuthExpectationType
    val expectationGetIdentifierTypes: AuthExpectationType
    val expectationCreateLegalForm: AuthExpectationType
    val expectationGetLegalForms: AuthExpectationType

    @Test
    fun createIdentifierType(){
        val identifierType = IdentifierTypeDto("any", IdentifierBusinessPartnerType.LEGAL_ENTITY, "any", null, null, null, emptySet())
        authAssertionHelper.assert(expectationCreateIdentifierType){ createIdentifierType(identifierType) }
    }

    override fun createIdentifierType(identifierType: IdentifierTypeDto): IdentifierTypeDto {
        return poolClient.metadata.createIdentifierType(identifierType)
    }

    @Test
    fun getIdentifierTypes(){
        authAssertionHelper.assert(expectationGetIdentifierTypes){ getIdentifierTypes(PaginationRequest(), IdentifierBusinessPartnerType.LEGAL_ENTITY, null) }
    }

    override fun getIdentifierTypes(
        paginationRequest: PaginationRequest,
        businessPartnerType: IdentifierBusinessPartnerType,
        country: CountryCode?
    ): PageDto<IdentifierTypeDto> {
        return poolClient.metadata.getIdentifierTypes(paginationRequest, businessPartnerType, country)
    }

    @Test
    fun createLegalForm(){
        val legalForm = LegalFormRequest("any", "any", null, null, null, null, null, null, true)
        authAssertionHelper.assert(expectationCreateLegalForm){ createLegalForm(legalForm) }
    }

    override fun createLegalForm(type: LegalFormRequest): LegalFormDto {
        return poolClient.metadata.createLegalForm(type)
    }

    @Test
    fun getLegalForms(){
        authAssertionHelper.assert(expectationGetLegalForms){ getLegalForms(PaginationRequest()) }
    }

    override fun getLegalForms(paginationRequest: PaginationRequest): PageDto<LegalFormDto> {
        return poolClient.metadata.getLegalForms(paginationRequest)
    }
}