````mermaid
classDiagram

LogisticAddressResponse ..> AddressStateResponse
LogisticAddressResponse ..> IdentifierResponse
LogisticAddressResponse ..> PhysicalPostalAddress
LogisticAddressResponse ..> AlternativePostalAddress
PageResponseSitePoolResponse ..> SitePoolResponse
SitePoolResponse ..> SiteStateResponse
SitePoolResponse ..> LogisticAddressResponse
SiteStateResponse ..> TypeKeyNameDtoBusinessStateType
AlternativePostalAddress ..> GeoCoordinates
PhysicalPostalAddress ..> GeoCoordinates
PhysicalPostalAddress ..> Street
IdentifierResponse ..> TypeKeyNameDtoString
AddressStateResponse ..> TypeKeyNameDtoBusinessStateType



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
class SitePoolResponse{
+string: bpns
+string: name
+string: bpnLegalEntity
+date: createdAt
+date: updatedAt
+SiteStateResponse[]: states;
+LogisticAddressResponse: mainAddress;
}
class TypeKeyNameDtoString{
+string: technicalKey
+string: name
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
class SiteStateResponse{
+string: description
+date: validFrom
+date: validTo
+TypeKeyNameDtoBusinessStateType: type;
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

````