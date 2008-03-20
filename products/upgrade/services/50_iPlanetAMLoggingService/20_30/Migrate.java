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
 * $Id: Migrate.java,v 1.2 2008-03-20 17:23:59 bina Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */

import com.sun.identity.upgrade.MigrateTasks;
import com.sun.identity.upgrade.UpgradeException;
import com.sun.identity.upgrade.UpgradeUtils;

/**
 * Updates <code>iPlanetAMLoggingService</code> service schema.
 * This class is invoked during migration from older versions
 * of Access Manager to the latest version.
 */
public class Migrate implements MigrateTasks {

    final static String SERVICE_NAME = "iPlanetAMLoggingService";
    final static String SERVICE_DIR = "50_iPlanetAMLoggingService/20_30";
    final static String SCHEMA_FILE1 = "add_amLogging0.xml";
    final static String SCHEMA_FILE2 = "add_amLogging1.xml";
    final static String ATTR_NAME = "sun-am-logging-db-read-handler";
    final static String SCHEMA_TYPE = "Global";

    /**
     * Updates the <code>iPlanetAMLoggingService<code> service schema.
     *
     * @return true if successful otherwise false.
     */
    public boolean migrateService() {
        boolean isSuccess = false;
        try {
            if (!UpgradeUtils.attributeExists(
                    SERVICE_NAME, ATTR_NAME, SCHEMA_TYPE)) {
                String[] fileList = new String[2];
                fileList[0] =
                        UpgradeUtils.getAbsolutePath(SERVICE_DIR, SCHEMA_FILE1);
                fileList[1] =
                        UpgradeUtils.getAbsolutePath(SERVICE_DIR, SCHEMA_FILE2);
                UpgradeUtils.importServiceData(fileList);
                isSuccess = true;
            } else {
                // attributes already exists
                // do nothing.
                isSuccess = true;
            }
        } catch (UpgradeException e) {
            UpgradeUtils.debug.error("Error loading data:" + SERVICE_NAME, e);
        }
        return isSuccess;
    }

    /**
     * Post Migration operations.
     *
     * @return true if successful else error.
     */
    public boolean postMigrateTask() {
        return true;
    }

    /**
     * Pre Migration operations.
     *
     * @return true if successful else error.
     */
    public boolean preMigrateTask() {
        return true;
    }
}
