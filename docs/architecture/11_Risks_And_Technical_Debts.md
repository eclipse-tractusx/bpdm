# Risks and Technical Debts

**Dependency on third party service provider**
* Currently we are not flexible enough to easily change the third party service provider for golden record creation. Therefore the next step will be to introduce an own data persistence layer, getting more independent.
* ✔️Solved via "Simulator Application"

**Data Storage and anonymize concept**
* How to anonymize the relations between CX-Member and its belonging Business Partner?
* 💡 Idea: using kind of "ticket numbering"
* ✔️ Solved via ticketing.

**Accessability for SMEs**
* Uploading via CSV File. Does it requires an EDC?
* ⚠️Current State: Yes, is needed.

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