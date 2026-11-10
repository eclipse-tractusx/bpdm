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

package org.eclipse.tractusx.bpdm.pool.v7.site

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressSearchV7Request
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.copyWithBpnRequests
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.AdditionalSiteV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BpnReferenceV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BpnReferenceTypeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BusinessPartnerV7Dto
import org.junit.jupiter.api.Test

class AdditionalSitesInTaskV7IT : UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN a golden record task for a site that states a further site of its main address which does not exist yet
     * WHEN the task is processed
     * THEN that site is created on the main address and the address belongs to both sites
     */
    @Test
    fun `create a stated additional site on the record address`() {
        //GIVEN
        val statedSite = AdditionalSiteV7Dto(
            bpnReference = BpnReferenceV7Dto("Additional Site Reference $testName", null, BpnReferenceTypeV7.BpnRequestIdentifier),
            siteName = "Additional Site $testName"
        )
        val businessPartner = orchestratorRequestFactory.buildSiteBusinessPartner(testName)
            .copyWithBpnRequests()
            .copy(additionalSites = listOf(statedSite))

        //WHEN
        val result = testDataClient.processTask(testName, businessPartner)

        //THEN
        val createdSite = result.additionalSites.single()
        assertThat(createdSite.siteName).isEqualTo(statedSite.siteName)
        assertThat(createdSite.bpnReference.referenceType).isEqualTo(BpnReferenceTypeV7.Bpn)
        assertThat(sitesOfMainAddress(result)).containsExactlyInAnyOrder(result.recordSiteBpn(), createdSite.bpn())
    }

    /**
     * GIVEN a golden record task for a site that states the same further site of its main address twice
     * WHEN the task is processed
     * THEN that site is created once and the main address belongs to it and to the site of the record itself
     */
    @Test
    fun `create a stated additional site once when it is stated twice`() {
        //GIVEN
        val statedSite = AdditionalSiteV7Dto(bpnReference = BpnReferenceV7Dto.empty, siteName = "Additional Site $testName")
        val businessPartner = orchestratorRequestFactory.buildSiteBusinessPartner(testName)
            .copyWithBpnRequests()
            .copy(additionalSites = listOf(statedSite, statedSite))

        //WHEN
        val result = testDataClient.processTask(testName, businessPartner)

        //THEN
        val createdSite = result.additionalSites.single()
        assertThat(createdSite.siteName).isEqualTo(statedSite.siteName)
        assertThat(sitesOfMainAddress(result)).containsExactlyInAnyOrder(result.recordSiteBpn(), createdSite.bpn())
    }

    /**
     * GIVEN a golden record task for a site that states a further site of its main address which already exists
     * WHEN the task is processed
     * THEN the main address belongs to that existing site as well, and no second site of that name is created
     */
    @Test
    fun `link a stated additional site that already exists`() {
        //GIVEN
        val existing = testDataClient.processTask("First $testName", orchestratorRequestFactory.buildSiteBusinessPartner("First $testName").copyWithBpnRequests())
        val existingSiteBpn = existing.recordSiteBpn()

        val businessPartner = orchestratorRequestFactory.buildSiteBusinessPartner("Second $testName")
            .copyWithBpnRequests()
            .underLegalEntity(existing.legalEntity.bpnReference.referenceValue!!)
            .copy(additionalSites = listOf(AdditionalSiteV7Dto(BpnReferenceV7Dto(existingSiteBpn, null, BpnReferenceTypeV7.Bpn), null)))

        //WHEN
        val result = testDataClient.processTask("Second $testName", businessPartner)

        //THEN
        assertThat(result.additionalSites.map { it.bpn() }).containsExactly(existingSiteBpn)
        assertThat(sitesOfMainAddress(result)).containsExactlyInAnyOrder(result.recordSiteBpn(), existingSiteBpn)
    }

    /**
     * GIVEN an address that belongs to a further site because an earlier task stated it
     * WHEN the record is processed again without stating that site
     * THEN the site is unlinked and the address belongs to the site of the record alone
     */
    @Test
    fun `unlink a site the record no longer states`() {
        //GIVEN
        val existing = testDataClient.processTask("First $testName", orchestratorRequestFactory.buildSiteBusinessPartner("First $testName").copyWithBpnRequests())
        val existingSiteBpn = existing.recordSiteBpn()

        val stating = orchestratorRequestFactory.buildSiteBusinessPartner("Second $testName")
            .copyWithBpnRequests()
            .underLegalEntity(existing.legalEntity.bpnReference.referenceValue!!)
            .copy(additionalSites = listOf(AdditionalSiteV7Dto(BpnReferenceV7Dto(existingSiteBpn, null, BpnReferenceTypeV7.Bpn), null)))
        val linked = testDataClient.processTask("Second $testName", stating)
        assertThat(sitesOfMainAddress(linked)).containsExactlyInAnyOrder(linked.recordSiteBpn(), existingSiteBpn)

        //WHEN
        val result = testDataClient.processTask("Second $testName", stating.copy(additionalSites = emptyList()))

        //THEN
        assertThat(result.additionalSites).isEmpty()
        assertThat(sitesOfMainAddress(result)).containsExactly(result.recordSiteBpn())
    }

    /**
     * GIVEN an address that is the main address of a site an earlier task created on it
     * WHEN the record is processed again without stating that site
     * THEN the task is resolved as an error, because the site is bound to the address by its own main-address relation
     */
    @Test
    fun `reject dropping a site the record address is the main address of`() {
        //GIVEN
        val statedSite = AdditionalSiteV7Dto(
            bpnReference = BpnReferenceV7Dto("Additional Site Reference $testName", null, BpnReferenceTypeV7.BpnRequestIdentifier),
            siteName = "Additional Site $testName"
        )
        val stating = orchestratorRequestFactory.buildSiteBusinessPartner(testName)
            .copyWithBpnRequests()
            .copy(additionalSites = listOf(statedSite))
        val createdSiteBpn = testDataClient.processTask(testName, stating).additionalSites.single().bpn()

        //WHEN
        val errors = testDataClient.processTaskToErrors(testName, stating.copy(additionalSites = emptyList()))

        //THEN
        assertThat(errors).hasSize(1)
        assertThat(errors.single().description).contains(createdSiteBpn, "must be stated")
    }

    /**
     * GIVEN a golden record task for a site that states a further site belonging to another legal entity
     * WHEN the task is processed
     * THEN the task is resolved as an error and the main address keeps belonging to its own site alone
     */
    @Test
    fun `reject a stated additional site of another legal entity`() {
        //GIVEN
        val foreign = testDataClient.processTask("Foreign $testName", orchestratorRequestFactory.buildSiteBusinessPartner("Foreign $testName").copyWithBpnRequests())
        val foreignSiteBpn = foreign.recordSiteBpn()

        val businessPartner = orchestratorRequestFactory.buildSiteBusinessPartner("Own $testName")
            .copyWithBpnRequests()
            .copy(additionalSites = listOf(AdditionalSiteV7Dto(BpnReferenceV7Dto(foreignSiteBpn, null, BpnReferenceTypeV7.Bpn), null)))

        //WHEN
        val errors = testDataClient.processTaskToErrors("Own $testName", businessPartner)

        //THEN
        assertThat(errors).hasSize(1)
        assertThat(errors.single().description).contains(foreignSiteBpn, "does not belong to legal entity")
    }

    /**
     * GIVEN a golden record task for a site that states no further sites of its main address
     * WHEN the task is processed
     * THEN no additional sites are reported back and the main address belongs to its own site alone
     */
    @Test
    fun `report no additional sites for an address of a single site`() {
        //GIVEN
        val businessPartner = orchestratorRequestFactory.buildSiteBusinessPartner(testName).copyWithBpnRequests()

        //WHEN
        val result = testDataClient.processTask(testName, businessPartner)

        //THEN
        assertThat(result.additionalSites).isEmpty()
        assertThat(sitesOfMainAddress(result)).containsExactly(result.recordSiteBpn())
    }

    /**
     * GIVEN a legal entity whose legal address is the main address of a site
     * WHEN a golden record task without a site of its own is processed for that legal entity
     * THEN no additional sites are reported back, since there is no site of its own for them to be additional to
     */
    @Test
    fun `report no additional sites for a record without a site`() {
        //GIVEN
        val onLegalAddress = testDataClient.processTask(
            "Site $testName",
            orchestratorRequestFactory.buildLegalAddressSiteBusinessPartner("Site $testName").copyWithBpnRequests()
                .let { it.copy(site = it.site!!.copy(scriptVariants = emptyList())) }
        )
        val legalEntityBpn = onLegalAddress.legalEntity.bpnReference.referenceValue!!

        //WHEN
        val businessPartner = orchestratorRequestFactory.buildLegalEntityBusinessPartner("Legal Entity $testName")
            .copyWithBpnRequests()
            .underLegalEntity(legalEntityBpn)
        val result = testDataClient.processTask("Legal Entity $testName", businessPartner)

        //THEN
        assertThat(result.site).isNull()
        assertThat(result.additionalSites).isEmpty()
    }

    private fun BusinessPartnerV7Dto.underLegalEntity(bpnL: String) =
        copy(legalEntity = legalEntity.copy(bpnReference = BpnReferenceV7Dto(bpnL, null, BpnReferenceTypeV7.Bpn)))

    private fun BusinessPartnerV7Dto.recordSiteBpn() = site!!.bpnReference.referenceValue!!

    private fun AdditionalSiteV7Dto.bpn() = bpnReference.referenceValue!!

    private fun sitesOfMainAddress(result: BusinessPartnerV7Dto): List<String> {
        val addressBpn = result.site!!.siteMainAddress!!.bpnReference.referenceValue!!
        val address = poolClient.addresses
            .getAddresses(AddressSearchV7Request(addressBpns = listOf(addressBpn)), PaginationRequest())
            .content.single().address

        return listOfNotNull(address.bpnSite) + address.additionalSites
    }
}
