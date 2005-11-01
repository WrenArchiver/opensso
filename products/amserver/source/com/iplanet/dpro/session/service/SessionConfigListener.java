/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: SessionConfigListener.java,v 1.1 2005-11-01 00:29:55 arvindp Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.iplanet.dpro.session.service;

import java.util.Map;
import java.util.Set;

import com.iplanet.am.util.Debug;
import com.iplanet.am.util.Misc;
import com.sun.identity.common.Constants;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

/**
 * This class implements the interface <code>ServiceListener</code> in order
 * to receive session service data change notifications. The method
 * schemaChanged() is invoked when a session service schema data under
 * followings has been changed. Console/Service Configuration/Access Manager
 * Configuration/Session - Upper limit for session search result set size -
 * Search timeout
 * @see com.sun.identity.sm.ServiceSchemaManager
 */
public class SessionConfigListener implements ServiceListener {
    private static String SESSION_SERVICE_NAME = "iPlanetAMSessionService";

    private static final String SESSION_RETRIEVAL_TIMEOUT = 
        "iplanet-am-session-session-list-retrieval-timeout";

    private static final String MAX_SESSION_LIST_SIZE = 
        "iplanet-am-session-max-session-list-size";

    private static long defSessionRetrievalTimeout;

    private static int defMaxSessionListSize;

    private static Debug debug = Debug.getInstance("amSession");

    private static ServiceSchemaManager sSchemaMgr = null;

    public static long defSessionRetrievalTimeoutLong = 5;

    public static int defMaxSessionListSizeInt = 200;

    public static String defSessionRetrievalTimeoutStr = Long
            .toString(defSessionRetrievalTimeoutLong);

    public static String defMaxSessionListSizeStr = Integer
            .toString(defMaxSessionListSizeInt);

    private static String enablePropertyNotificationStr = "OFF";

   /**
    * Creates a new SessionConfigListener
    * @param ssm ServiceSchemaManager
    */
    public SessionConfigListener(ServiceSchemaManager ssm) {
        sSchemaMgr = ssm;
    }

    /**
     * This method is used to receive notifications if schema changes.
     * 
     * @param serviceName
     *            the name of the service.
     * @param version
     *            the version of the service. this method is No-op.
     */
    public void schemaChanged(String serviceName, String version) {
        if ((serviceName != null)
                && !serviceName.equalsIgnoreCase(SESSION_SERVICE_NAME)) {
            return;
        }

        try {
            ServiceSchema schema = sSchemaMgr.getGlobalSchema();
            Map attrs = schema.getAttributeDefaults();
            defSessionRetrievalTimeoutStr = Misc.getMapAttr(attrs,
                    SESSION_RETRIEVAL_TIMEOUT, defSessionRetrievalTimeoutStr);
            defMaxSessionListSizeStr = Misc.getMapAttr(attrs,
                    MAX_SESSION_LIST_SIZE, defMaxSessionListSizeStr);
            enablePropertyNotificationStr = Misc.getMapAttr(attrs,
                    Constants.PROPERTY_CHANGE_NOTIFICATION, "OFF");

            if (enablePropertyNotificationStr.equalsIgnoreCase("ON")) {
                SessionService.setPropertyNotificationEnabled(true);
                Set notProp = (Set) attrs
                        .get(Constants.NOTIFICATION_PROPERTY_LIST);
                SessionService.setNotificationProperties(notProp);
            } else {
                SessionService.setPropertyNotificationEnabled(false);
            }

        } catch (Exception e) {
            debug.error("SessionConfigListener : "
                    + "Unable to get Timeout & ListSize values", e);
        }

        try {
            defSessionRetrievalTimeout = Long
                    .parseLong(defSessionRetrievalTimeoutStr) * 1000;
        } catch (Exception e) {
            defSessionRetrievalTimeout = defSessionRetrievalTimeoutLong * 1000;
            debug.error(
                    "SessionConfigListener : Unable to parse Timeout values",
                            e);
        }

        try {
            defMaxSessionListSize = Integer.parseInt(defMaxSessionListSizeStr);
        } catch (Exception e) {
            defMaxSessionListSize = defMaxSessionListSizeInt;
            debug.error(
                    "SessionConfigListener : Unable to parse ListSize values",
                    e);
        }

    }

    /**
     * This method for implementing ServiceListener. As this object listens for
     * changes in schema of amConsoleService. this method is No-op.
     * 
     * @param serviceName
     *            name of the service
     * @param version
     *            version of the service
     * @param groupName
     *            name of the group
     * @param serviceComponent
     *            service component
     * @param type
     *            type of modification
     */
    public void globalConfigChanged(String serviceName, String version,
            String groupName, String serviceComponent, int type) {
        // No op.
    }

    /**
     * This method for implementing ServiveListener. As this object listens for
     * changes in schema of amConsoleService. this method is No-op.
     * 
     * @param serviceName
     *            name of the service
     * @param version
     *            version of the service
     * @param orgName
     *            name of the org
     * @param groupName
     *            name of the group
     * @param serviceComponent
     *            service component
     * @param type
     *            type of modification this method is No-op.
     */

    public void organizationConfigChanged(String serviceName, String version,
            String orgName, String groupName, String serviceComponent, int type)
    {
        // No op.
    }

    /**
     * Retrieves Timeout of Session search.
     * @return timeout for search
     */
    public static long getTimeout() {
        return defSessionRetrievalTimeout;
    }

    /**
     * Gets Max list size of Session search.
     * @return maximum size of sessions
     */
    public static int getMaxsize() {
        return defMaxSessionListSize;
    }
}
