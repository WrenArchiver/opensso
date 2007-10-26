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
 * $Id: ShowAgent.java,v 1.1 2007-10-26 17:15:17 veiming Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.cli.agentconfig;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * This command gets attribute values of an agent.
 */
public class ShowAgent extends AuthenticatedCommand {
    /**
     * Services a Commandline Request.
     *
     * @param rc Request Context.
     * @throw CLIException if the request cannot serviced.
     */
    public void handleRequest(RequestContext rc) 
        throws CLIException {
        super.handleRequest(rc);
        ldapLogin();

        SSOToken adminSSOToken = getAdminSSOToken();
        IOutput outputWriter = getOutputWriter();
        String realm = "/";
        String agentName = getStringOptionValue(IArgument.AGENT_NAME);
        String outfile = getStringOptionValue(IArgument.OUTPUT_FILE);
        String[] params = {realm, agentName};

        try {
            writeLog(LogWriter.LOG_ACCESS, Level.INFO, "ATTEMPT_SHOW_AGENT",
                params);
            AMIdentity amid = new AMIdentity(adminSSOToken, agentName,
                IdType.AGENTONLY, realm, null); 
            Map values = amid.getAttributes();

            if ((values != null) && !values.isEmpty()) {
                StringBuffer buff = new StringBuffer();
                for (Iterator i = values.keySet().iterator(); i.hasNext();) {
                    String attrName = (String)i.next();
                    Set attrValues = (Set)values.get(attrName);

                    for (Iterator j = attrValues.iterator(); j.hasNext(); ){
                        String val = (String)j.next();
                        buff.append(attrName).append("=").append(val)
                            .append("\n");
                    }
                }
                if (outfile == null) {
                    outputWriter.printlnMessage(getResourceString(
                        "show-agent-succeeded"));
                    outputWriter.printlnMessage(buff.toString());
                } else {
                    writeToFile(outfile, buff.toString());
                    outputWriter.printlnMessage(getResourceString(
                        "show-agent-to-file"));
                }
            } else {
                outputWriter.printlnMessage(getResourceString(
                    "show-agent-no-attributes"));
            }
            writeLog(LogWriter.LOG_ACCESS, Level.INFO, "SUCCEED_SHOW_AGENT",
                params);
        } catch (IdRepoException e) {
            String[] args = {realm, agentName, e.getMessage()};
            debugError("ShowAgent.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_SHOW_AGENT", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {realm, agentName, e.getMessage()};
            debugError("ShowAgent.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_SHOW_AGENT", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private void writeToFile(String outfile, String content)
        throws CLIException {
        FileOutputStream fout = null;
        PrintWriter pwout = null;

        try {
            fout = new FileOutputStream(outfile, true);
            pwout = new PrintWriter(fout, true);
            pwout.write(content);
        } catch (FileNotFoundException e) {
            debugError("ShowAgent.writeToFile", e);
            throw new CLIException(e, ExitCodes.IO_EXCEPTION);
        } catch (SecurityException e) {
            debugError("ShowAgent.writeToFile", e);
            throw new CLIException(e, ExitCodes.IO_EXCEPTION);
        } finally {
            try {
                if (fout != null) {
                    fout.close();
                }
                if (pwout != null) {
                    pwout.close();
                }
            } catch (IOException ex) {
                //do nothing
            }
        }
    }
}
