````mermaid
classDiagram

AddressGateInputResponse ..> AddressState
AddressGateInputResponse ..> AddressIdentifier
AddressGateInputResponse ..> PhysicalPostalAddressGateDto
AddressGateInputResponse ..> AlternativePostalAddress
AlternativePostalAddress ..> GeoCoordinates
PageResponseAddressGateInputResponse ..> AddressGateInputResponse
PhysicalPostalAddressGateDto ..> GeoCoordinates
PhysicalPostalAddressGateDto ..> StreetGate

class AddressGateInputResponse{
+string[]: nameParts
+string[]: roles
+string: externalId
+string: legalEntityExternalId
+string: siteExternalId
+string: bpn
+AddressState[]: states;
+AddressIdentifier[]: identifiers;
+PhysicalPostalAddressGateDto: physicalPostalAddress;
+AlternativePostalAddress: alternativePostalAddress;
}
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
class GeoCoordinates{
+number: longitude
+number: latitude
+number: altitude
}
class PageResponseAddressGateInputResponse{
+number: totalElements
+number: totalPages
+number: page
+number: contentSize
+AddressGateInputResponse[]: content;
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