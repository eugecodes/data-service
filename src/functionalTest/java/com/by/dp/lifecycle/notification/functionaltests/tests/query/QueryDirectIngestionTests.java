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
package com.by.dp.lifecycle.notification.functionaltests.tests.query;

import com.by.dp.functionaltests.constants.ConfigConstants;
import com.by.dp.functionaltests.dataproviders.EntityDataProviderCommons;
import com.by.dp.functionaltests.endpoints.EntityEndpointsCommons;
import com.by.dp.functionaltests.testobjects.TestObject;
import com.by.dp.functionaltests.tests.TestBase;
import com.by.dp.functionaltests.testvalidators.EntityTestValidatorsCommons;
import com.by.dp.lifecycle.notification.functionaltests.endpoints.QueryEndpoints;
import com.by.dp.lifecycle.notification.functionaltests.testvalidators.QueryTestValidators;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import java.io.UnsupportedEncodingException;
import org.apache.http.HttpStatus;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.by.dp.functionaltests.constants.IngestionResponseFields.INGESTION_ID;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.EVENTS_CURATED_MESSAGE;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.EVENTS_DESCRIPTION;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.EVENTS_INGESTION_ID;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.EVENTS_STATUS;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.EVENTS_VALIDATION_MESSAGE;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.FAILED;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.SUCCESSFUL;
import static com.jda.testautomation.common.utils.CommonUtils.pause;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test Class that queries persisted entities ingested via synchronous calls made to Data Processor.
 **/

public class QueryDirectIngestionTests extends TestBase {

    EntityEndpointsCommons entityEndpointsCommons;
    QueryEndpoints queryEndpoints;
    QueryTestValidators queryTestValidators;
    EntityTestValidatorsCommons entityTestValidatorsCommons;

    String statusLink;

    String statusLinkResponse;

    @BeforeMethod(alwaysRun = true)
    public void initialize() {
        entityEndpointsCommons = new EntityEndpointsCommons();
        queryEndpoints = new QueryEndpoints();
        queryTestValidators = new QueryTestValidators();
        entityTestValidatorsCommons = new EntityTestValidatorsCommons();
        statusLink = queryEndpoints.getStatusLinkPath();
        statusLinkResponse = queryEndpoints.getStatusLinkResponsePath();
    }

    @AfterMethod(alwaysRun = true)
    public void cleanUp() {
        entityEndpointsCommons.cleanUp();
        queryEndpoints.cleanUp();
    }

    @Test(groups = {"smoke", "acceptance", "regression"},
            dataProvider = "getEntities",
            dataProviderClass = EntityDataProviderCommons.class,
            description = "DM-2570- Test to ingest synchronous entity with valid payload and validate the response in V2 API by querying with ingestionID"
                    + "GIVEN: an endpoint that accepts ingestion data"
                    + "WHEN: I issue a POST to that endpoint"
                    + "AND: The POST contains a valid payload and V1 API Status Completed"
                    + "AND: when I get status with V2 API"
                    + "THEN: I should receive a successful status for V2 API"
                    + "AND: the response should contain an ingestionId,Status and Description"
    )
    public void getEntitiesSuccessfulStatusDirectSmoke(TestObject entity) throws InterruptedException, UnsupportedEncodingException {
        response = entityEndpointsCommons.postEntityDirect(entity);

        entityTestValidatorsCommons.validateDirectEntityIngestion(response, entity);
        response.then()
                .statusCode(HttpStatus.SC_CREATED);
        String ingestionId = response.path(INGESTION_ID);

        pause(ConfigConstants.SLEEP_DURATION);
        response = queryEndpoints.getEntityMessagesByIngestionIdV2(ingestionId);

        pause(ConfigConstants.SLEEP_DURATION);

        response.then()
                .body(EVENTS_INGESTION_ID, equalTo(ingestionId))
                .body(EVENTS_DESCRIPTION, equalTo(EVENTS_CURATED_MESSAGE))
                .body(EVENTS_STATUS, equalTo(SUCCESSFUL))
                .statusCode(HttpStatus.SC_OK);
    }

    @Test(groups = {"smoke", "acceptance", "regression"},
            dataProvider = "getEntitiesWithoutPrimaryId",
            dataProviderClass = EntityDataProviderCommons.class,
            description = "DM-2570- Test to ingest synchronous entity with error payload without primaryId and validate the response in V2 API by querying with ingestionID"
                    + "GIVEN: an endpoint that accepts ingestion data"
                    + "WHEN: I issue a POST to that endpoint"
                    + "AND: The POST contains a error payload and v1-status:COMPLETED_WITH_ERRORS"
                    + "AND: I get status with V2 API"
                    + "THEN: I should receive a Processing status for V2 API"
                    + "AND: the response should contain an ingestionId,Status and Description"
    )
    public void getEntityProcessingByIngestionIdDirectSmoke(TestObject entity) throws InterruptedException {

        response = entityEndpointsCommons.postErrorEntityDirect(entity);
        response.then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
        String ingestionId = response.path(INGESTION_ID);

        pause(ConfigConstants.SLEEP_DURATION);
        response = queryEndpoints.getEntityMessagesByIngestionIdV2(ingestionId);
        response.then()
                .body(EVENTS_INGESTION_ID, equalTo(ingestionId))
                .body(EVENTS_DESCRIPTION, equalTo(EVENTS_VALIDATION_MESSAGE))
                .body(EVENTS_STATUS, equalTo(FAILED))
                .statusCode(HttpStatus.SC_OK);
    }

    @Test(groups = {"smoke", "acceptance", "regression"},
            dataProvider = "getItemArrayWithOneValidAndOneInvalidItem",
            dataProviderClass = EntityDataProviderCommons.class,
            description = "DM-2570- Test to ingest synchronous entity with valid and invalid payload and then validate the response in V2 API by querying with ingestionID"
                    + "GIVEN: an endpoint that accepts ingestion data"
                    + "WHEN: I issue a POST multiple items to that endpoint"
                    + "AND: The POST contains a valid and one invalid payload and v1 status COMPLETED with server curator"
                    + "AND: I get status with V2 API"
                    + "THEN: I should receive a Successful with errors status for V2 API"
                    + "AND: the response should contain an ingestionId,Status and Description"
    )
    public void getEntitySuccessfulWithErrorPartialValidDataDirectSmoke(TestObject entity) throws InterruptedException {
        JsonArray itemArray = new Gson().fromJson(entity.getBody(), JsonArray.class);
        response = entityEndpointsCommons.postEntityDirect(itemArray.toString(), entity.getResourceEntityType());
        response.then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
        String ingestionId = response.path(INGESTION_ID);

        pause(ConfigConstants.SLEEP_DURATION);
        response = queryEndpoints.getEntityMessagesByIngestionIdV2(ingestionId);

        response.then()
                .body(EVENTS_INGESTION_ID, equalTo(ingestionId))
                .body(EVENTS_STATUS, equalTo(FAILED))
                .statusCode(HttpStatus.SC_OK);
    }
}
