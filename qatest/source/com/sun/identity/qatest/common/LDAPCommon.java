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
 * $Id: LDAPCommon.java,v 1.3 2007-09-18 00:35:06 bt199000 Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common;

import netscape.ldap.LDAPAttribute;
import netscape.ldap.LDAPAttributeSchema;
import netscape.ldap.LDAPAttributeSet;
import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPEntry;
import netscape.ldap.LDAPException;
import netscape.ldap.LDAPModification;
import netscape.ldap.LDAPModificationSet;
import netscape.ldap.LDAPSchema;
import netscape.ldap.util.LDIF;
import netscape.ldap.util.LDIFAddContent;
import netscape.ldap.util.LDIFAttributeContent;
import netscape.ldap.util.LDIFContent;
import netscape.ldap.util.LDIFModifyContent;
import netscape.ldap.util.LDIFRecord;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.logging.Level;

/**
 * This class has helper methods to perform the LDAP related operations
 */
public class LDAPCommon extends TestCommon {
    private String dstype;
    private String dshost;
    private String dsport;
    private String dsdirmgrdn;
    private String dsdirmgrpwd;
    private String dsrootsuffix;
    private String basedir;
    private static LDAPConnection ld = null;
    private String fileSeparator = System.getProperty("file.separator");
    
    /**
     * Creates a new instance of LDAPCommon
     */
    private LDAPCommon() {
        super("LDAPCommon");
    }
    
    /**
     * Create new instant for LDAPCommon
     * @param    dh  Directory Server hostname
     * @param    dp  Directory Server port number
     * @param    du  Directory Server admin dn
     * @param    dw  Directory Server password
     * @param    dr  Directory Server root suffix dn
     */
    public LDAPCommon(String dh, String dp, String du, String dw, String dr)
    throws Exception {
        super("LDAPCommon");
        try {
            dshost = dh;
            dsport = dp;
            dsdirmgrdn = du;
            dsdirmgrpwd = dw;
            dsrootsuffix = dr;
            basedir = getBaseDir();
            log(Level.FINEST, "LDAPCommon", "LDAP info : " + dshost +
                    ":" + dsport + ":" + dsdirmgrdn + ":" + dsdirmgrpwd);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * This method loads Access Manager user schema files
     * @param   schemaList  a list of AM user schema file name(s)
     * @param   schemaAttr  a list of schema attributes to be checked for the
     * existing of the schema in LDAP server.  If empty or not defined, schema
     * will be loaded without checking.
     */
    public void loadAMUserSchema(String schemaList, String schemaAttr)
    throws Exception {
        entering("loadAMUserSchema", null);
        try {
            log(Level.FINE, "loadAMUserSchema",
                    "Loading AM user schema for server " +
                    dshost + ":" + dsport + ":" + dsrootsuffix + "...");
            if (!isDServerUp()) {
                log(Level.SEVERE, "loadAMUserSchema",
                        "Server is down. Cannot proceed.");
                assert false;
            } else {
                List schemaFilesList = getAttributeList(schemaList, ";");
                List schemaAttrsList = getAttributeList(schemaAttr, ";");
                log(Level.FINEST, "loadAMUserSchema",
                    	"User schema list is " + schemaList);
                log(Level.FINE,
                        "loadAMUserSchema", "Start loading AM user schema...");
                String schemaFile;
                String schemaAttrItem;
                String fn;
                int index;
                Map ldMap = new HashMap();
                Iterator j = schemaAttrsList.iterator();
                for (Iterator i = schemaFilesList.iterator(); i.hasNext();) {
                    schemaFile = (String)i.next();
                    if (j.hasNext())
                        schemaAttrItem = ((String)j.next()).trim();  
                    else 
                        schemaAttrItem = "";
                    log(Level.FINEST,
                            "loadAMUserSchema", "Loading schema file " +
                            schemaFile + " with attribute " + schemaAttrItem + 
                            "...");
                    if (schemaAttrItem.length() == 0 ||
                            !isAMUserSchemaLoad(schemaAttrItem)) {
                        log(Level.FINEST,
                            "loadAMUserSchema", "Loading schema file " +
                            schemaFile + " with attribute " + schemaAttrItem + 
                            "...");
                        index = schemaFile.lastIndexOf("/");
                        if (index >= 0) {
                            fn = basedir + fileSeparator + "resources" + 
                                    fileSeparator + 
                                    schemaFile.substring(index + 1);
                            ldMap.put("ROOT_SUFFIX", dsrootsuffix);
                            ldMap.put("@ROOT_SUFFIX@", dsrootsuffix);
                            replaceStringInFile(schemaFile, fn, ldMap);
                        } else
                            fn = schemaFile;
                        createSchemaFromLDIF(fn, ld);
                        if (isAMUserSchemaLoad(schemaAttrItem) || 
                                schemaAttrItem.length() == 0)
                            log(Level.FINE, "loadAMUserSchema", 
                                    "AM user schema " +  schemaFile + 
                                    " was loaded successful.");
                        else
                            log(Level.SEVERE, "loadAMUserSchema", 
                                    "Failed to load AM user schema " + 
                                    schemaFile);
                    } else
                        log(Level.FINE, "loadAMUserSchema", 
                                "AM user schema file " +  schemaFile + 
                                " is not loaded because it was already loaded");
                }
                
            }
        } catch (Exception e) {
            log(Level.SEVERE, "loadAMUserSchema", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("loadAMUserSchema");
        
    }
    
    /**
     * This method checks if Access Manager user schema loaded
     * @oaran   schema attribute to be checked.
     * @return  true if schema exists or false if schema not found
     */
    public boolean isAMUserSchemaLoad(String attrToBeChecked)
    throws Exception {
        entering("isAMUserSchemaLoad", null);
        boolean isLoad = false;
        LDAPConnection ld = getLDAPConnection();
        LDAPSchema dirSchema = new LDAPSchema();
        try {
            dirSchema.fetchSchema(ld);
            LDAPAttributeSchema newAttrType =
                    dirSchema.getAttribute(attrToBeChecked);
            if (newAttrType != null) {
                log(Level.FINEST, "isAMUserSchemaLoad", 
                        "Attribute " + attrToBeChecked + " := " + 
                        newAttrType.toString());
                isLoad = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        exiting("isAMUserSchemaLoad");
        return isLoad;
    }
    
    /**
     * This method validates if directory server is running and can be
     * connected at the specified host and port.
     * @return  true if directory server is running.
     */
    public boolean isDServerUp()
    throws Exception {
        return (getLDAPConnection() == null) ? false : true;
    }
    
    /**
     * This method creates LDAP schema from LDIF file.
     * @param   file    file containing LDIF entries.
     * @param   ld      LDAP Connection.
     */
    public void createSchemaFromLDIF(String file, LDAPConnection ld)
    throws IOException, LDAPException {
        entering("createSchemaFromLDIF", null);
        createSchemaFromLDIF(new LDIF(file), ld);
        exiting("createSchemaFromLDIF");
    }
    
    /**
     * This method creates LDAP schema from LDIF file.
     * @param   ldif    LDIF object.
     * @param   ld      LDAP Connection.
     */
    public void createSchemaFromLDIF(LDIF ldif, LDAPConnection ld)
    throws IOException, LDAPException {
        entering("createSchemaFromLDIF", null);
        LDIFContent content = null;
        String DN = null;
        LDAPAttributeSet attrSet = null;
        LDAPAttribute[] attrs;
        LDAPEntry amEntry;
        for(LDIFRecord rec = ldif.nextRecord(); rec != null;
        rec = ldif.nextRecord()) {
            try {
                content = rec.getContent();
                DN = rec.getDN();
                if (content instanceof LDIFModifyContent) {
                    ld.modify(DN,
                            ((LDIFModifyContent)content).getModifications());
                } else if ((content instanceof LDIFAttributeContent) ||
                        (content instanceof LDIFAddContent)) {
                    attrs = (content instanceof LDIFAttributeContent) ?
                        ((LDIFAttributeContent)content).getAttributes() :
                        ((LDIFAddContent)content).getAttributes();
                    amEntry = new LDAPEntry(DN,new LDAPAttributeSet(attrs));
                    ld.add(amEntry);
                }
            } catch (LDAPException e) {
                log(Level.FINEST, "createSchemaFromLDIF", "LDAP error " + 
                        e.getLDAPResultCode() + " " + e.getLDAPErrorMessage());
                switch (e.getLDAPResultCode()) {
                    case LDAPException.ATTRIBUTE_OR_VALUE_EXISTS:
                        //throw e;
			break;
                    case LDAPException.NO_SUCH_ATTRIBUTE:
                        // Ignore some attributes need to be deleted if present
                        break;
                    case LDAPException.ENTRY_ALREADY_EXISTS:
                        LDAPModificationSet modSet = new LDAPModificationSet();
                        attrs = (content instanceof LDIFAttributeContent) ?
                            ((LDIFAttributeContent)
                            content).getAttributes() :
                            ((LDIFAddContent)content).getAttributes();
                        for (int i = 0; i < attrs.length; i++) {
                            modSet.add(LDAPModification.ADD, attrs[i]);
                        }
                        try {
                            ld.modify(DN, modSet);
                        } catch (LDAPException ex) {
                            //Ignore the exception
                            log(Level.FINEST, "createSchemaFromLDIF",
                                    ex.getMessage());
                        }
                        break;
                    default:
                }
            }
        }
        exiting("createSchemaFromLDIF");
    }
    
    /**
     * This method creates a LDAP connection.
     */
    private LDAPConnection getLDAPConnection()
    throws Exception {
        if (ld == null) {
            try {
                ld = new LDAPConnection();
                ld.setConnectTimeout(300);
                ld.connect(3, dshost,
                        Integer.parseInt(dsport), dsdirmgrdn, dsdirmgrpwd);
            } catch (LDAPException e) {
                disconnectDServer();
                ld = null;
                log(Level.SEVERE, "getLDAPConnection",
                        "LDAP error with return code " + e.getLDAPResultCode());
                e.printStackTrace();
                throw e;
            }
        }
        return ld;
    }
    
    /**
     * This method disconnects and terminate LDAP connection.
     */
    private void disconnectDServer()
    throws Exception {
        if ((ld != null) && ld.isConnected()) {
            try {
                ld.disconnect();
                ld = null;
            } catch (LDAPException e) {
                log(Level.SEVERE, "disconnectDServer",
                        "LDAP error with return code " + e.getLDAPResultCode());
                e.printStackTrace();
                throw e;
            }
        }
    }
}
