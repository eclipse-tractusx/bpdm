```mermaid
classDiagram

AddressGateInputRequest ..> AddressState
AddressGateInputRequest ..> AddressIdentifier
AddressGateInputRequest ..> PhysicalPostalAddressGateDto
AddressGateInputRequest ..> AlternativePostalAddress
AddressGateInputResponse ..> AddressState
AddressGateInputResponse ..> AddressIdentifier
AddressGateInputResponse ..> PhysicalPostalAddressGateDto
AddressGateInputResponse ..> AlternativePostalAddress
AddressGateOutputChildRequest ..> AddressState
AddressGateOutputChildRequest ..> AddressIdentifier
AddressGateOutputChildRequest ..> PhysicalPostalAddressGateDto
AddressGateOutputChildRequest ..> AlternativePostalAddress
AddressGateOutputRequest ..> AddressState
AddressGateOutputRequest ..> AddressIdentifier
AddressGateOutputRequest ..> PhysicalPostalAddressGateDto
AddressGateOutputRequest ..> AlternativePostalAddress
AlternativePostalAddress ..> GeoCoordinates
LogisticAddressGateDto ..> AddressState
LogisticAddressGateDto ..> AddressIdentifier
LogisticAddressGateDto ..> PhysicalPostalAddressGateDto
LogisticAddressGateDto ..> AlternativePostalAddress
PageResponseAddressGateInputResponse ..> AddressGateInputResponse
PhysicalPostalAddressGateDto ..> GeoCoordinates
PhysicalPostalAddressGateDto ..> StreetGate

    note for AddressGateInputRequest "/api/catena/input/addresses [PUT] [requestBody]"

    note for PageResponseAddressGateInputResponse "/api/catena/input/addresses [GET] [response]"
    note for PageResponseAddressGateInputResponse "/api/catena/output/addresses/search [POST] [response]"
    note for PageResponseAddressGateInputResponse "/api/catena/input/addresses/search [POST] [response]"

    note for AddressGateOutputRequest "/api/catena/output/addresses [PUT] [requestBody]"

class AddressGateInputRequest{
+string[]: nameParts
+string[]: roles
+string: externalId
+string: legalEntityExternalId
+string: siteExternalId
+AddressState[]: states;
+AddressIdentifier[]: identifiers;
+PhysicalPostalAddressGateDto: physicalPostalAddress;
+AlternativePostalAddress: alternativePostalAddress;
}
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
class AddressGateOutputChildRequest{
+string[]: nameParts
+string[]: roles
+string: bpn
+AddressState[]: states;
+AddressIdentifier[]: identifiers;
+PhysicalPostalAddressGateDto: physicalPostalAddress;
+AlternativePostalAddress: alternativePostalAddress;
}
class AddressGateOutputRequest{
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
class LogisticAddressGateDto{
+string[]: nameParts
+string[]: roles
+AddressState[]: states;
+AddressIdentifier[]: identifiers;
+PhysicalPostalAddressGateDto: physicalPostalAddress;
+AlternativePostalAddress: alternativePostalAddress;
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
´´´