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

package org.eclipse.tractusx.bpdm.pool.v7

import jakarta.annotation.PostConstruct
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.v7.client.PoolV7Client
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressSearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.bpn.BpnRequestIdentifierSearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.bpn.IdentifiersSearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.changelog.ChangelogSearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPropertiesSearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntitySearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.IdentifierBusinessPartnerTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.IdentifierTypeV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.LegalFormV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.participant.DataSpaceParticipantSearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.participant.DataSpaceParticipantUpdateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteSearchV7Request
import org.eclipse.tractusx.bpdm.pool.v7.util.PoolTestClientProviderV7
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer
import org.eclipse.tractusx.bpdm.test.util.AuthAssertionHelper
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean

class PoolAuthV7IT: UnscheduledPoolTestBaseV7() {

    private val authAssertionHelper = AuthAssertionHelper()

    @Autowired
    lateinit var testClientProvider: PoolTestClientProviderV7

    lateinit var operatorClient: PoolV7Client
    lateinit var sharingMemberClient: PoolV7Client
    lateinit var participantClient: PoolV7Client
    lateinit var unauthorizedClient: PoolV7Client
    lateinit var anonymousClient: PoolV7Client

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
            poolClient.addresses.getAddresses(AddressSearchV7Request(), PaginationRequest())
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
            poolClient.addresses.searchAddresses(AddressSearchV7Request(), PaginationRequest())
        }
    }

    @Test
    fun createAddresses(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Forbidden
        ){
            val createRequest = requestFactory.buildAdditionalAddressCreateRequest("seed", "any")
            poolClient.addresses.createAddresses(listOf(createRequest))
        }
    }

    @Test
    fun updateAddresses(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Forbidden
        ){
            val updateRequest = requestFactory.buildAddressUpdateRequest("seed", "any")
            poolClient.addresses.updateAddresses(listOf(updateRequest))
        }
    }

    @Test
    fun findBpnsByIdentifiers(){
        assertAuth(
            AuthExpectationType.Authorized,
            AuthExpectationType.Forbidden
        ){
            val searchRequest = IdentifiersSearchV7Request(IdentifierBusinessPartnerTypeV7.LEGAL_ENTITY, "any", emptyList())
            poolClient.bpns.findBpnsByIdentifiers(searchRequest)
        }
    }

    @Test
    fun findBpnByRequestedIdentifiers(){
        assertAuth(
            AuthExpectationType.Authorized,
            AuthExpectationType.Forbidden
        ){
            val searchRequest = BpnRequestIdentifierSearchV7Request(emptySet())
            poolClient.bpns.findBpnByRequestedIdentifiers(searchRequest)
        }
    }

    @Test
    fun getChangelogEntries(){
        assertAuth(
            AuthExpectationType.Authorized,
            AuthExpectationType.Forbidden
        ){
            poolClient.changelogs.getChangelogEntries(ChangelogSearchV7Request(), PaginationRequest())
        }
    }

    @Test
    fun getParticipation(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Forbidden
        ){
            poolClient.participants.get(DataSpaceParticipantSearchV7Request(), PaginationRequest())
        }
    }

    @Test
    fun putParticipation(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Forbidden
        ){
            poolClient.participants.put(DataSpaceParticipantUpdateV7Request(emptyList()))
        }
    }

    @Test
    fun getLegalEntities(){
        assertAuth(
            AuthExpectationType.Authorized,
            AuthExpectationType.Forbidden
        ){
            poolClient.legalEntities.getLegalEntities(LegalEntitySearchV7Request(), PaginationRequest())
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
            poolClient.legalEntities.postLegalEntitySearch(LegalEntitySearchV7Request(), PaginationRequest())
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
            val createRequest = requestFactory.buildLegalEntityCreateRequest("any")
            poolClient.legalEntities.createBusinessPartners(listOf(createRequest))
        }
    }

    @Test
    fun updateLegalEntities(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Forbidden
        ){
            val updateRequest = requestFactory.buildLegalEntityUpdateRequest("any", "any")
            poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))
        }
    }

    @Test
    fun searchParticipantLegalEntities(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Authorized
        ){
            poolClient.members.searchLegalEntities(LegalEntitySearchV7Request(), PaginationRequest())
        }
    }

    @Test
    fun postParticipantSiteSearch(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Authorized
        ){
            poolClient.members.postSiteSearch(SiteSearchV7Request(), PaginationRequest())
        }
    }

    @Test
    fun searchParticipantAddresses(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Authorized
        ){
            poolClient.members.searchAddresses(AddressSearchV7Request(), PaginationRequest())
        }
    }

    @Test
    fun searchParticipantChangelogEntries(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Authorized
        ){
            poolClient.members.searchChangelogEntries(ChangelogSearchV7Request(), PaginationRequest())
        }
    }

    @Test
    fun createIdentifierType(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Forbidden
        ){
            val identifierType = IdentifierTypeV7Dto("any", IdentifierBusinessPartnerTypeV7.LEGAL_ENTITY, "any", null, null, null, null, sortedSetOf())
            poolClient.metadata.createIdentifierType(identifierType)
        }
    }

    @Test
    fun getIdentifierTypes(){
        assertAuth(
            AuthExpectationType.Authorized,
            AuthExpectationType.Authorized
        ){
            poolClient.metadata.getIdentifierTypes(PaginationRequest(), IdentifierBusinessPartnerTypeV7.LEGAL_ENTITY, null)
        }
    }

    @Test
    fun getFieldQualityRules(){
        assertAuth(
            AuthExpectationType.Authorized,
            AuthExpectationType.Authorized
        ){
            poolClient.metadata.getFieldQualityRules("DE")
        }
    }

    @Bean
    fun getAdminAreasLevel1(){
        assertAuth(
            AuthExpectationType.Authorized,
            AuthExpectationType.Authorized
        ){
            poolClient.metadata.getAdminAreasLevel1(PaginationRequest())
        }
    }

    @Test
    fun createLegalForm(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Forbidden
        ){
            val legalForm = LegalFormV7Request("any", "any", null, null, null, null, null, null, true)
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
            poolClient.sites.postSiteSearch(SiteSearchV7Request(), PaginationRequest())
        }
    }

    @Test
    fun createSite(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Forbidden
        ){
            val createRequest = requestFactory.buildSiteCreateRequest("any", "any")
            poolClient.sites.createSite(listOf(createRequest))
        }
    }

    @Test
    fun updateSite(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Forbidden
        ){
            val updateRequest = requestFactory.createSiteUpdateRequest("any", "any")
            poolClient.sites.updateSite(listOf(updateRequest))
        }
    }

    @Test
    fun getSites(){
        assertAuth(
            AuthExpectationType.Authorized,
            AuthExpectationType.Forbidden
        ){
            poolClient.sites.getSites(SiteSearchV7Request(), PaginationRequest())
        }
    }

    @Test
    fun createSiteWithLegalReference(){
        assertAuth(
            AuthExpectationType.Forbidden,
            AuthExpectationType.Forbidden
        ){
            val createRequest = requestFactory.buildLegalAddressSiteCreateRequest("any", "any")
            poolClient.sites.createSiteWithLegalReference(listOf(createRequest))
        }
    }

    @Test
    fun searchBusinessPartners(){
        assertAuth(
            AuthExpectationType.Authorized,
            AuthExpectationType.Forbidden
        ){
            poolClient.businessPartners.searchBusinessPartners(LegalEntityPropertiesSearchV7Request(
                legalName = null,
                bpn = null,
                streetName = null,
                postalCode = null,
                city = null,
                country = null
            ), emptySet(), PaginationRequest())
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