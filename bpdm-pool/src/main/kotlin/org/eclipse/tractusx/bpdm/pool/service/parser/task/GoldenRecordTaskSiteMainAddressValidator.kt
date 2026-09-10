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

package org.eclipse.tractusx.bpdm.pool.service.parser.task

import org.eclipse.tractusx.bpdm.pool.model.error.GoldenRecordTaskParseError
import org.eclipse.tractusx.bpdm.pool.model.error.SiteMainAddressMissing
import org.eclipse.tractusx.bpdm.pool.service.TaskEntryBpnMapping
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartner
import org.springframework.stereotype.Service

/**
 * The rule that a site a task writes must state the address to write as its main address, unless that address is the
 * legal entity's legal address.
 */
@Service
class GoldenRecordTaskSiteMainAddressValidator {

    /**
     * Reports the (at most one) violation of the rule in [businessPartner]. Empty where the task states no site, or
     * states one it reports as unchanged and therefore does not write.
     */
    fun validate(businessPartner: BusinessPartner, taskEntryBpnMapping: TaskEntryBpnMapping): List<GoldenRecordTaskParseError> {
        val site = businessPartner.site ?: return emptyList()

        val isUnchangedKnownSite = taskEntryBpnMapping.getBpn(site.bpnReference) != null && site.hasChanged == false
        if (isUnchangedKnownSite || site.siteMainIsLegalAddress) return emptyList()

        return if (site.siteMainAddress == null) listOf(SiteMainAddressMissing) else emptyList()
    }
}
