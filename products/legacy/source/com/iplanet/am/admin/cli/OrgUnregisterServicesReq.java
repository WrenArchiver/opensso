/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
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
 * $Id: OrgUnregisterServicesReq.java,v 1.2 2008-06-25 05:52:31 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

class OrgUnregisterServicesReq extends AdminReq {
    private Set serviceSet = new HashSet();
    
    /**
     * Constructs a new empty OrgUnregisterServicesReq.
     */
    OrgUnregisterServicesReq() {
        super();    
    }
        
    /**
     * Constructs a new OrgUnregisterServicesReq.
     *
     * @param  targetDN the Organization DN. 
     */        
    OrgUnregisterServicesReq(String targetDN) {
        super(targetDN);
    }

    /**
     * adds the org service's to a Set to unregister services.
     *
     * @param orgService the Service of the org
     */
    void unregisterServicesReq(String orgService) {
        serviceSet.add(orgService);
    }

    /**
     * gets the unregisterOrgreq set which contains all the services to 
     * unregister.
     *
     * @return serviceSet which contains all the services of the org.
     */
    Set getUnregisterOrgReq() {
        return serviceSet;
    }
    
    /**
     * converts this object into a string.
     *
     * @return String. 
     */
    public String toString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter prnWriter = new PrintWriter(stringWriter);
        PrintUtils prnUtl = new PrintUtils(prnWriter); 
        prnWriter.println(AdminReq.bundle.getString("requestdescription45") +
            " " + targetDN);
        prnUtl.printSet(serviceSet,1);
        prnWriter.flush();
        return stringWriter.toString();    
    }
    
    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        AdminReq.writer.println(bundle.getString("organization") + " " +
            targetDN + "\n" + bundle.getString("unregisteredservices"));

        try {
            AMOrganization org = dpConnection.getOrganization(targetDN);

            for (Iterator iter = serviceSet.iterator(); iter.hasNext(); ) {
                String serviceName = (String)iter.next();
                doLog(serviceName, org, AdminUtils.UNREGISTER_SERVICE_ATTEMPT);
                org.unregisterService(serviceName);
                AdminReq.writer.println(serviceName);
//                doLog(serviceName, org, "unregister-service");
                doLog(serviceName, org, AdminUtils.UNREGISTER_SERVICE);
            }
        } catch (AMException dpe) {
            throw new AdminException(dpe.toString());
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe.toString());
        }
    }
}
