# Deploy-only image: the WAR is built on the host by build.ps1 first (this
# project has no Maven/Gradle, see README), this image just runs it.
#
# Payara Server Full is used instead of a raw GlassFish image because GlassFish
# itself has no actively maintained official Docker image; Payara is the same
# Jakarta EE 10 runtime (a certified GlassFish derivative) and IS maintained.
FROM payara/server-full:6.2025.1-jdk17

# The connection pool in WEB-INF/glassfish-resources.xml is created by the
# container before the app loads, so the JDBC driver has to sit on the
# server's own classpath (domain lib), not inside the WAR. Same reason
# run.ps1 copies it into server\...\domain1\lib for the local GlassFish.
ADD https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar \
    ${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/lib/mysql-connector-j-8.4.0.jar

# Auto-deployed on container start: the base image's entrypoint scans
# DEPLOY_DIR and deploys everything in it.
COPY dist/placement.war ${DEPLOY_DIR}/placement.war

EXPOSE 8080 4848
