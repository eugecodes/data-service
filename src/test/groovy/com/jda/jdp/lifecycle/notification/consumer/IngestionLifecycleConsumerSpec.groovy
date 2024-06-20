/*
 * //==========================================================================
 * //               Copyright 2021, JDA Software Group, Inc.
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
import com.by.dp.lifecycle.notification.consumer.IngestionLifeCycleConsumer
import com.by.dp.lifecycle.notification.consumer.LifecycleMessageProcessor
import com.by.dp.service.util.compression.Compression
import com.by.dp.service.util.compression.CompressionFactory
import com.by.dp.service.util.compression.GzipCompression
import com.by.dp.service.util.compression.Lz4Compression
import com.by.dp.service.util.compression.NullCompression
import com.jda.jdp.lifecycle.notification.utils.JSONFileUtils
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.Headers
import org.springframework.kafka.support.Acknowledgment
import spock.lang.Shared
import spock.lang.Specification

import static com.by.dp.lifecycle.notification.common.Constants.CONTENT_TYPE_LOWER_CASE
import static com.by.dp.lifecycle.notification.common.Constants.GZ
import static com.by.dp.service.common.Constants.CARRIAGE_RETURN
import static com.by.dp.service.common.Constants.EMPTY_STRING
import static com.by.dp.service.common.Constants.NEWLINE

class IngestionLifecycleConsumerSpec extends Specification implements JSONFileUtils {

    private static final String component = "application"
    private static final String entityType = "items"
    private static final String service = "SCPO"
    private static final String tenantId = "a4b7a825f67930965747445709011120"
    private static final String ingestionId = "a4b7a825f67930965747445709011120-dpingestion-20200917212207277-jM-dpsuccess"
    private static final String realmId ="t30f011232daea2f9"
    private static final String serviceType ="distribution"

    ElasticsearchOperationsFactory elasticsearchOperationsFactory = Mock(ElasticsearchOperationsFactory)
    ElasticsearchSearchOperations elasticsearchSearchOperations = Mock(ElasticsearchSearchOperations)
    ElasticsearchOperationsWithMap elasticsearchOperationsWithMap = Mock(ElasticsearchOperationsWithMap)
    ElasticsearchAdmin elasticsearchAdmin = Mock(ElasticsearchAdmin)
    Acknowledgment acknowledgment = Mock(Acknowledgment)
    LifecycleMessageProcessor messageConsumer = new LifecycleMessageProcessor(elasticsearchOperationsFactory, elasticsearchAdmin)
    IngestionLifeCycleConsumer lifecycleConsumer = new IngestionLifeCycleConsumer(messageConsumer)
    String completed, invalidDateFormat, distributionCompleted, distributionInvalidDateFormat


    @Shared
    def topic = 'public.all.ingestion.status'

    def setup() {
        elasticsearchOperationsFactory.getSearchOperations() >> elasticsearchSearchOperations
        elasticsearchOperationsFactory.getOperationsWithMap() >> elasticsearchOperationsWithMap
        completed = readFromFile("samplepayload.json")
        invalidDateFormat = readFromFile("invalidDateFormat.json")
        distributionCompleted = readFromFile("sampledistributioneventpayload.json")
        distributionInvalidDateFormat = readFromFile("invalidDateFormat.json")
    }

    def "data processor success event is stored in elasticsearch "() {
        given:
        ConsumerRecord record = new ConsumerRecord<String, String>(topic, 1, 0, null, completed)
        Headers headers = record.headers()
        headers.add("entityType", entityType.getBytes())

        ElasticsearchResult<Map<String, Object>> result = new ElasticsearchResult<>()
        result.setSucceeded(true)
        result.setFound(true)

        elasticsearchAdmin.indexExistsNoCache(_ as String, _ as String) >> result
        elasticsearchSearchOperations.getIndexName(_ as String, _ as String) >> "t30f011232daea2f9_IngestionEvent"
        elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result

        when: "data processor success event is pushed into kafka topic"
        lifecycleConsumer.processIngestionLifecycle(record, acknowledgment)

        then: "consumer  should store message into elasticsearch"
        1 * elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result
        1 * acknowledgment.acknowledge()
    }

    def "when consumer fails to store message in elasticsearch"() {
        given:
        ConsumerRecord record = new ConsumerRecord<String, String>(topic, 1, 0, null, invalidDateFormat)
        ElasticsearchResult<Map<String, Object>> result = new ElasticsearchResult<>()
        result.setSucceeded(false)
        result.setFound(true)

        elasticsearchAdmin.indexExists(_ as String, _ as String) >> result
        elasticsearchSearchOperations.getIndexName(_ as String, _ as String) >> "t30f011232daea2f9-d89d7ec77b3d286f_IngestionEvent"
        elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result

        when: "when invalid date format is received into event message"
        lifecycleConsumer.processIngestionLifecycle(record, acknowledgment)

        then: "consumer  should handle the exception and log the info and message should get committed from topic"
        noExceptionThrown()
        !result.isSucceeded()
    }

    def "feedback loop message is stored into elastic search"() {
        given:
        ConsumerRecord record = new ConsumerRecord<String, String>(topic, 1, 0, null, invalidDateFormat)
        Headers headers = record.headers()
        headers.add("component", component.getBytes())
        headers.add("entityType", entityType.getBytes())
        headers.add("service", service.getBytes())
        headers.add("tenantId", tenantId.getBytes())
        headers.add("ingestionId", ingestionId.getBytes())

        ElasticsearchResult<Map<String, Object>> result = new ElasticsearchResult<>()
        result.setSucceeded(true)
        result.setFound(true)

        elasticsearchAdmin.indexExistsNoCache(_ as String, _ as String) >> result
        elasticsearchSearchOperations.getIndexName(_ as String, _ as String) >> "t30f011232daea2f9-d89d7ec77b3d286f_IngestionEvent"

        when: "when feedback loop message is pushed from data processor "
        lifecycleConsumer.processIngestionLifecycle(record, acknowledgment)

        then: "consumer  should store message in elastic search"
        1 * elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result
    }

    def "decompression gz message is stored into elasticsearch"() {
        given:
        String largeKafkaPayload = readFromFile("largeKafkaPayload.txt")
        ConsumerRecord record = new ConsumerRecord<String, String>(topic, 1, 0, null, largeKafkaPayload)
        Headers headers = record.headers()
        headers.add(CONTENT_TYPE_LOWER_CASE, GZ.getBytes())

        Set<Compression> compressionSet = new HashSet<>()
        compressionSet.add(new Lz4Compression())
        compressionSet.add(new GzipCompression())
        compressionSet.add(new NullCompression())
        messageConsumer.compressionFactory = new CompressionFactory(compressionSet)

        ElasticsearchResult<Map<String, Object>> result = new ElasticsearchResult<>()
        result.setSucceeded(true)
        result.setFound(true)

        elasticsearchAdmin.indexExistsNoCache(_ as String, _ as String) >> result
        elasticsearchSearchOperations.getIndexName(_ as String, _ as String) >> "t30f011232daea2f9_IngestionEvent"

        when: "when the error status messages are large and compressed in a gzip"
        lifecycleConsumer.processIngestionLifecycle(record, acknowledgment)

        then: "consumer should store message in elastic search"
        noExceptionThrown()
        1 * elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result
    }

    def "data processor large error event is stored with entityId in  elasticsearch "() {
        given:
        GzipCompression compression = new GzipCompression()
        byte[] fileBytes = readFromFileByteArray("sampleErrorLargePayload.json.gz")
        String largeErrorPayload = compression.decompress(fileBytes).replace(NEWLINE, EMPTY_STRING).replace(CARRIAGE_RETURN, EMPTY_STRING)
        ConsumerRecord record = new ConsumerRecord<String, String>(topic, 1, 0, null, largeErrorPayload)
        Headers headers = record.headers()
        headers.add("entityType", entityType.getBytes())

        ElasticsearchResult<Map<String, Object>> result = new ElasticsearchResult<>()
        result.setSucceeded(true)
        result.setFound(true)
        elasticsearchAdmin.indexExistsNoCache(_ as String, _ as String) >> result
        elasticsearchSearchOperations.getIndexName(_ as String, _ as String) >> "t30f011232daea2f9_IngestionEvent"

        when: "data processor error event have completed with error status"
        lifecycleConsumer.processIngestionLifecycle(record, acknowledgment)

        then: "consumer should store message into elasticsearch"
        noExceptionThrown()
        1 * elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result
    }

    def "creation of index before storing in elasticsearch "() {
        given:
        ConsumerRecord record = new ConsumerRecord<String, String>(topic, 1, 0, null, completed)
        Headers headers = record.headers()
        headers.add("entityType", entityType.getBytes())

        ElasticsearchResult<Map<String, Object>> result = new ElasticsearchResult<>()
        result.setSucceeded(true)
        result.setFound(false)

        elasticsearchAdmin.indexExistsNoCache(_ as String, _ as String) >> result
        ElasticsearchResult<Map<String, Object>> createIndexElasticsearchResult = new ElasticsearchResult<>()
        createIndexElasticsearchResult.setSucceeded(true)
        1 * elasticsearchAdmin.createIndex(_ as String, _ as String, _ as String, _ as String) >> createIndexElasticsearchResult

        elasticsearchSearchOperations.getIndexName(_ as String, _ as String) >> "t30f011232daea2f9_IngestionEvent"
        elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result

        when: "data processor success event is pushed into kafka topic"
        lifecycleConsumer.processIngestionLifecycle(record, acknowledgment)

        then: "consumer  should store message into elasticsearch"
        1 * elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result
        1 * acknowledgment.acknowledge()
    }

    def "error occurred while creating index "() {
        given:
        ConsumerRecord record = new ConsumerRecord<String, String>(topic, 1, 0, null, completed)
        Headers headers = record.headers()
        headers.add("entityType", entityType.getBytes())

        ElasticsearchResult<Map<String, Object>> result = new ElasticsearchResult<>()
        result.setSucceeded(true)
        result.setFound(false)

        elasticsearchAdmin.indexExistsNoCache(_ as String, _ as String) >> result
        ElasticsearchResult<Map<String, Object>> createIndexElasticsearchResult = new ElasticsearchResult<>()
        createIndexElasticsearchResult.setSucceeded(false)
        1 * elasticsearchAdmin.createIndex(_ as String, _ as String, _ as String, _ as String) >> createIndexElasticsearchResult

        elasticsearchSearchOperations.getIndexName(_ as String, _ as String) >> "t30f011232daea2f9_IngestionEvent"

        when: "data processor success event is pushed into kafka topic"
        lifecycleConsumer.processIngestionLifecycle(record, acknowledgment)

        then: "consumer  should store message into elasticsearch"
        1 * acknowledgment.acknowledge()
    }


    def " distribution event is stored in elasticsearch "() {
        given:
        ConsumerRecord record = new ConsumerRecord<String, String>(topic, 1, 0, null, distributionCompleted)
        Headers headers = record.headers()
        headers.add("entityType", entityType.getBytes())
        headers.add("service", serviceType.getBytes())
        ElasticsearchResult<Map<String, Object>> result = new ElasticsearchResult<>()
        result.setSucceeded(true)
        result.setFound(true)

        elasticsearchAdmin.indexExistsNoCache(_ as String, _ as String) >> result
        elasticsearchSearchOperations.getIndexName(_ as String, _ as String) >> "t30f011232daea2f9_DistributionEvent"
        elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result

        when: "success event is pushed into kafka topic"
        lifecycleConsumer.processIngestionLifecycle(record, acknowledgment)

        then: "consumer  should store message into elasticsearch"
        1 * elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result
        1 * acknowledgment.acknowledge()
    }

    def " distribution event is stored in elasticsearch with realmId header"() {
        given:
        ConsumerRecord record = new ConsumerRecord<String, String>(topic, 1, 0, null, distributionCompleted)
        Headers headers = record.headers()
        headers.add("entityType", entityType.getBytes())
        headers.add("realmId", realmId.getBytes())
        headers.add("service", serviceType.getBytes())
        ElasticsearchResult<Map<String, Object>> result = new ElasticsearchResult<>()
        result.setSucceeded(true)
        result.setFound(true)

        elasticsearchAdmin.indexExistsNoCache(_ as String, _ as String) >> result
        elasticsearchSearchOperations.getIndexName(_ as String, _ as String) >> "t30f011232daea2f9_DistributionEvent"
        elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result

        when: "success event is consumed from kafka topic"
        lifecycleConsumer.processIngestionLifecycle(record, acknowledgment)

        then: "consumer  should store message into elasticsearch"
        1 * elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result
        1 * acknowledgment.acknowledge()
    }

    def "creation of distribution index before storing in elasticsearch "() {
        given:
        ConsumerRecord record = new ConsumerRecord<String, String>(topic, 1, 0, null, distributionCompleted)
        Headers headers = record.headers()
        headers.add("entityType", entityType.getBytes())
        headers.add("service", serviceType.getBytes())
        ElasticsearchResult<Map<String, Object>> result = new ElasticsearchResult<>()
        result.setSucceeded(true)
        result.setFound(false)

        elasticsearchAdmin.indexExistsNoCache(_ as String, _ as String) >> result
        ElasticsearchResult<Map<String, Object>> createIndexElasticsearchResult = new ElasticsearchResult<>()
        createIndexElasticsearchResult.setSucceeded(true)
        1 * elasticsearchAdmin.createIndex(_ as String, _ as String, _ as String, _ as String) >> createIndexElasticsearchResult

        elasticsearchSearchOperations.getIndexName(_ as String, _ as String) >> "t30f011232daea2f9_DistributionEvent"
        elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result

        when: "success event is pushed into kafka topic"
        lifecycleConsumer.processIngestionLifecycle(record, acknowledgment)

        then: "consumer  should store message into elasticsearch"
        1 * elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result
        1 * acknowledgment.acknowledge()
    }

    def "error occurred while creating distribution index "() {
        given:
        ConsumerRecord record = new ConsumerRecord<String, String>(topic, 1, 0, null, distributionCompleted)
        Headers headers = record.headers()
        headers.add("entityType", entityType.getBytes())
        headers.add("service", serviceType.getBytes())

        ElasticsearchResult<Map<String, Object>> result = new ElasticsearchResult<>()
        result.setSucceeded(true)
        result.setFound(false)

        elasticsearchAdmin.indexExistsNoCache(_ as String, _ as String) >> result
        ElasticsearchResult<Map<String, Object>> createIndexElasticsearchResult = new ElasticsearchResult<>()
        createIndexElasticsearchResult.setSucceeded(false)
        1 * elasticsearchAdmin.createIndex(_ as String, _ as String, _ as String, _ as String) >> createIndexElasticsearchResult

        elasticsearchSearchOperations.getIndexName(_ as String, _ as String) >> "t30f011232daea2f9_DistributionEvent"

        when: "success event is pushed into kafka topic"
        lifecycleConsumer.processIngestionLifecycle(record, acknowledgment)

        then: "consumer  should store message into elasticsearch"
        1 * acknowledgment.acknowledge()
    }
}
