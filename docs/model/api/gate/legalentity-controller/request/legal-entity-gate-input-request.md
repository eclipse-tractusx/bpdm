````mermaid
classDiagram

AlternativePostalAddress ..> GeoCoordinates
LegalEntityGateInputRequest ..> LegalEntityIdentifier
LegalEntityGateInputRequest ..> LegalEntityState
LegalEntityGateInputRequest ..> Classification
LegalEntityGateInputRequest ..> LogisticAddressGateDto
LogisticAddressGateDto ..> AddressState
LogisticAddressGateDto ..> AddressIdentifier
LogisticAddressGateDto ..> PhysicalPostalAddressGateDto
LogisticAddressGateDto ..> AlternativePostalAddress
PhysicalPostalAddressGateDto ..> GeoCoordinates
PhysicalPostalAddressGateDto ..> StreetGate

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
class Classification{
+string: value
+string: code
+string: type
}
class GeoCoordinates{
+number: longitude
+number: latitude
+number: altitude
}
class LegalEntityGateInputRequest{
+string[]: legalNameParts
+string: legalShortName
+string: legalForm
+string[]: roles
+string: externalId
+LegalEntityIdentifier[]: identifiers;
+LegalEntityState[]: states;
+Classification[]: classifications;
+LogisticAddressGateDto: legalAddress;
}

class LegalEntityIdentifier{
+string: value
+string: type
+string: issuingBody
}
class LegalEntityState{
+string: description
+date: validFrom
+date: validTo
+string: type
}
class LogisticAddressGateDto{
+string[]: nameParts
+string[]: roles
+AddressState[]: states;
+AddressIdentifier[]: identifiers;
+PhysicalPostalAddressGateDto: physicalPostalAddress;
+AlternativePostalAddress: alternativePostalAddress;
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
````