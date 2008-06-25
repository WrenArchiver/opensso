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
 * $Id: RegisterServices.java,v 1.15 2008-06-25 05:44:03 qcheng Exp $
 *
 */

package com.sun.identity.setup;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceManager;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

/**
 * Registers service during setup time.
 */
public class RegisterServices {
    
    private static final List serviceNames = new ArrayList();
    private static final String umEmbeddedDS;
        
    static {
        ResourceBundle rb = ResourceBundle.getBundle(
            SetupConstants.PROPERTY_FILENAME);
        String names = rb.getString(SetupConstants.SERVICE_NAMES);
        StringTokenizer st = new StringTokenizer(names);
        while (st.hasMoreTokens()) {
            serviceNames.add(st.nextToken());
        }
        umEmbeddedDS = rb.getString("umEmbeddedDS");
    }

    /**
     * Registers services.
     *
     * @param adminToken Administrator Single Sign On token.
     * @Qparam bUseExtUMDS <code>true</code> to use external DS as
     *         user management datastore.
     * @throws IOException if file operation errors occur.
     * @throws SMSException if services cannot be registered.
     * @throws SSOException if single sign on token is not valid.
     */
    public void registers(SSOToken adminToken, boolean bUseExtUMDS)
        throws IOException, SMSException, SSOException {
        System.setProperty(Constants.SYS_PROPERTY_INSTALL_TIME, "true");
        ServiceManager serviceManager = new ServiceManager(adminToken);
        Map map = ServicesDefaultValues.getDefaultValues();
        String basedir = (String)map.get(SetupConstants.CONFIG_VAR_BASE_DIR);
        String dirXML = basedir + "/config/xml";
        (new File(dirXML)).mkdirs();

        for (Iterator i = serviceNames.iterator(); i.hasNext(); ) {
            String serviceFileName = (String) i.next();
            boolean tagswap = true;
            if (serviceFileName.startsWith("*")) {
                serviceFileName = serviceFileName.substring(1);
                tagswap = false;
            }

            Object[] params = {serviceFileName};
            SetupProgress.reportStart("emb.registerservice", params);
            String strXML = getResourceContent(serviceFileName);
            if (tagswap) {
                strXML = ServicesDefaultValues.tagSwap(strXML, true);
            }
            AMSetupServlet.writeToFile(dirXML + "/" + serviceFileName, strXML);
            registerService(strXML, adminToken);
            SetupProgress.reportEnd("emb.success", null);
        }
        
        if (!bUseExtUMDS) {
            addSubConfigForEmbeddedDS(adminToken);
        }
 
        // Set installTime to false, to avoid in-memory notification from
        // SMS in cases where not needed, and to denote that service  
        // registration got completed during configuration phase and it 
        // has passed installtime.
        System.setProperty(Constants.SYS_PROPERTY_INSTALL_TIME, "false");
    }

    private void addSubConfigForEmbeddedDS(SSOToken adminSSOToken)
        throws SSOException, SMSException, IOException {
        Map data = ServicesDefaultValues.getDefaultValues();
        String xml = getResourceContent(umEmbeddedDS);

        xml = xml.replaceAll("@UM_CONFIG_ROOT_SUFFIX@",
            (String) data.get(SetupConstants.SM_CONFIG_ROOT_SUFFIX));
        xml = xml.replaceAll("@" + SetupConstants.UM_DIRECTORY_SERVER + "@",
            (String) data.get(
            SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_HOST));
        xml = xml.replaceAll("@" + SetupConstants.UM_DIRECTORY_PORT + "@",
            (String) data.get(
            SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_PORT));
        xml = xml.replaceAll("@UM_DS_DIRMGRDN@",
            (String) data.get(SetupConstants.CONFIG_VAR_DS_MGR_DN));
        xml = xml.replaceAll("@UM_DS_DIRMGRPASSWD@",
            (String) data.get(SetupConstants.CONFIG_VAR_DS_MGR_PWD));

        registerService(xml, adminSSOToken);
    }
    
    private void registerService(String xml, SSOToken adminSSOToken) 
        throws SSOException, SMSException, IOException {
        ServiceManager serviceManager = new ServiceManager(adminSSOToken);
        InputStream serviceStream = null;
        try {
            serviceStream = (InputStream) new ByteArrayInputStream(
                xml.getBytes());
            serviceManager.registerServices(serviceStream);
        } finally {
            if (serviceStream != null) {
                serviceStream.close();
            }
            serviceManager.clearCache();
        }
    }
    
    private String getResourceContent(String resName) 
        throws IOException {
        BufferedReader rawReader = null;
        
        String content = null;

        try {
            rawReader = new BufferedReader(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream(resName)));
            StringBuffer buff = new StringBuffer();
            String line = null;

            while ((line = rawReader.readLine()) != null) {
                buff.append(line);
            }

            rawReader.close();
            rawReader = null;
            content = buff.toString();
        } finally {
            if (rawReader != null) {
                rawReader.close();
            }
        }
        return content;
    }
    
    private String manipulateServiceXML(String serviceFileName, String strXML){
        if (serviceFileName.equals("idRepoService.xml")) {
            strXML = strXML.replaceAll(IDREPO_SUB_CONFIG_MARKER,
                IDREPO_SUB_CONFIG);
        }

        return strXML;
    }

    private static final String IDREPO_SUB_CONFIG_MARKER = 
        "<SubConfiguration name=\"@IDREPO_DATABASE@\" id=\"@IDREPO_DATABASE@\" />";

    private static final String IDREPO_SUB_CONFIG = 
        "<SubConfiguration name=\"files\" id=\"files\"><AttributeValuePair><Attribute name=\"sunIdRepoClass\" /><Value>com.sun.identity.idm.plugins.files.FilesRepo</Value></AttributeValuePair><AttributeValuePair><Attribute name=\"sunFilesIdRepoDirectory\" /><Value>@BASE_DIR@/@SERVER_URI@/idRepo</Value></AttributeValuePair></SubConfiguration>";
}
