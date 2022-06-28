package com.catenax.gpdm.service

import com.catenax.gpdm.dto.response.AddressResponse
import com.catenax.gpdm.dto.response.AddressWithReferenceResponse
import com.catenax.gpdm.dto.response.PageResponse
import com.catenax.gpdm.entity.Address
import com.catenax.gpdm.exception.BpdmNotFoundException
import com.catenax.gpdm.repository.AddressRepository
import com.catenax.gpdm.repository.BusinessPartnerRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class AddressService(
    private val addressRepository: AddressRepository,
    private val businessPartnerRepository: BusinessPartnerRepository
) {
    fun findByPartnerBpn(bpn: String, pageIndex: Int, pageSize: Int): PageResponse<AddressResponse> {
        if (!businessPartnerRepository.existsByBpn(bpn)) {
            throw BpdmNotFoundException("Business Partner", bpn)
        }

        val page = addressRepository.findByPartnerBpn(bpn, PageRequest.of(pageIndex, pageSize))
        fetchAddressDependencies(page.toSet())
        return page.toDto(page.content.map { it.toDto() })
    }

    fun findByBpn(bpn: String): AddressWithReferenceResponse {
        val address = addressRepository.findByBpn(bpn) ?: throw BpdmNotFoundException("Address", bpn)
        return address.toDtoWithReference()
    }

    fun fetchAddressDependencies(addresses: Set<Address>): Set<Address> {
        addressRepository.joinContexts(addresses)
        addressRepository.joinTypes(addresses)
        addressRepository.joinVersions(addresses)
        addressRepository.joinAdminAreas(addresses)
        addressRepository.joinPostCodes(addresses)
        addressRepository.joinLocalities(addresses)
        addressRepository.joinPremises(addresses)
        addressRepository.joinPostalDeliveryPoints(addresses)
        addressRepository.joinThoroughfares(addresses)

        return addresses
    }
}