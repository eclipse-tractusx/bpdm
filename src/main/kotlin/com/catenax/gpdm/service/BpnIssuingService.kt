package com.catenax.gpdm.service

import com.catenax.gpdm.config.BpnConfigProperties
import com.catenax.gpdm.entity.*
import com.catenax.gpdm.exception.BpnInvalidCounterValueException
import com.catenax.gpdm.exception.BpnMaxNumberReachedException
import com.catenax.gpdm.repository.ConfigurationEntryRepository
import com.catenax.gpdm.repository.IdentifierStatusRepository
import com.catenax.gpdm.repository.IdentifierTypeRepository
import com.catenax.gpdm.repository.IssuingBodyRepository
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


    @Transactional
    fun issueLegalEntities(count: Int): Collection<String>{
        val counterEntry = getOrCreateCounter()
        val startCounter =  counterEntry.value.toLongOrNull() ?: throw BpnInvalidCounterValueException(counterEntry.value)

        val createdBpns = (0 .. count)
            .map {
                createLegalEntityNumber(startCounter + it)
            }

        counterEntry.value = (startCounter+count).toString()
        configurationEntryRepository.save(counterEntry)

        return createdBpns
    }


    fun addIdentifiers(partners: Collection<BusinessPartner>){
        val type = getOrCreateIdentifierType()
        val status = getOrCreateIdentifierStatus()
        val agency = getOrCreateAgency()

       partners.forEach{ it.identifiers.add(Identifier(it.bpn, type, status, agency, it)) }
    }


    fun createIdentifier(bp: BusinessPartner): Identifier{
        return Identifier(bp.bpn, getOrCreateIdentifierType(), getOrCreateIdentifierStatus(), getOrCreateAgency(), bp)
    }

    private fun createLegalEntityNumber(number: Long): String{
        val code = toBpnCode(number)
        val checksum = calculateChecksum(code)

        return "${bpnConfigProperties.id}${bpnConfigProperties.legalEntityChar}$code$checksum"
    }

    private fun getOrCreateCounter(): ConfigurationEntry {
        return configurationEntryRepository.findByKey(bpnConfigProperties.counterKey)?: run {
            val newEntry = ConfigurationEntry(bpnConfigProperties.counterKey, 0.toString())
            configurationEntryRepository.save(newEntry)
        }
    }

    private fun getOrCreateAgency(): IssuingBody{
        return issuingBodyRepository.findByTechnicalKey(bpnConfigProperties.agencyKey) ?: run {
            val catenaIssuingBody = IssuingBody(bpnConfigProperties.agencyName, "", bpnConfigProperties.agencyKey)
            issuingBodyRepository.save(catenaIssuingBody)
        }

    }

    private fun getOrCreateIdentifierType(): IdentifierType{
        return identifierTypeRepository.findByTechnicalKey(bpnConfigProperties.id) ?: run{
            val catenaIdentifierType =  IdentifierType(bpnConfigProperties.name, "", bpnConfigProperties.id)
            identifierTypeRepository.save(catenaIdentifierType)
        }

    }

    private fun getOrCreateIdentifierStatus(): IdentifierStatus{
        return identifierStatusRepository.findByTechnicalKey("UNKNOWN") ?: run{
            val catenaIdentifierStatus=  IdentifierStatus("Unknown", "UNKNOWN")
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