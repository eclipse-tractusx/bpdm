````mermaid
classDiagram

PageResponseLegalFormResponse ..> LegalFormResponse


class LegalFormResponse{
+string: technicalKey
+string: name
+string: abbreviation
}

class PageResponseLegalFormResponse{
+number: totalElements
+number: totalPages
+number: page
+number: contentSize
+LegalFormResponse[]: content;
}

````