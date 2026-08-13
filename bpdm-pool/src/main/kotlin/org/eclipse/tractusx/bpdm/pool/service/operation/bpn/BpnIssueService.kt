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

package org.eclipse.tractusx.bpdm.pool.service.operation.bpn

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.pool.entity.ConfigurationEntryDb
import org.eclipse.tractusx.bpdm.pool.exception.BpnInvalidCounterValueException
import org.eclipse.tractusx.bpdm.pool.exception.BpnMaxNumberReachedException
import org.eclipse.tractusx.bpdm.pool.repository.ConfigurationEntryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.pow
import kotlin.text.iterator

/**
 * The single authority for issuing BPNs: advances the stored counter of the requested business partner kind and turns
 * each number it reserves into a check-summed BPN. Every BPN it hands back is new, so a caller that discards one leaves
 * a permanent gap in the sequence.
 */
@Service
class BpnIssueService(
    private val configurationEntryRepository: ConfigurationEntryRepository,
    private val bpnConfigProperties: BpnConfigProperties
) {
    private val logger = KotlinLogging.logger { }

    private val maxSupportedCount = bpnConfigProperties.alphabet.length.toDouble().pow(bpnConfigProperties.counterDigits).toLong()

    /**
     * Issues the given number of fresh legal entity BPNs, in the order they were reserved.
     */
    @Transactional
    fun issueLegalEntityBpns(count: Int): List<String> {
        return issueBpns(count, bpnConfigProperties.legalEntityChar, bpnConfigProperties.counterKeyLegalEntities)
    }

    /**
     * Issues the given number of fresh address BPNs, in the order they were reserved.
     */
    @Transactional
    fun issueAddressBpns(count: Int): List<String> {
        return issueBpns(count, bpnConfigProperties.addressChar, bpnConfigProperties.counterKeyAddresses)
    }

    /**
     * Issues the given number of fresh site BPNs, in the order they were reserved.
     */
    @Transactional
    fun issueSiteBpns(count: Int): List<String> {
        return issueBpns(count, bpnConfigProperties.siteChar, bpnConfigProperties.counterKeySites)
    }

    private fun issueBpns(count: Int, bpnChar: Char, bpnCounterKey: String): List<String> {
        if (count == 0) return emptyList()

        logger.info { "Issuing $count new BPNs of type $bpnChar" }

        val counterEntry = getOrCreateCounter(bpnCounterKey)
        val startCounter = counterEntry.value.toLongOrNull() ?: throw BpnInvalidCounterValueException(counterEntry.value)

        val createdBpns = (0 until count)
            .map {
                createBpn(startCounter + it, bpnChar)
            }

        counterEntry.value = (startCounter + count).toString()
        configurationEntryRepository.save(counterEntry)

        logger.debug { "Created BPNs: ${createdBpns.joinToString()}" }

        return createdBpns
    }

    private fun createBpn(number: Long, bpnChar: Char): String {
        val code = toBpnCode(number)
        val checksum = calculateChecksum(bpnConfigProperties.id + bpnChar + code)

        return "${bpnConfigProperties.id}$bpnChar$code$checksum"
    }

    private fun getOrCreateCounter(bpnCounterKey: String): ConfigurationEntryDb {
        return configurationEntryRepository.findByKey(bpnCounterKey) ?: run {
            val newEntry = ConfigurationEntryDb(bpnCounterKey, 0.toString())
            configurationEntryRepository.save(newEntry)
        }
    }

    private fun toBpnCode(count: Long): String{
        val bpnCode = StringBuilder()
        var remainingCount = count
        var fitIn: Long
        var remainder: Int
        var currentDigit: Char
        val toBase = bpnConfigProperties.alphabet.length

        if(count >= maxSupportedCount)
            throw BpnMaxNumberReachedException(maxSupportedCount)

        do{
            fitIn = remainingCount.floorDiv(toBase)
            remainder = (remainingCount % toBase).toInt()
            currentDigit = bpnConfigProperties.alphabet[remainder]
            bpnCode.append(currentDigit)
            remainingCount = fitIn
        }while(remainingCount > 0)
        val padAmount = bpnConfigProperties.counterDigits - bpnCode.length
        if(padAmount > 0)
            bpnCode.append(bpnConfigProperties.alphabet[0].toString().repeat(padAmount))

        bpnCode.reverse()

        return bpnCode.toString()
    }

    private fun calculateChecksum(code: String): String{
        var p = 0
        for (element in code) {
            val value: Int = bpnConfigProperties.alphabet.indexOf(element)
            p = (p + value) * bpnConfigProperties.checksumRadix % bpnConfigProperties.checksumModulus
        }
        p = p * bpnConfigProperties.checksumRadix % bpnConfigProperties.checksumModulus

        val checksum = (bpnConfigProperties.checksumModulus - p + 1) % bpnConfigProperties.checksumModulus
        val second = checksum % bpnConfigProperties.checksumRadix
        val first = (checksum - second) / bpnConfigProperties.checksumRadix

        return  bpnConfigProperties.alphabet[first].toString() + bpnConfigProperties.alphabet[second]
    }

}
