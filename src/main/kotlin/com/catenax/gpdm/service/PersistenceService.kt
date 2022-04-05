package com.catenax.gpdm.service

import com.catenax.gpdm.entity.BusinessPartner
import com.catenax.gpdm.repository.entity.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Helper service for persisting [BusinessPartner] records in batches
 */
@Service
class PersistenceService(
    val businessPartnerRepository: BusinessPartnerRepository,
    val identifierRepository: IdentifierRepository,
    val nameRepository: NameRepository,
    val businessStatusRepository: BusinessStatusRepository,
    val classificationRepository: ClassificationRepository,
    val bankAccountRepository: BankAccountRepository,
    val relationRepository: RelationRepository,
    val addressRepository: AddressRepository,
    val administrativeAreaRepository: AdministrativeAreaRepository,
    val postCodeRepository: PostCodeRepository,
    val thoroughfareRepository: ThoroughfareRepository,
    val premiseRepository: PremiseRepository,
    val postalDeliveryPointRepository: PostalDeliveryPointRepository,
    val localityRepository: LocalityRepository
) {


    /**
     * Persists [businessPartners] and all their dependent entities
     */
    @Transactional
    fun saveAll(businessPartners: Collection<BusinessPartner>): Collection<BusinessPartner> {

        val identifiers = businessPartners.flatMap { it.identifiers }
        val names = businessPartners.flatMap { it.names }
        val statuses = businessPartners.flatMap { it.stati }
        val classifications = businessPartners.flatMap { it.classification }
        val bankAccounts = businessPartners.flatMap { it.bankAccounts }
        val relations = businessPartners.flatMap { it.startNodeRelations }.plus(businessPartners.flatMap { it.endNodeRelations })

        val addresses = businessPartners.flatMap { it.addresses }
        val adminAreas = addresses.flatMap { it.administrativeAreas }
        val postCodes = addresses.flatMap { it.postCodes }
        val thoroughfares = addresses.flatMap { it.thoroughfares }
        val premises = addresses.flatMap { it.premises }
        val postalDeliveryPoints = addresses.flatMap { it.postalDeliveryPoints }
        val localities = addresses.flatMap { it.localities }

        businessPartnerRepository.saveAll(businessPartners)
        identifierRepository.saveAll(identifiers)
        nameRepository.saveAll(names)
        businessStatusRepository.saveAll(statuses)
        classificationRepository.saveAll(classifications)
        bankAccountRepository.saveAll(bankAccounts)
        relationRepository.saveAll(relations)

        addressRepository.saveAll(addresses)
        administrativeAreaRepository.saveAll(adminAreas)
        postCodeRepository.saveAll(postCodes)
        thoroughfareRepository.saveAll(thoroughfares)
        premiseRepository.saveAll(premises)
        postalDeliveryPointRepository.saveAll(postalDeliveryPoints)
        localityRepository.saveAll(localities)

        return businessPartners
    }
}