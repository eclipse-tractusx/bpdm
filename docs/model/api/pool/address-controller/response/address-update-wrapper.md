````mermaid
classDiagram

AddressStateResponse ..> TypeKeyNameDtoBusinessStateType
AddressUpdateWrapper ..> LogisticAddressResponse
AddressUpdateWrapper ..> ErrorInfoAddressUpdateError
AlternativePostalAddress ..> GeoCoordinates
LogisticAddressResponse ..> AddressStateResponse
LogisticAddressResponse ..> IdentifierResponse
LogisticAddressResponse ..> PhysicalPostalAddress
LogisticAddressResponse ..> AlternativePostalAddress
PhysicalPostalAddress ..> GeoCoordinates
PhysicalPostalAddress ..> Street
IdentifierResponse ..> TypeKeyNameDtoString

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

class IdentifierResponse{
+string: value
+TypeKeyNameDtoString: type;
}

class ErrorInfoAddressUpdateError{
+string: errorCode
+string: message
+string: entityKey
}
class TypeKeyNameDtoBusinessStateType{
+string: technicalKey
+string: name
}
class TypeKeyNameDtoString{
+string: technicalKey
+string: name
}

````