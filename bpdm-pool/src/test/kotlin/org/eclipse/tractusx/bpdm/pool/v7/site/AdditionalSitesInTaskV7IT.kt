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
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressSearchRequest
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.copyWithBpnRequests
import org.eclipse.tractusx.orchestrator.api.model.AdditionalSite
import org.eclipse.tractusx.orchestrator.api.model.BpnReference
import org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartner
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
        val statedSite = AdditionalSite(
            bpnReference = BpnReference("Additional Site Reference $testName", null, BpnReferenceType.BpnRequestIdentifier),
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
        assertThat(createdSite.bpnReference.referenceType).isEqualTo(BpnReferenceType.Bpn)
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
        val statedSite = AdditionalSite(bpnReference = BpnReference.empty, siteName = "Additional Site $testName")
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
            .copy(additionalSites = listOf(AdditionalSite(BpnReference(existingSiteBpn, null, BpnReferenceType.Bpn), null)))

        //WHEN
        val result = testDataClient.processTask("Second $testName", businessPartner)

        //THEN
        assertThat(result.additionalSites.map { it.bpn() }).containsExactly(existingSiteBpn)
        assertThat(sitesOfMainAddress(result)).containsExactlyInAnyOrder(result.recordSiteBpn(), existingSiteBpn)
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
            .copy(additionalSites = listOf(AdditionalSite(BpnReference(foreignSiteBpn, null, BpnReferenceType.Bpn), null)))

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

    private fun BusinessPartner.underLegalEntity(bpnL: String) =
        copy(legalEntity = legalEntity.copy(bpnReference = BpnReference(bpnL, null, BpnReferenceType.Bpn)))

    private fun BusinessPartner.recordSiteBpn() = site!!.bpnReference.referenceValue!!

    private fun AdditionalSite.bpn() = bpnReference.referenceValue!!

    private fun sitesOfMainAddress(result: BusinessPartner): List<String> {
        val addressBpn = result.site!!.siteMainAddress!!.bpnReference.referenceValue!!
        val address = poolClient.addresses
            .getAddresses(AddressSearchRequest(addressBpns = listOf(addressBpn)), PaginationRequest())
            .content.single().address

        return listOfNotNull(address.bpnSite) + address.additionalSites
    }
}
