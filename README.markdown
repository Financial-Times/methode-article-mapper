[![CircleCI](https://circleci.com/gh/Financial-Times/methode-article-mapper.svg?style=svg)](https://circleci.com/gh/Financial-Times/methode-article-mapper) [![Coverage Status](https://coveralls.io/repos/github/Financial-Times/methode-article-mapper/badge.svg)](https://coveralls.io/github/Financial-Times/methode-article-mapper)

# Methode Article Mapper
Methode Article Mapper is a Dropwizard application which receives native Methode content (either from a Kafka topic or HTTP) and maps articles into content according to UPP JSON format.

## Introduction
This application has optional dependencies on the following micro-services:
* kafka-proxy;
* document store API (required to check and transform links within an article to other FT content);
* concordance API (required to transform `<company>` links into FT tear-sheets).

Without the Kafka proxy, the mapper only handles HTTP traffic. Set `messagingEndpointEnabled` to `false` in the configuration YAML or on the command-line to disable Kafka integration.

Without the document store or concordance APIs, the mapper only works in `suggest` mode (see below for mapping modes). Set `documentStoreApiEnabled` or `concordanceApiEnabled`, respectively, to `false` in the configuration YAML or on the command-line to disable these integrations.

* Dropwizard properties may be set on the command-line as JVM system properties with the prefix `dw.`, i.e. use `-Ddw.<properyName>=<value>`.

## Running

`java -jar target/methode-article-mapper-service-1.0-SNAPSHOT.jar server methode-article-mapper.yaml`

## Endpoints

### Posting content to be mapped

Transformation can be triggered through a POST message containing a Methode article to `http://localhost:11070/map`
* The legacy URL pattern `/content-transform/{uuid}` is also supported (though the UUID is no longer validated)

### Mapping modes
By default, the mapper applies the required validation and transformations for article publication. Other modes with less stringent validation and/or transformation rules can be selected using the `mode` query-string parameter. The supported values of mode are `publish` (the default), `preview` (for content preview) and `suggest` (for suggestion).
* Preview mode relaxes rules around workflow status, embargo date, transformable body, and so on.
    * The legacy query-string parameter `preview` is also supported with `true` as value.
    * Note that if `preview` and `mode` are both supplied and conflict with each other, the `mode` value is used. 
* Suggest mode relaxes rules as for preview, and also omits call-outs to the document store and concordance APIs (which do not affect the actual text content of the transformed document).

### Healthcheck

A GET request to `http://localhost:11071/healthcheck` or `http://localhost:11070/__health`

Disabling any of the Kafka, document store and concordance API integrations also removes dependency checks from this application's health check. If all integrations are disabled, a dummy healthcheck is added to prevent warning messages at application startup.

## Example of transformation output 

You can find an example of a transformed article below. 

```
{
  "uuid": "59b2f3b4-69c2-11e6-a0b1-d87a9fea034f",
  "title": "Myanmar calls on Kofi Annan to head landmark Rohingya effort",
  "alternativeTitles": {
    "promotionalTitle": "Annan to head new Myanmar Rohingya effort"
  },
  "type": "http://www.ft.com/ontology/content/ContentPackage",
  "byline": "Michael Peel, Bangkok Regional Correspondent",
  "brands": [
    {
      "id": "http://api.ft.com/things/dbb0bdae-1f0c-11e4-b0cb-b2227cce2b54"
    }
  ],
  "identifiers": [
    {
      "authority": "http://api.ft.com/system/FTCOM-METHODE",
      "identifierValue": "59b2f3b4-69c2-11e6-a0b1-d87a9fea034f"
    }
  ],
  "publishedDate": "2016-08-24T08:18:54.000Z",
  "standfirst": "Former UN chief will lead group to deal with problems that fuelled regional migration crisis",
  "body": "<body>The body of the article</body>",
  "description": "<p>Description text. Sed feugiat turpis at massa tristique sagittis.</p>",
  "mediaType": null,
  "pixelWidth": null,
  "pixelHeight": null,
  "internalBinaryUrl": null,
  "externalBinaryUrl": null,
  "members": null,
  "mainImage": "af6b69ba-69ce-11e6-3ed7-4fe0459ea806",
  "storyPackage": "d6fc8da7-fc26-4187-a38a-291442408969",
  "contentPackage": "45163790-eec9-11e6-abbc-ee7d9c5b3b90",
  "standout": {
    "editorsChoice": false,
    "exclusive": false,
    "scoop": false
  },
  "comments": {
    "enabled": true
  },
  "copyright": null,
  "webUrl": null,
  "publishReference": "tid_dn4kzqpoxf",
  "lastModified": "2016-08-25T06:06:23.532Z",
  "canBeSyndicated": "yes",
  "firstPublishedDate": "2016-08-15T10:31:22.000Z",
  "accessLevel": "subscribed",
  "canBeDsitributed": "yes"
}
```
