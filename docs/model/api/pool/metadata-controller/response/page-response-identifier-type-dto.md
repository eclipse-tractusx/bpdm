````mermaid
classDiagram

IdentifierTypeDto ..> IdentifierTypeDetailDto
PageResponseIdentifierTypeDto ..> IdentifierTypeDto

class PageResponseIdentifierTypeDto{
+number: totalElements
+number: totalPages
+number: page
+number: contentSize
+IdentifierTypeDto[]: content;
}
class IdentifierTypeDetailDto{
+string: country
+bool: mandatory
}
class IdentifierTypeDto{
+string: technicalKey
+string: businessPartnerType
+string: name
+IdentifierTypeDetailDto[]: details;
}

````