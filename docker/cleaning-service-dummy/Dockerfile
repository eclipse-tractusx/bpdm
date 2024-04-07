################################################################################
# Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
#
# See the NOTICE file(s) distributed with this work for additional
# information regarding copyright ownership.
#
# This program and the accompanying materials are made available under the
# terms of the Apache License, Version 2.0 which is available at
# https://www.apache.org/licenses/LICENSE-2.0.
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.
#
# SPDX-License-Identifier: Apache-2.0
################################################################################

FROM maven:3.9-eclipse-temurin-21 AS build
COPY . /home/app
WORKDIR /home/app
RUN mvn -B -U clean package -pl bpdm-cleaning-service-dummy -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
# Adding wget for the health check
RUN apk --no-cache add wget
COPY --from=build /home/app/bpdm-cleaning-service-dummy/target/bpdm-cleaning-service-dummy.jar /usr/local/lib/bpdm/app.jar
ARG USERNAME=bpdm
ARG USERID=10001
ARG GID=10001
RUN addgroup -g $GID -S $USERNAME
RUN adduser -u $USERID -S $USERNAME $USERNAME
USER $USERNAME
WORKDIR /usr/local/lib/bpdm
EXPOSE 8080
# Health check instruction
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8084/actuator/health || exit 1
ENTRYPOINT ["java","-jar","app.jar"]
