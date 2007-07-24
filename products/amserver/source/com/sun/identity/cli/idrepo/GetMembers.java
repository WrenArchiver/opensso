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
 * $Id: GetMembers.java,v 1.3 2007-07-24 19:39:34 veiming Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.cli.idrepo;


import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

/**
 * This command gets the members of an identity.
 */
public class GetMembers extends IdentityCommand {
    static final String ARGUMENT_MEMBERSHIP_IDTYPE = "membershipidtype";

    /**
     * Services a Commandline Request.
     *
     * @param rc Request Context.
     * @throw CLIException if the request cannot serviced.
     */
    public void handleRequest(RequestContext rc) 
        throws CLIException {
        super.handleRequest(rc);

        SSOToken adminSSOToken = getAdminSSOToken();
        IOutput outputWriter = getOutputWriter();
        String realm = getStringOptionValue(IArgument.REALM_NAME);
        String idName = getStringOptionValue(ARGUMENT_ID_NAME);
        String type = getStringOptionValue(ARGUMENT_ID_TYPE);
        IdType idType = convert2IdType(type);
        String membershipType = getStringOptionValue(
            ARGUMENT_MEMBERSHIP_IDTYPE);
        IdType membershipIdType = convert2IdType(membershipType);

        String[] params = {realm, type, idName, membershipType};

        try {
            AMIdentityRepository amir = new AMIdentityRepository(
                adminSSOToken, realm);
            Set memberOfs = membershipIdType.canBeMemberOf();
            if (!memberOfs.contains(idType)) {
                String[] args = {type, membershipType};
                throw new CLIException(MessageFormat.format(getResourceString(
                    "idrepo-cannot-be-member"), (Object[])args),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_IDREPO_GET_MEMBERS", params);
            AMIdentity amid = new AMIdentity(
                adminSSOToken, idName, idType, realm, null); 
            Set members = amid.getMembers(membershipIdType);

            if ((members != null) && !members.isEmpty()) {
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("idrepo-get-members-succeed"), 
                        (Object[])params));
                outputWriter.printlnMessage("");
                String msg = getResourceString("idrepo-members-result");
                String[] arg = {"", ""};

                for (Iterator i = members.iterator(); i.hasNext(); ) {
                    AMIdentity a = (AMIdentity)i.next();
                    arg[0] = a.getName();
                    arg[1] = a.getUniversalId();
                    outputWriter.printlnMessage(
                        MessageFormat.format(msg, (Object[])arg));
                }
            } else {
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("idrepo-no-members"),
                        (Object[])params));
            }
            writeLog(LogWriter.LOG_ACCESS, Level.INFO, 
                "SUCCEED_IDREPO_GET_MEMBERS", params);
        } catch (IdRepoException e) {
            String[] args = {realm, type, idName, membershipType,
                e.getMessage()};
            debugError("GetMembers.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_IDREPO_GET_MEMBERS", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {realm, type, idName, membershipType,
                e.getMessage()};
            debugError("GetMembers.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_IDREPO_GET_MEMBERS", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
