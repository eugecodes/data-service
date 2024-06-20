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

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;


@Component
public class IngestionLifeCycleConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(IngestionLifeCycleConsumer.class);


    private final LifecycleMessageProcessor lifecycleMessageProcessor;

    @Autowired
    public IngestionLifeCycleConsumer(LifecycleMessageProcessor lifeCycleMessageProcessor) {
        this.lifecycleMessageProcessor = lifeCycleMessageProcessor;
    }

    /**
     * kafka message received in public-all-ingestion-topic
     *
     * @param consumerRecord kafka message object
     * @param acknowledgment kafka Acknowledgment object
     */
    @KafkaListener(topics = "#{'${ingestion.all.status}'}",
            properties = {"#{'${kafka.consumer.properties}'.split(';')}"})
    public void processIngestionLifecycle(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {
        LOGGER.debug("ConsumerRecord From Kafka: {} ", consumerRecord);
        lifecycleMessageProcessor.processStatusEvent(consumerRecord, acknowledgment);
    }

}

