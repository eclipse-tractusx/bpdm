````mermaid
classDiagram

AddressGateInputResponse ..> AddressState
AddressGateInputResponse ..> AddressIdentifier
AddressGateInputResponse ..> PhysicalPostalAddressGateDto
AddressGateInputResponse ..> AlternativePostalAddress
PageResponseSiteGateOutputResponse ..> SiteGateOutputResponse
SiteGateOutputResponse ..> SiteState
SiteGateOutputResponse ..> AddressGateInputResponse
PhysicalPostalAddressGateDto ..> GeoCoordinates
PhysicalPostalAddressGateDto ..> StreetGate
AlternativePostalAddress ..> GeoCoordinates

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
class PageResponseSiteGateOutputResponse{
+number: totalElements
+number: totalPages
+number: page
+number: contentSize
+SiteGateOutputResponse[]: content;
}
class SiteGateOutputResponse{
+string[]: nameParts
+string[]: roles
+string: externalId
+string: legalEntityExternalId
+string: bpn
+SiteState[]: states;
+AddressGateInputResponse: mainAddress;
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