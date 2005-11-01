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
 * $Id: AdminUtils.java,v 1.1 2005-11-01 00:29:36 arvindp Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.iplanet.am.util;

import java.security.AccessController;

import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.ldap.LDAPServiceException;
import com.iplanet.services.ldap.LDAPUser;
import com.iplanet.services.ldap.ServerInstance;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.services.util.Crypt;
import com.iplanet.ums.IUMSConstants;
import com.sun.identity.security.ISSecurityPermission;
import com.sun.identity.security.ServerInstanceAction;

/* iPlanet-PUBLIC-CLASS */

/**
 * This class contains methods to retrieve Top Level Administrator information.
 * The information comes from the server configuration file 
 * (<code>serverconfig.xml</code>).
 */
public class AdminUtils {

    private static String adminDN = null;

    private static byte[] adminPassword = null;

    private static Debug debug;

    static {
        debug = Debug.getInstance(IUMSConstants.UMS_DEBUG);

        try {
            DSConfigMgr dscMgr = DSConfigMgr.getDSConfigMgr();
            ServerInstance svrInstance = dscMgr
                    .getServerInstance(LDAPUser.Type.AUTH_ADMIN);

            if (svrInstance != null) {
                adminDN = svrInstance.getAuthID();
                String adminPW = (String) AccessController
                        .doPrivileged(new ServerInstanceAction(svrInstance));
                adminPassword = xor(adminPW.getBytes());
            } else {
                debug.error("AdminUtils: server instance not found");
            }

        } catch (LDAPServiceException e) {
            if (WebtopNaming.isServerMode()) {
                debug.error("AdminUtils: Initialize admin info ", e);
            } else if (debug.messageEnabled()) {
                debug.message("AdminUtils: Could not initialize admin "
                        + " info message: " + e.getMessage());
            }
        }
    }

    /**
     * Returns the DN of the Top Level Administrator.
     * 
     * @return The DN of the Top Level Administrator; null if the Top Level
     *         Administrator is not defined in the server configuration file.
     */
    public static String getAdminDN() {
        if (Crypt.checkCaller()) {
            ISSecurityPermission isp = new ISSecurityPermission("access",
                    "adminpassword");
            try {
                if (Crypt.securityManager != null) {
                    Crypt.securityManager.checkPermission(isp);
                }

            } catch (SecurityException e) {
                debug.error(
                        "Security Alert: Unauthorized access to Administative "
                                + "password utility: Returning NULL", e);
                return null;
            }
        }
        return adminDN;
    }

    /**
     * Returns the password of the Top Level Administrator.
     * 
     * @return The password of the Top Level Administrator; null if the Top
     *         Level Administrator is not defined in the server configuration
     *         file.
     */
    public static byte[] getAdminPassword() {
        if (Crypt.checkCaller()) {
            ISSecurityPermission isp = new ISSecurityPermission("access",
                    "adminpassword");
            try {
                if (Crypt.securityManager != null) {
                    Crypt.securityManager.checkPermission(isp);
                }
            } catch (SecurityException e) {
                debug.error(
                        "Security Alert: Unauthorized access to Administative "
                                + "password utility: Returning NULL", e);
                return null;
            }
        }
        return xor(adminPassword);
    }

    /**
     * To encode and decode the password.
     */
    private static byte[] xor(byte[] password) {
        if (password != null) {
            int len = password.length;
            byte[] retPassword = new byte[len];
            for (int i = 0; i < len; i++) {
                retPassword[i] = (byte) (password[i] ^ 1);
            }
            return retPassword;
        } else {
            return null;
        }
    }

}
