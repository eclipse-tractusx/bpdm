classDiagram

AddressGateInputResponse ..> AddressState AddressGateInputResponse ..> AddressIdentifier AddressGateInputResponse ..> PhysicalPostalAddressGateDto
AddressGateInputResponse ..> AlternativePostalAddress AddressGateOutputChildRequest ..> AddressState AddressGateOutputChildRequest ..> AddressIdentifier
AddressGateOutputChildRequest ..> PhysicalPostalAddressGateDto AddressGateOutputChildRequest ..> AlternativePostalAddress PageResponseSiteGateInputResponse ..>
SiteGateInputResponse PageResponseSiteGateOutputResponse ..> SiteGateOutputResponse SiteGateInputRequest ..> SiteState SiteGateInputRequest ..>
LogisticAddressGateDto SiteGateInputResponse ..> SiteState SiteGateInputResponse ..> AddressGateInputResponse SiteGateOutputRequest ..> SiteState
SiteGateOutputRequest ..> AddressGateOutputChildRequest SiteGateOutputResponse ..> SiteState SiteGateOutputResponse ..> AddressGateInputResponse
PhysicalPostalAddressGateDto ..> GeoCoordinates PhysicalPostalAddressGateDto ..> StreetGate LogisticAddressGateDto ..> AddressState LogisticAddressGateDto ..>
AddressIdentifier LogisticAddressGateDto ..> PhysicalPostalAddressGateDto LogisticAddressGateDto ..> AlternativePostalAddress AlternativePostalAddress ..>
GeoCoordinates

    note for SiteGateOutputRequest "/api/catena/output/sites [PUT] [requestBody]"

    note for SiteGateInputRequest "/api/catena/input/sites [PUT] [requestBody]"

    note for PageResponseSiteGateInputResponse "/api/catena/input/sites/search [POST] [response]"
    note for PageResponseSiteGateInputResponse "/api/catena/input/sites [GET] [response]"

    note for PageResponseSiteGateOutputResponse "/api/catena/output/sites/search [POST] [response]"

class AddressGateInputResponse{ +string[]: nameParts +string[]: roles +string: externalId +string: legalEntityExternalId +string: siteExternalId +string: bpn
+AddressState[]: states; +AddressIdentifier[]: identifiers; +PhysicalPostalAddressGateDto: physicalPostalAddress; +AlternativePostalAddress:
alternativePostalAddress; } class AddressGateOutputChildRequest{ +string[]: nameParts +string[]: roles +string: bpn +AddressState[]: states;
+AddressIdentifier[]: identifiers; +PhysicalPostalAddressGateDto: physicalPostalAddress; +AlternativePostalAddress: alternativePostalAddress; } class
AddressIdentifier{ +string: value +string: type } class AddressState{ +string: description +date: validFrom +date: validTo +string: type } class
PageResponseSiteGateInputResponse{ +number: totalElements +number: totalPages +number: page +number: contentSize +SiteGateInputResponse[]: content; } class
PageResponseSiteGateOutputResponse{ +number: totalElements +number: totalPages +number: page +number: contentSize +SiteGateOutputResponse[]: content; } class
SiteGateInputRequest{ +string[]: nameParts +string[]: roles +string: externalId +string: legalEntityExternalId +SiteState[]: states; +LogisticAddressGateDto:
mainAddress; } class SiteGateInputResponse{ +string[]: nameParts +string[]: roles +string: externalId +string: legalEntityExternalId +SiteState[]: states;
+AddressGateInputResponse: mainAddress; } class SiteGateOutputRequest{ +string[]: nameParts +string[]: roles +string: externalId +string: legalEntityExternalId
+string: bpn +SiteState[]: states; +AddressGateOutputChildRequest: mainAddress; } class SiteGateOutputResponse{ +string[]: nameParts +string[]: roles +string:
externalId +string: legalEntityExternalId +string: bpn +SiteState[]: states; +AddressGateInputResponse: mainAddress; } class PhysicalPostalAddressGateDto{
+string: country +string: postalCode +string: city +string: administrativeAreaLevel1 +string: administrativeAreaLevel2 +string: administrativeAreaLevel3
+string: district +string: companyPostalCode +string: industrialZone +string: building +string: floor +string: door +GeoCoordinates: geographicCoordinates;
+StreetGate: street; } class GeoCoordinates{ +number: longitude +number: latitude +number: altitude } class StreetGate{ +string: namePrefix +string:
additionalNamePrefix +string: name +string: additionalNameSuffix +string: houseNumber +string: milestone +string: direction +string: nameSuffix } class
LogisticAddressGateDto{ +string[]: nameParts +string[]: roles +AddressState[]: states; +AddressIdentifier[]: identifiers; +PhysicalPostalAddressGateDto:
physicalPostalAddress; +AlternativePostalAddress: alternativePostalAddress; } class AlternativePostalAddress{ +string: country +string: postalCode +string: city
+string: administrativeAreaLevel1 +string: deliveryServiceNumber +string: deliveryServiceType +string: deliveryServiceQualifier +GeoCoordinates:
geographicCoordinates; } class SiteState{ +string: description +date: validFrom +date: validTo +string: type }

  