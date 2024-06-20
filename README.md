# data-service

## Overview
Lifecycle events are stored in Elasticsearch by realm; i.e., process data and indexes.

## Ingestion
Collects the events from data ingestion.

## Functional Tests
Functional tests are only run for direction ingestion and not for async

## Local Development
For ingestion, need to have the services up and running:
- Config, Auth, Gateway, and Ingest
- The data processor services need to be up and running

```sh
./gradlew clean bootrun
```

## Kafka consumer configurations
Consumer configurations can be overridden/added via helm charts using the kafka.consumer.properties value.
This allows to pass in different configurations based on the environment; however, a deployment is required.
