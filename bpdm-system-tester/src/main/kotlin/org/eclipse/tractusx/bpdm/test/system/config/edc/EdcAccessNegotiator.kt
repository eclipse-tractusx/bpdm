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

import com.nimbusds.jwt.JWTParser
import mu.KotlinLogging
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

data class EdcAccess(
    val agreementId: String,
    val transferProcessId: String,
    val dataAddress: EdcDataAddress
)

/**
 * Holds the access to one data offer for the whole run: negotiates it once and keeps its token fresh.
 *
 * Negotiating in the constructor makes one agreement per asset a property of the singleton lifecycle. A failed
 * negotiation is captured rather than thrown, so a run reports every offer it could not reach instead of dying
 * on the first; the client using it fails on its first call, with that failure as the cause.
 */
class EdcAccessNegotiator(
    private val assetName: String,
    private val management: EdcManagementClient,
    private val asset: EdcAssetProperties
) {

    companion object {
        private val logger = KotlinLogging.logger { }

        private val NEGOTIATION_TIMEOUT = Duration.ofMinutes(2)
        private val NEGOTIATION_POLL_INTERVAL = Duration.ofSeconds(2)

        // A call started just under the wire must still carry a token the data plane accepts when it arrives.
        private val REFRESH_MARGIN = Duration.ofSeconds(60)
    }

    private val access: Result<EdcAccess> = runCatching { negotiate() }

    @Volatile
    private var dataAddress: EdcDataAddress? = access.getOrNull()?.dataAddress

    private val unreadableExpiryReported = AtomicBoolean(false)

    init {
        access.fold(
            onSuccess = {
                logger.info {
                    "Negotiated access to the '$assetName' data offer: agreement ${it.agreementId}," +
                            " transfer ${it.transferProcessId}, data plane at ${it.dataAddress.endpoint}"
                }
            },
            onFailure = { logger.warn(it) { "No access to the '$assetName' data offer: ${it.message}" } }
        )
    }

    /** Reports whether the negotiation for this offer succeeded. */
    val isAvailable get() = access.isSuccess

    /** Returns why the negotiation for this offer failed, or null where it did not. */
    val failure: Throwable? get() = access.exceptionOrNull()

    /** Returns the address of the provider's data plane, which every call to this offer goes to. */
    fun dataPlaneEndpoint(): String = accessOrThrow().dataAddress.endpoint

    /** Returns a token the data plane accepts, replacing the held one where it is about to expire. */
    fun currentToken(): String {
        val held = heldAddress()
        return if (isFresh(held)) held.authorization else refreshed(held).authorization
    }

    /** Returns a token fetched after [rejectedToken] was refused, or the token that replaced it in the meantime. */
    fun tokenAfterRejectionOf(rejectedToken: String): String {
        val held = heldAddress()
        return if (held.authorization != rejectedToken) held.authorization else refreshed(held).authorization
    }

    private fun heldAddress() = dataAddress ?: accessOrThrow().dataAddress

    private fun accessOrThrow(): EdcAccess =
        access.getOrElse {
            throw IllegalStateException(
                "The '$assetName' data offer was not negotiated, so its API cannot be reached over the EDC." +
                        " See the startup log for what the negotiation answered.", it
            )
        }

    private fun refreshed(stale: EdcDataAddress): EdcDataAddress = synchronized(this) {
        // Whoever held the monitor first may already have fetched the replacement this call was about to.
        val held = dataAddress
        if (held != null && held !== stale && isFresh(held)) return held

        val transferProcessId = accessOrThrow().transferProcessId
        return management.getDataAddress(transferProcessId).also {
            dataAddress = it
            logger.debug {
                "Refreshed the transfer token of the '$assetName' data offer from transfer $transferProcessId," +
                        " expiring ${expiryOf(it)?.toString() ?: "at no time it states"}"
            }
        }
    }

    private fun negotiate(): EdcAccess {
        val offer = management.requestCatalog(asset)
            ?: error(
                "the provider catalogs no offer matching type '${asset.type}', subject '${asset.subject}'" +
                        " and version '${asset.version}'" +
                        (if (asset.bpnScoped) " for this consumer's BPNL" else "") +
                        " - check that the offer was created for this consumer"
            )

        management.findTransferProcessesOfAsset(offer.assetId).firstOrNull()?.let { existing ->
            return EdcAccess(existing.agreementId, existing.id, management.getDataAddress(existing.id))
        }

        val agreementId = negotiateAgreement(offer)
        val transferProcessId = startAndAwaitTransfer(agreementId)

        return EdcAccess(agreementId, transferProcessId, management.getDataAddress(transferProcessId))
    }

    private fun negotiateAgreement(offer: EdcOffer): String {
        val negotiationId = management.startNegotiation(offer, asset)
        val deadline = Instant.now().plus(NEGOTIATION_TIMEOUT)

        while (true) {
            val negotiation = management.getNegotiation(negotiationId)
            if (negotiation.isFinalized) return negotiation.agreementId
                ?: error("negotiation $negotiationId is finalized but names no agreement")
            check(!negotiation.isTerminated) {
                "negotiation $negotiationId was terminated: ${negotiation.errorDetail ?: "no reason given"}"
            }
            check(Instant.now().isBefore(deadline)) {
                "negotiation $negotiationId did not finalize within $NEGOTIATION_TIMEOUT, last state was ${negotiation.state}"
            }
            Thread.sleep(NEGOTIATION_POLL_INTERVAL.toMillis())
        }
    }

    private fun startAndAwaitTransfer(agreementId: String): String {
        management.startTransfer(agreementId)
        val deadline = Instant.now().plus(NEGOTIATION_TIMEOUT)

        while (true) {
            management.findTransferProcessesOfAgreement(agreementId).firstOrNull()?.let { return it.id }
            check(Instant.now().isBefore(deadline)) {
                "the transfer for agreement $agreementId did not start within $NEGOTIATION_TIMEOUT"
            }
            Thread.sleep(NEGOTIATION_POLL_INTERVAL.toMillis())
        }
    }

    // A token whose expiry cannot be read is reported stale rather than fresh: that costs a refresh per call,
    // but never holds a token past an expiry no one here can see.
    private fun isFresh(address: EdcDataAddress): Boolean {
        val expiresAt = expiryOf(address) ?: return false.also { reportUnreadableExpiryOnce() }
        return Instant.now().plus(REFRESH_MARGIN).isBefore(expiresAt)
    }

    private fun expiryOf(address: EdcDataAddress): Instant? =
        runCatching { JWTParser.parse(address.authorization).jwtClaimsSet.expirationTime }.getOrNull()?.toInstant()

    private fun reportUnreadableExpiryOnce() {
        if (unreadableExpiryReported.compareAndSet(false, true)) {
            logger.warn {
                "The transfer token of the '$assetName' data offer states no expiry this run can read, so every" +
                        " call to it fetches a token of its own"
            }
        }
    }
}
