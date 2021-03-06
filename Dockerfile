FROM openjdk:8u212-jdk-alpine3.9

COPY . /methode-article-mapper

ARG SONATYPE_USER
ARG SONATYPE_PASSWORD
ARG GIT_TAG

ENV MAVEN_HOME=/root/.m2
ENV TAG=$GIT_TAG

RUN apk --update add git maven curl \
 && mkdir $MAVEN_HOME \
 && curl -v -o $MAVEN_HOME/settings.xml "https://raw.githubusercontent.com/Financial-Times/nexus-settings/master/public-settings.xml" \
 && cd methode-article-mapper \
 && HASH=$(git log -1 --pretty=format:%H) \
 && TAG=$(git tag --sort=committerdate | tail -1) \
 && VERSION=${TAG:-untagged} \
 && mvn versions:set -DnewVersion=$VERSION \
 && mvn install -Dbuild.git.revision=$HASH -Djava.net.preferIPv4Stack=true \
 && rm -f target/methode-article-mapper-*sources.jar \
 && mv target/methode-article-mapper-*.jar /methode-article-mapper.jar \
 && mv methode-article-mapper.yaml /config.yaml \
 && apk del git maven \
 && rm -rf /var/cache/apk/* \
 && rm -rf $MAVEN_HOME/*

EXPOSE 8080 8081

CMD exec java $JAVA_OPTS \
     -Ddw.server.applicationConnectors[0].port=8080 \
     -Ddw.server.adminConnectors[0].port=8081 \
     -Ddw.documentStoreApiEnabled=$DOCUMENT_STORE_API_ENABLED \
     -Ddw.documentStoreApi.endpointConfiguration.primaryNodes=$DOCUMENT_STORE_API_URL \
     -Ddw.concordanceApiEnabled=$CONCORDANCE_API_ENABLED \
     -Ddw.concordanceApi.endpointConfiguration.primaryNodes=$CONCORDANCE_API_URL \
     -Ddw.messagingEndpointEnabled=$KAFKA_ENABLED \
     -Ddw.consumer.messageConsumer.queueProxyHost=http://$KAFKA_PROXY_URL \
     -Ddw.producer.messageProducer.proxyHostAndPort=$KAFKA_PROXY_URL \
     -Ddw.apiHost=$API_HOST \
     -Ddw.additionalNativeContentProperties.$NATIVE_TX_REF \
     -Ddw.lastModifiedSource=$LAST_MODIFIED_SOURCE \
     -Ddw.transactionIdSource=$TX_ID_SOURCE \
     -Ddw.transactionIdProperty=$TX_PROPERTY \
     -Ddw.logging.appenders[0].logFormat="%-5p [%d{ISO8601, GMT}] %c: %X{transaction_id} %replace(%m%n[%thread]%xEx){'\n', '|'}%nopex%n" \
     -jar methode-article-mapper.jar server config.yaml
