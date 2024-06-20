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
package com.by.dp.lifecycle.notification.common;

/**
 * Class to hold common Constants across the ingestion life cycle
 */
public class Constants {
    public static final String V_1 = "_v1";

    public static final String ERRORS = "errors";
    public static final String EVENT_INDEX = "ingestionevents";
    public static final String EGRESS_INDEX = "egressevents";
    public static final String EGRESS_TYPES = "egress";
    public static final String EVENT_TYPES = "entity";

    public static final String INGESTION_IDS = "ingestionIds";
    public static final String DESCRIPTION = "description";
    public static final String COMPONENT = "component";
    public static final String SERVICE = "service";
    public static final String STATUS = "status";
    public static final String ERROR_MSG = "errorMessage";
    public static final String ENTITY_ID = "entityId";
    public static final String DEFAULT_ENTITY = "default_entity";
    public static final String AUTH_CONTEXT = "authContext";
    public static final String BODY = "body";
    public static final String TENANT_ID = "currentTid";
    public static final String TENANTID = "tenantId";
    public static final String REALM_ID = "realmId";
    public static final String CLIENT_ID = "clientId";
    public static final String USER = "user";
    public static final String EGRESS_ID = "egressId";
    public static final String CURRENT_USER = "currentUser";
    public static final String END_POINTS = "endpoint";
    public static final String EVENT_TIME = "eventTime";
    public static final String APPLICATION = "application";
    public static final String ENTITY_TYPE = "entityType";
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    //Compression
    public static final String GZ = "gz";
    public static final String CONTENT_TYPE_LOWER_CASE = "content-type";

    public static final String DETAILED_INFO = "detailed_info";
    public static final String RUN_ID = "run_id";
    public static final String RUNID = "runId";
    public static final String DISTRIBUTION_INDEX = "distributionevents";
    public static final String SERVICE_TYPE = "service";
    public static final String DISTRIBUTION = "distribution";
    public static final String DATEFORMAT = "MM/dd/yyyy HH:mm:ss";
    public static final String UTC_ZONE = "UTC";
    public static final String REALMID = "realm_id";
    public static final String TEMPLATE_GROUP = "template_group";
    private Constants() {
    }
}
