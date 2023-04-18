package org.eclipse.tractusx.bpdm.gate.service

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.saas.TypeValueSaas
import org.eclipse.tractusx.bpdm.common.model.*
import org.eclipse.tractusx.bpdm.common.model.SaasAdministrativeAreaType.COUNTY
import org.eclipse.tractusx.bpdm.common.model.SaasAdministrativeAreaType.REGION
import org.eclipse.tractusx.bpdm.common.service.SaasMappings
import org.eclipse.tractusx.bpdm.common.service.SaasMappings.toLogisticAddressDto
import org.eclipse.tractusx.bpdm.gate.util.RequestValues
import org.eclipse.tractusx.bpdm.gate.util.SaasValues
import org.junit.jupiter.api.Test

class SaasDtoToSaasAddressMappingTest {

    @Test
    fun mappingTest() {

        val addressDto = SaasMappings.convertSaasAdressesToLogisticAddressDto(SaasValues.addressBusinessPartner1.addresses, "TestId")

        val addressSaas = SaasValues.addressBusinessPartner1.addresses.first()
        val baseAddressDto = addressDto.physicalPostalAddress.baseAddress
        assertThat(baseAddressDto.administrativeAreaLevel1).isEqualTo(findValue(addressSaas.administrativeAreas, REGION))
        assertThat(baseAddressDto.administrativeAreaLevel2).isEqualTo(findValue(addressSaas.administrativeAreas, COUNTY))
        assertThat(baseAddressDto.administrativeAreaLevel3).isEqualTo(null)
        assertThat(baseAddressDto.administrativeAreaLevel4).isEqualTo(null)
        assertThat(baseAddressDto.city).isEqualTo(findValue(addressSaas.localities, SaasLocalityType.CITY))
        assertThat(baseAddressDto.country).isEqualTo(addressSaas.country?.shortName)
        assertThat(baseAddressDto.districtLevel1).isEqualTo(findValue(addressSaas.localities, SaasLocalityType.DISTRICT))
        assertThat(baseAddressDto.districtLevel2).isEqualTo(findValue(addressSaas.localities, SaasLocalityType.QUARTER))
        assertThat(baseAddressDto.geographicCoordinates?.latitude).isEqualTo(addressSaas.geographicCoordinates?.latitude)
        assertThat(baseAddressDto.geographicCoordinates?.longitude).isEqualTo(addressSaas.geographicCoordinates?.longitude)
        assertThat(baseAddressDto.postCode).isEqualTo(findValue(addressSaas.postCodes, SaasPostCodeType.REGULAR))
        assertThat(baseAddressDto.street?.name).isEqualTo(findObject(addressSaas.thoroughfares, SaasThoroughfareType.STREET)?.name)
        assertThat(baseAddressDto.street?.houseNumber).isEqualTo(findObject(addressSaas.thoroughfares, SaasThoroughfareType.STREET)?.number)
        assertThat(baseAddressDto.street?.direction).isEqualTo(findObject(addressSaas.thoroughfares, SaasThoroughfareType.STREET)?.direction)
        assertThat(baseAddressDto.street?.milestone).isEqualTo(findObject(addressSaas.thoroughfares, SaasThoroughfareType.STREET)?.shortName)

        assertThat(addressDto.physicalPostalAddress.industrialZone).isEqualTo(findObject(addressSaas.thoroughfares, SaasThoroughfareType.INDUSTRIAL_ZONE)?.name)
        assertThat(addressDto.physicalPostalAddress.building).isEqualTo(findValue(addressSaas.premises, SaasPremiseType.BUILDING))
        assertThat(addressDto.physicalPostalAddress.floor).isEqualTo(findValue(addressSaas.premises, SaasPremiseType.LEVEL))
        assertThat(addressDto.physicalPostalAddress.door).isEqualTo(findValue(addressSaas.premises, SaasPremiseType.ROOM))
    }

    @Test
    fun toLogisticAddressDtoTest() {

        val logisticAddress = SaasValues.addressBusinessPartnerWithRelations1.toLogisticAddressDto()
        assertThat(logisticAddress.name).isEqualTo(SaasValues.addressBusinessPartnerWithRelations1.names.firstOrNull()?.value)
        assertThat(logisticAddress.identifiers.size).isEqualTo(2)
        assertThat(logisticAddress.identifiers.first().value).isEqualTo(SaasValues.identifier1.value)
        assertThat(logisticAddress.identifiers.first().type).isEqualTo(SaasValues.identifier1.type?.technicalKey)
//        assertThat(logisticAddress.states.first().type).isEqualTo(SaasValues.addressBusinessPartnerWithRelations1.status?.type);
//        assertThat(logisticAddress.states.first().description).isEqualTo(SaasValues.addressBusinessPartnerWithRelations1.status?.officialDenotation);
//        assertThat(logisticAddress.states.first().validFrom).isEqualTo(SaasValues.addressBusinessPartnerWithRelations1.status?.validFrom);
//        assertThat(logisticAddress.states.first().validTo).isEqualTo(SaasValues.addressBusinessPartnerWithRelations1.status?.validUntil);

    }



    @Test
    fun mappingAddressDto1Test() {

        val addressDto = RequestValues.address1

        val addressSaas = SaasValues.addressBusinessPartner1.addresses.first()
        val baseAddressDto = addressDto.physicalPostalAddress.baseAddress
        assertThat(baseAddressDto.administrativeAreaLevel1).isEqualTo(findValue(addressSaas.administrativeAreas, REGION))
        assertThat(baseAddressDto.administrativeAreaLevel2).isEqualTo(findValue(addressSaas.administrativeAreas, COUNTY))
        assertThat(baseAddressDto.administrativeAreaLevel3).isEqualTo(null)
        assertThat(baseAddressDto.administrativeAreaLevel4).isEqualTo(null)
        assertThat(baseAddressDto.city).isEqualTo(findValue(addressSaas.localities, SaasLocalityType.CITY))
        assertThat(baseAddressDto.country).isEqualTo(addressSaas.country?.shortName)
        assertThat(baseAddressDto.districtLevel1).isEqualTo(findValue(addressSaas.localities, SaasLocalityType.DISTRICT))
        assertThat(baseAddressDto.districtLevel2).isEqualTo(findValue(addressSaas.localities, SaasLocalityType.QUARTER))
        assertThat(baseAddressDto.geographicCoordinates?.latitude).isEqualTo(addressSaas.geographicCoordinates?.latitude)
        assertThat(baseAddressDto.geographicCoordinates?.longitude).isEqualTo(addressSaas.geographicCoordinates?.longitude)
        assertThat(baseAddressDto.postCode).isEqualTo(findValue(addressSaas.postCodes, SaasPostCodeType.REGULAR))
        assertThat(baseAddressDto.street?.name).isEqualTo(findObject(addressSaas.thoroughfares, SaasThoroughfareType.STREET)?.name)
        assertThat(baseAddressDto.street?.houseNumber).isEqualTo(findObject(addressSaas.thoroughfares, SaasThoroughfareType.STREET)?.number)
        assertThat(baseAddressDto.street?.direction).isEqualTo(findObject(addressSaas.thoroughfares, SaasThoroughfareType.STREET)?.direction)
        assertThat(baseAddressDto.street?.milestone).isEqualTo(findObject(addressSaas.thoroughfares, SaasThoroughfareType.STREET)?.shortName)

        assertThat(addressDto.physicalPostalAddress.industrialZone).isEqualTo(findObject(addressSaas.thoroughfares, SaasThoroughfareType.INDUSTRIAL_ZONE)?.name)
        assertThat(addressDto.physicalPostalAddress.building).isEqualTo(findValue(addressSaas.premises, SaasPremiseType.BUILDING))
        assertThat(addressDto.physicalPostalAddress.floor).isEqualTo(findValue(addressSaas.premises, SaasPremiseType.LEVEL))
        assertThat(addressDto.physicalPostalAddress.door).isEqualTo(findValue(addressSaas.premises, SaasPremiseType.ROOM))
    }

    @Test
    fun mappingAddressDto2Test() {

        val addressDto = RequestValues.address2

        val addressSaas = SaasValues.addressBusinessPartner2.addresses.first()
        val baseAddressDto = addressDto.physicalPostalAddress.baseAddress
        assertThat(baseAddressDto.administrativeAreaLevel1).isEqualTo(findValue(addressSaas.administrativeAreas, REGION))
        assertThat(baseAddressDto.administrativeAreaLevel2).isEqualTo(findValue(addressSaas.administrativeAreas, COUNTY))
        assertThat(baseAddressDto.administrativeAreaLevel3).isEqualTo(null)
        assertThat(baseAddressDto.administrativeAreaLevel4).isEqualTo(null)
        assertThat(baseAddressDto.city).isEqualTo(findValue(addressSaas.localities, SaasLocalityType.CITY))
        assertThat(baseAddressDto.country).isEqualTo(addressSaas.country?.shortName)
        assertThat(baseAddressDto.districtLevel1).isEqualTo(findValue(addressSaas.localities, SaasLocalityType.DISTRICT))
        assertThat(baseAddressDto.districtLevel2).isEqualTo(findValue(addressSaas.localities, SaasLocalityType.QUARTER))
        assertThat(baseAddressDto.geographicCoordinates?.latitude).isEqualTo(addressSaas.geographicCoordinates?.latitude)
        assertThat(baseAddressDto.geographicCoordinates?.longitude).isEqualTo(addressSaas.geographicCoordinates?.longitude)
        assertThat(baseAddressDto.postCode).isEqualTo(findValue(addressSaas.postCodes, SaasPostCodeType.REGULAR))
        assertThat(baseAddressDto.street?.name).isEqualTo(findObject(addressSaas.thoroughfares, SaasThoroughfareType.STREET)?.name)
        assertThat(baseAddressDto.street?.houseNumber).isEqualTo(findObject(addressSaas.thoroughfares, SaasThoroughfareType.STREET)?.number)
        assertThat(baseAddressDto.street?.direction).isEqualTo(findObject(addressSaas.thoroughfares, SaasThoroughfareType.STREET)?.direction)
        assertThat(baseAddressDto.street?.milestone).isEqualTo(findObject(addressSaas.thoroughfares, SaasThoroughfareType.STREET)?.shortName)

        assertThat(addressDto.physicalPostalAddress.industrialZone).isEqualTo(findObject(addressSaas.thoroughfares, SaasThoroughfareType.INDUSTRIAL_ZONE)?.name)
        assertThat(addressDto.physicalPostalAddress.building).isEqualTo(findValue(addressSaas.premises, SaasPremiseType.BUILDING))
        assertThat(addressDto.physicalPostalAddress.floor).isEqualTo(findValue(addressSaas.premises, SaasPremiseType.LEVEL))
        assertThat(addressDto.physicalPostalAddress.door).isEqualTo(findValue(addressSaas.premises, SaasPremiseType.ROOM))
    }

    private fun <T : TypeValueSaas> findValue(values: Collection<T>, enumType: SaasType): String? {
        return findObject(values, enumType)?.value
    }

    private fun <T : TypeValueSaas> findObject(values: Collection<T>, enumType: SaasType): T? {
        return values.find { it.type?.technicalKey == enumType.getTechnicalKey() }
    }
}