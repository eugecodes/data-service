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
package com.by.dp.lifecycle.notification.functionaltests.testvalidators;

import io.restassured.response.Response;
import org.apache.http.HttpStatus;

import java.util.Date;

import static com.jda.testautomation.common.utils.DateUtils.formattedDateUTC;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static com.by.dp.functionaltests.constants.DateFormats.YYYY_MM_DD;
import static com.by.dp.functionaltests.constants.IngestionResponseFields.INGESTION_ID;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.EVENTS_CONTENT_SIZE;
import static com.by.dp.functionaltests.constants.QueryConstants.NO_RECORDS_FOUND;
import static com.by.dp.functionaltests.constants.QueryStatusConstants.QUERY_INGESTION_LIFECYCLE_ENTITY_DATA_TYPE;


/**
 * Validator Class with methods to validate if a synchronous ingestion has made it to Data Processor and if the data is curated and persisted as expected.
 **/


public class QueryTestValidators {
    String today = formattedDateUTC(YYYY_MM_DD, new Date());
    private static final String ERROR = "error";

    /*************************
     * Query Test Validators *
     *************************/

    public void validateSuccessResponseEntityTypeV2(Response queryResponse, String entityType) {
        queryResponse.then()
                .body(containsString(INGESTION_ID))
                .body(QUERY_INGESTION_LIFECYCLE_ENTITY_DATA_TYPE, equalTo("items"))
                .statusCode(HttpStatus.SC_OK);
    }

    public void validateErrorResponseInvalidEntityTypeV2(Response queryResponse, String invalidEntityType) {
        queryResponse.then()
                .body(ERROR, equalTo(NO_RECORDS_FOUND))
                .statusCode(HttpStatus.SC_OK);
    }

    public void validateErrorResponseInvalidIngestionIdV2(Response queryResponse, String invalidIngestionId) {
        queryResponse.then()
                .body(ERROR, equalTo(NO_RECORDS_FOUND))
                .statusCode(HttpStatus.SC_OK);
    }

    public void validateSuccessResponseV2(Response queryResponse) {
        queryResponse.then()
                .body(containsString(INGESTION_ID))
                .body(EVENTS_CONTENT_SIZE, greaterThan(0))
                .statusCode(HttpStatus.SC_OK);
    }
}
