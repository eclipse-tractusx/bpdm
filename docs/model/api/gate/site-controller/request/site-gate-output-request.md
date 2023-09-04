````mermaid
classDiagram


AddressGateOutputChildRequest ..> AddressState
AddressGateOutputChildRequest ..> AddressIdentifier
AddressGateOutputChildRequest ..> PhysicalPostalAddressGateDto
AddressGateOutputChildRequest ..> AlternativePostalAddress
SiteGateOutputRequest ..> SiteState
SiteGateOutputRequest ..> AddressGateOutputChildRequest
PhysicalPostalAddressGateDto ..> GeoCoordinates
PhysicalPostalAddressGateDto ..> StreetGate
AlternativePostalAddress ..> GeoCoordinates

class AddressGateOutputChildRequest{
+string[]: nameParts
+string[]: roles
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
class SiteGateOutputRequest{
+string[]: nameParts
+string[]: roles
+string: externalId
+string: legalEntityExternalId
+string: bpn
+SiteState[]: states;
+AddressGateOutputChildRequest: mainAddress;
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