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
 * $Id: ImportMetaData.java,v 1.13 2009-01-07 21:51:29 veiming Exp $
 *
 */

package com.sun.identity.federation.cli;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.CommandManager;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.cot.COTException;
import com.sun.identity.federation.jaxb.entityconfig.IDPDescriptorConfigElement;
import com.sun.identity.federation.jaxb.entityconfig.SPDescriptorConfigElement;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.meta.SAML2MetaConstants;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaSecurityUtils;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.identity.wsfederation.meta.WSFederationMetaManager;
import com.sun.identity.wsfederation.meta.WSFederationMetaException;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Import Meta Data.
 */
public class ImportMetaData extends AuthenticatedCommand {
    static Debug debug = SAML2MetaUtils.debug;
    private String metadata;
    private String extendedData;
    private String cot;
    private String realm;
    private String spec;
    private boolean webAccess;

    /**
 * Imports Meta Data.
     *
     * @param rc Request Context.
     * @throws CLIException if unable to process this request.
     */
    public void handleRequest(RequestContext rc) 
        throws CLIException {
        super.handleRequest(rc);
        ldapLogin();
        realm = getStringOptionValue(FedCLIConstants.ARGUMENT_REALM, "/");
        metadata = getStringOptionValue(FedCLIConstants.ARGUMENT_METADATA);
        extendedData = getStringOptionValue(
            FedCLIConstants.ARGUMENT_EXTENDED_DATA);
        cot = getStringOptionValue(FedCLIConstants.ARGUMENT_COT);

        spec = FederationManager.getIDFFSubCommandSpecification(rc);
        String[] params = {realm, metadata, extendedData, cot, spec};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_IMPORT_ENTITY", params);

        if ((metadata == null) && (extendedData == null)) {
            String[] args = {realm, metadata, extendedData, cot,
                spec, getResourceString("import-entity-exception-no-datafile")};
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_IMPORT_ENTITY", args);
            throw new CLIException(
                getResourceString("import-entity-exception-no-datafile"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        
        validateCOT();
        
        CommandManager mgr = getCommandManager();
        String url = mgr.getWebEnabledURL();
        webAccess = (url != null) && (url.length() > 0);

        try {
            if (spec.equals(FederationManager.DEFAULT_SPECIFICATION)) {
                handleSAML2Request(rc);
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEEDED_IMPORT_ENTITY", params);
            } else if (spec.equals(FedCLIConstants.IDFF_SPECIFICATION)) {
                handleIDFFRequest(rc);
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEEDED_IMPORT_ENTITY", params);
            } else if (spec.equals(FedCLIConstants.WSFED_SPECIFICATION)) {
                handleWSFedRequest(rc);
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEEDED_IMPORT_ENTITY", params);
            } else {
                throw new CLIException(
                    getResourceString("unsupported-specification"),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        } catch (CLIException e) {
            String[] args = {realm, metadata, extendedData, cot,
                spec, e.getMessage()};
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_IMPORT_ENTITY", args);
            throw e;
        }
    }
    
        
    private void validateCOT() 
        throws CLIException {
        if ((cot != null) && (cot.length() > 0))  {
            try {
                CircleOfTrustManager cotManager = new CircleOfTrustManager();
                if (!cotManager.getAllCirclesOfTrust(realm).contains(cot)) {
                    String[] args = {realm, metadata, extendedData, cot,
                        spec,
                        getResourceString(
                        "import-entity-exception-cot-no-exist")
                    };
                    writeLog(LogWriter.LOG_ERROR, Level.INFO,
                        "FAILED_IMPORT_ENTITY", args);
                    throw new CLIException(
                        getResourceString(
                        "import-entity-exception-cot-no-exist"),
                        ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
                }
            } catch (COTException e) {
                throw new CLIException(e.getMessage(),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        }
    }

    private void handleSAML2Request(RequestContext rc)
        throws CLIException {
        try {
            SAML2MetaManager metaManager = new SAML2MetaManager();
            String entityID = null;
            EntityConfigElement configElt = null;
            
            if (extendedData != null) {
                configElt = geEntityConfigElement();
                /*
                 * see note at the end of this class for how we decide
                 * the realm value
                 */
                if (configElt != null && configElt.isHosted()) {
                    List config = configElt.
                       getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
                    if (!config.isEmpty()) {
                        BaseConfigType bConfig = (BaseConfigType)
                            config.iterator().next();
                        realm = SAML2MetaUtils.getRealmByMetaAlias(
                            bConfig.getMetaAlias());
                    }
                }
            }
            
            EntityDescriptorElement descriptor = null;
            if (metadata != null) {
                descriptor = getSAML2EntityDescriptorElement(metaManager);
                if (descriptor != null) {
                    entityID = descriptor.getEntityID();
                }
            }
            
            metaManager.createEntity(realm, descriptor, configElt);
            if (descriptor != null) {
                debug.message(
                    "ImportMetaData.handleSAML2Request:descriptor is not null");
                String out = (webAccess) ? "web" : metadata;
                Object[] objs = { out };
                getOutputWriter().printlnMessage(MessageFormat.format(
                    getResourceString("import-entity-succeeded"), objs));
            }
            if (configElt != null) {
                if (debug.messageEnabled()) {
                    debug.message("ImportMetaData.handleSAML2Request: "
                        + "entity config is not null");
                }
                String out = (webAccess) ? "web" : extendedData;
                Object[] objs = { out };
                getOutputWriter().printlnMessage(MessageFormat.format(
                    getResourceString("import-entity-succeeded"), objs));
            }

            if ((cot != null) && (cot.length() > 0) &&
                (entityID != null) && (entityID.length() > 0)) {
                CircleOfTrustManager cotManager = new CircleOfTrustManager();
                if (!cotManager.isInCircleOfTrust(realm, cot, spec, entityID)) {
                    cotManager.addCircleOfTrustMember(
                        realm, cot, spec, entityID);
                }
            }
        } catch (COTException e) {
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SAML2MetaException e) {
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
    
    private void handleIDFFRequest(RequestContext rc)
        throws CLIException {
        try {
            IDFFMetaManager metaManager = new IDFFMetaManager(
                getAdminSSOToken());
            String entityID = null;
            com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement
                configElt = null;
            
            if (extendedData != null) {
                configElt = getIDFFEntityConfigElement();
                
                /*
                 * see note at the end of this class for how we decide
                 * the realm value
                 */
                if ((configElt != null) && configElt.isHosted()) {
                    IDPDescriptorConfigElement idpConfig = 
                        IDFFMetaUtils.getIDPDescriptorConfig(configElt);
                    if (idpConfig != null) {
                        realm = SAML2MetaUtils.getRealmByMetaAlias(
                            idpConfig.getMetaAlias());
                    } else {
                        SPDescriptorConfigElement spConfig =
                            IDFFMetaUtils.getSPDescriptorConfig(configElt);
                        if (spConfig != null) {
                            realm = SAML2MetaUtils.getRealmByMetaAlias(
                                spConfig.getMetaAlias());
                        }
                    }
                }
            }
            
            if (metadata != null) {
                entityID = importIDFFMetaData(realm, metaManager);
            }
            if (configElt != null) {
                String out = (webAccess) ? "web" : extendedData;
                Object[] objs = { out };
                metaManager.createEntityConfig(realm, configElt);
                getOutputWriter().printlnMessage(MessageFormat.format(
                    getResourceString("import-entity-succeeded"), objs));
            }

            if ((cot != null) && (cot.length() > 0) &&
                (entityID != null) && (entityID.length() > 0)) {
                CircleOfTrustManager cotManager = new CircleOfTrustManager();
                if (!cotManager.isInCircleOfTrust(realm, cot, spec, entityID)) {
                    cotManager.addCircleOfTrustMember(realm, cot, spec,
                        entityID);
                }
            }
        } catch (IDFFMetaException e) {
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (COTException e) {
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private void handleWSFedRequest(RequestContext rc)
        throws CLIException {
        try {
            String federationID = null;
            com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement 
                configElt = null;
            
            if (extendedData != null) {
                configElt = getWSFedEntityConfigElement();
                /*
                 * see note at the end of this class for how we decide
                 * the realm value
                 */
                if (configElt != null && configElt.isHosted()) {
                    List config = configElt.
                       getIDPSSOConfigOrSPSSOConfig();
                    if (!config.isEmpty()) {
                        com.sun.identity.wsfederation.jaxb.entityconfig.BaseConfigType 
                            bConfig = 
                            (com.sun.identity.wsfederation.jaxb.entityconfig.BaseConfigType)
                            config.iterator().next();
                        realm = WSFederationMetaUtils.getRealmByMetaAlias(
                            bConfig.getMetaAlias());
                    }
                }
            }
            
            if (metadata != null) {
                federationID = importWSFedMetaData();
            }
            
            if (configElt != null) {
                WSFederationMetaManager.createEntityConfig(realm, configElt);
                
                String out = (webAccess) ? "web" : extendedData;
                Object[] objs = { out };
                getOutputWriter().printlnMessage(MessageFormat.format(
                    getResourceString("import-entity-succeeded"), objs));
            }

            if ((cot != null) && (cot.length() > 0) &&
                (federationID != null)) {
                CircleOfTrustManager cotManager = new CircleOfTrustManager();
                if (!cotManager.isInCircleOfTrust(realm, cot, spec, 
                    federationID)
                ) {
                    cotManager.addCircleOfTrustMember(realm, cot, spec, 
                        federationID);
                }
            }
        } catch (COTException e) {
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (WSFederationMetaException e) {
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
    
    private EntityDescriptorElement getSAML2EntityDescriptorElement(
        SAML2MetaManager metaManager)
        throws SAML2MetaException, CLIException
    {
        InputStream is = null;
        String out = (webAccess) ? "web" : metadata;
        Object[] objs = { out };
        String entityID = null;
        
        try {
            Object obj;
            Document doc;
            if (webAccess) {
                doc = XMLUtils.toDOMDocument(metadata, debug);
            } else {
                is = new FileInputStream(metadata);
                doc = XMLUtils.toDOMDocument(is, debug);
            }

            if (doc == null) {
                throw new CLIException(MessageFormat.format(
                    getResourceString(
                    "import-entity-exception-invalid-descriptor-file"),
                    objs), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
            Element docElem = doc.getDocumentElement();
            if ((!SAML2MetaConstants.ENTITY_DESCRIPTOR.equals(
                docElem.getLocalName())) ||
                (!SAML2MetaConstants.NS_METADATA.equals(
                docElem.getNamespaceURI()))) {
                throw new CLIException(MessageFormat.format(
                    getResourceString(
                    "import-entity-exception-invalid-descriptor-file"),
                    objs), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
            SAML2MetaSecurityUtils.verifySignature(doc);
            workaroundAbstractRoleDescriptor(doc);
            if (debug.messageEnabled()) {
                debug.message("ImportMetaData.getSAML2EntityDescriptorElement: "
                    + "modified metadata = " + XMLUtils.print(doc));
            }
            obj = SAML2MetaUtils.convertNodeToJAXB(doc);

            if (obj instanceof EntityDescriptorElement) {
                return (EntityDescriptorElement)obj;
            }
            return null;
        } catch (FileNotFoundException e) {
            throw new CLIException(MessageFormat.format(
                getResourceString("file-not-found"), objs),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (JAXBException e) {
            debug.message("ImportMetaData.importMetaData", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                    "import-entity-exception-invalid-descriptor-file"),
                objs), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IllegalArgumentException e) {
            debug.message("ImportMetaData.importMetaData", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                    "import-entity-exception-invalid-descriptor-file"),
                objs), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if (is !=null ) {
                try {
                    is.close();
                } catch (IOException e) {
                    //do not if the file cannot be closed.
                }
            }
        }
    }

    private String importIDFFMetaData(String realm, IDFFMetaManager metaManager)
        throws IDFFMetaException, CLIException
    {
        InputStream is = null;
        String out = (webAccess) ? "web" : metadata;
        Object[] objs = { out };
        String entityID = null;
        
        try {
            Object obj;
            if (webAccess) {
                obj = IDFFMetaUtils.convertStringToJAXB(metadata);
            } else {
                is = new FileInputStream(metadata);
                Document doc = XMLUtils.toDOMDocument(is, debug);
                obj = IDFFMetaUtils.convertNodeToJAXB(doc);
            }

            if (obj instanceof
                com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement) {
                com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement
                    descriptor =
                 (com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement)
                    obj;
                entityID = descriptor.getProviderID();
                //TODO: signature
                //SAML2MetaSecurityUtils.verifySignature(doc);
                //
                metaManager.createEntityDescriptor(realm, descriptor);
                getOutputWriter().printlnMessage(MessageFormat.format(
                    getResourceString("import-entity-succeeded"), objs));
            }
            return entityID;
        } catch (FileNotFoundException e) {
            throw new CLIException(MessageFormat.format(
                getResourceString("file-not-found"), objs),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (JAXBException e) {
            debug.message("ImportMetaData.importMetaData", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                    "import-entity-exception-invalid-descriptor-file"),
                objs), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IllegalArgumentException e) {
            debug.message("ImportMetaData.importMetaData", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                    "import-entity-exception-invalid-descriptor-file"),
                objs), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if (is !=null ) {
                try {
                    is.close();
                } catch (IOException e) {
                    //do not if the file cannot be closed.
                }
            }
        }
    }
   
    private String importWSFedMetaData()
        throws WSFederationMetaException, CLIException
    {
        InputStream is = null;
        String out = (webAccess) ? "web" : metadata;
        Object[] objs = { out };
        String federationID = null;
        
        try {
            Object obj;
            Document doc;
            if (webAccess) {
                obj = WSFederationMetaUtils.convertStringToJAXB(metadata);
                doc = XMLUtils.toDOMDocument(metadata, debug);
            } else {
                is = new FileInputStream(metadata);
                doc = XMLUtils.toDOMDocument(is, debug);
                obj = WSFederationMetaUtils.convertNodeToJAXB(doc);
            }

            if (obj instanceof com.sun.identity.wsfederation.jaxb.wsfederation.FederationMetadataElement) {
                // Just get the first element for now...
                // TODO - loop through Federation elements?
                obj = ((com.sun.identity.wsfederation.jaxb.wsfederation.FederationMetadataElement)obj).getAny().get(0);
            }

            if (obj instanceof com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement) {
                com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement 
                federation =
                (com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement)obj;
                federationID = federation.getFederationID();
                if ( federationID == null )
                {
                    federationID = WSFederationConstants.DEFAULT_FEDERATION_ID;
                }
                // WSFederationMetaSecurityUtils.verifySignature(doc);
                WSFederationMetaManager.createFederation(realm, federation);
                getOutputWriter().printlnMessage(MessageFormat.format(
                    getResourceString("import-entity-succeeded"), objs));
            }
            return federationID;
        } catch (FileNotFoundException e) {
            throw new CLIException(MessageFormat.format(
                getResourceString("file-not-found"), objs),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (JAXBException e) {
            debug.message("ImportMetaData.importMetaData", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                    "import-entity-exception-invalid-descriptor-file"),
                objs), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IllegalArgumentException e) {
            debug.message("ImportMetaData.importMetaData", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                    "import-entity-exception-invalid-descriptor-file"),
                objs), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if (is !=null ) {
                try {
                    is.close();
                } catch (IOException e) {
                    //do not if the file cannot be closed.
                }
            }
        }
    }
    
    private EntityConfigElement geEntityConfigElement()
        throws SAML2MetaException, CLIException {
        String out = (webAccess) ? "web" : extendedData;
        Object[] objs = { out };
        InputStream is = null;

        try {
            Object obj = null;
            if (webAccess) {
                obj = SAML2MetaUtils.convertStringToJAXB(extendedData);
            } else {
                is = new FileInputStream(extendedData);
                obj = SAML2MetaUtils.convertInputStreamToJAXB(is);
            }

            return (obj instanceof EntityConfigElement) ?
                (EntityConfigElement)obj : null;
        } catch (FileNotFoundException e) {
            throw new CLIException(MessageFormat.format(
                getResourceString("file-not-found"), objs),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (JAXBException e) {
            debug.message("ImportMetaData.importExtendedData", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                    "import-entity-exception-invalid-config-file"), objs),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IllegalArgumentException e) {
            debug.message("ImportMetaData.importExtendedData", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                    "import-entity-exception-invalid-config-file"), objs),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if (is !=null ) {
                try {
                    is.close();
                } catch (IOException e) {
                    //do not if the file cannot be closed.
                }
            }
        }
    }

    private com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement
        getIDFFEntityConfigElement() throws IDFFMetaException, CLIException {
        String out = (webAccess) ? "web" : extendedData;
        Object[] objs = { out };

        try {
            Object obj;

            if (webAccess) {
                obj = IDFFMetaUtils.convertStringToJAXB(extendedData);
            } else {
                obj = IDFFMetaUtils.convertStringToJAXB(
                    getFileContent(extendedData));
            }

            return (obj instanceof 
            com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement) ?
             (com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement)
                obj : null;
        } catch (IOException e) {
            throw new CLIException(MessageFormat.format(
                getResourceString("file-not-found"), objs),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (JAXBException e) {
            debug.message("ImportMetaData.importExtendedData", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                    "import-entity-exception-invalid-config-file"), objs),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IllegalArgumentException e) {
            debug.message("ImportMetaData.importExtendedData", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                    "import-entity-exception-invalid-config-file"), objs),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
    
    private com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement 
        getWSFedEntityConfigElement()
        throws WSFederationMetaException, CLIException {
        String out = (webAccess) ? "web" : extendedData;
        Object[] objs = { out };
        InputStream is = null;

        try {
            Object obj = null;
            if (webAccess) {
                obj = WSFederationMetaUtils.convertStringToJAXB(extendedData);
            } else {
                is = new FileInputStream(extendedData);
                obj = WSFederationMetaUtils.convertInputStreamToJAXB(is);
            }

            return (obj instanceof 
                com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement) ?
                (com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement)obj : 
                null;
        } catch (FileNotFoundException e) {
            throw new CLIException(MessageFormat.format(
                getResourceString("file-not-found"), objs),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (JAXBException e) {
            debug.message("ImportMetaData.importExtendedData", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                    "import-entity-exception-invalid-config-file"), objs),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IllegalArgumentException e) {
            debug.message("ImportMetaData.importExtendedData", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                    "import-entity-exception-invalid-config-file"), objs),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if (is !=null ) {
                try {
                    is.close();
                } catch (IOException e) {
                    //do not if the file cannot be closed.
                }
            }
        }
    }

    private static String getFileContent(String fileName)
        throws IOException {
        BufferedReader br = null;
        StringBuffer buff = new StringBuffer();
        try {
            br = new BufferedReader(new FileReader(fileName));
            String line = br.readLine();
            while (line != null) {
                buff.append(line).append("\n");
                line = br.readLine();
            }
        } finally {
            if (br != null) {
                br.close();
            }
        }
        return buff.toString();
    }

    public static void workaroundAbstractRoleDescriptor(
        Document doc) {

        NodeList nl = doc.getDocumentElement().getElementsByTagNameNS(
            SAML2MetaConstants.NS_METADATA,SAML2MetaConstants.ROLE_DESCRIPTOR);
        int length = nl.getLength();
        if (length == 0) {
            return;
        }

        for(int i = 0; i < length; i++) {
            Element child = (Element)nl.item(i);
            String type = child.getAttributeNS(SAML2Constants.NS_XSI, "type");
            if (type != null) {
                if ((type.equals(
                    SAML2MetaConstants.ATTRIBUTE_QUERY_DESCRIPTOR_TYPE)) ||
                    (type.endsWith(":" +
                    SAML2MetaConstants.ATTRIBUTE_QUERY_DESCRIPTOR_TYPE))) {

                    String newTag = type.substring(0, type.length() - 4);

                    String xmlstr = XMLUtils.print(child);
                    int index = xmlstr.indexOf(
                        SAML2MetaConstants.ROLE_DESCRIPTOR);
                    xmlstr = "<" + newTag + xmlstr.substring(index +
                        SAML2MetaConstants.ROLE_DESCRIPTOR.length());
                    if (!xmlstr.endsWith("/>")) {
                        index = xmlstr.lastIndexOf("</");
                        xmlstr = xmlstr.substring(0, index) + "</" + newTag +
                            ">";
                    }

                    Document tmpDoc = XMLUtils.toDOMDocument(xmlstr, debug);
                    Node newChild =
                        doc.importNode(tmpDoc.getDocumentElement(), true);
                    child.getParentNode().replaceChild(newChild, child);
                }
            }
        }                
    }
}

/* Deciding realm value
if (extended metadata xml exists) {
    if (hosted) {
        get the realm value from meta alias either from IDP or SP
        config element.
    } else {
        use the value provide by --realm/-e option
    }
} else {
    use the value provide by --realm/-e option
}
 */
