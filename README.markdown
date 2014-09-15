# Methode Transformer
Methode Transformer is a Dropwizard application which responds to a request for a piece of content by 
obtaining it from Methode and converting it into the canonical universal publishing json format.

## Introduction
This application uses the following APIs:
 * Methode API

## Running
In order to run the project, please run com.ft.methodetransformer.MethodeTransformerApplication with the following program
parameters: server methode-article-transformer.yaml

Please make sure you are running it in the correct working directory (methode-article-transformer-service).

Healthcheck: http://localhost:11071/healthcheck

## Content retrieval
Make a GET request to http://localhost:11070/content/{uuid} with Content-Type set to application/json.

You will receive a json response for the Content. As an example:

{
"uuid": "3c99c2ba-a6ae-11e2-95b1-00144feabdc0",
"headline": "a headline",
"byline": "By someone",
"source": "FT",
"lastPublicationDate": "2014-01-01T00:00:00.000Z",
"body": "<body>The body</body>"
}



