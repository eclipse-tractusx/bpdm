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

package org.eclipse.tractusx.bpdm.pool.v6

import jakarta.annotation.PostConstruct
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.client.PoolV6Client
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressSearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.bpn.BpnRequestIdentifierSearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.bpn.IdentifiersSearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.changelog.ChangelogSearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntitySearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.IdentifierBusinessPartnerTypeV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.IdentifierTypeV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.LegalFormV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.participant.CxMembershipSearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.participant.CxMembershipUpdateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteSearchV6Request
import org.eclipse.tractusx.bpdm.pool.v6.util.PoolTestClientProviderV6
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer
import org.eclipse.tractusx.bpdm.test.util.AuthAssertionHelper
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PoolAuthV6IT: UnscheduledPoolTestBaseV6() {

    private val authAssertionHelper = AuthAssertionHelper()

    @Autowired
    lateinit var testClientProvider: PoolTestClientProviderV6

    lateinit var operatorClient: PoolV6Client
    lateinit var sharingMemberClient: PoolV6Client
    lateinit var participantClient: PoolV6Client
    lateinit var unauthorizedClient: PoolV6Client
    lateinit var anonymousClient: PoolV6Client

    @PostConstruct
    fun init(){
        operatorClient = testClientProvider.createClient(KeyCloakInitializer.CLIENT_ID_OPERATOR)
        sharingMemberClient = testClientProvider.createClient(KeyCloakInitializer.CLIENT_ID_SHARING_MEMBER)
        participantClient = testClientProvider.createClient(KeyCloakInitializer.CLIENT_ID_PARTICIPANT)
        unauthorizedClient = testClientProvider.createClient(KeyCloakInitializer.CLIENT_ID_UNAUTHORIZED)
        anonymousClient = testClientProvider.createClient(null)
    }

    @Test
    fun getAddresses(){
        assertAuth(
            AuthExpectationType.Authorized,
            AuthExpectationType.Forbidden
        ){
            poolClient.addresses.getAddresses(AddressSearchV6Request(), PaginationRequest())
        }
    }

    @Test
    fun getAddress(){
        assertAuth(
            AuthExpectationType.Authorized,
            AuthExpectationType.Forbidden
        ){
            poolClient.addresses.getAddress("BPNA")
        }
    }

    @Test
    fun searchAddresses(){
        assertAuth(
            AuthExpectationType.Authorized,
            AuthExpectationType.Forbidden
        ){
            poolClient.addresses.searchAddresses(AddressSearchV6Request(), PaginationRequest())
        }
    }

    @Test
    fun createAddresses(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Forbidden
        ){
            val createRequest = testDataFactory.request.buildAdditionalAddressCreateRequest("seed", "any")
            poolClient.addresses.createAddresses(listOf(createRequest))
        }
    }

    @Test
    fun updateAddresses(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Forbidden
        ){
            val updateRequest = testDataFactory.request.buildAddressUpdateRequest("seed", "any")
            poolClient.addresses.updateAddresses(listOf(updateRequest))
        }
    }

    @Test
    fun findBpnsByIdentifiers(){
        assertAuth(
            AuthExpectationType.Authorized,
            AuthExpectationType.Forbidden
        ){
            val searchRequest = IdentifiersSearchV6Request(IdentifierBusinessPartnerTypeV6.LEGAL_ENTITY, "any", emptyList())
            poolClient.bpns.findBpnsByIdentifiers(searchRequest)
        }
    }

    @Test
    fun findBpnByRequestedIdentifiers(){
        assertAuth(
            AuthExpectationType.Authorized,
            AuthExpectationType.Forbidden
        ){
            val searchRequest = BpnRequestIdentifierSearchV6Request(emptySet())
            poolClient.bpns.findBpnByRequestedIdentifiers(searchRequest)
        }
    }

    @Test
    fun getChangelogEntries(){
        assertAuth(
            AuthExpectationType.Authorized,
            AuthExpectationType.Forbidden
        ){
            poolClient.changelogs.getChangelogEntries(ChangelogSearchV6Request(), PaginationRequest())
        }
    }

    @Test
    fun getMemberships(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Forbidden
        ){
            poolClient.memberships.get(CxMembershipSearchV6Request(), PaginationRequest())
        }
    }

    @Test
    fun putMemberships(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Forbidden
        ){
            poolClient.memberships.put(CxMembershipUpdateV6Request(emptyList()))
        }
    }

    @Test
    fun getLegalEntities(){
        assertAuth(
            AuthExpectationType.Authorized,
            AuthExpectationType.Forbidden
        ){
            poolClient.legalEntities.getLegalEntities(LegalEntitySearchV6Request(), PaginationRequest())
        }
    }

    @Test
    fun getLegalEntity(){
        assertAuth(
            AuthExpectationType.Authorized,
            AuthExpectationType.Forbidden
        ){
            poolClient.legalEntities.getLegalEntity("any", "any")
        }
    }

    @Test
    fun postLegalEntitySearch(){
        assertAuth(
            AuthExpectationType.Authorized,
            AuthExpectationType.Forbidden
        ){
            poolClient.legalEntities.postLegalEntitySearch(LegalEntitySearchV6Request(), PaginationRequest())
        }
    }

    @Test
    fun getLegalEntitySites(){
        assertAuth(
            AuthExpectationType.Authorized,
            AuthExpectationType.Forbidden
        ){
            poolClient.legalEntities.getSites("any", PaginationRequest())
        }
    }

    @Test
    fun getLegalEntityAddresses(){
        assertAuth(
            AuthExpectationType.Authorized,
            AuthExpectationType.Forbidden
        ){
            poolClient.legalEntities.getAddresses("any", PaginationRequest())
        }
    }

    @Test
    fun createLegalEntities(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Forbidden
        ){
            val createRequest = testDataFactory.request.buildLegalEntityCreateRequest("any")
            poolClient.legalEntities.createBusinessPartners(listOf(createRequest))
        }
    }

    @Test
    fun updateLegalEntities(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Forbidden
        ){
            val updateRequest = testDataFactory.request.createLegalEntityUpdateRequest("any", "any")
            poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))
        }
    }

    @Test
    fun searchMemberLegalEntities(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Authorized
        ){
            poolClient.members.searchLegalEntities(LegalEntitySearchV6Request(), PaginationRequest())
        }
    }

    @Test
    fun postMemberSiteSearch(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Authorized
        ){
            poolClient.members.postSiteSearch(SiteSearchV6Request(), PaginationRequest())
        }
    }

    @Test
    fun searchMemberAddresses(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Authorized
        ){
            poolClient.members.searchAddresses(AddressSearchV6Request(), PaginationRequest())
        }
    }

    @Test
    fun searchMemberChangelogEntries(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Authorized
        ){
            poolClient.members.searchChangelogEntries(ChangelogSearchV6Request(), PaginationRequest())
        }
    }

    @Test
    fun createIdentifierType(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Forbidden
        ){
            val identifierType = IdentifierTypeV6Dto("any", IdentifierBusinessPartnerTypeV6.LEGAL_ENTITY, "any", null, null, null, emptySet())
            poolClient.metadata.createIdentifierType(identifierType)
        }
    }

    @Test
    fun getIdentifierTypes(){
        assertAuth(
            AuthExpectationType.Authorized,
            AuthExpectationType.Authorized
        ){
            poolClient.metadata.getIdentifierTypes(PaginationRequest(), IdentifierBusinessPartnerTypeV6.LEGAL_ENTITY, null)
        }
    }

    @Test
    fun createLegalForm(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Forbidden
        ){
            val legalForm = LegalFormV6Request("any", "any", null, null, null, null, null, null, true)
            poolClient.metadata.createLegalForm(legalForm)
        }
    }

    @Test
    fun getLegalForms(){
        assertAuth(
            AuthExpectationType.Authorized,
            AuthExpectationType.Authorized
        ){
            poolClient.metadata.getLegalForms(PaginationRequest())
        }
    }

    @Test
    fun getSite(){
        assertAuth(
            AuthExpectationType.Authorized,
            AuthExpectationType.Forbidden
        ){
            poolClient.sites.getSite("any")
        }
    }

    @Test
    fun postSiteSearch(){
        assertAuth(
            AuthExpectationType.Authorized,
            AuthExpectationType.Forbidden
        ){
            poolClient.sites.postSiteSearch(SiteSearchV6Request(), PaginationRequest())
        }
    }

    @Test
    fun createSite(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Forbidden
        ){
            val createRequest = testDataFactory.request.buildSiteCreateRequest("any", "any")
            poolClient.sites.createSite(listOf(createRequest))
        }
    }

    @Test
    fun updateSite(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Forbidden
        ){
            val updateRequest = testDataFactory.request.createSiteUpdateRequest("any", "any")
            poolClient.sites.updateSite(listOf(updateRequest))
        }
    }

    @Test
    fun getSites(){
        assertAuth(
            AuthExpectationType.Authorized,
            AuthExpectationType.Forbidden
        ){
            poolClient.sites.getSites(SiteSearchV6Request(), PaginationRequest())
        }
    }

    @Test
    fun createSiteWithLegalReference(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Forbidden
        ){
            val createRequest = testDataFactory.request.buildLegalAddressSiteCreateRequest("any", "any")
            poolClient.sites.createSiteWithLegalReference(listOf(createRequest))
        }
    }

    private fun assertAuth(
        sharingMember: AuthExpectationType,
        participant: AuthExpectationType,
        methodUnderTest: () -> Unit
    ){
        val savedPoolClient = poolClient

        poolClient = sharingMemberClient
        authAssertionHelper.assert(sharingMember, methodUnderTest)

        poolClient = participantClient
        authAssertionHelper.assert(participant, methodUnderTest)

        poolClient = operatorClient
        authAssertionHelper.assert(AuthExpectationType.Authorized, methodUnderTest)

        poolClient = unauthorizedClient
        authAssertionHelper.assert(AuthExpectationType.Forbidden, methodUnderTest)

        poolClient = anonymousClient
        authAssertionHelper.assert(AuthExpectationType.Unauthorized, methodUnderTest)

        poolClient = savedPoolClient
    }

}