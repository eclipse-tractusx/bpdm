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

package org.eclipse.tractusx.bpdm.test.system.config.edc

import org.eclipse.tractusx.bpdm.common.util.BpdmClientProperties

/** The consumer connector a client negotiates through, and the provider it negotiates with. */
data class EdcConsumerProperties(
    val managementApiUrl: String = "",
    val apiKey: String = "",
    val providerDataspaceApiUrl: String = "",
    val providerDid: String = "",
    val consumerBpnl: String = "",
    val protocol: String = "dataspace-protocol-http:2025-1"
)

/**
 * The catalog filter that selects one data offer, and the policy the consumer accepts for it.
 *
 * The taxonomy IRIs and the version are matched against the asset's properties, so they have to name the
 * asset exactly as the provider created it. A Gate asset additionally carries the sharing member's BPNL,
 * which is what [bpnScoped] adds to the filter; the Pool asset is offered to every dataspace participant and
 * must not be filtered by it.
 */
data class EdcAssetProperties(
    val type: String = "",
    val subject: String = "",
    val version: String = "7",
    val usagePurpose: String = "",
    val frameworkAgreement: String = "DataExchangeGovernance:1.0",
    val bpnScoped: Boolean = false
)

/**
 * Whether a client reaches its API over an EDC, and through which offer.
 *
 * [enabled] is a scalar rather than the presence of the block on purpose: Spring cannot unset a nested object
 * from a source of higher precedence, so a profile that configured EDC access could never be turned off again
 * from the environment or the command line. As a boolean the route can be switched either way from any
 * source, which is what lets one build compare an EDC run against a direct one.
 */
data class EdcClientProperties(
    val enabled: Boolean = false,
    val consumer: EdcConsumerProperties = EdcConsumerProperties(),
    val asset: EdcAssetProperties = EdcAssetProperties()
) {

    /**
     * Fails unless every value the negotiation needs is present, naming the properties that are not.
     *
     * The block is bound whether or not it is enabled, so a half-written EDC section is no longer caught by
     * its absence. Reporting all of them at once spares the reader one startup failure per missing value.
     */
    fun validate(propertyPrefix: String) {
        if (!enabled) return

        val missing = buildList {
            if (consumer.managementApiUrl.isBlank()) add("consumer.management-api-url")
            if (consumer.apiKey.isBlank()) add("consumer.api-key")
            if (consumer.providerDataspaceApiUrl.isBlank()) add("consumer.provider-dataspace-api-url")
            if (consumer.providerDid.isBlank()) add("consumer.provider-did")
            if (consumer.consumerBpnl.isBlank()) add("consumer.consumer-bpnl")
            if (asset.type.isBlank()) add("asset.type")
            if (asset.subject.isBlank()) add("asset.subject")
            if (asset.usagePurpose.isBlank()) add("asset.usage-purpose")
        }

        check(missing.isEmpty()) {
            "'$propertyPrefix.edc' is enabled but incomplete: ${missing.joinToString(", ") { "$propertyPrefix.edc.$it" }}." +
                    " Set them, or set '$propertyPrefix.edc.enabled' to false to reach the API directly instead."
        }
    }
}

/** A client that can reach its API over an EDC instead of with credentials of its own. */
interface EdcCapableClientProperties : BpdmClientProperties {

    val edc: EdcClientProperties
}
