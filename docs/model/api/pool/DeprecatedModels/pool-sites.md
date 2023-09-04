```mermaid
classDiagram

LogisticAddressResponse ..> AddressStateResponse
LogisticAddressResponse ..> IdentifierResponse
LogisticAddressResponse ..> PhysicalPostalAddress
LogisticAddressResponse ..> AlternativePostalAddress
PageResponseSitePoolResponse ..> SitePoolResponse
PageResponseSiteResponse ..> SiteResponse
SiteCreateWrapper ..> SitePartnerCreateResponse
SiteCreateWrapper ..> ErrorInfoSiteCreateError
SitePartnerCreateRequest ..> SiteState
SitePartnerCreateRequest ..> LogisticAddressDto
SitePartnerCreateResponse ..> SiteStateResponse
SitePartnerCreateResponse ..> LogisticAddressResponse
SitePartnerUpdateRequest ..> SiteState
SitePartnerUpdateRequest ..> LogisticAddressDto
SitePoolResponse ..> SiteStateResponse
SitePoolResponse ..> LogisticAddressResponse
SiteResponse ..> SiteStateResponse
SiteStateResponse ..> TypeKeyNameDtoBusinessStateType
SiteUpdateWrapper ..> SitePartnerCreateResponse
SiteUpdateWrapper ..> ErrorInfoSiteUpdateError
AlternativePostalAddress ..> GeoCoordinates
PhysicalPostalAddress ..> GeoCoordinates
PhysicalPostalAddress ..> Street
IdentifierResponse ..> TypeKeyNameDtoString
AddressStateResponse ..> TypeKeyNameDtoBusinessStateType
LogisticAddressDto ..> AddressState
LogisticAddressDto ..> AddressIdentifier
LogisticAddressDto ..> PhysicalPostalAddress
LogisticAddressDto ..> AlternativePostalAddress

    note for PageResponseSiteResponse "/api/catena/legal-entities/{bpnl}/sites [GET] [Response]"

    note for SiteCreateWrapper "/api/catena/sites [POST] [Response]"

    note for SitePartnerUpdateRequest "/api/catena/sites [PUT] [requestBody]"
    note for SitePartnerCreateRequest "/api/catena/sites [POST] [requestBody]"

    note for PageResponseSitePoolResponse "/api/catena/sites/search [POST] [Response]"

    note for SiteUpdateWrapper "/api/catena/sites [PUT] [Response]"

    note for SiteBpnSearchRequest "/api/catena/sites/search [POST] [requestBody]"

class ErrorInfoSiteCreateError{
+string: errorCode
+string: message
+string: entityKey
}
class ErrorInfoSiteUpdateError{
+string: errorCode
+string: message
+string: entityKey
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
class PageResponseSitePoolResponse{
+number: totalElements
+number: totalPages
+number: page
+number: contentSize
+SitePoolResponse[]: content;
}
class PageResponseSiteResponse{
+number: totalElements
+number: totalPages
+number: page
+number: contentSize
+SiteResponse[]: content;
}
class SiteCreateWrapper{
+number: errorCount
+number: entityCount
+SitePartnerCreateResponse[]: entities;
+ErrorInfoSiteCreateError[]: errors;
}
class SitePartnerCreateRequest{
+string: name
+string: bpnlParent
+string: index
+SiteState[]: states;
+LogisticAddressDto: mainAddress;
}
class SitePartnerCreateResponse{
+string: bpns
+string: name
+string: bpnLegalEntity
+date: createdAt
+date: updatedAt
+string: index
+SiteStateResponse[]: states;
+LogisticAddressResponse: mainAddress;
}
class SitePartnerUpdateRequest{
+string: bpns
+string: name
+SiteState[]: states;
+LogisticAddressDto: mainAddress;
}
class SitePoolResponse{
+string: bpns
+string: name
+string: bpnLegalEntity
+date: createdAt
+date: updatedAt
+SiteStateResponse[]: states;
+LogisticAddressResponse: mainAddress;
}
class SiteResponse{
+string: bpns
+string: name
+string: bpnLegalEntity
+date: createdAt
+date: updatedAt
+SiteStateResponse[]: states;
}
class SiteState{
+string: description
+date: validFrom
+date: validTo
+string: type
}
class SiteStateResponse{
+string: description
+date: validFrom
+date: validTo
+TypeKeyNameDtoBusinessStateType: type;
}
class SiteUpdateWrapper{
+number: errorCount
+number: entityCount
+SitePartnerCreateResponse[]: entities;
+ErrorInfoSiteUpdateError[]: errors;
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
class AddressStateResponse{
+string: description
+date: validFrom
+date: validTo
+TypeKeyNameDtoBusinessStateType: type;
}
class TypeKeyNameDtoBusinessStateType{
+string: technicalKey
+string: name
}
class GeoCoordinates{
+number: longitude
+number: latitude
+number: altitude
}
class Street{
+string: name
+string: houseNumber
+string: milestone
+string: direction
}
class TypeKeyNameDtoString{
+string: technicalKey
+string: name
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
class SiteBpnSearchRequest{
+string[]: legalEntities
+string[]: sites
}
´´´