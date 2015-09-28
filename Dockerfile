FROM up-registry.ft.com/coco/dropwizardbase

ADD .git/ /.git/
ADD methode-article-transformer-service/ /methode-article-transformer-service/
ADD pom.xml /
RUN apk --update add git \
 && cd methode-article-transformer-service \
 && HASH=$(git log -1 --pretty=format:%H) \
 && mvn install -DskipTests -Dbuild.git.revision=$HASH -Djava.net.preferIPv4Stack=true \
 && rm -f target/methode-article-transformer-service-*sources.jar \
 && mv target/methode-article-transformer-service-*.jar /app.jar \
 && mv methode-article-transformer.yaml /config.yaml \
 && apk del git \
 && rm -rf /var/cache/apk/* \
 && rm -rf /root/.m2/*
EXPOSE 8080 8081

CMD java -Ddw.server.applicationConnectors[0].port=8080 \
     -Ddw.server.adminConnectors[0].port=8081 \
     -Ddw.sourceApi.endpointConfiguration.primaryNodes=$HOSTNAME:8080 \
     -Ddw.semanticContentStoreReader.endpointConfiguration.primaryNodes=$HOSTNAME:8080 \
     -Ddw.logging.appenders[0].logFormat="%-5p [%d{ISO8601, GMT}] %c: %X{transaction_id} %replace(%m%n[%thread]%xEx){'\n', '|'}%nopex%n" \
     -jar app.jar server config.yaml