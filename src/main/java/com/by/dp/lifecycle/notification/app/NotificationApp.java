/*
 * //==========================================================================
 * //               Copyright 2019, JDA Software Group, Inc.
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

package com.by.dp.lifecycle.notification.app;

import com.by.dp.elasticsearch.ElasticsearchQueryGenerator;
import com.by.dp.service.retry.kafka.downstream.DownstreamRequestBuilder;
import com.by.dp.service.retry.kafka.downstream.DownstreamRetryConfig;
import com.by.dp.service.retry.kafka.lifecycle.LifecycleRequestBuilder;
import com.by.dp.service.retry.kafka.lifecycle.LifecycleRetryConfig;
import com.by.dp.service.retry.kafka.referentialintegrityvalidator.ReferentialIntegrityValidatorRequestBuilder;
import com.by.dp.service.retry.kafka.referentialintegrityvalidator.ReferentialIntegrityValidatorRetryConfig;
import com.by.dp.service.retry.restrequest.luminate.LuminateIngestRequestBuilder;
import com.by.dp.service.retry.restrequest.luminate.LuminateIngestRetryConfig;
import com.by.dp.service.retry.restrequest.ummservice.UmmServiceRequestBuilder;
import com.by.dp.service.retry.restrequest.ummservice.UmmServiceRetryConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication(exclude = {
        KafkaAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        ManagementWebSecurityAutoConfiguration.class
})
@ComponentScan(basePackages = {
        "com.by.dp.kafka.common.configuration",
        "com.by.dp.elasticsearch",
//        "com.by.dp.service.auth",
        "com.by.dp.service.rest",
        "com.by.dp.service.retry",
        "com.by.dp.service.util.compression",
        "com.by.dp.lifecycle"},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = DownstreamRequestBuilder.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = LuminateIngestRequestBuilder.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = UmmServiceRequestBuilder.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = LifecycleRequestBuilder.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = LuminateIngestRetryConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = DownstreamRetryConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = UmmServiceRetryConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = LifecycleRetryConfig.class),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.by.dp.service.retry.adlsgen2.*"),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                        ReferentialIntegrityValidatorRequestBuilder.class,
                        ReferentialIntegrityValidatorRetryConfig.class,
                        ElasticsearchQueryGenerator.class
                })
        })
@EnableRetry
public class NotificationApp {
    public static void main(String[] args) {
        SpringApplication.run(NotificationApp.class, args);
    }
}
