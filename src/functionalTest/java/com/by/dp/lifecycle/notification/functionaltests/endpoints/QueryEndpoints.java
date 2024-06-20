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

package com.by.dp.lifecycle.notification.functionaltests.endpoints;

import io.restassured.response.Response;
import com.by.dp.functionaltests.constants.IngestionStatusConstants;
import com.by.dp.functionaltests.endpoints.QueryEndpointsCommons;

import static com.by.dp.functionaltests.constants.DateFormats.HRS_FILTER;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.ALL_ENTITY_MESSAGES_ENTITY_TYPE_V2;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.ALL_ENTITY_MESSAGES_QUERY_INGESTION_ID_V2;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.ALL_ENTITY_MESSAGES_QUERY_REQUESTED_ON_V2;
import static com.by.dp.functionaltests.constants.IngestionStatusConstants.ENTITY_MESSAGES_QUERY_BY_INGESTION_ID_AND_24HRS_FILTER_V2;


/**
 * Class for synchronous get endpoints
 **/

public class QueryEndpoints extends QueryEndpointsCommons {


    public QueryEndpoints() {
        setUrl();
    }

    /**
     * Get All Entity Messages V2
     *
     * @return
     */
    public Response getAllEntityMessagesV2() {
        setUrl();
        String getAllEntityMessagesV2 = String.format(IngestionStatusConstants.ALL_ENTITY_MESSAGES_V2);

        return httpRequest
                .contentType(APPLICATION_JSON_CONTENT_TYPE)
                .get(getAllEntityMessagesV2);
    }

    /**
     * Get All Entity Messages By Entity Type V2
     *
     * @param entityType
     * @return
     */
    public Response getAllEntityMessagesByEntityTypeV2(String entityType) {
        setUrl();
        String getEntities = String.format(ALL_ENTITY_MESSAGES_ENTITY_TYPE_V2, entityType);

        return httpRequest
                .contentType(APPLICATION_JSON_CONTENT_TYPE)
                .get(getEntities);
    }

    /**
     * Get Entity Messages By IngestionId V2
     *
     * @param ingestionId
     * @return
     */
    public Response getEntityMessagesByIngestionIdV2(String ingestionId) {
        setUrl();
        String getEntities = String.format(ALL_ENTITY_MESSAGES_QUERY_INGESTION_ID_V2, ingestionId);
        return httpRequest
                .contentType(APPLICATION_JSON_CONTENT_TYPE)
                .get(getEntities);
    }

    /**
     * Get Entity Messages By RequestedOn V2
     *
     * @param requestedOn
     * @return
     */
    public Response getEntityMessagesByRequestedOnV2(String requestedOn) {
        setUrl();
        String getEntities = String.format(ALL_ENTITY_MESSAGES_QUERY_REQUESTED_ON_V2, requestedOn);
        return httpRequest
                .contentType(APPLICATION_JSON_CONTENT_TYPE)
                .get(getEntities);
    }

    /**
     * Get Entity Messages By Invalid IngestionId V2
     *
     * @param invalidIngestionId
     * @return
     */
    public Response getEntityMessagesByInvalidIngestionIdV2(String invalidIngestionId) {
        setUrl();
        String getEntities = String.format(ALL_ENTITY_MESSAGES_QUERY_INGESTION_ID_V2, invalidIngestionId);
        return httpRequest
                .contentType(APPLICATION_JSON_CONTENT_TYPE)
                .get(getEntities);
    }

    /**
     * Get Error Entity Messages By IngestionId And 24Hrs On V2
     *
     * @param ingestionId
     * @return
     */
    public Response getErrorEntityMessagesByIngestionIdAnd24HrsOnV2(String ingestionId) {
        setUrl();
        String getEntities = String.format(ENTITY_MESSAGES_QUERY_BY_INGESTION_ID_AND_24HRS_FILTER_V2, ingestionId, HRS_FILTER);
        return httpRequest
                .contentType(APPLICATION_JSON_CONTENT_TYPE)
                .get(getEntities);
    }

}
