<!-- Template based on: https://adr.github.io/madr/ -->

<!-- These are optional elements. Feel free to remove any of them. -->
<!-- * status: {proposed | rejected | accepted | deprecated | … | superseded by [ADR-0005](0005-example.md)} -->
* status: accepted
* date: 2023-09-01
* deciders: catena-x bpdm team
<!-- * informed: {list everyone who is kept up-to-date on progress; and with whom there is a one-way communication} -->
---
<!-- we need to disable MD025, because we use the different heading "ADR Template" in the homepage (see above) than it is foreseen in the template -->
<!-- markdownlint-disable-next-line MD025 -->
# Using an API based service component approach for orchestration logic instead of a message bus approach

## Context and Problem Statement

Based on this [github issue](https://github.com/eclipse-tractusx/bpdm/issues/377) an orchestration logic is needed for the bpdm solution to manage communication between services and handles processing states of business partner records during the golden record process.

Orchestration logic can basically be realized via an API and service based approach or via a message bus approach. To keep on going with the development of BPDM solution a decision is needed which approach the team will follow to plan and implement the next tasks.

## Considered Options

1. Using an API based service communication with an orchestrator service to handle business logic
2. Using a messaging based service communication with a message bus to handle business logic
3. Using a combination of orchestrator service together with a message bus to handle business logic

## Decision Outcome

### Chosen option: "1. Using an API based service communication with an orchestrator service to handle business logic", because

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

### Decision against option 2. "Using a messaging based service communication with a message bus to handle business logic", because
 
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

### **Decision against option 3. "Using a combination of orchestrator service together with a message bus to handle business logic", because**

* Please see the downsides above for option 2

### **Sum-up:**
> Arguments or advantages that comes with message bus, like a push mechanism, decoupling of services and asynchronous communication can also be realized via an API-based service interaction approach. Use cases for message bus are more focused on scenarios where you have to handle a lot of messages together with lots of message producers and consumers where most of them might be unknown in the network. But in our use case services are well-known and the number of producers and consumers are not that high. In addition, instead of communication via message bus, a callback approach for asynchronous communication might be more sufficient and could also be easier secured via EDC communication.

  
* **Push mechanism**: In regards to push mechanism, we do not have time critical requirements so polling is suitable for the moment. And in addition a push based solution can also be realized without a message bus in between the services.

* **Decoupling of services**: Making services more independent or decoupled is no good argument, because good API design also solves this issue and makes the services even more decoupled. In a message bus approach, every service depends on the input data and format which another service pushes inside

* **Asynchronous communication**: Asynchronous communication can be done via message bus as well as with API based communication

> **To sum up the benefits that brings a message bus approach, cannot be fully leveraged in our use case, so that the downsides outweigh the possible advantages.**

## Alternatives in more detail
### Using an API based service communication with an orchestrator service to handle business logic

[Here](https://github.com/eclipse-tractusx/bpdm/issues/377#issuecomment-1683880275) you can find a description of the first Variant.<p>
**❗Disclaimer**: Keep in mind that the shown interaction diagram is only a rough idea and the business logic and process flow must still be iterated and adjusted!

### Using a messaging based service communication with a message bus to handle business logic

[Here](https://github.com/eclipse-tractusx/bpdm/issues/377#issuecomment-1683924791) you can find a description of the second Variant. <p>
**❗Disclaimer**: Keep in mind that the shown interaction diagram is only a rough idea and the business logic and process flow must still be iterated and adjusted!

### Using a combination of orchestrator service together with a message bus to handle business logic

[Here](https://github.com/eclipse-tractusx/bpdm/issues/377#issuecomment-1683942552) you can find a description of the third Variant. <p>
**❗Disclaimer**: Keep in mind that the shown interaction diagram is only a rough idea and the business logic and process flow must still be iterated and adjusted!

<!-- This is an optional element. Feel free to remove. -->
## More Information / Outlook

(Further/Next Steps to be discussed)

Having in mind that a pushing mechanism might become required for a more efficient process orchestration or some other cases, it is not excluded to introduce an event queuing technology. We are open minded to this. But from current perspective we don't see hard requirements for this, so we want to focus on a minimal viable solution focusing on simplicity based on the KISS principle.

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
