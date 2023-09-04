````mermaid
classDiagram

AddressCreateWrapper ..> AddressPartnerCreateResponse
AddressCreateWrapper ..> ErrorInfoAddressCreateError
AddressPartnerCreateResponse ..> AddressStateResponse
AddressPartnerCreateResponse ..> IdentifierResponse
AddressPartnerCreateResponse ..> PhysicalPostalAddress
AddressPartnerCreateResponse ..> AlternativePostalAddress
AddressStateResponse ..> TypeKeyNameDtoBusinessStateType
AlternativePostalAddress ..> GeoCoordinates
PhysicalPostalAddress ..> GeoCoordinates
PhysicalPostalAddress ..> Street
IdentifierResponse ..> TypeKeyNameDtoString

class AddressCreateWrapper{
+number: errorCount
+number: entityCount
+AddressPartnerCreateResponse[]: entities;
+ErrorInfoAddressCreateError[]: errors;
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
class AddressStateResponse{
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
class ErrorInfoAddressCreateError{
+string: errorCode
+string: message
+string: entityKey
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

````