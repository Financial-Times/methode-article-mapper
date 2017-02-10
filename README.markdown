# Methode Article Mapper
Methode Article Mapper is a Dropwizard application which consumes Kafka events and maps raw Methode articles to content according to UPP JSON format.

## Introduction
This application depends on the following micro-services:

* kafka-proxy;
* document store API;
* concordance API.

## Running

`java -jar target/methode-article-mapper-service-1.0-SNAPSHOT.jar server methode-article-mapper.yaml`

## Endpoints

### Posting content to be mapped

Transformation can be triggered through a POST message containing a Methode article to http://localhost:11070/content-transform/{uuid}
The {uuid} value must match the UUID of the posted Methode article.
In case the required transformation is triggered to provide an article preview, you need to set a `preview` query parameter in the URL with `true` as value: 
e.g., http://localhost:11070/content-transform/d8bca7c3-e8b8-4dbf-9bd1-4df8d2e0c086?preview=true 
This `preview` setting will not trigger an exception in case of empty article body.

### Healthcheck

A GET request to http://localhost:11071/healthcheck or http://localhost:11070/__health

## Example of transformation output 
You can find an example of a transformed article below. 

```
{
  "uuid": "59b2f3b4-69c2-11e6-a0b1-d87a9fea034f",
  "title": "Myanmar calls on Kofi Annan to head landmark Rohingya effort",
  "alternativeTitles": {
    "promotionalTitle": "Annan to head new Myanmar Rohingya effort"
  },
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
  "description": null,
  "mediaType": null,
  "pixelWidth": null,
  "pixelHeight": null,
  "internalBinaryUrl": null,
  "externalBinaryUrl": null,
  "members": null,
  "mainImage": "af6b69ba-69ce-11e6-3ed7-4fe0459ea806",
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
  "canBeSyndicated": "yes"
  "firstPublishedDate": "2016-08-15T10:31:22.000Z"
}
```
