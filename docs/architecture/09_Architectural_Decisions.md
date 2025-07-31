# Architecture Decisions


1. [Use a multi gate deployment approach to realize multi-tenancy](#use-a-multi-gate-deployment-approach-to-realize-multi-tenancy)
2. [EDC is not a mandatory, but recommended component for accessing BPDM Pool API/Data](#edc-is-not-a-mandatory-but-recommended-component-for-accessing-bpdm-pool-apidata)
3. [Using an API based service component approach for orchestration logic instead of a message bus approach](#using-an-api-based-service-component-approach-for-orchestration-logic-instead-of-a-message-bus-approach)
4. [Limitations of OpenAPI text descriptions](#limitations-of-openapi-text-descriptions)
5. [Recommended usage scenarios of an EDC enabled communication in Business Partner Data Management Solution](#recommended-usage-scenarios-of-an-edc-enabled-communication-in-business-partner-data-management-solution)

## ~~Use a multi gate deployment approach to realize multi-tenancy~~

> **Disclaimer:** This decision has been overturned as deploying one gate per tenant is not feasible with thousands of tenants

* status: accepted
* date: 2023-06-01
* deciders: devs, architects
* consulted: ea, pca

### Context and Problem Statement

In BPDM a wide range of CX Member share their business partner data with our system. It must be ensured that each CX Member has only access to its own data. That's why our system must realize some kind of multi-tenancy.

<!-- This is an optional element. Feel free to remove. -->
### Decision Drivers

* in the automotive industry there are requirements and standards like TISAX that high confidential business partner data must be stored in secure manner

### Considered Options

* Use one Gate and implement multi-tenancy within the code base and database
* Use multiple Gates so that every member will have its own Gate with database

### Decision Outcome

Chosen option: "Use multiple Gates so that every member will have its own Gate with database", because so far its the most easiest and secure way to realize multi-tenancy in context of a reference implementation. It also provides the highest flexibility regarding to possible upcoming requirements. For example perspectively Gates could be deployed in different regions or locations. Also data is stored by default in different databases which gives additional security by default.

<!-- This is an optional element. Feel free to remove. -->
#### Consequences

* Good, because easier Identity and Access Management
* Good, because data separation by default
* Good, because better failure tolerance.
* Good, because flexibility in upcoming requirements.
* Bad, because we need a separate deployment and configuration for a new Gate when a new CX Member wants to use BPDM Service. As reference implementation this is fine, for production Usecases these deployments can be automated.

#### Implications on EDC and Asset Configuration
* Even if there are multiple BPDM Gate instances there will be only one deployed EDC
* In fact, new EDC Assets and Configurations must be applied for each new Catena-X Member who subscribes BPDM Application Service
* In context of reference implementation it is done manually. For operationalization an Operator should automate this.

#### Implications on SMEs
* To exchange business partner data accross legal entities and enabling contract negotiation, each SME needs to have its own EDC
* The EDC itself can be provided as offer by the operator or other "EDC as a Service" Service Provider

#### Implications on Value-Added-Services
* Currently it is out-of-scope that BPDM provides a kind of list or routing mechanism about which Gates are available to consume. The team is evaluating the possibility getting this information based on Catena-X Portal registrations.
* In fact for reference implementation a customer who wants to subscribe a Value Added Services has to provide his Gate/EDC Endpoints
* The Value Added Services also have to ensure by its own to secure and separate the data of each customer

<!-- This is an optional element. Feel free to remove. -->
### Pros and Cons of the Options

#### Use one Gate and implement multi-tenancy within the code base and database

* Good, because only one deployment is required
* Good, because better cost saving, because only one database is used
<!-- use "neutral" if the given argument weights neither for good nor bad -->
* Bad, because higher implementation effort
* Bad, because unknown requirements in data separation. If data **must** be stored in different databases, all our efforts would be for nothing.
* … <!-- numbers of pros and cons can vary -->

#### Use multiple Gates so that every member will have its own Gate with database

* Good, because easier Identity and Access Management
* Good, because data separation by default
* Good, because better failure tolerance.
* Good, because flexibility in upcoming requirements.
* Bad, because we need a separate deployment and configuration for a new Gate when a new CX Member wants to use BPDM Service. As reference implementation this is fine, for production Usecases these deployments can be automated.

## EDC is not a mandatory, but recommended component for accessing BPDM Pool API/Data

* status: accepted
* date: 2023-06-07
* deciders: devs, architects
* consulted: ea, pca

### Context and Problem Statement
Ensuring Data Sovereignty is a very crucial point to be compliant to Catena-X Guidelines and passing the Quality Gates. A key aspect to technical realize Data Sovereignty is the Eclipse Dataspace Component (EDC). The question for this ADR is, clarifying if an EDC is required to access the BPDM Pool API/Data.

### Decision Outcome
In alignment with PCA (Maximilian Ong) and BDA (Christopher Winter) it is not mandatory to have an EDC as a "Gatekeeper" in front of the BPDM Pool API for passing the Quality criteria/gates of Catena-X. Nevertheless it is recommended to use one. Especially when you think long-term about sharing data across other Dataspaces.

#### Reason
In case of BPDM Pool provides no confidential data about Business Partners. It's like a "phone book" which has public available data about the Business Partners which are commercially offered, because of the additional data quality and data enhancement features.

#### Implications
It must be ensured that only Catena-X Member have access to the BPDM Pool API. In Fact an Identity and Access Management is required in the Pool Backend which checks the technical users based on its associated roles and rights.


## Using an API based service component approach for orchestration logic instead of a message bus approach

### Context and Problem Statement

Based on this [github issue](https://github.com/eclipse-tractusx/bpdm/issues/377) an orchestration logic is needed for the bpdm solution to manage communication between services and handles processing states of business partner records during the golden record process.

Orchestration logic can basically be realized via an API and service based approach or via a message bus approach. To keep on going with the development of BPDM solution a decision is needed which approach the team will follow to plan and implement the next tasks.

### Considered Options

1. Using an API based service communication with an orchestrator service to handle business logic
2. Using a messaging based service communication with a message bus to handle business logic
3. Using a combination of orchestrator service together with a message bus to handle business logic

### Decision Outcome

#### Chosen option: "1. Using an API based service communication with an orchestrator service to handle business logic", because

* **Interoperability & Standardization**:
    * Interoperability can be better realized and standardized via standardized APIs to grant third party services access and helps to prevent a vendor lock-in.
    * Especially when thinking about BPDM as a reference implementation and there might be multiple operating environments in the future that offer BPDM solution. <p>

* **Flexibility**:
    * Thinking about future requirements that might come up like decentralized Gates, encryption of data, not storing business partner data for long-term, this solution is more flexible to deal with new requirements. <p>

* **Anonymity**:
    * Having a service that works as a proxy for the connection between Sharing Member data and cleaning services, can ensure that the uploaded data stay anonymous. <p>

* **Abstraction**:
    * The API based service approach allows better abstraction (who can access which kind of data?). Based on API access and the modelling of input and output data object, we can easily configure/decide which service should be able to access which kind of data or only sub-models of the data
    * Instead in a message bus and topic approach every subscriber would be able to easily see all data and can draw conclusions on ownership information and which sharing member was uploading which business partner data. <p>

* **Cost-effectiveness**:
    * Building up on the existing infrastructure instead of setting up and operating an additional message bus system. <p>

* **Request/Response Model**
    * Defined order via API, but not via messaging
    * Defined input and output formats / data models for service interaction

#### Decision against option 2. "Using a messaging based service communication with a message bus to handle business logic", because

* **Error handling**:
    * Error handling, error detection and tracing might become very complex in an event-based message bus architecture
    * Also race conditions might get problematic for event-based development <p>

* **Missing expertise**
    * Missing expertise in Catena-X team in regards to event-based data exchange (RabbitMQ, AMQP)
    * Missing expertise in operating and configuring securely a message bus system
    * Higher Effort in research, because of new concepts and business-logic for data processing and service interaction <p>

* **Cross-cutting concerns**:
    * Cross-cutting aspects should not depend on technology specific solutions like a message bus
    * Also there are already existing standard solutions available in for example Kubernetes or Spring Boot Framework <p>


* **Difficulty in interoperability and integration**:
    * Services in the chain need to 'play ball', they need to integrate into each other very well so well-defined payloads is important (Event Queue will just take any payload at first naturally)
    * No Request/Response Feedback <p>

* **Data Security**:
    * Cleaning requests in the queue are visible to every Gate. Even if business partners are anonymous in principle this could be a security issue.
    * Separate queues can also be problematic as it makes it visible in a message bus which Gate shared what business partner. So conclusions can be drawn which Member interacts with which business partners. <p>

* **Higher Costs**:
    * potential higher cost operating cluster<p>

* **Complexity**:
    * More complexity due to the Gates having to integrate to a message bus as well as an additional service
    * More complexity, because of bigger changes in business logic <p>

* **Less flexibility for maybe upcoming requirements**
    * Hypothesis: We assume it will be easier to implement EDC with an API based service orchestrator solution than with  a message bus system
    * Not clear how message queuing based solution would work with EDC component/communication
    * Not clear how a decentralized approach would look like with an message bus approach <p>

#### **Decision against option 3. "Using a combination of orchestrator service together with a message bus to handle business logic", because**

* Please see the downsides above for option 2

#### **Sum-up:**
> Arguments or advantages that comes with message bus, like a push mechanism, decoupling of services and asynchronous communication can also be realized via an API-based service interaction approach. Use cases for message bus are more focused on scenarios where you have to handle a lot of messages together with lots of message producers and consumers where most of them might be unknown in the network. But in our use case services are well-known and the number of producers and consumers are not that high. In addition, instead of communication via message bus, a callback approach for asynchronous communication might be more sufficient and could also be easier secured via EDC communication.


* **Push mechanism**: In regards to push mechanism, we do not have time critical requirements so polling is suitable for the moment. And in addition a push based solution can also be realized without a message bus in between the services.

* **Decoupling of services**: Making services more independent or decoupled is no good argument, because good API design also solves this issue and makes the services even more decoupled. In a message bus approach, every service depends on the input data and format which another service pushes inside

* **Asynchronous communication**: Asynchronous communication can be done via message bus as well as with API based communication

> **To sum up the benefits that brings a message bus approach, cannot be fully leveraged in our use case, so that the downsides outweigh the possible advantages.**

### Alternatives in more detail
#### Using an API based service communication with an orchestrator service to handle business logic

[Here](https://github.com/eclipse-tractusx/bpdm/issues/377#issuecomment-1683880275) you can find a description of the first Variant.<p>
**❗Disclaimer**: Keep in mind that the shown interaction diagram is only a rough idea and the business logic and process flow must still be iterated and adjusted!

#### Using a messaging based service communication with a message bus to handle business logic

[Here](https://github.com/eclipse-tractusx/bpdm/issues/377#issuecomment-1683924791) you can find a description of the second Variant. <p>
**❗Disclaimer**: Keep in mind that the shown interaction diagram is only a rough idea and the business logic and process flow must still be iterated and adjusted!

#### Using a combination of orchestrator service together with a message bus to handle business logic

[Here](https://github.com/eclipse-tractusx/bpdm/issues/377#issuecomment-1683942552) you can find a description of the third Variant. <p>
**❗Disclaimer**: Keep in mind that the shown interaction diagram is only a rough idea and the business logic and process flow must still be iterated and adjusted!

<!-- This is an optional element. Feel free to remove. -->
### More Information / Outlook

(Further/Next Steps to be discussed)

Having in mind that a pushing mechanism might become required for a more efficient process orchestration or some other cases, it is not excluded to introduce an event queuing technology. We are open minded to this. But from current perspective we don't see hard requirements for this, so we want to focus on a minimal viable solution focusing on simplicity based on the KISS principle.


## Limitations of OpenAPI text descriptions

### Context and Problem Statement

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

### Considered Options

* Programmatically change the schema description of specific generic class instances (Workaround for issue 1).
* Programmatically create a schema clone for each case a specific field description is needed (Workaround for issue 2).
* Live with the OpenAPI limitations.

### Decision Outcome

Chosen option: "Live with the OpenAPI limitations", because the improvement is not worth the added complexity.

<!-- This is an optional element. Feel free to remove. -->

### Pros and Cons of the Options

#### Programmatically change the schema description of specific generic class instances (Workaround for issue 1)

Using the workaround described
in [Github issue: Ability to define different schemas for the same class](https://github.com/springdoc/springdoc-openapi/issues/685) it is possible to manually
override the description of each generated schema corresponding to a specific type instance in the `OpenAPI` configuration object, e.g.
for `TypeKeyNameVerboseDto<CountryCode>` the generated schema name is `TypeKeyNameVerboseDtoCountryCode`.

* Good, because this allows specific text descriptions for generic type instances (solves issue 1).
* Bad, because the descriptions must be assigned in the OpenAPI configuration class, not in the specific DTOs as for other descriptions.
* Bad, because this is hard to maintain.

This option could be potentially improved introducing custom annotations that define the description for a specific type instance inside the relevant DTO,
like `@GenericSchema(type=CountryCode::class, description="...")"`. But the result is not worth the effort.

#### Programmatically create a schema clone for each case a specific field description is needed (Workaround for issue 2)

This is based on the first option but additionally adds schema clones with different name and description, e.g. `legalAddressAliasForLogisticAddressDto` might
be the clone of `LogisticAddressDto` used for field `legalAddress`. This schema name is referred by the field
using `@get:Schema(ref = "legalAddressAliasForLogisticAddressDto")`.

* Bad, because this adds additional nearly identical class schemas that show up in the documentation.
* Bad, because the descriptions must be assigned in the OpenAPI configuration class, not in the specific DTOs as for other descriptions.
* Bad, because the correct schema clone must be referenced for each field using it which is very error-prone and inconsistent to other fields (
  using `@get:Schema(ref=...)` instead of `@get:Schema(description=...)`).
* Bad, because this is hard to maintain.

<!-- This is an optional element. Feel free to remove. -->

### More Information

The potential workarounds are implemented as proof-of-concept
in [Github pull request: Schema overriding hook for OpenApiConfig](https://github.com/eclipse-tractusx/bpdm/pull/405).

## Recommended usage scenarios of an EDC enabled communication in Business Partner Data Management Solution

### Context and Problem Statement

Again and again the discussion arises in which scenarios third party applications (also often called value-added-services (VAS)) must use EDC enabled communication and in which scenarios no EDC is needed. In this document we want to outpoint some scenarios and give guidance for it.

> :warning: NOTE: <p>
> In the following diagrams the EDC component might be added multiple times within the same operating environment. This does not mean that multiple instances of EDC are used. It should only make more transparent when data or API calls takes place via EDC. It's on conceptual level, not on logical or physical. It's up to you how many instances of EDC you are operating.

#### TLDR;

> EDC enabled communication must always be used when business data get exchanged between the systems of different legal entities!

> For reference implementations you should always assume that the value-added-service will be operated by a different operating environment than the operating environment of the core Business Partner Data Management Solution! That means the reference implementation must support EDC enabled communication between itself and the Business Partner Data Management Solution!

### Scenario 1.1: External web application/service that only visualizes data based on gate data and/or pool data

#### Description
In this scenario a third party service provider offers a value added services that implements a web dashboard to visualize processed data based on bpdm gate data and/or pool data and presenting it via this dashboard to the customer who owns the bpdm gate data.

> EDC enabled communication is needed between the Master Data Management System of the Sharing Member and the bpdm gate operated by the Operating Environment.

> EDC enabled communication is needed between the bpdm gate and the backend service that processes the data.

> EDC enabled communication is needed between the bpdm pool and the backend service that processes the data.

> No EDC is needed for presenting the visualization via a web frontend to the customer.

![External web application that only visualizes data based on gate data](assets/edc_usage_1_1.drawio.svg)

### Scenario 1.2: Internal web application that only visualizes data based on gate data and/or pool data

#### Description
In this scenario the operating environment itself operates a web application that implements a web dashboard to visualize processed data based on bpdm gate data and/or pool data and presenting it via this dashboard to the customer who owns the bpdm gate data.

> EDC enabled communication is needed between the Master Data Management System of the Sharing Member and the bpdm gate operated by the Operating Environment.

> No EDC enabled communication is needed for the backend service, processing gate and/or pool data, since every component is operated by the same legal entity, the operating environment.

> No EDC is needed for presenting the visualization via a web frontend to the customer.


![Internal web app that only visualizes data based on gate data](assets/edc_usage_1_2.drawio.svg)

### Scenario 2.1: External web application/service that provides enriched data based on gate data and/or pool data

#### Description
In this scenario a third party service provider offers a value added services that implements an interface for exchanging data between its own backend system and the system of the customer. This means that business data get exchanged between the systems of two different legal entities.

> EDC enabled communication is needed between the Master Data Management System of the Sharing Member and the bpdm gate operated by the Operating Environment.

> EDC enabled communication is needed between the bpdm gate and the backend service that processes the data.

> EDC enabled communication is needed between the bpdm pool and the backend service that processes the data.

> EDC enabled communication is needed between the value-added-service backend and the customer system.

![External web application that only visualizes data based on gate data](assets/edc_usage_2_1.drawio.svg)

### Scenario 2.2: Internal web application/service that provides enriched data based on gate data and/or pool data

#### Description
In this scenario the operating environment itself operates a backend service or value added service that processes bpdm gate and/or pool data and implements an interface for exchanging data between its own backend system and the system of the customer. This means that business data get exchanged between the systems of two different legal entities.

> EDC enabled communication is needed between the Master Data Management System of the Sharing Member and the bpdm gate operated by the Operating Environment.

> EDC enabled communication is needed between the value-added-service backend and the customer system.

> No EDC enabled communication is needed between the bpdm gate and the backend service that processes the data.

> No EDC enabled communication is needed between the bpdm pool and the backend service that processes the data.


![Internal web application/service that provides enriched data based on gate data and/or pool data](assets/edc_usage_2_2.drawio.svg)


## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2024 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2024 SAP SE
- SPDX-FileCopyrightText: 2023,2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2024 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2024 Robert Bosch GmbH
- SPDX-FileCopyrightText: 2023,2024 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2024 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm