/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package com.catenax.bpdm.bridge.dummy

import com.catenax.bpdm.bridge.dummy.client.BridgeClient
import com.catenax.bpdm.bridge.dummy.testdata.CommonValues
import com.catenax.bpdm.bridge.dummy.testdata.GateRequestValues
import com.catenax.bpdm.bridge.dummy.testdata.GateRequestValues.addressGateInputRequest1
import com.catenax.bpdm.bridge.dummy.util.*
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.request.AddressPartnerBpnSearchRequest
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.request.SiteBpnSearchRequest
import org.eclipse.tractusx.bpdm.common.dto.response.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.StreetGateDto
import org.eclipse.tractusx.bpdm.gate.api.model.request.AddressGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.LegalEntityGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.SiteGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.SharingStateDto
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityMatchVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SitePoolVerboseDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

private val DEFAULT_PAGINATION_REQUEST = PaginationRequest(0, 100)

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class, OpenSearchContextInitializer::class, BpdmPoolContextInitializer::class, BpdmGateContextInitializer::class])
class BridgeSyncIT @Autowired constructor(
    val bridgeClient: BridgeClient,
    val gateClient: GateClient,
    val poolClient: PoolApiClient,
    val testHelpers: TestHelpers
) {

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        testHelpers.createPoolMetadata()
    }

    @Test
    fun `just use API clients`() {
        assertGateChangelogHasCount(0)
        val poolChangelogResponses = poolClient.changelogs.getChangelogEntries(
            paginationRequest = DEFAULT_PAGINATION_REQUEST, changelogSearchRequest = ChangelogSearchRequest(timestampAfter = null, bpns = null)
        )
        assertThat(poolChangelogResponses.contentSize).isZero()
        bridgeClient.bridge().triggerSync()
    }

    @Test
    fun `sync new legal entities`() {
        val gateLegalEntityRequests = listOf(
            GateRequestValues.legalEntityGateInputRequest1,
            GateRequestValues.legalEntityGateInputRequest2,
            GateRequestValues.legalEntityGateInputRequest3
        )
        gateClient.legalEntities.upsertLegalEntities(gateLegalEntityRequests)

        assertGateChangelogHasCount(3)  // 3 LEs
        assertThat(readSuccessfulSharingStatesWithBpn().size).isEqualTo(0)

        // Action: Sync from Gate to Pool and BPN back to Gate
        bridgeClient.bridge().triggerSync()

        // 3 legal entities + 3 addresses
        assertPoolChangelogHasCount(3 + 3)

        // 3 legal entities
        val sharingStatesOkay = readSuccessfulSharingStatesWithBpn()
        assertThat(sharingStatesOkay.size).isEqualTo(3)
        val bpnByExternalId = buildBpnByExternalIdMap(sharingStatesOkay)

        val gateLegalEntityRequestByBpn = gateLegalEntityRequests.associateBy { bpnByExternalId[it.externalId]!! }

        val poolLegalEntityResponses = poolClient.legalEntities.getLegalEntities(
            bpSearchRequest = LegalEntityPropertiesSearchRequest.EmptySearchRequest,
            paginationRequest = DEFAULT_PAGINATION_REQUEST
        )
        assertThat(poolLegalEntityResponses.contentSize).isEqualTo(3)

        val poolLegalEntityByBpn = poolLegalEntityResponses.content.associateBy { it.legalEntity.bpnl }

        val legalEntitiesGateAndPool = gateLegalEntityRequestByBpn.keys
            .map { Pair(gateLegalEntityRequestByBpn[it]!!, poolLegalEntityByBpn[it]!!) }

        assertThat(legalEntitiesGateAndPool.size).isEqualTo(3)
        legalEntitiesGateAndPool.forEach { (gateVersion, poolVersion) ->
            assertEqualLegalEntity(gateVersion, poolVersion)
        }
    }

    @Test
    fun `sync new sites`() {
        // site needs parent legal entity!
        val gateLegalEntityRequests = listOf(
            GateRequestValues.legalEntityGateInputRequest1,
            GateRequestValues.legalEntityGateInputRequest2,
            GateRequestValues.legalEntityGateInputRequest3
        )
        gateClient.legalEntities.upsertLegalEntities(gateLegalEntityRequests)

        val gateSiteRequests = listOf(
            GateRequestValues.siteGateInputRequest1,
            GateRequestValues.siteGateInputRequest2
        )
        gateClient.sites.upsertSites(gateSiteRequests)

        assertGateChangelogHasCount(3 + 2)   // 3 LEs + 2 sites
        assertThat(readSuccessfulSharingStatesWithBpn().size).isEqualTo(0)

        // Action: Sync from Gate to Pool and BPN back to Gate
        bridgeClient.bridge().triggerSync()

        // 3 legal entities + address & 2 sites + address
        assertPoolChangelogHasCount(3 + 3 + 2 + 2)

        // 3 LEs + 2 sites
        val sharingStatesOkay = readSuccessfulSharingStatesWithBpn()
        assertThat(sharingStatesOkay.size).isEqualTo(3 + 2)
        val bpnByExternalId = buildBpnByExternalIdMap(sharingStatesOkay)

        val gateSiteRequestsByBpn = gateSiteRequests.associateBy { bpnByExternalId[it.externalId]!! }

        val poolSiteResponses = poolClient.sites.searchSites(
            siteSearchRequest = SiteBpnSearchRequest(sites = gateSiteRequestsByBpn.keys),
            paginationRequest = DEFAULT_PAGINATION_REQUEST
        )
        assertThat(poolSiteResponses.contentSize).isEqualTo(2)

        val poolSiteByBpn = poolSiteResponses.content.associateBy { it.site.bpns }

        val sitesGateAndPool = gateSiteRequestsByBpn.keys
            .map { Pair(gateSiteRequestsByBpn[it]!!, poolSiteByBpn[it]!!) }

        assertThat(sitesGateAndPool.size).isEqualTo(2)
        sitesGateAndPool.forEach { (gateVersion, poolVersion) ->
            assertEqualSite(gateVersion, poolVersion)
        }
    }

    @Test
    fun `sync new addresses`() {
        // address needs parent legal entity and site!
        val gateLegalEntityRequests = listOf(
            GateRequestValues.legalEntityGateInputRequest1,
        )
        gateClient.legalEntities.upsertLegalEntities(gateLegalEntityRequests)
        val gateSiteRequests = listOf(
            GateRequestValues.siteGateInputRequest1,
        )
        gateClient.sites.upsertSites(gateSiteRequests)

        val gateAddressRequests = listOf(
            addressGateInputRequest1,
            GateRequestValues.addressGateInputRequest2
        )
        gateClient.addresses.upsertAddresses(gateAddressRequests)

        assertGateChangelogHasCount(1 + 1 + 2)  // 1 LE + 1 site + 2 addresses
        assertThat(readSuccessfulSharingStatesWithBpn().size).isEqualTo(0)

        // Action: Sync from Gate to Pool and BPN back to Gate
        bridgeClient.bridge().triggerSync()

        // 1 legal entity + la & 1 site + la  & 2 addresses
        assertPoolChangelogHasCount(1 + 1 + 1 + 1 + 2)

        // 1 LE + 1 site + 2 addresses
        val sharingStatesOkay = readSuccessfulSharingStatesWithBpn()
        assertThat(sharingStatesOkay.size).isEqualTo(1 + 1 + 2)
        val bpnByExternalId = buildBpnByExternalIdMap(sharingStatesOkay)

        val gateAddressRequestsByBpn = gateAddressRequests.associateBy { bpnByExternalId[it.externalId]!! }

        val poolAddressResponses = poolClient.addresses.searchAddresses(
            addressSearchRequest = AddressPartnerBpnSearchRequest(addresses = gateAddressRequestsByBpn.keys),
            paginationRequest = DEFAULT_PAGINATION_REQUEST
        )
        assertThat(poolAddressResponses.contentSize).isEqualTo(2)

        val poolAddressByBpn = poolAddressResponses.content.associateBy { it.bpna }

        val addressesGateAndPool = gateAddressRequestsByBpn.keys
            .map { Pair(gateAddressRequestsByBpn[it]!!, poolAddressByBpn[it]!!) }

        assertThat(addressesGateAndPool.size).isEqualTo(2)
        addressesGateAndPool.forEach { (gateVersion, poolVersion) ->
            assertEqualAddress(gateVersion, poolVersion)
        }
    }

    @Test
    fun `sync updated legal entities`() {
        val gateLegalEntityRequests = listOf(
            GateRequestValues.legalEntityGateInputRequest1,
            GateRequestValues.legalEntityGateInputRequest2,
            GateRequestValues.legalEntityGateInputRequest3
        )
        gateClient.legalEntities.upsertLegalEntities(gateLegalEntityRequests)
        assertGateChangelogHasCount(3)
        // Action: Sync from Gate to Pool and BPN back to Gate
        bridgeClient.bridge().triggerSync()
        assertPoolChangelogHasCount(3 + 3)

        val legalEntitiesFromPool = allLegalEntitiesFromPool()
        assertThat(legalEntitiesFromPool.size).isEqualTo(3)

        val bpnByExternalId = buildBpnByExternalIdMap(readSuccessfulSharingStatesWithBpn())
        val gateLegalEntityRequestByBpn = gateLegalEntityRequests.associateBy { bpnByExternalId[it.externalId]!! }
        legalEntitiesFromPool.forEach { poolLe ->
            assertEqualLegalEntity(gateLegalEntityRequestByBpn[poolLe.legalEntity.bpnl]!!, poolLe)
        }

        // update legal entity and le address and trigger sync
        val leRquestUpdate = GateRequestValues.legalEntityGateInputRequest1.copy(
            legalEntity = GateRequestValues.legalEntity1.copy(legalShortName = "ChangedShortNam"),
            legalAddress = GateRequestValues.address1.copy(
                physicalPostalAddress = GateRequestValues.postalAddress1.copy(
                    street = StreetGateDto(name = "Changed Street Entiy", houseNumber = CommonValues.houseNumber1, direction = CommonValues.direction1),
                ),
            ),
        )
        gateClient.legalEntities.upsertLegalEntities(listOf(leRquestUpdate))
        assertGateChangelogHasCount(3 + 1) // 3 insert + 1 update
        bridgeClient.bridge().triggerSync()
        assertPoolChangelogHasCount(3 + 3 + 1 + 1) // 6 insert (LE/LA) + 6 update (LE/LA) //TODO Check if it is right

        val entitiesFromPoolAFterUpdate = allLegalEntitiesFromPool()
        assertThat(legalEntitiesFromPool.size).isEqualTo(3)
        val gateLegalEntityUpdateRequestsByBpn = listOf(
            leRquestUpdate,
            GateRequestValues.legalEntityGateInputRequest2,
            GateRequestValues.legalEntityGateInputRequest3
        ).associateBy { bpnByExternalId[it.externalId]!! }

        entitiesFromPoolAFterUpdate.forEach { poolLe ->
            assertEqualLegalEntity(gateLegalEntityUpdateRequestsByBpn[poolLe.legalEntity.bpnl]!!, poolLe)
        }
    }

    @Test
    fun `sync update sites`() {
        // site needs parent legal entity!
        val gateLegalEntityRequests = listOf(
            GateRequestValues.legalEntityGateInputRequest1,
            GateRequestValues.legalEntityGateInputRequest2,
            GateRequestValues.legalEntityGateInputRequest3
        )
        gateClient.legalEntities.upsertLegalEntities(gateLegalEntityRequests)

        val gateSiteRequests = listOf(
            GateRequestValues.siteGateInputRequest1,
            GateRequestValues.siteGateInputRequest2
        )
        gateClient.sites.upsertSites(gateSiteRequests)

        assertGateChangelogHasCount(3 + 2)   // 3 LEs + 2 sites
        assertThat(readSuccessfulSharingStatesWithBpn().size).isEqualTo(0)

        // Action: Sync from Gate to Pool and BPN back to Gate
        bridgeClient.bridge().triggerSync()

        // 3 legal entities + 3 la & 2 sites + 2 la
        assertPoolChangelogHasCount(3 + 3 + 2 + 2)

        // 3 LEs + 2 sites
        val sharingStatesOkay = readSuccessfulSharingStatesWithBpn()
        assertThat(sharingStatesOkay.size).isEqualTo(3 + 2)
        val bpnByExternalId = buildBpnByExternalIdMap(sharingStatesOkay)
        val gateSiteRequestsByBpn = gateSiteRequests.associateBy { bpnByExternalId[it.externalId]!! }

        val poolSiteResponses = poolClient.sites.searchSites(
            siteSearchRequest = SiteBpnSearchRequest(sites = gateSiteRequestsByBpn.keys),
            paginationRequest = DEFAULT_PAGINATION_REQUEST
        )
        assertThat(poolSiteResponses.contentSize).isEqualTo(2)
        poolSiteResponses.content.forEach { poolSite ->
            assertEqualSite(gateSiteRequestsByBpn[poolSite.site.bpns]!!, poolSite)
        }

        // update site and main address and trigger sync
        val siteRquestUpdate = GateRequestValues.siteGateInputRequest1.copy(
            site = GateRequestValues.site1.copy(nameParts = listOf("ChangedNamePart1")),
            mainAddress = GateRequestValues.address1.copy(
                physicalPostalAddress = GateRequestValues.postalAddress1.copy(
                    street = StreetGateDto(name = "Changed Street Site", houseNumber = CommonValues.houseNumber1, direction = CommonValues.direction1),
                ),
            )
        )
        gateClient.sites.upsertSites(listOf(siteRquestUpdate))
        bridgeClient.bridge().triggerSync()
        val poolSiteUpdateResponses = poolClient.sites.searchSites(
            siteSearchRequest = SiteBpnSearchRequest(sites = gateSiteRequestsByBpn.keys),
            paginationRequest = DEFAULT_PAGINATION_REQUEST
        )

        val gateSiteUpdateRequestsByBpn = listOf(
            siteRquestUpdate,
            GateRequestValues.siteGateInputRequest2
        ).associateBy { bpnByExternalId[it.externalId]!! }

        poolSiteUpdateResponses.content.forEach { poolSite ->
            assertEqualSite(gateSiteUpdateRequestsByBpn[poolSite.site.bpns]!!, poolSite)
        }

    }

    @Test
    fun `sync update addresses`() {
        // insert le, sites and addresses
        val gateLegalEntityRequests = listOf(
            GateRequestValues.legalEntityGateInputRequest1,
        )
        gateClient.legalEntities.upsertLegalEntities(gateLegalEntityRequests)
        val gateSiteRequests = listOf(
            GateRequestValues.siteGateInputRequest1,
        )
        gateClient.sites.upsertSites(gateSiteRequests)

        val gateAddressRequests = listOf(
            addressGateInputRequest1,
            GateRequestValues.addressGateInputRequest2
        )
        gateClient.addresses.upsertAddresses(gateAddressRequests)

        assertGateChangelogHasCount(1 + 1 + 2)  // 1 LE + 1 site + 2 addresses
        assertThat(readSuccessfulSharingStatesWithBpn().size).isEqualTo(0)

        // Action: Sync from Gate to Pool and BPN back to Gate
        bridgeClient.bridge().triggerSync()

        // 1 legal entity + la + 1 site + la & 2 addresses
        assertPoolChangelogHasCount(1 + 1 + 1 + 1 + 2)

        val bpnByExternalId = buildBpnByExternalIdMap(readSuccessfulSharingStatesWithBpn())
        val gateAddressRequestsByBpn = gateAddressRequests.associateBy { bpnByExternalId[it.externalId]!! }

        val poolAddressResponses = poolClient.addresses.searchAddresses(
            addressSearchRequest = AddressPartnerBpnSearchRequest(addresses = gateAddressRequestsByBpn.keys),
            paginationRequest = DEFAULT_PAGINATION_REQUEST
        ).content
        assertThat(poolAddressResponses.size).isEqualTo(2)

        poolAddressResponses.forEach { poolVersion ->
            assertEqualAddress(gateAddressRequestsByBpn[poolVersion.bpna]!!, poolVersion)
        }

        // update address nad trigger sync
        val addressRequestUpdate = addressGateInputRequest1.copy(
            address = GateRequestValues.address1.copy(
                nameParts = listOf("Changed Address Name"),
                physicalPostalAddress = GateRequestValues.postalAddress1
                    .copy(street = StreetGateDto(name = "UpdateSteet", houseNumber = CommonValues.houseNumber1, direction = CommonValues.direction1))
            ),

            )
        gateClient.addresses.upsertAddresses(listOf(addressRequestUpdate))
        bridgeClient.bridge().triggerSync()
        val poolAddressUpdateResponses = poolClient.addresses.searchAddresses(
            addressSearchRequest = AddressPartnerBpnSearchRequest(addresses = gateAddressRequestsByBpn.keys),
            paginationRequest = DEFAULT_PAGINATION_REQUEST
        ).content

        val gateAddressUpdateRequestsByBpn = listOf(
            addressRequestUpdate,
            GateRequestValues.addressGateInputRequest2
        ).associateBy { bpnByExternalId[it.externalId]!! }

        poolAddressUpdateResponses.forEach { poolVersion ->
            assertEqualAddress(gateAddressUpdateRequestsByBpn[poolVersion.bpna]!!, poolVersion)
        }

    }

    private fun allLegalEntitiesFromPool(): Collection<LegalEntityMatchVerboseDto> {
        val poolLegalEntityResponses = poolClient.legalEntities.getLegalEntities(
            bpSearchRequest = LegalEntityPropertiesSearchRequest.EmptySearchRequest,
            paginationRequest = DEFAULT_PAGINATION_REQUEST
        )
        return poolLegalEntityResponses.content

    }

    private fun buildBpnByExternalIdMap(sharingStatesOkay: List<SharingStateDto>) =
        sharingStatesOkay
            .associateBy { it.externalId }
            .mapValues { it.value.bpn }

    private fun assertGateChangelogHasCount(changelogCount: Int) {
        val gateChangelogResponses = gateClient.changelog.getInputChangelog(
            paginationRequest = DEFAULT_PAGINATION_REQUEST,
            searchRequest = org.eclipse.tractusx.bpdm.gate.api.model.request.ChangelogSearchRequest(timestampAfter = null, businessPartnerTypes = emptySet())
        )
        assertThat(gateChangelogResponses.contentSize).isEqualTo(changelogCount)
    }

    private fun assertPoolChangelogHasCount(changelogCount: Int) {
        val poolChangelogResponses = poolClient.changelogs.getChangelogEntries(
            paginationRequest = DEFAULT_PAGINATION_REQUEST,
            changelogSearchRequest = ChangelogSearchRequest(timestampAfter = null, bpns = null)

        )
        assertThat(poolChangelogResponses.contentSize).isEqualTo(changelogCount)

    }

    private fun readSuccessfulSharingStatesWithBpn(): List<SharingStateDto> {
        val sharingStates = gateClient.sharingState.getSharingStates(
            paginationRequest = DEFAULT_PAGINATION_REQUEST,
            businessPartnerType = null,
            externalIds = null
        )
        val sharingStatesOkay = sharingStates.content
            .filter { it.sharingStateType == SharingStateType.Success && it.bpn != null }
        return sharingStatesOkay
    }

    private fun assertEqualLegalEntity(gateVersion: LegalEntityGateInputRequest, poolVersion: LegalEntityMatchVerboseDto) {
        assertThat(poolVersion.legalEntity.legalShortName).isEqualTo(gateVersion.legalEntity.legalShortName)
        //       assertThat(poolVersion.legalAddress.name).isEqualTo(gateVersion.legalAddress.nameParts.first())
        assertThat(poolVersion.legalAddress.physicalPostalAddress.street?.name).isEqualTo(gateVersion.legalAddress.physicalPostalAddress.street?.name)
        assertThat(poolVersion.legalAddress.physicalPostalAddress.city).isEqualTo(gateVersion.legalAddress.physicalPostalAddress.city)
//        assertThat(poolVersion.legalName).isEqualTo(gateVersion.legalNameParts.first())       // TODO not working, not yet persisted!
        assertThat(poolVersion.legalAddress.alternativePostalAddress?.deliveryServiceNumber).isEqualTo(gateVersion.legalAddress.alternativePostalAddress?.deliveryServiceNumber)
        assertThat(poolVersion.legalAddress.alternativePostalAddress?.city).isEqualTo(gateVersion.legalAddress.alternativePostalAddress?.city)
    }

    private fun assertEqualSite(gateVersion: SiteGateInputRequest, poolVersion: SitePoolVerboseDto) {
        assertThat(poolVersion.site.name).isEqualTo(gateVersion.site.nameParts.first())
        assertThat(poolVersion.site.states.map { it.description }).isEqualTo(gateVersion.site.states.map { it.description })
        assertThat(poolVersion.mainAddress.physicalPostalAddress.street?.name).isEqualTo(gateVersion.mainAddress.physicalPostalAddress.street?.name)
    }

    private fun assertEqualAddress(gateVersion: AddressGateInputRequest, poolVersion: LogisticAddressVerboseDto) {
        assertThat(poolVersion.name).isEqualTo(gateVersion.address.nameParts.first())
        assertThat(poolVersion.physicalPostalAddress.street?.name).isEqualTo(gateVersion.address.physicalPostalAddress.street?.name)
        assertThat(poolVersion.physicalPostalAddress.city).isEqualTo(gateVersion.address.physicalPostalAddress.city)
        assertThat(poolVersion.alternativePostalAddress?.deliveryServiceNumber).isEqualTo(gateVersion.address.alternativePostalAddress?.deliveryServiceNumber)
        assertThat(poolVersion.alternativePostalAddress?.city).isEqualTo(gateVersion.address.alternativePostalAddress?.city)
    }
}
