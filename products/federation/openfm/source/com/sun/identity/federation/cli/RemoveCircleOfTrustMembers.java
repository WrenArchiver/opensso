/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: RemoveCircleOfTrustMembers.java,v 1.3 2008-06-25 05:49:53 qcheng Exp $
 *
 */

package com.sun.identity.federation.cli;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.cot.COTException;
import com.sun.identity.cot.COTUtils;
import java.text.MessageFormat;

/**
 * Remove member from a Circle of Trust.
 */
public class RemoveCircleOfTrustMembers extends AuthenticatedCommand {
    private static Debug debug = COTUtils.debug;
    
    private String realm;
    private String cot;
    private String entityID;
    private String spec;
    
    /**
     * Removes member from a circle of trust.
     *
     * @param rc Request Context.
     * @throws CLIException if unable to process this request.
     */
    public void handleRequest(RequestContext rc) 
        throws CLIException {
        super.handleRequest(rc);
        ldapLogin();
        realm = getStringOptionValue(FedCLIConstants.ARGUMENT_REALM, "/");
        cot = getStringOptionValue(FedCLIConstants.ARGUMENT_COT);
        entityID = getStringOptionValue(FedCLIConstants.ARGUMENT_ENTITY_ID);
        spec = FederationManager.getIDFFSubCommandSpecification(rc);
        
        try {
            CircleOfTrustManager cotManager = new CircleOfTrustManager();
            cotManager.removeCircleOfTrustMember(realm,cot,spec,entityID);
            
            Object[] objs = {cot, entityID, realm};
            getOutputWriter().printlnMessage(MessageFormat.format(
                getResourceString("remove-circle-of-trust-member-succeeded"),
                objs));
        } catch (COTException e) {
            debug.warning("RemoveCircleOfTrustMembers.handleRequest", e);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
