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
+string: lsaType
+string: name
+IdentifierTypeDetailDto[]: details;
}

````