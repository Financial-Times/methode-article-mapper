# Methode Article Mapper for PAC

Methode Article Mapper is a microservice that transforms native Methode articles into UPP format. This microservice is used in PAC to return draft Methode articles in UPP format, which is used to suggest annotations based on article text.

## Primary URL

<https://pac-prod-glb.upp.ft.com/__methode-article-mapper/>

## Code

pac-methode-article-mapper

## Service Tier

Bronze

## Lifecycle Stage

Production

## Delivered By

content

## Supported By

content

## Known About By

- hristo.georgiev
- robert.marinov
- elina.kaneva
- georgi.ivanov
- tsvetan.dimitrov
- kalin.arsov
- mihail.mihaylov
- boyko.boykov
- donislav.belev
- dimitar.terziev

## Host Platform

AWS

## Architecture

Methode Article Mapper in PAC is a Dropwizard application which receives native Methode content through HTTP requests and maps articles into content according to UPP JSON format.

## Contains Personal Data

No

## Contains Sensitive Data

No

## Failover Architecture Type

ActiveActive

## Failover Process Type

FullyAutomated

## Failback Process Type

FullyAutomated

## Failover Details

The service is deployed in both PAC clusters. The failover guide for the cluster is located here:
<https://github.com/Financial-Times/upp-docs/tree/master/failover-guides/pac-cluster>

## Data Recovery Process Type

NotApplicable

## Data Recovery Details

The service does not store data, so it does not require any data recovery steps.

## Release Process Type

PartiallyAutomated

## Rollback Process Type

Manual

## Release Details

Manual failover is needed when a new version of the service is deployed to production. Otherwise, an automated failover is going to take place when releasing. For more details about the failover process please see: <https://github.com/Financial-Times/upp-docs/tree/master/failover-guides/pac-cluster>

## Key Management Process Type

Manual

## Key Management Details

To access the service clients need to provide basic auth credentials.
To rotate credentials you need to login to a particular cluster and update varnish-auth secrets.

## Monitoring

- PAC-Prod-EU health: <https://pac-prod-eu.upp.ft.com/__health/__pods-health?service-name=methode-article-mapper>
- PAC-Prod-US health: <https://pac-prod-us.upp.ft.com/__health/__pods-health?service-name=methode-article-mapper>

## First Line Troubleshooting

<https://github.com/Financial-Times/upp-docs/tree/master/guides/ops/first-line-troubleshooting>

## Second Line Troubleshooting

Please refer to the GitHub repository README for troubleshooting information.
