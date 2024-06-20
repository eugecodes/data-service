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

import com.by.dp.functionaltests.constants.ConfigConstants;
import com.by.dp.functionaltests.constants.StatusEnum;
import com.by.dp.functionaltests.dataproviders.EntityDataProviderCommons;
import com.by.dp.functionaltests.endpoints.EntityEndpointsCommons;
import com.by.dp.functionaltests.testobjects.TestObject;
import com.by.dp.functionaltests.tests.TestBase;
import com.by.dp.functionaltests.testvalidators.EntityTestValidatorsCommons;
import com.by.dp.lifecycle.notification.common.Constants;
import com.by.dp.lifecycle.notification.functionaltests.endpoints.QueryEndpoints;
import com.by.dp.lifecycle.notification.functionaltests.testvalidators.QueryTestValidators;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import utilities.stubapplication.app.StubProducer;
import utilities.stubapplication.consumer.DownstreamConsumerException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static com.by.dp.functionaltests.constants.EntityConstants.ITEM_ENTITY_TYPE;
import static com.by.dp.functionaltests.constants.EntityConstants.RESOURCE_ITEM_ENTITY_TYPE;
import static com.by.dp.functionaltests.constants.IngestionResponseFields.CURATOR;
import static com.by.dp.functionaltests.constants.IngestionResponseFields.INGESTION_ID;
import static com.by.dp.functionaltests.constants.IngestionResponseFields.VALIDATOR;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.CONSUME;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.EVENTS_ACCEPTING_MESSAGE;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.EVENTS_CONSUMED_FAILED_MESSAGE;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.EVENTS_CONSUMED_MESSAGE;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.EVENTS_CURATED_MESSAGE;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.EVENTS_INGESTION_ID;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.EVENTS_REQUESTED_ON;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.EVENTS_STATUS;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.EVENTS_VALIDATION_MESSAGE;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.EVENT_DESCRIPTION;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.EVENT_SERVICE;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.EVENT_STATUS;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.FAILED;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.INGESTIONID;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.INVALID_INGESTION_ID;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.INVALID_ITEMS_ENTITY;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.REQUESTED_ON;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.SUCCESSFUL;
import static com.jda.testautomation.common.utils.CommonUtils.pause;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Test Class that queries persisted entities ingested via Yoda.
 **/

@SuppressWarnings("checkstyle:magicnumber")
public class QueryAsynchIngestionTests extends TestBase {

    EntityEndpointsCommons entityEndpointsCommons;
    QueryEndpoints queryEndpoints;
    QueryTestValidators queryTestValidators;
    EntityTestValidatorsCommons entityTestValidatorsCommons;
    StubProducer stubProducer;

    String statusLink;

    String statusLinkResponse;

    @BeforeMethod(alwaysRun = true)
    public void initialize() {
        entityEndpointsCommons = new EntityEndpointsCommons();
        queryEndpoints = new QueryEndpoints();
        queryTestValidators = new QueryTestValidators();
        entityTestValidatorsCommons = new EntityTestValidatorsCommons();
        stubProducer = new StubProducer(new HashMap<>());
        statusLink = entityEndpointsCommons.getStatusLinkPath();
        statusLinkResponse = entityEndpointsCommons.getStatusLinkResponsePath();
    }

    @AfterMethod(alwaysRun = true)
    public void cleanUp() {
        queryEndpoints.cleanUp();
        entityEndpointsCommons.cleanUp();
    }

    /**********************************
     * For entities ingested via yoda *
     **********************************/

    @Test(groups = {"regression"},
            description = "DM-2570- Test the response for v2 api by querying status"
                    + "GIVEN: an endpoint that returns all messages for a any entityType"
                    + "WHEN: I issue a GET to that endpoint for a status"
                    + "THEN: I should receive a list of messages"
    )
    public void getEntityStatusAllMessagesAsynchSmoke() {

        response = queryEndpoints.getAllEntityMessagesV2();

        queryTestValidators.validateSuccessResponseV2(response);
    }

    @Test(groups = {"regression"},
            description = "DM-2570- Test the response for v2 api by Entity type query"
                    + "GIVEN: an endpoint that returns all messages for a specific entityType"
                    + "WHEN: I issue a GET to that endpoint for an entityType for an item"
                    + "THEN: I should receive a list of messages"
    )
    public void getEntityMessagesbyEntityTypeAsynchSmoke() {
        response = queryEndpoints.getAllEntityMessagesByEntityTypeV2(RESOURCE_ITEM_ENTITY_TYPE);

        queryTestValidators.validateSuccessResponseEntityTypeV2(response, RESOURCE_ITEM_ENTITY_TYPE);

    }

    @Test(groups = {"regression"},
            description = "DM-2570- Test the response for v2 api by Invalid Entity type query"
                    + "GIVEN: an endpoint that returns all messages for a specific entityType"
                    + "WHEN: I issue a GET to that endpoint for an invalid entityType for an item"
                    + "THEN: I should receive an Error- No record found"
    )
    public void getInvalidEntityNotFoundMessagesAsynchSmoke() {

        response = queryEndpoints.getAllEntityMessagesByEntityTypeV2(INVALID_ITEMS_ENTITY);

        queryTestValidators.validateErrorResponseInvalidEntityTypeV2(response, INVALID_ITEMS_ENTITY);
    }


    @Test(groups = {"regression"},
            description = "DM-2570- Test the response for v2 api by Invalid Ingestion ID query"
                    + "GIVEN: an endpoint that returns all messages for a specific IngestionId"
                    + "WHEN: I issue a GET to that endpoint for an invalid IngestionId for an item"
                    + "THEN: I should receive an Error- No record found"
    )
    public void getInvalidIngestionIdErrorMessagesAsynchSmoke() {

        response = queryEndpoints.getEntityMessagesByInvalidIngestionIdV2(INVALID_INGESTION_ID);

        queryTestValidators.validateErrorResponseInvalidIngestionIdV2(response, INVALID_INGESTION_ID);
    }


    @Test(groups = {"regression"},
            dataProvider = "getEntities",
            dataProviderClass = EntityDataProviderCommons.class,
            description = "DM-2570- Test to ingest asynchronous entity with valid payload and validate the V2 response by querying with ingestionID"
                    + "GIVEN: an endpoint that accepts ingestion data"
                    + "WHEN: I issue a POST to that endpoint"
                    + "AND: The POST contains a valid payload"
                    + "AND: I get status with V2 API"
                    + "THEN: I should receive a successfull status for V2 API"
                    + "AND: the response should contain an ingestionId,Status and Description"
    )
    public void getEntitiesSuccessfulStatusByIngestionIdAsyncSmoke(TestObject entity) throws InterruptedException {
        response = entityEndpointsCommons.postEntityAsynch(entity);

        entityTestValidatorsCommons.validateAsynchEntityIngestion(response, statusLinkResponse);
        String ingestionId = response.path(INGESTIONID);

        Response statusResponse = entityEndpointsCommons.retryUntilIngestionComplete(statusLink + response.path(INGESTION_ID));
        statusResponse.then()
                .body(EVENT_SERVICE, hasItem(VALIDATOR))
                .body(EVENT_SERVICE, hasItem(CURATOR))
                .body(EVENT_STATUS, hasItem(String.valueOf(StatusEnum.COMPLETED)))
                .body(EVENT_STATUS, not(String.valueOf(StatusEnum.COMPLETED_WITH_ERROR)))
                .body(EVENT_DESCRIPTION, hasItem(EVENTS_CONSUMED_MESSAGE))
                .statusCode(HttpStatus.SC_ACCEPTED);

        pause(ConfigConstants.SLEEP_DURATION);
        response = queryEndpoints.getEntityMessagesByIngestionIdV2(ingestionId);
        response.then()
                .body(EVENTS_INGESTION_ID, equalTo(ingestionId))
                .body(EVENTS_STATUS, equalTo(SUCCESSFUL))
                .statusCode(HttpStatus.SC_OK);

    }

    @Test(groups = {"regression"},
            dataProvider = "getEntitiesWithoutPrimaryId",
            dataProviderClass = EntityDataProviderCommons.class,
            description = "DM-2570- Test to ingest asynchronous entity with invalid payload and validate the response by querying with ingestionID"
                    + "GIVEN: an endpoint that accepts ingestion data"
                    + "WHEN: I issue a POST to that endpoint"
                    + "AND: The POST contains a invalid payload"
                    + "AND: I get status with V2 API"
                    + "THEN: I should receive a Failed status for V2 API"
                    + "AND: the response should contain an ingestionId,Status and Description"
    )
    public void getEntityFailedErrorByIngestionIdAsyncSmoke(TestObject entity) throws InterruptedException {
        response = entityEndpointsCommons.postEntityAsynch(entity);

        entityTestValidatorsCommons.validateAsynchEntityIngestion(response, statusLinkResponse);

        String ingestionId = response.path(INGESTIONID);

        Response statusResponse = entityEndpointsCommons.retryUntilIngestionComplete(statusLink + response.path(INGESTION_ID));
        statusResponse.then()
                .body(EVENT_SERVICE, hasItem(VALIDATOR))
                .body(EVENT_SERVICE, hasItem(CONSUME))
                .body(EVENT_STATUS, hasItem(String.valueOf(StatusEnum.COMPLETED_WITH_ERROR)))
                .body(EVENT_STATUS, not(String.valueOf(StatusEnum.COMPLETED)))
                .body(EVENT_DESCRIPTION, hasItem(EVENTS_ACCEPTING_MESSAGE))
                .body(EVENT_DESCRIPTION, hasItem(EVENTS_CONSUMED_FAILED_MESSAGE))
                .body(EVENT_DESCRIPTION, hasItem(EVENTS_VALIDATION_MESSAGE))
                .statusCode(HttpStatus.SC_ACCEPTED);

        pause(ConfigConstants.SLEEP_DURATION);
        response = queryEndpoints.getEntityMessagesByIngestionIdV2(ingestionId);
        response.then()
                .body(EVENTS_INGESTION_ID, equalTo(ingestionId))
                .body(EVENTS_STATUS, equalTo(FAILED))
                .statusCode(HttpStatus.SC_OK);
    }

    @Test(groups = {"regression"},
            dataProvider = "getItemArrayWithOneValidAndOneInvalidItem",
            dataProviderClass = EntityDataProviderCommons.class,
            description = "DM-2570- Test to ingest asynchronous entity with valid and invalid payload and validate the response by querying with ingestionID"
                    + "GIVEN: an endpoint that accepts ingestion data"
                    + "WHEN: I issue a POST to that endpoint"
                    + "AND: The POST contains a valid and invalid payload"
                    + "AND: I get status with V2 API"
                    + "THEN: I should receive a Successfull with errors status for V2 API"
                    + "AND: the response should contain an ingestionId,Status and Description"
    )
    public void getEntitySuccessfulWithErrorPartialValidDataAsynchSmoke(TestObject entity) throws InterruptedException {

        response = entityEndpointsCommons.postEntityAsynch(entity);

        entityTestValidatorsCommons.validateAsynchEntityIngestion(response, statusLinkResponse);
        String ingestionId = response.path(INGESTIONID);

        Response statusResponse = entityEndpointsCommons.retryUntilIngestionComplete(statusLink + response.path(INGESTION_ID));
        statusResponse.then()
                .body(EVENT_SERVICE, hasItem(VALIDATOR))
                .body(EVENT_SERVICE, hasItem(CURATOR))
                .body(EVENT_STATUS, hasItem(String.valueOf(StatusEnum.COMPLETED_WITH_ERROR)))
                .body(EVENT_DESCRIPTION, hasItem(EVENTS_VALIDATION_MESSAGE))
                .body(EVENT_DESCRIPTION, hasItem(EVENTS_CURATED_MESSAGE))
                .statusCode(HttpStatus.SC_ACCEPTED);

        pause(ConfigConstants.SLEEP_DURATION);
        response = queryEndpoints.getEntityMessagesByIngestionIdV2(ingestionId);
        response.then()
                .body(EVENTS_INGESTION_ID, equalTo(ingestionId))
                .body(EVENTS_STATUS, equalTo(FAILED))
                .statusCode(HttpStatus.SC_OK);
    }

    @Test(groups = {"regression"},
            dataProvider = "getEntities",
            dataProviderClass = EntityDataProviderCommons.class,
            description = "DM-2570- Test to ingest asynchronous entity with valid payload and validate the V2 response by querying with requestedOn filter"
                    + "GIVEN: an endpoint that accepts ingestion data"
                    + "WHEN: I issue a POST to that endpoint"
                    + "AND: The POST contains a valid payload"
                    + "AND: I get status with V2 API"
                    + "THEN: I should receive a successfull status for V2 API"
                    + "AND: the response should contain an ingestionId,Status and Description"
    )
    public void getEntitiesSuccessfullStatusbyRequestedOnAsyncSmoke(TestObject entity) throws InterruptedException, ParseException {
        response = entityEndpointsCommons.postEntityAsynch(entity);

        entityTestValidatorsCommons.validateAsynchEntityIngestion(response, statusLinkResponse);

        String ingestionId = response.path(INGESTIONID);

        Response statusResponse = entityEndpointsCommons.retryUntilIngestionComplete(statusLink + response.path(INGESTION_ID));
        String requestedDateTime = statusResponse.path(REQUESTED_ON);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        Date addedDateTime = simpleDateFormat.parse(requestedDateTime);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(addedDateTime);
        calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) - 5);

        String requestedOn = simpleDateFormat.format(calendar.getTime()) + "," + statusResponse.path(REQUESTED_ON).toString();
        statusResponse.then()
                .body(EVENT_SERVICE, hasItem(VALIDATOR))
                .body(EVENT_SERVICE, hasItem(CURATOR))
                .body(EVENT_STATUS, hasItem(String.valueOf(StatusEnum.COMPLETED)))
                .body(EVENT_STATUS, not(String.valueOf(StatusEnum.COMPLETED_WITH_ERROR)))
                .body(EVENT_DESCRIPTION, hasItem(EVENTS_CONSUMED_MESSAGE))
                .statusCode(HttpStatus.SC_ACCEPTED);

        pause(ConfigConstants.SLEEP_DURATION);
        response = queryEndpoints.getEntityMessagesByRequestedOnV2(requestedOn);
        response.then()
                .body(containsString(ingestionId))
                .body(EVENTS_REQUESTED_ON, notNullValue())
                .body(EVENTS_STATUS, equalTo(SUCCESSFUL))
                .statusCode(HttpStatus.SC_OK);

    }

    @Test(groups = {"regression"},
            dataProvider = "getEntitiesWithoutPrimaryId",
            dataProviderClass = EntityDataProviderCommons.class,
            description = "DM-2570- Test to ingest synchronous entity with error payload without primaryId and validate the response in V2 API by querying with ingestionID and 24Hrs filter"
                    + "GIVEN: an endpoint that accepts ingestion data"
                    + "WHEN: I issue a POST to that endpoint"
                    + "AND: The POST contains a error payload and v1-status:COMPLETED_WITH_ERRORS"
                    + "AND: I get status with V2 API"
                    + "THEN: I should receive a Processing status for V2 API"
                    + "AND: the response should contain an ingestionId,Status and Description"
    )
    public void getErrorEntityByIngestionIdAndLast24hrsFilterDirectSmoke(TestObject entity) throws InterruptedException {

        response = entityEndpointsCommons.postEntityAsynch(entity);

        String ingestionId = response.then().extract().path(INGESTION_ID);

        Response statusResponse = entityEndpointsCommons.retryUntilIngestionComplete(statusLink + ingestionId);
        statusResponse.then()
                .body(EVENT_SERVICE, hasItem(VALIDATOR))
                .body(EVENT_STATUS, hasItem(String.valueOf(StatusEnum.COMPLETED_WITH_ERROR)))
                .body(EVENT_STATUS, not(String.valueOf(StatusEnum.COMPLETED)))
                .body(EVENT_DESCRIPTION, hasItem(EVENTS_VALIDATION_MESSAGE))
                .statusCode(HttpStatus.SC_ACCEPTED);

        pause(ConfigConstants.SLEEP_DURATION);
        response = queryEndpoints.getErrorEntityMessagesByIngestionIdAnd24HrsOnV2(ingestionId);
        response.then()
                .body(containsString(ingestionId))
                .body(containsString(EVENTS_CONSUMED_FAILED_MESSAGE))
                .body(containsString(FAILED))
                .statusCode(HttpStatus.SC_OK);
    }


    @Test(groups = {"regression"},
            dataProvider = "getEntitiesWithoutPrimaryId",
            dataProviderClass = EntityDataProviderCommons.class,
            description = "DM-7615- Test to post message directly to public.dp.application.status topic and validate the response in V2 AP "
                    + "GIVEN: an endpoint that accepts ingestion data"
                    + "WHEN: I issue a POST to that endpoint"
                    + "AND: The POST contains a error payload and v1-status:COMPLETED_WITH_ERRORS"
                    + "AND: I use the ingestionId from the POST asynch as kafka header to directly publish the application status event"
                    + "AND: I get status with V2 API"
                    + "THEN: I should receive a Processing status for V2 API"
                    + "AND: the response should contain an ingestionId,Status and Description"
    )
    public void getErrorEntityAsynch(TestObject entity) throws InterruptedException, DownstreamConsumerException {

        response = entityEndpointsCommons.postEntityAsynch(entity);
        entityTestValidatorsCommons.validateAsynchEntityIngestion(response, statusLinkResponse);

        String ingestionId = response.path(INGESTION_ID);
        String message = TestObject.readFromFile("feedbackMessageWithNoEntityId.json");

        Map<String, String> kafkaHeaders = new HashMap<>();
        kafkaHeaders.put(INGESTION_ID, ingestionId);
        String[] ingestionIdSplit = ingestionId.split("-");
        kafkaHeaders.put("tenantId", ingestionIdSplit[0]);
        kafkaHeaders.put("entityType", ITEM_ENTITY_TYPE);

        stubProducer.publishApplicationStatusEvent("public.dp.application.status", message, kafkaHeaders);


        Response statusResponse = entityEndpointsCommons.retryUntilIngestionComplete(statusLink + ingestionId);
        statusResponse.then()
                .body(EVENT_SERVICE, hasItem(VALIDATOR))
                .body(EVENT_STATUS, hasItem(String.valueOf(StatusEnum.COMPLETED_WITH_ERROR)))
                .body(EVENT_STATUS, not(String.valueOf(StatusEnum.COMPLETED)))
                .body(EVENT_DESCRIPTION, hasItem(EVENTS_VALIDATION_MESSAGE))
                .statusCode(HttpStatus.SC_ACCEPTED);

        pause(ConfigConstants.SLEEP_DURATION);
        response = queryEndpoints.getErrorEntityMessagesByIngestionIdAnd24HrsOnV2(ingestionId);
        response.then()
                .body(containsString(ingestionId))
                .body(containsString(FAILED))
                .statusCode(HttpStatus.SC_OK);
    }
}

