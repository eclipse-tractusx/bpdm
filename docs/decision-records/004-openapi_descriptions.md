<!-- Template based on: https://adr.github.io/madr/ -->

<!-- These are optional elements. Feel free to remove any of them. -->
<!-- * status: {proposed | rejected | accepted | deprecated | … | superseded by [ADR-0005](0005-example.md)} -->
<!-- * date: {YYYY-MM-DD when the decision was last updated} -->
<!-- * deciders: {list everyone involved in the decision} -->
<!-- * consulted: {list everyone whose opinions are sought (typically subject-matter experts); and with whom there is a two-way communication} -->
<!-- * informed: {list everyone who is kept up-to-date on progress; and with whom there is a one-way communication} -->
---
<!-- we need to disable MD025, because we use the different heading "ADR Template" in the homepage (see above) than it is foreseen in the template -->
<!-- markdownlint-disable-next-line MD025 -->

# Limitations of OpenAPI text descriptions

## Context and Problem Statement

There are two known issues with defining text descriptions in OpenAPI/SpringDoc that affect us:

1. Generic classes can't get specific schema descriptions determined by the type parameter using SpringDoc annotations.<br>
   Example: `TypeKeyNameVerboseDto<CountryCode>`<br>
   With SpringDoc's annotation `@Schema(description=...)` we can set a description for `TypeKeyNameVerboseDto` in general, but not
   for `TypeKeyNameVerboseDto<CountryCode>` specifically. Internally OpenAPI generates a specific class schema named `TypeKeyNameVerboseDtoCountryCode` that
   could theoretically have a different description.
2. There is an OpenAPI limitation not allowing to specify a field description for singular objects of complex type (contrary to collection objects of complex
   type and objects of primitive type),
   see [Github issue: Description of complex object parameters]( https://github.com/springdoc/springdoc-openapi/issues/1178).<br>
   E.g. OpenAPI supports field descriptions for `val name: String` and `val states: Collection<AddressStateDto>`, but *not*
   for `val legalAddress: LogisticAddressDto`.<br>
   The reason is that in the OpenAPI definition file, singular fields of complex type directly refer to the class schema using `$ref` and don't support a field
   description, while collection fields contain an automatic wrapper type which supports a description.<br>
   So the only description possible for the last example is the catch-all schema description of `LogisticAddressDto`. The user should ideally get a more
   specific description for the field `legalAddress` than for just any other `LogisticAddressDto`.

## Considered Options

* Programmatically change the schema description of specific generic class instances (Workaround for issue 1).
* Programmatically create a schema clone for each case a specific field description is needed (Workaround for issue 2).
* Live with the OpenAPI limitations.

## Decision Outcome

Chosen option: "Live with the OpenAPI limitations", because the improvement is not worth the added complexity.

<!-- This is an optional element. Feel free to remove. -->

## Pros and Cons of the Options

### Programmatically change the schema description of specific generic class instances (Workaround for issue 1)

Using the workaround described
in [Github issue: Ability to define different schemas for the same class](https://github.com/springdoc/springdoc-openapi/issues/685) it is possible to manually
override the description of each generated schema corresponding to a specific type instance in the `OpenAPI` configuration object, e.g.
for `TypeKeyNameVerboseDto<CountryCode>` the generated schema name is `TypeKeyNameVerboseDtoCountryCode`.

* Good, because this allows specific text descriptions for generic type instances (solves issue 1).
* Bad, because the descriptions must be assigned in the OpenAPI configuration class, not in the specific DTOs as for other descriptions.
* Bad, because this is hard to maintain.

This option could be potentially improved introducing custom annotations that define the description for a specific type instance inside the relevant DTO,
like `@GenericSchema(type=CountryCode::class, description="...")"`. But the result is not worth the effort.

### Programmatically create a schema clone for each case a specific field description is needed (Workaround for issue 2)

This is based on the first option but additionally adds schema clones with different name and description, e.g. `legalAddressAliasForLogisticAddressDto` might
be the clone of `LogisticAddressDto` used for field `legalAddress`. This schema name is referred by the field
using `@get:Schema(ref = "legalAddressAliasForLogisticAddressDto")`.

* Bad, because this adds additional nearly identical class schemas that show up in the documentation.
* Bad, because the descriptions must be assigned in the OpenAPI configuration class, not in the specific DTOs as for other descriptions.
* Bad, because the correct schema clone must be referenced for each field using it which is very error-prone and inconsistent to other fields (
  using `@get:Schema(ref=...)` instead of `@get:Schema(description=...)`).
* Bad, because this is hard to maintain.

<!-- This is an optional element. Feel free to remove. -->

## More Information

The potential workarounds are implemented as proof-of-concept
in [Github pull request: Schema overriding hook for OpenApiConfig](https://github.com/eclipse-tractusx/bpdm/pull/405).
