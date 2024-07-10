Feature: Error Handling in Business Partner Creation and Update
  Scenario Outline: Validate error handling for missing or invalid data
    Given the sharing member provides invalid business partner input with missing "<errorField>"
    When the sharing member attempts to create the business partner
    Then the sharing member should receive an error message "<errorMessage>"

    Examples:
      | errorField                                | errorMessage                                                |
      | physicalAddress.country                   | Physical Address has no country                              |
      | physicalAddress.city                      | Physical Address has no city                                 |
      | alternativeAddress.country                | Alternative Address has no country                           |
      | alternativeAddress.city                   | Alternative Address has no city                              |
      | alternativeAddress.deliveryServiceType    | Alternative Address has no delivery service type             |
      | alternativeAddress.deliveryServiceNumber  | Alternative Address has no delivery service number           |