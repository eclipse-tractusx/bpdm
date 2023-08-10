classDiagram

AddressGateInputResponse ..> AddressState AddressGateInputResponse ..> AddressIdentifier AddressGateInputResponse ..> PhysicalPostalAddressGateDto
AddressGateInputResponse ..> AlternativePostalAddress AddressGateOutputChildRequest ..> AddressState AddressGateOutputChildRequest ..> AddressIdentifier
AddressGateOutputChildRequest ..> PhysicalPostalAddressGateDto AddressGateOutputChildRequest ..> AlternativePostalAddress AlternativePostalAddress ..>
GeoCoordinates LegalEntityGateInputRequest ..> LegalEntityIdentifier LegalEntityGateInputRequest ..> LegalEntityState LegalEntityGateInputRequest ..>
Classification LegalEntityGateInputRequest ..> LogisticAddressGateDto LegalEntityGateInputResponse ..> LegalEntityIdentifier LegalEntityGateInputResponse ..>
LegalEntityState LegalEntityGateInputResponse ..> Classification LegalEntityGateInputResponse ..> AddressGateInputResponse LegalEntityGateOutputRequest ..>
LegalEntityIdentifier LegalEntityGateOutputRequest ..> LegalEntityState LegalEntityGateOutputRequest ..> Classification LegalEntityGateOutputRequest ..>
AddressGateOutputChildRequest LegalEntityGateOutputResponse ..> LegalEntityIdentifier LegalEntityGateOutputResponse ..> LegalEntityState
LegalEntityGateOutputResponse ..> Classification LegalEntityGateOutputResponse ..> AddressGateInputResponse LogisticAddressGateDto ..> AddressState
LogisticAddressGateDto ..> AddressIdentifier LogisticAddressGateDto ..> PhysicalPostalAddressGateDto LogisticAddressGateDto ..> AlternativePostalAddress
PageResponseLegalEntityGateInputResponse ..> LegalEntityGateInputResponse PageResponseLegalEntityGateOutputResponse ..> LegalEntityGateOutputResponse
PhysicalPostalAddressGateDto ..> GeoCoordinates PhysicalPostalAddressGateDto ..> StreetGate

    note for LegalEntityGateInputRequest "/api/catena/input/legal-entities [PUT] [requestBody]"

    note for PageResponseLegalEntityGateInputResponse "/api/catena/input/legal-entities [GET] [response]"

    note for LegalEntityGateOutputRequest "/api/catena/output/legal-entities [PUT] [requestBody]"

    note for PageResponseLegalEntityGateOutputResponse "/api/catena/output/legal-entities/search [POST] [response]"

class AddressGateInputResponse{ +string[]: nameParts +string[]: roles +string: externalId +string: legalEntityExternalId +string: siteExternalId +string: bpn
+AddressState[]: states; +AddressIdentifier[]: identifiers; +PhysicalPostalAddressGateDto: physicalPostalAddress; +AlternativePostalAddress:
alternativePostalAddress; } class AddressGateOutputChildRequest{ +string[]: nameParts +string[]: roles +string: bpn +AddressState[]: states;
+AddressIdentifier[]: identifiers; +PhysicalPostalAddressGateDto: physicalPostalAddress; +AlternativePostalAddress: alternativePostalAddress; } class
AddressIdentifier{ +string: value +string: type } class AddressState{ +string: description +date: validFrom +date: validTo +string: type } class
AlternativePostalAddress{ +string: country +string: postalCode +string: city +string: administrativeAreaLevel1 +string: deliveryServiceNumber +string:
deliveryServiceType +string: deliveryServiceQualifier +GeoCoordinates: geographicCoordinates; } class Classification{ +string: value +string: code +string: type
} class GeoCoordinates{ +number: longitude +number: latitude +number: altitude } class LegalEntityGateInputRequest{ +string[]: legalNameParts +string:
legalShortName +string: legalForm +string[]: roles +string: externalId +LegalEntityIdentifier[]: identifiers; +LegalEntityState[]: states; +Classification[]:
classifications; +LogisticAddressGateDto: legalAddress; } class LegalEntityGateInputResponse{ +string[]: legalNameParts +string: legalShortName +string:
legalForm +string[]: roles +string: externalId +LegalEntityIdentifier[]: identifiers; +LegalEntityState[]: states; +Classification[]: classifications;
+AddressGateInputResponse: legalAddress; } class LegalEntityGateOutputRequest{ +string[]: legalNameParts +string: legalShortName +string: legalForm +string[]:
roles +string: externalId +string: bpn +LegalEntityIdentifier[]: identifiers; +LegalEntityState[]: states; +Classification[]: classifications;
+AddressGateOutputChildRequest: legalAddress; } class LegalEntityGateOutputResponse{ +string: legalShortName +string: legalForm +string[]: legalNameParts
+string[]: roles +string: externalId +string: bpn +LegalEntityIdentifier[]: identifiers; +LegalEntityState[]: states; +Classification[]: classifications;
+AddressGateInputResponse: legalAddress; } class LegalEntityIdentifier{ +string: value +string: type +string: issuingBody } class LegalEntityState{ +string:
officialDenotation +date: validFrom +date: validTo +string: type } class LogisticAddressGateDto{ +string[]: nameParts +string[]: roles +AddressState[]: states;
+AddressIdentifier[]: identifiers; +PhysicalPostalAddressGateDto: physicalPostalAddress; +AlternativePostalAddress: alternativePostalAddress; } class
PageResponseLegalEntityGateInputResponse{ +number: totalElements +number: totalPages +number: page +number: contentSize +LegalEntityGateInputResponse[]:
content; } class PageResponseLegalEntityGateOutputResponse{ +number: totalElements +number: totalPages +number: page +number: contentSize
+LegalEntityGateOutputResponse[]: content; } class PhysicalPostalAddressGateDto{ +string: country +string: postalCode +string: city +string:
administrativeAreaLevel1 +string: administrativeAreaLevel2 +string: administrativeAreaLevel3 +string: district +string: companyPostalCode +string:
industrialZone +string: building +string: floor +string: door +GeoCoordinates: geographicCoordinates; +StreetGate: street; } class StreetGate{ +string:
namePrefix +string: additionalNamePrefix +string: name +string: additionalNameSuffix +string: houseNumber +string: milestone +string: direction +string:
nameSuffix }