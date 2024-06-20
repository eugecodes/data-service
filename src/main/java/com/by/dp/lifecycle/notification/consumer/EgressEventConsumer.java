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
package com.by.dp.lifecycle.notification.consumer;

import com.by.dp.elasticsearch.ElasticsearchAdmin;
import com.by.dp.elasticsearch.ElasticsearchOperationsFactory;
import com.by.dp.elasticsearch.ElasticsearchResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.by.dp.elasticsearch.Constants.CONNECT_CHARACTER;
import static com.by.dp.lifecycle.notification.common.Constants.CLIENT_ID;
import static com.by.dp.lifecycle.notification.common.Constants.COMPONENT;
import static com.by.dp.lifecycle.notification.common.Constants.DESCRIPTION;
import static com.by.dp.lifecycle.notification.common.Constants.EGRESS_ID;
import static com.by.dp.lifecycle.notification.common.Constants.EGRESS_INDEX;
import static com.by.dp.lifecycle.notification.common.Constants.EGRESS_TYPES;
import static com.by.dp.lifecycle.notification.common.Constants.END_POINTS;
import static com.by.dp.lifecycle.notification.common.Constants.ENTITY_TYPE;
import static com.by.dp.lifecycle.notification.common.Constants.ERRORS;
import static com.by.dp.lifecycle.notification.common.Constants.EVENT_TIME;
import static com.by.dp.lifecycle.notification.common.Constants.INGESTION_IDS;
import static com.by.dp.lifecycle.notification.common.Constants.SERVICE;
import static com.by.dp.lifecycle.notification.common.Constants.STATUS;
import static com.by.dp.lifecycle.notification.common.Constants.TENANTID;
import static com.by.dp.lifecycle.notification.common.Constants.V_1;
import static com.by.dp.service.common.CommonServiceErrorConstants.BYDP_ES0106;
import static com.by.dp.service.common.CommonServiceErrorConstants.BYDP_ES0109;
import static com.by.dp.service.common.ResponseErrorMessageEnum.ELASTICSEARCH_INDEX_NOT_FOUND;

@Component
@Slf4j
public class EgressEventConsumer {
    public static final String MAPPING = "{\"properties\":{\"component\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\"}}},\"eventTime\":{\"type\":\"date\",\"format\":\"yyyy-MM-dd'T'HH:mm:ss.SSSX||yyyy-MM-dd'T'HH:mm:ssX||yyyy-MM-dd||yyyy-MM-dd'T'HH:mm:ss.SSSZ||yyyy-MM-dd HH:mm:ss||dd-MM-yyyy HH:mm:ss||yyyy-MM-dd'T'HH:mm:ss\"},\"errors\":{\"type\":\"nested\",\"include_in_parent\":false},\"realmId\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\"}}},\"description\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\"}}},\"entityType\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\"}}},\"endpoint\":{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\"}}},\"status\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\"}}},\"description\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\"}}},\"errors\":{\"type\":\"nested\",\"include_in_parent\":false}}},\"egressId\":{\"type\":\"text\",\"fielddata\":true},\"ingestionIds\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\"}}},\"service\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\"}}},\"status\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\"}}},\"clientId\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\"}}}}}";

    private final ElasticsearchOperationsFactory elasticsearchOperationsFactory;
    private final ElasticsearchAdmin elasticsearchAdmin;

    @Autowired
    public EgressEventConsumer(ElasticsearchOperationsFactory elasticsearchOperationsFactory, ElasticsearchAdmin elasticsearchAdmin) {
        this.elasticsearchOperationsFactory = elasticsearchOperationsFactory;
        this.elasticsearchAdmin = elasticsearchAdmin;
    }

    /**
     * kafka message received in public.dp.egress.status topic
     *
     * @param consumerRecord kafka message object
     * @param acknowledgment kafka Acknowledgment object
     */
    @KafkaListener(topics = "#{'${public.egress.status}'}",
            properties = {"#{'${kafka.consumer.properties}'.split(';')}"})
    public void processEgressLifecycle(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {
        processEgressEvent(consumerRecord, acknowledgment);
    }

    private void processEgressEvent(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {
        try {
            Map<String, String> inboundHeader = new HashMap<>();
            consumerRecord.headers().forEach(header ->
                    inboundHeader.put(header.key(), new String(header.value())));
            if (inboundHeader.isEmpty()) {
                log.warn("kafka header data is empty:");
            }
            final String value = consumerRecord.value();
            JSONObject jsonObject = new JSONObject(value);
            processEgressEvents(jsonObject, inboundHeader);
        } catch (Exception e) {
            log.error("Error occurred while processing egress for {} with error :{}", consumerRecord, e.getMessage());
        } finally {
            acknowledgment.acknowledge();
        }
    }

    private void processEgressEvents(JSONObject message, Map<String, String> inboundHeader) {

        if (inboundHeader.containsKey(CLIENT_ID) && inboundHeader.containsKey(TENANTID) && inboundHeader.containsKey(ENTITY_TYPE)) {
            JSONObject event = buildMessage(message, inboundHeader);
            ElasticsearchResult<Map<String, Object>> result = saveMessageToElasticStore(event, inboundHeader.get(TENANTID));
            if (!result.isSucceeded()) {
                log.error("message store to elastic search failed : {} ", message);
            }
        } else {
            log.error("Required kafka header is invalid : {}", inboundHeader);
            throw new IllegalArgumentException("invalid header data");
        }
    }

    private JSONObject buildMessage(JSONObject message, Map<String, String> inboundHeader) {
        JSONObject egressEvent = new JSONObject();
        egressEvent.put(COMPONENT, message.get(COMPONENT));
        egressEvent.put(SERVICE, message.get(SERVICE));
        egressEvent.put(EGRESS_ID, inboundHeader.getOrDefault((EGRESS_ID), ""));
        egressEvent.put(ENTITY_TYPE, inboundHeader.getOrDefault((ENTITY_TYPE), ""));
        egressEvent.put(TENANTID, inboundHeader.getOrDefault((TENANTID), ""));
        egressEvent.put(CLIENT_ID, inboundHeader.getOrDefault((CLIENT_ID), ""));
        egressEvent.put(EVENT_TIME, message.get(EVENT_TIME));
        egressEvent.put(DESCRIPTION, message.get(DESCRIPTION));
        egressEvent.put(STATUS, message.get(STATUS));
        JSONArray ingestionIds = (JSONArray) message.get(INGESTION_IDS);
        egressEvent.put(INGESTION_IDS, ingestionIds);
        if (message.has(END_POINTS)) {
            egressEvent.put(END_POINTS, message.get(END_POINTS));
        }
        if (message.has(ERRORS)) {
            egressEvent.put(ERRORS, message.get(ERRORS));
        }
        return egressEvent;
    }


    /**
     * save egress event message to Elastic search store.
     *
     * @param egressEvent the egress event to store
     * @param realmId the realm id
     * @return a {@link ElasticsearchResult } with the results of the operation
     */
    private ElasticsearchResult<Map<String, Object>> saveMessageToElasticStore(JSONObject egressEvent, String realmId) {
        ElasticsearchResult<Map<String, Object>> elasticsearchResult = new ElasticsearchResult<>();
        ElasticsearchResult<Map<String, Object>> createIndexResult;
        String indexNameAlias = elasticsearchOperationsFactory.getSearchOperations().getIndexName(realmId, EGRESS_INDEX);
        String indexName = indexNameAlias + V_1;

        if (elasticsearchAdmin.indexExists(indexName, BYDP_ES0106).isFound()) {
            elasticsearchResult = elasticsearchOperationsFactory.getOperationsWithMap().storeMessage(indexName, egressEvent.toMap(), null, EGRESS_TYPES);
        } else {
            createIndexResult = this.elasticsearchAdmin.createIndex(indexName, indexNameAlias, MAPPING, BYDP_ES0109);

            if (!createIndexResult.isSucceeded()) {
                elasticsearchResult.setSucceeded(false);
                elasticsearchResult.setErrorMessage(String.format(ELASTICSEARCH_INDEX_NOT_FOUND.getValue(), indexName.split(CONNECT_CHARACTER)[1]));
            } else {
                elasticsearchResult = elasticsearchOperationsFactory.getOperationsWithMap().storeMessage(indexName, egressEvent.toMap(), null, EGRESS_TYPES);
            }
        }
        return elasticsearchResult;
    }
}

