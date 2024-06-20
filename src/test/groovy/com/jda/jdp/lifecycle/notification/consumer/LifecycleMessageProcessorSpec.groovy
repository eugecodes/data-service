/*
 * //==========================================================================
 * //               Copyright 2020, JDA Software Group, Inc.
 * //                            All Rights Reserved
 * //
 * //              THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF
 * //                         JDA SOFTWARE GROUP, INC.
 * //
 * //
 * //          The copyright notice above does not evidence any actual
 * //               or intended publication of such source code.
 * //
 * //==========================================================================
 */
package com.jda.jdp.lifecycle.notification.consumer

import com.by.dp.elasticsearch.ElasticsearchAdmin
import com.by.dp.elasticsearch.ElasticsearchOperationsFactory
import com.by.dp.elasticsearch.ElasticsearchOperationsWithMap
import com.by.dp.elasticsearch.ElasticsearchResult
import com.by.dp.elasticsearch.ElasticsearchSearchOperations

import com.by.dp.lifecycle.notification.consumer.LifecycleMessageProcessor
import com.jda.jdp.lifecycle.notification.utils.JSONFileUtils
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.Headers
import org.springframework.kafka.support.Acknowledgment
import spock.lang.Shared
import spock.lang.Specification

class LifecycleMessageProcessorSpec extends Specification implements JSONFileUtils {

    private static final String component = "application"
    private static final String entityType = "items"
    private static final String service = "SCPO"
    private static final String tenantId = "a4b7a825f67930965747445709011120"
    private static final String ingestionId = "a4b7a825f67930965747445709011120-dpingestion-20200917212207277-jM-dpsuccess"
    private static final String realmId ="t30f011232daea2f9"
    private static final String serviceType ="distribution"

    private static final String ENTITY_TYPE = "items"

    ElasticsearchOperationsFactory elasticsearchOperationsFactory = Mock(ElasticsearchOperationsFactory)
    ElasticsearchSearchOperations elasticsearchSearchOperations = Mock(ElasticsearchSearchOperations)
    ElasticsearchOperationsWithMap elasticsearchOperationsWithMap = Mock(ElasticsearchOperationsWithMap)
    ElasticsearchAdmin elasticsearchAdmin = Mock(ElasticsearchAdmin)
    Acknowledgment acknowledgment = Mock(Acknowledgment)
    LifecycleMessageProcessor messageConsumer = new LifecycleMessageProcessor(elasticsearchOperationsFactory, elasticsearchAdmin)
    String completed, error, downStreamError, invalidDateFormat, feedbackMessageWithNoEntityId, distributionCompleted, distributionCompletedWithRunId, distributionErrors, distributionInvalidDateFormat

    @Shared
    def topic = 'public.all.ingestion.status'

    def setup() {
        elasticsearchOperationsFactory.getSearchOperations() >> elasticsearchSearchOperations
        elasticsearchOperationsFactory.getOperationsWithMap() >> elasticsearchOperationsWithMap
        completed = readFromFile("samplepayload.json")
        error = readFromFile("sampleerrorpayload.json")
        invalidDateFormat = readFromFile("invalidDateFormat.json")
        downStreamError = readFromFile("samplePaylodwithnonAdmin.json")
        feedbackMessageWithNoEntityId = readFromFile("feedbackmessage_no_entityid.json")

        distributionCompleted = readFromFile("sampledistributioneventpayload.json")
        distributionCompletedWithRunId = readFromFile("sampledistributioneventpayloadwithrunid.json")
        distributionErrors = readFromFile("sampledistributionerroreventpayload.json")

        distributionInvalidDateFormat = readFromFile("invalidDateFormat.json")
    }



    def "data processor failed event is stored in elasticsearch "() {
        given:
        ConsumerRecord record = new ConsumerRecord<String, String>(topic, 1, 0, null, error)
        Headers headers = record.headers()
        headers.add("entityType", ENTITY_TYPE.getBytes())

        ElasticsearchResult<Map<String, Object>> result = new ElasticsearchResult<>()
        result.setSucceeded(true)
        result.setFound(true)
        elasticsearchAdmin.indexExistsNoCache(_ as String, _ as String) >> result
        elasticsearchSearchOperations.getIndexName(_ as String, _ as String) >> "t30f011232daea2f9_IngestionEvent"

        when: "public all ingestion has kafka message"
        messageConsumer.processStatusEvent(record, acknowledgment)

        then: "consumer should save message in elasticsearch index"
        1 * elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result
    }

    def "data processor failed event with error and no entityId is stored in elasticsearch "() {
        given:
        ConsumerRecord record = new ConsumerRecord<String, String>(topic, 1, 0, null, feedbackMessageWithNoEntityId)
        Headers headers = record.headers()
        headers.add("entityType", ENTITY_TYPE.getBytes())

        ElasticsearchResult<Map<String, Object>> result = new ElasticsearchResult<>()
        result.setSucceeded(true)
        result.setFound(true)
        elasticsearchAdmin.indexExistsNoCache(_ as String, _ as String) >> result
        elasticsearchSearchOperations.getIndexName(_ as String, _ as String) >> "t30f011232daea2f9_IngestionEvent"

        when: "public all ingestion has kafka message"
        messageConsumer.processStatusEvent(record, acknowledgment)

        then: "consumer should save message in elasticsearch index"
        1 * elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result
    }

    def "downstream error event is stored with entityId in  elasticsearch "() {
        given:
        ConsumerRecord record = new ConsumerRecord<String, String>(topic, 1, 0, null, downStreamError)
        Headers headers = record.headers()
        headers.add("entityType", ENTITY_TYPE.getBytes())

        ElasticsearchResult<Map<String, Object>> result = new ElasticsearchResult<>()
        result.setSucceeded(true)
        result.setFound(false)
        elasticsearchAdmin.indexExistsNoCache(_ as String, _ as String) >> result

        ElasticsearchResult<Map<String, Object>> createIndexElasticsearchResult = new ElasticsearchResult<>()
        createIndexElasticsearchResult.setSucceeded(true)
        1 * elasticsearchAdmin.createIndex(_ as String, _ as String, _ as String, _ as String) >> createIndexElasticsearchResult

        elasticsearchSearchOperations.getIndexName(_ as String, _ as String) >> "t30f011232daea2f9_IngestionEvent"

        when: "downstream error event have completed with error status"
        messageConsumer.processStatusEvent(record, acknowledgment)

        then: "consumer  should store message into elasticsearch"
        1 * elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result
    }

    def "test when entityType is empty from ingestion service"() {
        given:
        ConsumerRecord record = new ConsumerRecord<String, String>(topic, 1, 0, null, completed)
        ElasticsearchResult<Map<String, Object>> result = new ElasticsearchResult<>()
        result.setSucceeded(true)
        result.setFound(true)

        elasticsearchAdmin.indexExistsNoCache(_ as String, _ as String) >> result
        elasticsearchSearchOperations.getIndexName(_ as String, _ as String) >> "t30f011232daea2f9_IngestionEvent"
        elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> any()

        when: "when entityType is empty from ingest events"
        messageConsumer.processStatusEvent(record, acknowledgment)

        then: "consumer  should store message into elasticsearch"
        1 * elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result
    }

    def "message does not contain the right headers and there is not auth"() {
        given:
        String message = readFromFile("samplepayload-noauth.json")
        ConsumerRecord record = new ConsumerRecord<String, String>(topic, 1, 0, null, message)

        when: "when message is processed"
        messageConsumer.processStatusEvent(record, acknowledgment)

        then: "message is not stored"
        0 * elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String)
        1 * acknowledgment.acknowledge()
    }

    def "test distribution event with ingestionId"() {
        given:
        ConsumerRecord record = new ConsumerRecord<String, String>(topic, 1, 0, null, distributionCompleted)
        Headers headers = record.headers()
        headers.add("service", serviceType.getBytes())
        ElasticsearchResult<Map<String, Object>> result = new ElasticsearchResult<>()
        result.setSucceeded(true)
        result.setFound(true)

        elasticsearchAdmin.indexExistsNoCache(_ as String, _ as String) >> result
        elasticsearchSearchOperations.getIndexName(_ as String, _ as String) >> "t30f011232daea2f9_DistributionEvent"
        elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> any()

        when: "when entityType is empty from distribution events"
        messageConsumer.processStatusEvent(record, acknowledgment)

        then: "consumer  should store message into elasticsearch"
        1 * elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result
    }

    def "test distribution event with runId"() {
        given:
        ConsumerRecord record = new ConsumerRecord<String, String>(topic, 1, 0, null, distributionCompletedWithRunId)
        Headers headers = record.headers()
        headers.add("service", serviceType.getBytes())
        headers.add("realmId", realmId.getBytes())
        ElasticsearchResult<Map<String, Object>> result = new ElasticsearchResult<>()
        result.setSucceeded(true)
        result.setFound(true)

        elasticsearchAdmin.indexExistsNoCache(_ as String, _ as String) >> result
        elasticsearchSearchOperations.getIndexName(_ as String, _ as String) >> "t30f011232daea2f9_DistributionEvent"
        elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> any()

        when: "when entityType is empty from distribution events"
        messageConsumer.processStatusEvent(record, acknowledgment)

        then: "consumer  should store message into elasticsearch"
        1 * elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result
    }



    def "test distribution event with errors"() {
        given:
        ConsumerRecord record = new ConsumerRecord<String, String>(topic, 1, 0, null, distributionErrors)
        Headers headers = record.headers()
        headers.add("realmId", realmId.getBytes())
        headers.add("service", serviceType.getBytes())
        ElasticsearchResult<Map<String, Object>> result = new ElasticsearchResult<>()
        result.setSucceeded(true)
        result.setFound(true)

        elasticsearchAdmin.indexExistsNoCache(_ as String, _ as String) >> result
        elasticsearchSearchOperations.getIndexName(_ as String, _ as String) >> "t30f011232daea2f9_DistributionEvent"
        elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> any()

        when: "when entityType is empty from distribution events"
        messageConsumer.processStatusEvent(record, acknowledgment)

        then: "consumer  should store message into elasticsearch"
        1 * elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result
    }

    def "message does not contain the right headers and there is not auth for distribution"() {
        given:
        String message = readFromFile("sampledistibutionpayload-noauth.json")
        ConsumerRecord record = new ConsumerRecord<String, String>(topic, 1, 0, null, message)
        Headers headers = record.headers()
        headers.add("service", serviceType.getBytes())
        when: "when message is processed"
        messageConsumer.processStatusEvent(record, acknowledgment)

        then: "message is not stored"
        0 * elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String)
        1 * acknowledgment.acknowledge()
    }
}

