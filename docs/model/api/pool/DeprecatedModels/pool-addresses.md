```mermaid
classDiagram

AddressCreateWrapper ..> AddressPartnerCreateResponse
AddressCreateWrapper ..> ErrorInfoAddressCreateError
AddressMatchResponse ..> LogisticAddressResponse
AddressPartnerCreateRequest ..> AddressState
AddressPartnerCreateRequest ..> AddressIdentifier
AddressPartnerCreateRequest ..> PhysicalPostalAddress
AddressPartnerCreateRequest ..> AlternativePostalAddress
AddressPartnerCreateResponse ..> AddressStateResponse
AddressPartnerCreateResponse ..> IdentifierResponse
AddressPartnerCreateResponse ..> PhysicalPostalAddress
AddressPartnerCreateResponse ..> AlternativePostalAddress
AddressPartnerUpdateRequest ..> AddressState
AddressPartnerUpdateRequest ..> AddressIdentifier
AddressPartnerUpdateRequest ..> PhysicalPostalAddress
AddressPartnerUpdateRequest ..> AlternativePostalAddress
AddressStateResponse ..> TypeKeyNameDtoBusinessStateType
AddressUpdateWrapper ..> LogisticAddressResponse
AddressUpdateWrapper ..> ErrorInfoAddressUpdateError
AlternativePostalAddress ..> GeoCoordinates
LogisticAddressResponse ..> AddressStateResponse
LogisticAddressResponse ..> IdentifierResponse
LogisticAddressResponse ..> PhysicalPostalAddress
LogisticAddressResponse ..> AlternativePostalAddress
MainAddressResponse ..> PhysicalPostalAddress
MainAddressResponse ..> AlternativePostalAddress
PageResponseAddressMatchResponse ..> AddressMatchResponse
PhysicalPostalAddress ..> GeoCoordinates
PhysicalPostalAddress ..> Street
IdentifierResponse ..> TypeKeyNameDtoString
PageResponseLogisticAddressResponse ..> LogisticAddressResponse
LegalAddressResponse ..> PhysicalPostalAddress
LegalAddressResponse ..> AlternativePostalAddress

    note for AddressCreateWrapper "/api/catena/addresses [POST] [Response]"

    note for PageResponseAddressMatchResponse "/api/catena/addresses [GET] [Response]"

    note for AddressUpdateWrapper "/api/catena/addresses [PUT] [Response]"

    note for AddressPartnerCreateRequest "/api/catena/addresses [POST] [requestBody]"

    note for AddressPartnerUpdateRequest "/api/catena/addresses [PUT] [requestBody]"

    note for PageResponseLogisticAddressResponse "/api/catena/addresses/search [POST] [Response]"

    note for MainAddressResponse "/api/catena/sites/main-addresses/search [POST] [Response]"

    note for LegalAddressResponse "/api/catena/legal-entities/legal-addresses/search [POST] [Response]"

    note for AddressPartnerBpnSearchRequest "/api/catena/addresses/search [POST] [requestBody]"

class AddressPartnerBpnSearchRequest{
+string[]: legalEntities
+string[]: sites
+string[]: addresses
}

class AddressCreateWrapper{
+number: errorCount
+number: entityCount
+AddressPartnerCreateResponse[]: entities;
+ErrorInfoAddressCreateError[]: errors;
}
class AddressIdentifier{
+string: value
+string: type
}
class AddressMatchResponse{
+number: score
+LogisticAddressResponse: address;
}
class AddressPartnerCreateRequest{
+string: name
+string: bpnParent
+string: index
+AddressState[]: states;
+AddressIdentifier[]: identifiers;
+PhysicalPostalAddress: physicalPostalAddress;
+AlternativePostalAddress: alternativePostalAddress;
}
class AddressPartnerCreateResponse{
+string: bpna
+string: name
+string: bpnLegalEntity
+string: bpnSite
+date: createdAt
+date: updatedAt
+bool: isLegalAddress
+bool: isMainAddress
+string: index
+AddressStateResponse[]: states;
+IdentifierResponse[]: identifiers;
+PhysicalPostalAddress: physicalPostalAddress;
+AlternativePostalAddress: alternativePostalAddress;
}
class AddressPartnerUpdateRequest{
+string: bpna
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
class AddressStateResponse{
+string: description
+date: validFrom
+date: validTo
+TypeKeyNameDtoBusinessStateType: type;
}
class AddressUpdateWrapper{
+number: errorCount
+number: entityCount
+LogisticAddressResponse[]: entities;
+ErrorInfoAddressUpdateError[]: errors;
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
class ErrorInfoAddressCreateError{
+string: errorCode
+string: message
+string: entityKey
}
class ErrorInfoAddressUpdateError{
+string: errorCode
+string: message
+string: entityKey
}
class GeoCoordinates{
+number: longitude
+number: latitude
+number: altitude
}
class LogisticAddressResponse{
+string: bpna
+string: name
+string: bpnLegalEntity
+string: bpnSite
+date: createdAt
+date: updatedAt
+bool: isLegalAddress
+bool: isMainAddress
+AddressStateResponse[]: states;
+IdentifierResponse[]: identifiers;
+PhysicalPostalAddress: physicalPostalAddress;
+AlternativePostalAddress: alternativePostalAddress;
}
class MainAddressResponse{
+string: bpnSite
+date: createdAt
+date: updatedAt
+PhysicalPostalAddress: physicalPostalAddress;
+AlternativePostalAddress: alternativePostalAddress;
}
class PageResponseAddressMatchResponse{
+number: totalElements
+number: totalPages
+number: page
+number: contentSize
+AddressMatchResponse[]: content;
}
class Street{
+string: name
+string: houseNumber
+string: milestone
+string: direction
}
class TypeKeyNameDtoBusinessStateType{
+string: technicalKey
+string: name
}
class TypeKeyNameDtoString{
+string: technicalKey
+string: name
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
class IdentifierResponse{
+string: value
+TypeKeyNameDtoString: type;
}
class PageResponseLogisticAddressResponse{
+number: totalElements
+number: totalPages
+number: page
+number: contentSize
+LogisticAddressResponse[]: content;
}
class LegalAddressResponse{
+string: bpnLegalEntity
+date: createdAt
+date: updatedAt
+PhysicalPostalAddress: physicalPostalAddress;
+AlternativePostalAddress: alternativePostalAddress;
}
´´´