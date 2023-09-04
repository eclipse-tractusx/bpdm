````mermaid
classDiagram

SiteGateInputRequest ..> SiteState
SiteGateInputRequest ..> LogisticAddressGateDto
PhysicalPostalAddressGateDto ..> GeoCoordinates
PhysicalPostalAddressGateDto ..> StreetGate
LogisticAddressGateDto ..> AddressState
LogisticAddressGateDto ..> AddressIdentifier
LogisticAddressGateDto ..> PhysicalPostalAddressGateDto
LogisticAddressGateDto ..> AlternativePostalAddress
AlternativePostalAddress ..> GeoCoordinates


class AddressIdentifier{
+string: value
+string: type
}
class AddressState{
+string: description
+date: validFrom
+date: validTo
+string: type
}

class SiteGateInputRequest{
+string[]: nameParts
+string[]: roles
+string: externalId
+string: legalEntityExternalId
+SiteState[]: states;
+LogisticAddressGateDto: mainAddress;
}
class PhysicalPostalAddressGateDto{
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
+StreetGate: street;
}
class GeoCoordinates{
+number: longitude
+number: latitude
+number: altitude
}
class StreetGate{
+string: namePrefix
+string: additionalNamePrefix
+string: name
+string: additionalNameSuffix
+string: houseNumber
+string: milestone
+string: direction
+string: nameSuffix
}
class LogisticAddressGateDto{
+string[]: nameParts
+string[]: roles
+AddressState[]: states;
+AddressIdentifier[]: identifiers;
+PhysicalPostalAddressGateDto: physicalPostalAddress;
+AlternativePostalAddress: alternativePostalAddress;
}
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
class SiteState{
+string: description
+date: validFrom
+date: validTo
+string: type
}
````