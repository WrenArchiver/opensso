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
 * $Id: IdGetSupportedIdTypesReq.java,v 1.2 2008-06-25 05:52:28 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;

class IdGetSupportedIdTypesReq extends AdminReq {
    private String realmPath = null;


    /**
     * Constructs a new IdGetSupportedIdTypesReq.
     *
     * @param  targetDN the parent Realm DN. 
     */        
    IdGetSupportedIdTypesReq(String targetDN) {
        super(targetDN);
        realmPath = targetDN;
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription119") +
            " " + targetDN);
        prnWriter.flush();
        return stringWriter.toString();    
    }
    
    void process(SSOToken ssoToken)
        throws AdminException
    {
        AdminReq.writer.println(bundle.getString("identity") + "\n" +
            bundle.getString("getSupportedIdTypes") + " " + targetDN);

        PrintUtils prnUtl = new PrintUtils(AdminReq.writer);

        try {
            AMIdentityRepository amir =
                new AMIdentityRepository (ssoToken, realmPath);

            /*
             * set up the search for the idPattern(s) of the
             * idType to get their AMIdentity objects
             */
            Set atSet = amir.getSupportedIdTypes();
            prnUtl.printSet(atSet, 1);

        } catch (IdRepoException ire) {
            throw new AdminException(ire);
        } catch (SSOException ssoex) {
            throw new AdminException(ssoex);
        }
    }
}

