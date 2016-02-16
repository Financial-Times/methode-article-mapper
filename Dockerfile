FROM up-registry.ft.com/coco/dropwizardbase

ADD .git/ /.git/
ADD methode-article-transformer-service/ /methode-article-transformer-service/
ADD pom.xml /
ADD buildnum.txt /
ADD buildurl.txt /
RUN apk --update add git \
 && cd methode-article-transformer-service \
 && HASH=$(git log -1 --pretty=format:%H) \
 && BUILD_NUMBER=$(cat ../buildnum.txt) \
 && BUILD_URL=$(cat ../buildurl.txt) \
 && echo "DEBUG Jenkins job url: $BUILD_URL" \
 && mvn install -Dbuild.git.revision=$HASH -Dbuild.number=$BUILD_NUMBER -Dbuild.url=$BUILD_URL -Djava.net.preferIPv4Stack=true \
 && rm -f target/methode-article-transformer-service-*sources.jar \
 && mv target/methode-article-transformer-service-*.jar /app.jar \
 && mv methode-article-transformer.yaml /config.yaml \
 && apk del git \
 && rm -rf /var/cache/apk/* \
 && rm -rf /root/.m2/*
EXPOSE 8080 8081

CMD exec java -Ddw.server.applicationConnectors[0].port=8080 \
     -Ddw.server.adminConnectors[0].port=8081 \
     -Ddw.sourceApi.endpointConfiguration.primaryNodes=$VULCAN_HOST \
     -Ddw.documentStoreApi.endpointConfiguration.primaryNodes=$VULCAN_HOST \
     -Ddw.logging.appenders[0].logFormat="%-5p [%d{ISO8601, GMT}] %c: %X{transaction_id} %replace(%m%n[%thread]%xEx){'\n', '|'}%nopex%n" \
     -jar app.jar server config.yaml
