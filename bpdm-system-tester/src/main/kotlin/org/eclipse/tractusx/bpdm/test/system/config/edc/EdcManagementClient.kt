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

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

data class EdcOffer(
    val assetId: String,
    val offerId: String
)

data class EdcNegotiationState(
    val state: String,
    val agreementId: String?,
    val errorDetail: String?
) {
    val isFinalized get() = state == "FINALIZED"

    val isTerminated get() = state == "TERMINATED"
}

data class EdcTransferProcess(
    val id: String,
    val agreementId: String,
    val createdAt: Long
)

data class EdcDataAddress(
    val endpoint: String,
    val authorization: String
)

/**
 * The consumer connector's management API, as far as negotiating access to one data offer needs it.
 *
 * Bodies are built as plain maps and responses read field by field, so that reaching the dataspace costs no EDC
 * dependency. Responses are read leniently: the connector compacts a single-element JSON-LD array into the bare
 * object.
 */
class EdcManagementClient(
    private val webClient: WebClient,
    private val consumer: EdcConsumerProperties
) {

    companion object {
        private val JSON_OBJECT = object : ParameterizedTypeReference<Map<String, Any?>>() {}
        private val JSON_ARRAY = object : ParameterizedTypeReference<List<Map<String, Any?>>>() {}

        private const val EDC_NAMESPACE = "https://w3id.org/edc/v0.0.1/ns/"
    }

    /** Returns the offer the provider catalogs for the asset, or null where it catalogs none. */
    fun requestCatalog(asset: EdcAssetProperties): EdcOffer? {
        val body = mapOf(
            "@context" to mapOf("@vocab" to EDC_NAMESPACE),
            "@type" to "CatalogRequest",
            "counterPartyAddress" to consumer.providerDataspaceApiUrl,
            "counterPartyId" to consumer.providerDid,
            "protocol" to consumer.protocol,
            "querySpec" to mapOf(
                "offset" to 0,
                "limit" to 1,
                "filterExpression" to catalogFilterOf(asset)
            )
        )

        val response = post("/v3/catalog/request", body, JSON_OBJECT)
        val dataset = singleOrNull(response["dataset"]) ?: return null
        val policy = singleOrNull(dataset["hasPolicy"]) ?: return null

        return EdcOffer(assetId = dataset["@id"] as String, offerId = policy["@id"] as String)
    }

    /** Starts a contract negotiation for the offer and returns the id under which to follow it. */
    fun startNegotiation(offer: EdcOffer, asset: EdcAssetProperties): String {
        val body = mapOf(
            "@context" to listOf(
                "https://w3id.org/dspace/2025/1/odrl-profile.jsonld",
                "https://w3id.org/catenax/2025/9/policy/context.jsonld",
                mapOf("@vocab" to EDC_NAMESPACE)
            ),
            "@type" to "ContractRequest",
            "counterPartyAddress" to consumer.providerDataspaceApiUrl,
            "protocol" to consumer.protocol,
            "counterPartyId" to consumer.providerDid,
            "policy" to mapOf(
                "@id" to offer.offerId,
                "@type" to "Offer",
                "permission" to listOf(
                    mapOf(
                        "action" to "use",
                        "constraint" to mapOf(
                            "and" to listOf(
                                mapOf(
                                    "leftOperand" to "FrameworkAgreement",
                                    "operator" to "eq",
                                    "rightOperand" to asset.frameworkAgreement
                                ),
                                mapOf(
                                    "leftOperand" to "UsagePurpose",
                                    "operator" to "isAnyOf",
                                    "rightOperand" to asset.usagePurpose
                                )
                            )
                        )
                    )
                ),
                "target" to offer.assetId,
                "assigner" to consumer.providerDid
            )
        )

        return post("/v3/edrs", body, JSON_OBJECT)["@id"] as String
    }

    /** Returns how far the negotiation has come. */
    fun getNegotiation(negotiationId: String): EdcNegotiationState {
        val response = webClient.get()
            .uri("/v3/contractnegotiations/{id}", negotiationId)
            .retrieve()
            .bodyToMono(JSON_OBJECT)
            .block() ?: error("the management API answered '/v3/contractnegotiations/$negotiationId' with no body")

        return EdcNegotiationState(
            state = response["state"] as? String ?: "UNKNOWN",
            agreementId = response["contractAgreementId"] as? String,
            errorDetail = response["errorDetail"] as? String
        )
    }

    /** Starts a transfer for the agreement and returns the id of the transfer process. */
    fun startTransfer(agreementId: String): String {
        val body = mapOf(
            "@context" to mapOf("@vocab" to EDC_NAMESPACE),
            "@type" to "TransferRequest",
            "contractId" to agreementId,
            "counterPartyAddress" to consumer.providerDataspaceApiUrl,
            "protocol" to consumer.protocol,
            "transferType" to "HttpData-PULL",
            "dataDestination" to mapOf("type" to "HttpProxy")
        )

        return post("/v3/transferprocesses", body, JSON_OBJECT)["@id"] as String
    }

    /** Returns the transfer processes the consumer holds a data reference for under the agreement, newest first. */
    fun findTransferProcessesOfAgreement(agreementId: String) = findTransferProcesses("agreementId", agreementId)

    /**
     * Returns the transfer processes the consumer holds a data reference for on the asset, newest first.
     *
     * An agreement outlives the run that made it, so a re-run reuses one instead of negotiating a second.
     */
    fun findTransferProcessesOfAsset(assetId: String) = findTransferProcesses("assetId", assetId)

    private fun findTransferProcesses(filterField: String, value: String): List<EdcTransferProcess> {
        val body = mapOf(
            "@context" to mapOf("@vocab" to EDC_NAMESPACE),
            "@type" to "QuerySpec",
            "offset" to 0,
            "limit" to 50,
            "filterExpression" to listOf(
                mapOf("operandLeft" to filterField, "operator" to "=", "operandRight" to value)
            )
        )

        return webClient.post()
            .uri("/v3/edrs/request")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(JSON_ARRAY)
            .block()
            .orEmpty()
            .map {
                EdcTransferProcess(
                    id = it["transferProcessId"] as String,
                    agreementId = it["agreementId"] as? String ?: "",
                    createdAt = (it["createdAt"] as? Number)?.toLong() ?: 0L
                )
            }
            .sortedByDescending { it.createdAt }
    }

    /** Returns the data plane address and a token for it, refreshing the token where it has expired. */
    fun getDataAddress(transferProcessId: String): EdcDataAddress {
        val response = webClient.get()
            .uri { builder ->
                builder.path("/v3/edrs/{id}/dataaddress").queryParam("auto_refresh", true).build(transferProcessId)
            }
            .retrieve()
            .bodyToMono(JSON_OBJECT)
            .block() ?: error("the management API answered the data address of transfer '$transferProcessId' with no body")

        return EdcDataAddress(
            endpoint = response["endpoint"] as? String
                ?: error("the data address of transfer '$transferProcessId' names no endpoint"),
            authorization = response["authorization"] as? String
                ?: error("the data address of transfer '$transferProcessId' carries no authorization")
        )
    }

    private fun catalogFilterOf(asset: EdcAssetProperties) = buildList {
        add(filter("'http://purl.org/dc/terms/type'.'@id'", asset.type))
        add(filter("'http://purl.org/dc/terms/subject'.'@id'", asset.subject))
        add(filter("https://w3id.org/catenax/ontology/common#version", asset.version))
        if (asset.bpnScoped) add(filter("${EDC_NAMESPACE}BusinessPartnerNumber", consumer.consumerBpnl))
    }

    private fun filter(operandLeft: String, operandRight: String) =
        mapOf("operandLeft" to operandLeft, "operator" to "=", "operandRight" to operandRight)

    private fun <T : Any> post(path: String, body: Any, responseType: ParameterizedTypeReference<T>): T =
        webClient.post()
            .uri(path)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(responseType)
            .block() ?: error("the management API answered '$path' with no body")

    @Suppress("UNCHECKED_CAST")
    private fun singleOrNull(field: Any?): Map<String, Any?>? =
        when (field) {
            is List<*> -> field.firstOrNull() as? Map<String, Any?>
            is Map<*, *> -> field as Map<String, Any?>
            else -> null
        }
}
