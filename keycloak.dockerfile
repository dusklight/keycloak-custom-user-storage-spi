# Build the project using maven (based on https://stackoverflow.com/questions/27767264/how-to-dockerize-maven-project-and-how-many-ways-to-accomplish-it)
FROM maven:3.8.4-openjdk-11-slim AS build
COPY ./ear-module /usr/src/maven/ear-module
COPY ./jar-module /usr/src/maven/jar-module
COPY ./pom.xml /usr/src/maven
RUN mvn --file /usr/src/maven/pom.xml clean package

# Copy the package from above to Keycloak deployments folder
FROM quay.io/keycloak/keycloak:15.0.2
COPY --from=build /usr/src/maven/ear-module/target/dusklight-keycloak-user-spi-ear-*.ear /opt/jboss/keycloak/standalone/deployments
