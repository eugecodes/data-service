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
import com.by.dp.lifecycle.notification.consumer.EgressEventConsumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.Headers
import org.json.JSONObject
import org.springframework.kafka.support.Acknowledgment
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class EgressConsumerSpec extends Specification {


    private static final String egressId = "sample egress Id"
    private static final String entityType = "items"
    private static final String tenantId = "a4b7a825f67930965747445709011120"
    private static final String clientId = "DHL"


    ElasticsearchOperationsFactory elasticsearchOperationsFactory = Mock(ElasticsearchOperationsFactory)
    ElasticsearchSearchOperations elasticsearchSearchOperations = Mock(ElasticsearchSearchOperations)
    ElasticsearchOperationsWithMap elasticsearchOperationsWithMap = Mock(ElasticsearchOperationsWithMap)
    ElasticsearchAdmin elasticsearchAdmin = Mock(ElasticsearchAdmin)
    Acknowledgment acknowledgment = Mock(Acknowledgment)
    EgressEventConsumer messageConsumer = new EgressEventConsumer(elasticsearchOperationsFactory, elasticsearchAdmin)
    String egressEventstarted, publishedMessage, pluginsErrorEvent

    @Shared
    def topic = 'public.dp.egress.status'


    def setup() {
        elasticsearchOperationsFactory.getSearchOperations() >> elasticsearchSearchOperations
        elasticsearchOperationsFactory.getOperationsWithMap() >> elasticsearchOperationsWithMap
        egressEventstarted = readFromFile("egressEvenetmessage.json")
        publishedMessage = readFromFile("egressEventendpoint.json")
        pluginsErrorEvent = readFromFile("pluginsErrorEvent.json")
    }

    def "readFromFile"(String fileName) {
        Path path = Paths.get(getClass().getClassLoader()
                .getResource(fileName).toURI())
        byte[] fileBytes = Files.readAllBytes(path)
        String data = new String(fileBytes)
        return data
    }

    def "data plugins Accepting event is stored into elastic search "() {
        given:
        def record = new ConsumerRecord<String, String>(topic, 1, 0, null, egressEventstarted)
        Headers headers = record.headers()
        headers.add("entityType", entityType.getBytes())
        headers.add("egressId", egressId.getBytes())
        headers.add("clientId", clientId.getBytes())
        headers.add("egressStartEventTime", ("2020-10-06T10:10:47.899+0000").getBytes())
        headers.add("tenantId", tenantId.getBytes())

        ElasticsearchResult<Map<String, Object>> result = new ElasticsearchResult<>()
        result.setSucceeded(true)
        result.setFound(true)
        elasticsearchAdmin.indexExists(_ as String, _ as String) >> result
        elasticsearchSearchOperations.getIndexName(_ as String, _ as String ) >> "t30f011232daea2f9_egressevents"

        when: "public.dp.egress.status topic has data plugin message"
        messageConsumer.processEgressLifecycle(record, acknowledgment)

        then: "consumer should save message in elasticsearch index"
        1 * elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result
        1 * acknowledgment.acknowledge()
    }

    def "data plugins  events is not  stored into elastic search "() {
        given:
        def record = new ConsumerRecord<String, String>(topic, 1, 0, null, egressEventstarted)
        Headers headers = record.headers()
        headers.add("entityType", entityType.getBytes())
        headers.add("egressId", egressId.getBytes())
        headers.add("clientId", clientId.getBytes())
        headers.add("egressStartEventTime", ("2020-10-06T10:10:47.899+0000").getBytes())
        headers.add("tenantId", tenantId.getBytes())

        ElasticsearchResult<Map<String, Object>> result = new ElasticsearchResult<>()
        result.setSucceeded(false)
        result.setFound(false)
        elasticsearchAdmin.indexExists(_ as String, _ as String) >> result
        ElasticsearchResult<Map<String, Object>> createIndexElasticsearchResult = new ElasticsearchResult<>()
        createIndexElasticsearchResult.setSucceeded(true)
        1 * elasticsearchAdmin.createIndex(_ as String, _ as String, _ as String, _ as String) >> createIndexElasticsearchResult

        elasticsearchSearchOperations.getIndexName(_ as String, _ as String ) >> "t30f011232daea2f9_egressevents"

        when: "public.dp.egress.status topic has data plugin message"
        messageConsumer.processEgressLifecycle(record, acknowledgment)

        then: "consumer should save message in elasticsearch index"
        1 * elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result
    }

    def "data plugins sends no kafka header "() {
        given:
        ConsumerRecord record = new ConsumerRecord<String, String>(topic, 1, 0, null, egressEventstarted)

        ElasticsearchResult<Map<String, Object>> result = new ElasticsearchResult<>()
        result.setSucceeded(false)
        result.setFound(true)

        elasticsearchSearchOperations.getIndexName(_ as String, _ as String) >> "t30f011232daea2f9_egressevents"
        elasticsearchAdmin.indexExists(_ as String, _ as String) >> result
        elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result

        when: "public.dp.egress.status topic has data plugin message"
        messageConsumer.processEgressLifecycle(record, acknowledgment)

        then: "consumer should log the message and it should not store event into elastic search"
        0 * elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String)
    }

    def "data plugins sends endpoint published event "() {
        given:
        ConsumerRecord record = new ConsumerRecord<String, String>(topic, 1, 0, null, publishedMessage)
        Headers headers = record.headers()
        headers.add("entityType", entityType.getBytes())
        headers.add("egressId", egressId.getBytes())
        headers.add("clientId", clientId.getBytes())
        headers.add("tenantId", tenantId.getBytes())

        ElasticsearchResult<Map<String, Object>> result = new ElasticsearchResult<>()
        result.setSucceeded(false)
        result.setFound(true)
        elasticsearchSearchOperations.getIndexName(_ as String, _ as String) >> "t30f011232daea2f9_egressevents"
        elasticsearchAdmin.indexExists(_ as String, _ as String) >> result

        when: "public.dp.egress.status topic has data plugin message"
        messageConsumer.processEgressLifecycle(record, acknowledgment)

        then: "consumer should push the messages to elastic search."
        1 * elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result
    }

    def "data plugins sends invalid kafka header "() {
        given:
        ConsumerRecord record = new ConsumerRecord<String, String>(topic, 1, 0, null, publishedMessage)
        Map<String, String> headers = new HashMap<>();
        headers.put("entitytype", "entitytype")
        headers.put("egressId", "egressId")
        headers.put("clientid", "clientid")
        headers.put("tenantid", "tenantid")
        ElasticsearchResult<Map<String, Object>> result = new ElasticsearchResult<>()
        result.setSucceeded(false)
        result.setFound(true)

        elasticsearchSearchOperations.getIndexName(_ as String, _ as String) >> "t30f011232daea2f9_egressevents"
        elasticsearchAdmin.indexExists(_ as String, _ as String) >> result
        elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result

        when: "public.dp.egress.status topic has data plugin message"
        messageConsumer.processEgressEvents(new JSONObject(record.value()), headers)

        then: "consumer should push the messages to elastic search."
        def ex = thrown(IllegalArgumentException)
        ex.message == "invalid header data"
        0 * elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String)


    }

    def "data distribution plugins failed event is stored "() {
        given:
        ConsumerRecord record = new ConsumerRecord<String, String>(topic, 1, 0, null, pluginsErrorEvent)
        Headers headers = record.headers()
        headers.add("entityType", entityType.getBytes())
        headers.add("egressId", egressId.getBytes())
        headers.add("clientId", clientId.getBytes())
        headers.add("tenantId", tenantId.getBytes())

        ElasticsearchResult<Map<String, Object>> result = new ElasticsearchResult<>()
        result.setSucceeded(false)
        result.setFound(true)
        elasticsearchSearchOperations.getIndexName(_ as String, _ as String) >> "t30f011232daea2f9_egressevents"
        elasticsearchAdmin.indexExists(_ as String, _ as String) >> result

        when: "public.dp.egress.status topic has data plugin message"
        messageConsumer.processEgressLifecycle(record, acknowledgment)

        then: "consumer should push the messages to elastic search."
        1 * elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result
    }

    def "test index creation before storing into elastic search "() {
        given:
        def record = new ConsumerRecord<String, String>(topic, 1, 0, null, egressEventstarted)
        Headers headers = record.headers()
        headers.add("entityType", entityType.getBytes())
        headers.add("egressId", egressId.getBytes())
        headers.add("clientId", clientId.getBytes())
        headers.add("egressStartEventTime", ("2020-10-06T10:10:47.899+0000").getBytes())
        headers.add("tenantId", tenantId.getBytes())

        ElasticsearchResult<Map<String, Object>> result = new ElasticsearchResult<>()
        result.setSucceeded(true)
        result.setFound(false)
        elasticsearchAdmin.indexExists(_ as String, _ as String) >> result

        ElasticsearchResult<Map<String, Object>> createIndexElasticsearchResult = new ElasticsearchResult<>()
        createIndexElasticsearchResult.setSucceeded(true)
        1 * elasticsearchAdmin.createIndex(_ as String, _ as String, _ as String, _ as String) >> createIndexElasticsearchResult

        elasticsearchSearchOperations.getIndexName(_ as String, _ as String) >> "t30f011232daea2f9_egressevents"

        when: "public.dp.egress.status topic has data plugin message"
        messageConsumer.processEgressLifecycle(record, acknowledgment)

        then: "consumer should create the index and save message in elasticsearch index"
        1 * elasticsearchOperationsWithMap.storeMessage(_ as String, _ as Map, null, _ as String) >> result
        1 * acknowledgment.acknowledge()
    }

    def "test error occurred while creating index "() {
        given:
        def record = new ConsumerRecord<String, String>(topic, 1, 0, null, egressEventstarted)
        Headers headers = record.headers()
        headers.add("entityType", entityType.getBytes())
        headers.add("egressId", egressId.getBytes())
        headers.add("clientId", clientId.getBytes())
        headers.add("egressStartEventTime", ("2020-10-06T10:10:47.899+0000").getBytes())
        headers.add("tenantId", tenantId.getBytes())

        ElasticsearchResult<Map<String, Object>> result = new ElasticsearchResult<>()
        result.setSucceeded(true)
        result.setFound(false)
        elasticsearchAdmin.indexExists(_ as String, _ as String) >> result

        ElasticsearchResult<Map<String, Object>> createIndexElasticsearchResult = new ElasticsearchResult<>()
        createIndexElasticsearchResult.setSucceeded(false)
        1 * elasticsearchAdmin.createIndex(_ as String, _ as String, _ as String, _ as String) >> createIndexElasticsearchResult

        elasticsearchSearchOperations.getIndexName(_ as String, _ as String) >> "t30f011232daea2f9_egressevents"

        when: "public.dp.egress.status topic has data plugin message"
        messageConsumer.processEgressLifecycle(record, acknowledgment)

        then: "consumer should not push messages to elastic store as there is error in index creation"
        1 * acknowledgment.acknowledge()
    }
}

