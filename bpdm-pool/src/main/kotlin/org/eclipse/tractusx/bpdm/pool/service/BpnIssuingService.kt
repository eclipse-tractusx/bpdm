package org.eclipse.tractusx.bpdm.pool.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.pool.entity.*
import org.eclipse.tractusx.bpdm.pool.exception.BpnInvalidCounterValueException
import org.eclipse.tractusx.bpdm.pool.exception.BpnMaxNumberReachedException
import org.eclipse.tractusx.bpdm.pool.repository.ConfigurationEntryRepository
import org.eclipse.tractusx.bpdm.pool.repository.IdentifierStatusRepository
import org.eclipse.tractusx.bpdm.pool.repository.IdentifierTypeRepository
import org.eclipse.tractusx.bpdm.pool.repository.IssuingBodyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.pow


@Service
class BpnIssuingService(
    val issuingBodyRepository: IssuingBodyRepository,
    val identifierTypeRepository: IdentifierTypeRepository,
    val identifierStatusRepository: IdentifierStatusRepository,
    val configurationEntryRepository: ConfigurationEntryRepository,
    val bpnConfigProperties: BpnConfigProperties
) {
    private val logger = KotlinLogging.logger { }

    @Transactional
    fun issueLegalEntityBpns(count: Int): Collection<String> {
        return issueBpns(count, bpnConfigProperties.legalEntityChar, bpnConfigProperties.counterKeyLegalEntities)
    }

    @Transactional
    fun issueAddressBpns(count: Int): Collection<String> {
        return issueBpns(count, bpnConfigProperties.addressChar, bpnConfigProperties.counterKeyAddresses)
    }

    @Transactional
    fun issueSiteBpns(count: Int): Collection<String> {
        return issueBpns(count, bpnConfigProperties.addressChar, bpnConfigProperties.counterKeySites)
    }

    private fun issueBpns(count: Int, bpnChar: Char, bpnCounterKey: String): Collection<String> {
        if (count == 0) return emptyList()

        logger.info { "Issuing $count new BPNs of type $bpnChar" }

        val counterEntry = getOrCreateCounter(bpnCounterKey)
        val startCounter = counterEntry.value.toLongOrNull() ?: throw BpnInvalidCounterValueException(counterEntry.value)

        val createdBpns = (0..count)
            .map {
                createBpn(startCounter + it, bpnChar)
            }

        counterEntry.value = (startCounter + count).toString()
        configurationEntryRepository.save(counterEntry)

        logger.debug { "Created BPNs: ${createdBpns.joinToString()}" }

        return createdBpns
    }

    fun addIdentifiers(partners: Collection<BusinessPartner>) {
        val type = getOrCreateIdentifierType()
        val status = getOrCreateIdentifierStatus()
        val agency = getOrCreateAgency()

        partners.forEach { it.identifiers.add(Identifier(it.bpn, type, status, agency, it)) }
    }

    private fun createBpn(number: Long, bpnChar: Char): String {
        val code = toBpnCode(number)
        val checksum = calculateChecksum(code)

        return "${bpnConfigProperties.id}$bpnChar$code$checksum"
    }

    private fun getOrCreateCounter(bpnCounterKey: String): ConfigurationEntry {
        return configurationEntryRepository.findByKey(bpnCounterKey) ?: run {
            val newEntry = ConfigurationEntry(bpnCounterKey, 0.toString())
            configurationEntryRepository.save(newEntry)
        }
    }

    private fun getOrCreateAgency(): IssuingBody {
        return issuingBodyRepository.findByTechnicalKey(bpnConfigProperties.agencyKey) ?: run {
            val catenaIssuingBody = IssuingBody(bpnConfigProperties.agencyName, "", bpnConfigProperties.agencyKey)
            logger.info { "Create Catena Issuing-Body with technical key ${catenaIssuingBody.technicalKey} and name ${catenaIssuingBody.name}" }
            issuingBodyRepository.save(catenaIssuingBody)
        }

    }

    private fun getOrCreateIdentifierType(): IdentifierType {
        return identifierTypeRepository.findByTechnicalKey(bpnConfigProperties.id) ?: run{
            val catenaIdentifierType =  IdentifierType(bpnConfigProperties.name, "", bpnConfigProperties.id)
            logger.info { "Create Catena Identifier-Type with technical key ${catenaIdentifierType.technicalKey} and name ${catenaIdentifierType.name}" }
            identifierTypeRepository.save(catenaIdentifierType)
        }

    }

    private fun getOrCreateIdentifierStatus(): IdentifierStatus {
        return identifierStatusRepository.findByTechnicalKey("UNKNOWN") ?: run{
            val catenaIdentifierStatus=  IdentifierStatus("Unknown", "UNKNOWN")
            logger.info { "Create Catena Identifier-Status with technical key ${catenaIdentifierStatus.technicalKey} and name ${catenaIdentifierStatus.name}" }
            identifierStatusRepository.save(catenaIdentifierStatus)
        }

    }

    private fun toBpnCode(count: Long): String{
        val bpnCode = StringBuilder()
        var remainingCount = count
        var fitIn: Long
        var remainder: Int
        var currentDigit: Char
        val toBase = bpnConfigProperties.alphabet.length

        val maxSupported = bpnConfigProperties.counterDigits.toDouble().pow(toBase).toLong()
        if(count >= maxSupported)
            throw BpnMaxNumberReachedException(maxSupported)

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