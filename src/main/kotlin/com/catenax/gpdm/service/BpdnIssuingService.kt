package com.catenax.gpdm.service

import com.catenax.gpdm.controller.dto.BusinessPartnerBaseDto
import com.catenax.gpdm.entity.*
import com.catenax.gpdm.repository.IssuingAgencyRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class BpdnIssuingService(
    val issuingAgencyRepository: IssuingAgencyRepository
) {
    val agencyName = "Catena"
    val random: Random = Random()

    fun issueLegalEntity(): String{
        return "BPNL" + random.nextInt(999999999)
    }

    fun issueAddress(): String{
        return "BPNL" + random.nextInt(999999999)
    }

    fun createIdentifier(bp: BusinessPartner): IdentifierPartner{
        val registration = Registration(HardeningGrade.GOLD, getOrCreateAgency(), RegistrationStatus.ISSUED, LocalDateTime.now(), LocalDateTime.now())
        return IdentifierPartner(bp.bpn, null, null, "BPN", registration, bp)
    }

    fun createIdentifier(address: Address): IdentifierAddress{
        val registration = Registration(HardeningGrade.GOLD, getOrCreateAgency(), RegistrationStatus.ISSUED, LocalDateTime.now(), LocalDateTime.now())
        return IdentifierAddress(address.bpn, null, null, "BPN", registration, address)
    }

    private fun getOrCreateAgency(): IssuingAgency{
        val agencies = issuingAgencyRepository.findAllByValueIn(setOf(agencyName))
        if (agencies.isEmpty()) return IssuingAgency(agencyName, null, null)
        return agencies.first()
    }


}