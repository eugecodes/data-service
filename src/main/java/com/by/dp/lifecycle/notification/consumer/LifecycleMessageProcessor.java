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
package com.by.dp.lifecycle.notification.consumer;

import com.by.dp.elasticsearch.ElasticsearchAdmin;
import com.by.dp.elasticsearch.ElasticsearchOperationsFactory;
import com.by.dp.elasticsearch.ElasticsearchResult;
import com.by.dp.lifecycle.notification.common.Constants;
import com.by.dp.service.util.compression.Compression;
import com.by.dp.service.util.compression.CompressionFactory;
import com.by.dp.service.util.compression.CompressionTypeEnum;
import com.by.dp.service.util.compression.DecompressionFailureException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import com.by.dp.service.common.MDCContextHolder;

import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static com.by.dp.elasticsearch.Constants.CONNECT_CHARACTER;
import static com.by.dp.lifecycle.notification.common.Constants.APPLICATION;
import static com.by.dp.lifecycle.notification.common.Constants.AUTH_CONTEXT;
import static com.by.dp.lifecycle.notification.common.Constants.BODY;
import static com.by.dp.lifecycle.notification.common.Constants.COMPONENT;
import static com.by.dp.lifecycle.notification.common.Constants.CONTENT_TYPE_LOWER_CASE;
import static com.by.dp.lifecycle.notification.common.Constants.CURRENT_USER;
import static com.by.dp.lifecycle.notification.common.Constants.DEFAULT_ENTITY;
import static com.by.dp.lifecycle.notification.common.Constants.DESCRIPTION;
import static com.by.dp.lifecycle.notification.common.Constants.DETAILED_INFO;
import static com.by.dp.lifecycle.notification.common.Constants.DISTRIBUTION;
import static com.by.dp.lifecycle.notification.common.Constants.ENTITY_ID;
import static com.by.dp.lifecycle.notification.common.Constants.ENTITY_TYPE;
import static com.by.dp.lifecycle.notification.common.Constants.ERRORS;
import static com.by.dp.lifecycle.notification.common.Constants.ERROR_MSG;
import static com.by.dp.lifecycle.notification.common.Constants.EVENT_TIME;
import static com.by.dp.lifecycle.notification.common.Constants.EVENT_TYPES;
import static com.by.dp.lifecycle.notification.common.Constants.GZ;
import static com.by.dp.lifecycle.notification.common.Constants.REALMID;
import static com.by.dp.lifecycle.notification.common.Constants.REALM_ID;
import static com.by.dp.lifecycle.notification.common.Constants.RUNID;
import static com.by.dp.lifecycle.notification.common.Constants.RUN_ID;
import static com.by.dp.lifecycle.notification.common.Constants.SERVICE;
import static com.by.dp.lifecycle.notification.common.Constants.SERVICE_TYPE;
import static com.by.dp.lifecycle.notification.common.Constants.STATUS;
import static com.by.dp.lifecycle.notification.common.Constants.TEMPLATE_GROUP;
import static com.by.dp.lifecycle.notification.common.Constants.TENANTID;
import static com.by.dp.lifecycle.notification.common.Constants.TENANT_ID;
import static com.by.dp.lifecycle.notification.common.Constants.USER;
import static com.by.dp.service.common.CommonServiceErrorConstants.BYDP_ES0106;
import static com.by.dp.service.common.CommonServiceErrorConstants.BYDP_ES0109;
import static com.by.dp.service.common.Constants.INGESTION_ID;
import static com.by.dp.service.common.ResponseErrorMessageEnum.ELASTICSEARCH_INDEX_NOT_FOUND;

/**
 * Process lifecycle messages
 */
@Component
@Slf4j
public class LifecycleMessageProcessor {

    private static final String INGESTION_MAPPING = "{\"properties\":{\"component\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\"}}},\"currentTid\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\"}}},\"description\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\"}}},\"entityType\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\"}}},\"errors\":{\"type\":\"nested\",\"include_in_parent\":false},\"eventTime\":{\"type\":\"date\",\"format\":\"yyyy-MM-dd'T'HH:mm:ss.SSSX||yyyy-MM-dd'T'HH:mm:ssX||yyyy-MM-dd||yyyy-MM-dd'T'HH:mm:ss.SSSZ||yyyy-MM-dd HH:mm:ss||dd-MM-yyyy HH:mm:ss||yyyy-MM-dd'T'HH:mm:ss\"},\"ingestionId\":{\"type\":\"text\",\"fielddata\":true},\"service\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\"}}},\"status\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\"}}},\"user\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\"}}}}}";

    private static final String DISTRIBUTION_MAPPING = "{\"properties\":{\"component\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\"}}},\"currentTid\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\"}}},\"description\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\"}}},\"entityType\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\"}}},\"errors\":{\"type\":\"nested\",\"include_in_parent\":false},\"detailed_info\":{\"type\":\"nested\",\"include_in_parent\":false},\"eventTime\":{\"type\":\"date\",\"format\":\"yyyy-MM-dd'T'HH:mm:ss.SSSX||yyyy-MM-dd'T'HH:mm:ssX||yyyy-MM-dd||yyyy-MM-dd'T'HH:mm:ss.SSSZ||yyyy-MM-dd HH:mm:ss||dd-MM-yyyy HH:mm:ss||yyyy-MM-dd'T'HH:mm:ss\"},\"ingestionId\":{\"type\":\"text\",\"fielddata\":true},\"runId\":{\"type\":\"text\",\"fielddata\":true},\"service\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\"}}},\"status\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\"}}},\"user\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\"}}}}}";

    private final ElasticsearchOperationsFactory elasticsearchOperationsFactory;
    private final ElasticsearchAdmin elasticsearchAdmin;

    @Autowired
    CompressionFactory compressionFactory;

    public LifecycleMessageProcessor(ElasticsearchOperationsFactory elasticsearchOperationsFactory, ElasticsearchAdmin elasticsearchAdmin) {
        this.elasticsearchOperationsFactory = elasticsearchOperationsFactory;
        this.elasticsearchAdmin = elasticsearchAdmin;
    }

    /**
     * process event meta data
     *
     * @param consumerRecord the message
     * @param acknowledgment the acknowledgment
     */
    public void processStatusEvent(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {
        try {
            Map<String, String> inboundHeader = new HashMap<>();
            consumerRecord.headers().forEach(header ->
                    inboundHeader.put(header.key(), new String(header.value())));
            if (inboundHeader.isEmpty()) {
                log.debug("No header received from data platform:");
            }

            String value;
            if (inboundHeader.containsKey(CONTENT_TYPE_LOWER_CASE)
                    && inboundHeader.get(CONTENT_TYPE_LOWER_CASE).equals(GZ)) {
                value = processCompressedMessage(consumerRecord);
            } else {
                value = consumerRecord.value();
            }

            JSONObject jsonObject = new JSONObject(value);
            processEvent(jsonObject, inboundHeader);
        } catch (Exception e) {
            log.error("Error occurred while processing for : {},and error is :{}", consumerRecord, e.getMessage());
        } finally {
            acknowledgment.acknowledge();
        }
    }

    private String processCompressedMessage(ConsumerRecord<String, String> consumerRecord) throws DecompressionFailureException {
        Compression compression = compressionFactory.findCompression(CompressionTypeEnum.GZIP);
        byte[] decodedValues = Base64.getDecoder().decode(consumerRecord.value());
        return compression.decompress(decodedValues);
    }

    private void processEvent(JSONObject message, Map<String, String> inboundHeader) {
        String user = null;
        String tenantId = null;
        JSONObject bodyObj = (JSONObject) message.get(BODY);
        if (inboundHeader.containsKey(COMPONENT) && inboundHeader.get(COMPONENT).equals(APPLICATION)) {
            tenantId = inboundHeader.get(TENANTID);
        } else if (inboundHeader.containsKey((TENANTID))) {
            tenantId = inboundHeader.get(TENANTID);
        } else if (inboundHeader.containsKey((REALM_ID))) {
            tenantId = inboundHeader.get(REALM_ID);
        } else {
            if (message.has(AUTH_CONTEXT)) {
                JSONObject authContextObj = (JSONObject) message.get(AUTH_CONTEXT);
                if (authContextObj.has(TENANT_ID) && !authContextObj.isNull(TENANT_ID)) {
                    tenantId = (String) authContextObj.get(TENANT_ID);
                }
                if (authContextObj.has(CURRENT_USER) && !authContextObj.isNull(CURRENT_USER)) {
                    user = (String) authContextObj.get(CURRENT_USER);
                    MDCContextHolder.setUser(user);
                }
            }
        }

        if (tenantId != null) {
            String entityType = inboundHeader.getOrDefault(ENTITY_TYPE, DEFAULT_ENTITY);
            ElasticsearchResult<Map<String, Object>> result = null;
            MDCContextHolder.setCustomParam("tenantId", tenantId);
            MDCContextHolder.setEntityType(entityType);
            MDCContextHolder.setIngestionId(inboundHeader.get(INGESTION_ID));

            // FIXME - Kafka headers coming from LTD appends invalid char to tenantId and service headers. Workaround - for now checking contains for service field
            if (inboundHeader.containsKey(SERVICE_TYPE) && inboundHeader.get(SERVICE_TYPE).contains(DISTRIBUTION)) {
                JSONObject event = buildDistributionStatusMessage(entityType, user, bodyObj);
                result = saveMessageToElasticStore(event, (String) event.get(TENANTID), DISTRIBUTION_MAPPING, Constants.DISTRIBUTION_INDEX);
            } else {
                JSONObject event = buildMessage(entityType, tenantId, user, bodyObj);
                result = saveMessageToElasticStore(event, tenantId, INGESTION_MAPPING, Constants.EVENT_INDEX);

            }
            if (!result.isSucceeded()) {
                log.error("Elasticsearch write failed for message {} with error {} ", message, result.getErrorMessage());
            }
        } else {
            log.error("Message does not conform to the contract. Missing tenant/realm Id {}", message);
        }
        MDCContextHolder.clearMDCContext();
    }

    private JSONObject buildMessage(String entityType, String realmId, String user, JSONObject message) {
        JSONObject ingestionEvent = new JSONObject();
        if (entityType.equals(DEFAULT_ENTITY)) {
            log.debug("Entity information not received from service: {} - realmId: {}?", message.get(SERVICE), realmId);
        }
        String component = (String) message.get(COMPONENT);
        ingestionEvent.put(INGESTION_ID, message.get(INGESTION_ID));
        ingestionEvent.put(DESCRIPTION, message.get(DESCRIPTION));
        ingestionEvent.put(ENTITY_TYPE, entityType);
        ingestionEvent.put(USER, user);
        ingestionEvent.put(TENANTID, realmId);
        ingestionEvent.put(COMPONENT, component);
        ingestionEvent.put(SERVICE, message.get(SERVICE));
        ingestionEvent.put(STATUS, message.get(STATUS));
        if (!message.has(EVENT_TIME)) {
            ingestionEvent.put(EVENT_TIME, Instant.now());
        }
        ingestionEvent.put(EVENT_TIME, message.get(EVENT_TIME));
        if (message.has(ERRORS)) {
            ingestionEvent.put(ERRORS, buildError(message, component));
        }
        return ingestionEvent;
    }

    /**
     * build error details for different ingestion lifecycle steps
     *
     * @param message   message
     * @param component the component
     */
    private JSONArray buildError(JSONObject message, String component) {
        JSONArray errorArray = message.getJSONArray(ERRORS);
        JSONArray errors = new JSONArray();
        for (int i = 0; i < errorArray.length(); i++) {
            JSONObject errorObject = (JSONObject) errorArray.get(i);
            JSONObject error = new JSONObject();
            error.put(ERROR_MSG, errorObject.get(ERROR_MSG));
            if (component.equals(APPLICATION) && errorObject.has(ENTITY_ID)) {
                error.put(ENTITY_ID, errorObject.get(ENTITY_ID));
            }
            errors.put(error);
        }
        return errors;
    }

    /**
     * save message to Elastic search store.
     *
     * @param ingestionEvent the ingestion event
     * @param realmId        the realmId
     */
    private ElasticsearchResult<Map<String, Object>> saveMessageToElasticStore(JSONObject ingestionEvent, String realmId, String mapping, String index) {
        ElasticsearchResult<Map<String, Object>> elasticsearchResult;
        ElasticsearchResult<Map<String, Object>> createIndexResult;
        String indexNameAlias = elasticsearchOperationsFactory.getSearchOperations().getIndexName(realmId, index);
        String indexName = indexNameAlias + Constants.V_1;
        if (elasticsearchAdmin.indexExistsNoCache(indexName, BYDP_ES0106).isFound()) {
            elasticsearchResult = elasticsearchOperationsFactory.getOperationsWithMap().storeMessage(indexName, ingestionEvent.toMap(), null, EVENT_TYPES);
        } else {
            createIndexResult = this.elasticsearchAdmin.createIndex(indexName, indexNameAlias, mapping, BYDP_ES0109);

            if (createIndexResult.isSucceeded()) {
                elasticsearchResult = elasticsearchOperationsFactory.getOperationsWithMap().storeMessage(indexName, ingestionEvent.toMap(), null, EVENT_TYPES);
            } else {
                elasticsearchResult = new ElasticsearchResult<>();
                elasticsearchResult.setSucceeded(false);
                elasticsearchResult.setErrorMessage(String.format(ELASTICSEARCH_INDEX_NOT_FOUND.getValue(), indexName.split(CONNECT_CHARACTER)[1]));
            }
        }
        return elasticsearchResult;
    }

    private JSONObject buildDistributionStatusMessage(String entityType, String user, JSONObject message) {
        JSONObject distributionEvent = new JSONObject();
        if (entityType.equals(DEFAULT_ENTITY)) {
            log.debug("Entity information not received from service: {}?", message.get(SERVICE));
        }
        String component = (String) message.get(COMPONENT);
        distributionEvent.put(ENTITY_TYPE, entityType);
        if (message.has(INGESTION_ID)) {
            distributionEvent.put(INGESTION_ID, message.get(INGESTION_ID));
        } else {
            if (message.has(DETAILED_INFO)) {
                JSONObject detailedInfo = (JSONObject) message.get(DETAILED_INFO);
                if (detailedInfo.has(RUN_ID)) {
                    distributionEvent.put(RUNID, detailedInfo.get(RUN_ID));
                    if (detailedInfo.has(TEMPLATE_GROUP)) {
                        distributionEvent.put(ENTITY_TYPE, detailedInfo.get(TEMPLATE_GROUP));
                    }
                }
            }
        }

        distributionEvent.put(DESCRIPTION, message.get(DESCRIPTION));

        distributionEvent.put(USER, user);

        distributionEvent.put(COMPONENT, component);
        distributionEvent.put(SERVICE, message.get(SERVICE));
        if (message.has(STATUS)) {
            distributionEvent.put(STATUS, message.get(STATUS));
        }
        if (message.has(EVENT_TIME)) {
            distributionEvent.put(EVENT_TIME, message.get(EVENT_TIME));
        }
        if (message.has(DETAILED_INFO)) {
            JSONObject detailedInfo = (JSONObject) message.get(DETAILED_INFO);
            distributionEvent.put(DETAILED_INFO, detailedInfo);
            // FIXME - LTD kafka header append invalid char to tenantId. workaround- for now reading from body
            if (detailedInfo.has(REALMID)) {
                distributionEvent.put(TENANTID, detailedInfo.get(REALMID));
            }
        }
        return distributionEvent;
    }
}

