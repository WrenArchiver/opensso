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
 * $Id: AddAttributeDefaults.java,v 1.3 2008-06-25 05:42:17 qcheng Exp $
 *
 */

package com.sun.identity.cli.schema;


import com.sun.identity.cli.AttributeValues;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Adds default attribute values of schema.
 */
public class AddAttributeDefaults extends SchemaCommand {
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

        String schemaType = getStringOptionValue(IArgument.SCHEMA_TYPE);
        String serviceName = getStringOptionValue(IArgument.SERVICE_NAME);
        String subSchemaName = getStringOptionValue(IArgument.SUBSCHEMA_NAME);
        String datafile = getStringOptionValue(IArgument.DATA_FILE);
        List attrValues = rc.getOption(IArgument.ATTRIBUTE_VALUES);

        if ((datafile == null) && (attrValues == null)) {
            throw new CLIException(getResourceString("missing-attributevalues"),
                ExitCodes.INCORRECT_OPTION, rc.getSubCommand().getName());
        }

        Map attributeValues = AttributeValues.parse(
            getCommandManager(), datafile, attrValues);
        
        ServiceSchema ss = getServiceSchema();
        IOutput outputWriter = getOutputWriter();
        String attributeName = null;

        try {
            Map mapOldValues = ss.getAttributeDefaults();
            for (Iterator i = attributeValues.keySet().iterator(); i.hasNext();
            ) {
                attributeName = (String)i.next();
                String[] params = {serviceName, schemaType, subSchemaName,
                    attributeName};
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "ATTEMPT_ADD_SCHEMA_ATTR_DEFAULTS", params);

                Set oldValues = (Set)mapOldValues.get(attributeName);
                Set newValues = 
                    ((oldValues == null) || oldValues.isEmpty()) ?
                        new HashSet() : new HashSet(oldValues);
                newValues.addAll((Set)attributeValues.get(attributeName));
                ss.setAttributeDefaults(attributeName, newValues);
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEED_ADD_SCHEMA_ATTR_DEFAULTS", params);
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("schema-add-attribute-defaults-succeed"),
                    (Object[])params));
            }
        } catch (SSOException e) {
            String[] args = {serviceName, schemaType, subSchemaName,
                attributeName, e.getMessage()};
            debugError("AddAttributeDefaults.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_ADD_SCHEMA_ATTR_DEFAULTS", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {serviceName, schemaType, subSchemaName,
                attributeName, e.getMessage()};
            debugError("AddAttributeDefaults.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_ADD_SCHEMA_ATTR_DEFAULTS", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
