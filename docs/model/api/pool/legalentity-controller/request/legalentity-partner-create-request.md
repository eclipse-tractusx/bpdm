````mermaid
classDiagram


AlternativePostalAddress ..> GeoCoordinates
LegalEntityPartnerCreateRequest ..> LegalEntityIdentifier
LegalEntityPartnerCreateRequest ..> LegalEntityState
LegalEntityPartnerCreateRequest ..> Classification
LegalEntityPartnerCreateRequest ..> LogisticAddressDto
LogisticAddressDto ..> AddressState
LogisticAddressDto ..> AddressIdentifier
LogisticAddressDto ..> PhysicalPostalAddress
LogisticAddressDto ..> AlternativePostalAddress
PhysicalPostalAddress ..> GeoCoordinates
PhysicalPostalAddress ..> Street


class AlternativePostalAddress{
    +string: country
    +string: postalCode
    +string: city
    +string: administrativeAreaLevel1
    +string: deliveryServiceNumber
    +string: deliveryServiceType
    +string: deliveryServiceQualifier
    +GeoCoordinates: geographicCoordinates;
}

class GeoCoordinates{
    +number: longitude
    +number: latitude
    +number: altitude
}

class LegalEntityPartnerCreateRequest{
    +string: legalName
    +string: legalShortName
    +string: legalForm
    +string: index
    +LegalEntityIdentifier[]: identifiers;
    +LegalEntityState[]: states;
    +Classification[]: classifications;
    +LogisticAddressDto: legalAddress;
}

class LegalEntityIdentifier{
    +string: value
    +string: type
    +string: issuingBody
}

class LegalEntityState{
    +string: officialDenotation
    +date: validFrom
    +date: validTo
    +string: type
}

class Classification{
    +string: value
    +string: code
    +string: type
}

class LogisticAddressDto{
    +string: name
    +AddressState[]: states;
    +AddressIdentifier[]: identifiers;
    +PhysicalPostalAddress: physicalPostalAddress;
    +AlternativePostalAddress: alternativePostalAddress;
}

class AddressState{
    +string: description
    +date: validFrom
    +date: validTo
    +string: type
}

class AddressIdentifier{
    +string: value
    +string: type
}

class PhysicalPostalAddress{
    +string: country
    +string: postalCode
    +string: city
    +string: administrativeAreaLevel1
    +string: administrativeAreaLevel2
    +string: administrativeAreaLevel3
    +string: district
    +string: companyPostalCode
    +string: industrialZone
    +string: building
    +string: floor
    +string: door
    +GeoCoordinates: geographicCoordinates;
    +Street: street;
}

class Street{
    +string: name
    +string: houseNumber
    +string: milestone
    +string: direction
}

````