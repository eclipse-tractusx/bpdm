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

package org.eclipse.tractusx.bpdm.pool.service.parser

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.model.AddressCoverageWrite
import org.eclipse.tractusx.bpdm.pool.model.PartnerScriptCodes
import org.eclipse.tractusx.bpdm.pool.model.error.ScriptVariantCoverageStillNeeded
import org.eclipse.tractusx.bpdm.pool.model.error.ScriptVariantNotCoveredByAddress
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressPartnerScriptCodeReader
import org.junit.jupiter.api.Test

class ScriptVariantCoverageValidatorTest {

    private val partnerReader = mockk<AddressPartnerScriptCodeReader>()
    private val validator = ScriptVariantCoverageValidator(partnerReader)

    private val sharedAddress = mockk<LogisticAddressDb> {
        every { bpn } returns "BPNA000000000001"
        every { scriptCodes() } returns listOf("Latn", "Cyrl")
    }

    /**
     * GIVEN two entries writing one address, each stating a site that gives up the script the other still carries today
     * WHEN the entries are judged together
     * THEN neither is rejected, because the script is one no partner of the address is named in afterwards
     */
    @Test
    fun `a partner another entry rewrites is judged on the script codes that entry states`() {
        every { partnerReader.storedPartners(sharedAddress, setOf("BPNS000000000001", "BPNS000000000002")) } returns emptyList()

        val errors = validator.validate(
            listOf(
                listOf(rewrittenBy("BPNS000000000001")),
                listOf(rewrittenBy("BPNS000000000002"))
            )
        )

        assertThat(errors).containsExactly(emptyList(), emptyList())
    }

    /**
     * GIVEN an entry adding a site to an address it leaves as it stands, while a partner already on that address is
     * named in a script the address does not cover
     * WHEN the entry is judged
     * THEN only the site it adds is judged, because a write that changes no script code can strand nobody else
     */
    @Test
    fun `a write that leaves the address content alone judges only the partner it states`() {
        val errors = validator.validate(
            listOf(listOf(AddressCoverageWrite.PartnerOnly(sharedAddress, listOf(PartnerScriptCodes(bpn = null, listOf("Latn"))))))
        )

        assertThat(errors).containsExactly(emptyList())
    }

    /**
     * GIVEN an entry narrowing an address to one script while a partner it does not write is still named in another
     * WHEN the entry is judged
     * THEN it is rejected, naming the partner that still needs the script
     */
    @Test
    fun `a partner the entry does not write keeps the coverage it needs`() {
        every { partnerReader.storedPartners(sharedAddress, setOf("BPNS000000000001")) } returns
                listOf(PartnerScriptCodes("BPNL000000000001", listOf("Cyrl")))

        val errors = validator.validate(listOf(listOf(rewrittenBy("BPNS000000000001"))))

        assertThat(errors).containsExactly(listOf(ScriptVariantCoverageStillNeeded("Cyrl", "BPNL000000000001")))
    }

    /**
     * GIVEN an entry creating an address in one script while naming the partner it creates in another
     * WHEN the entry is judged
     * THEN it is rejected, because the address it creates will not carry that script
     */
    @Test
    fun `a created address has to carry the scripts its own partner is named in`() {
        val errors = validator.validate(
            listOf(
                listOf(
                    AddressCoverageWrite.Created(
                        partners = listOf(PartnerScriptCodes(bpn = null, listOf("Cyrl"))),
                        scriptCodes = listOf("Latn")
                    )
                )
            )
        )

        assertThat(errors).containsExactly(listOf(ScriptVariantNotCoveredByAddress("Cyrl")))
    }

    private fun rewrittenBy(siteBpn: String) =
        AddressCoverageWrite.Rewritten(
            address = sharedAddress,
            partners = listOf(PartnerScriptCodes(siteBpn, listOf("Latn"))),
            scriptCodes = listOf("Latn")
        )
}
