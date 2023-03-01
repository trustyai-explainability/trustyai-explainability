# Copyright 2023 Red Hat
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Build arguments
ARG SOURCE_CODE=.
ARG CI_CONTAINER_VERSION="unknown"


## Livebuilder CODE BEGIN ##
FROM registry.access.redhat.com/ubi8/openjdk-17:1.14-3.1661798365 AS build

## Build args to be used at this step
ARG SOURCE_CODE

USER root
WORKDIR /build
ENV MAVEN_OPTS="-Dfile.encoding=UTF8"

RUN sed -i 's:security.provider.12=SunPKCS11:#security.provider.12=SunPKCS11:g' /usr/lib/jvm/java-17-openjdk-*/conf/security/java.security \
    && sed -i 's:#security.provider.1=SunPKCS11 ${java.home}/lib/security/nss.cfg:security.provider.12=SunPKCS11 ${java.home}/lib/security/nss.cfg:g' /usr/lib/jvm/java-17-openjdk-*/conf/security/java.security

COPY ${SOURCE_CODE}/explainability-core ./explainability-core
COPY ${SOURCE_CODE}/explainability-connectors ./explainability-connectors
COPY ${SOURCE_CODE}/explainability-service ./explainability-service
COPY ${SOURCE_CODE}/explainability-arrow ./explainability-arrow
COPY ${SOURCE_CODE}/explainability-integrationtests ./explainability-integrationtests
COPY ${SOURCE_CODE}/pom.xml ./pom.xml

# build and clean up everything we don't need
RUN mvn -B clean package --file pom.xml -P service-minimal -DskipTests; rm -Rf explainability-core explainability-connectors explainability-arrow explainability-integrationtests

## Livebuilder CODE END ##


###############################################################################
FROM registry.redhat.io/ubi8/openjdk-17-runtime:1.14 as runtime
ENV LANGUAGE='en_US:en'

## Livebuilder CODE BEGIN ##
# We make four distinct layers so if there are application changes the library layers can be re-used
COPY --from=build /build/explainability-service/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build /build/explainability-service/target/quarkus-app/*.jar /deployments/
COPY --from=build /build/explainability-service/target/quarkus-app/app/ /deployments/app/
COPY --from=build /build/explainability-service/target/quarkus-app/quarkus/ /deployments/quarkus/
## Livebuilder CODE END ##


## Build args to be used at this step
ARG CI_CONTAINER_VERSION
ARG USER=185
ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 -Djava.zutil.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

LABEL com.redhat.component="odh-trustyai-service" \
      name="managed-open-data-hub/odh-trustyai-service-rhel8" \
      version="${CI_CONTAINER_VERSION}" \
      summary="odh-trustyai-service" \
      io.openshift.expose-services="" \
      io.k8s.display-name="odh-trustyai-service" \
      maintainer="['managed-open-data-hub@redhat.com']" \
      description="TrustyAI is a service to provide integration fairness and bias tracking to modelmesh-served models" \
      com.redhat.license_terms="https://www.redhat.com/licenses/Red_Hat_Standard_EULA_20191108.pdf"
