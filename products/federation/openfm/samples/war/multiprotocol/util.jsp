<%--
   The contents of this file are subject to the terms
   of the Common Development and Distribution License
   (the License). You may not use this file except in
   compliance with the License.

   You can obtain a copy of the License at
   https://opensso.dev.java.net/public/CDDLv1.0.html or
   opensso/legal/CDDLv1.0.txt
   See the License for the specific language governing
   permission and limitations under the License.

   When distributing Covered Code, include this CDDL
   Header Notice in each file and include the License file
   at opensso/legal/CDDLv1.0.txt.
   If applicable, add the following below the CDDL Header,
   with the fields enclosed by brackets [] replaced by
   your own identifying information:
   "Portions Copyrighted [year] [name of copyright owner]"

   $Id: util.jsp,v 1.6 2008-05-28 18:47:20 qcheng Exp $ 

   Copyright 2007 Sun Microsystems Inc. All Rights Reserved
--%>


<%@ page language="java"
import="java.io.IOException,
        java.net.URLEncoder,
        java.text.MessageFormat,
        com.iplanet.sso.SSOException,
        com.iplanet.sso.SSOToken,
        com.iplanet.sso.SSOTokenManager,
        com.sun.identity.cli.StringOutputWriter,
        com.sun.identity.cli.CLIConstants,
        com.sun.identity.cli.CLIRequest,
        com.sun.identity.cli.CommandManager,
        com.sun.identity.federation.meta.IDFFMetaManager,
        com.sun.identity.federation.meta.IDFFMetaUtils,
        com.sun.identity.multiprotocol.SingleLogoutManager,
        com.sun.identity.plugin.session.SessionException,
        com.sun.identity.plugin.session.SessionProvider,
        com.sun.identity.plugin.session.SessionManager,
        com.sun.identity.saml2.meta.SAML2MetaManager,
        com.sun.identity.shared.Constants,
        com.sun.identity.shared.configuration.SystemPropertiesManager,
        com.sun.identity.cot.CircleOfTrustDescriptor,
        com.sun.identity.cot.CircleOfTrustManager,
        com.sun.identity.cot.COTConstants,
        com.sun.identity.cot.COTException,
        com.sun.identity.wsfederation.meta.WSFederationMetaManager,
        com.sun.identity.wsfederation.meta.WSFederationMetaUtils,
        java.net.URL,
        java.util.HashSet,
        java.util.List,
        java.util.Set,
        java.util.HashMap,
        java.util.Map"
%>
<!-- this code is only intended to work under single user mode -->
<%!
    String SAMPLE_COT_NAME = "samplemultiprotocolcot";
    String SAMPLE_PREFIX = "multiprotocolsample-";
    String IDP_SUFFIX = "-idp";
    String SP_SUFFIX = "-sp";
    String STATUS = "status";
    String SP_BASE_URL = "spBaseUrl";
    String PROTOCOL_PARAM_NAME = "spFederationProtocol";
    
    boolean loggedIn = false;
    String redirectUrl = null;
    String localAuthUrl = null;

    String localProto;
    String localHost;
    String localPort;
    String localDeploymentURI;
    String defaultRealm = "/";

    String baseHost = null;
    String baseURL = null;
    String realBaseURL = null;
    String baseURI = null;
    SSOToken ssoToken = null;

    public void createCircleOfTrust(String cotName,String hostedEntityID,
        String remoteEntityID, String protocol) throws COTException {
        // [START] Create Circle of Trust
        CircleOfTrustManager cotManager = new CircleOfTrustManager();
        Set cots = cotManager.getAllCirclesOfTrust(defaultRealm);
        boolean cotExists =  ((cots != null && !cots.isEmpty()) 
                                            && cots.contains(cotName));
        if (cotExists) {
            Set memberList = cotManager.listCircleOfTrustMember(
                defaultRealm, cotName, protocol);
            if ((memberList == null) || memberList.isEmpty() ||
                !memberList.contains(hostedEntityID)) {
                cotManager.addCircleOfTrustMember(defaultRealm, cotName,
                    protocol, hostedEntityID);
            }
            if ((memberList == null) || memberList.isEmpty() || 
                !memberList.contains(remoteEntityID)) {
                cotManager.addCircleOfTrustMember(defaultRealm, cotName,
                    protocol, remoteEntityID);
            }
        } else {
            Set<String> providers = new HashSet<String>();
            providers.add(hostedEntityID + COTConstants.DELIMITER + protocol);
            providers.add(remoteEntityID + COTConstants.DELIMITER + protocol);
            cotManager = new CircleOfTrustManager();
            cotManager.createCircleOfTrust(defaultRealm, 
                new CircleOfTrustDescriptor(
                cotName, defaultRealm, COTConstants.ACTIVE, "", null, null,
                null, null, providers));
            // [END] Create Circle of Trust
        }
    }

    /**
     * Checks if SAML2/IDFF/WSFed service provider have already been
     * configured in this instance. Throw exception if yes.
     */
    public void checkCurrentSPConfiguration(String hostedSPEntitySuffix, 
        SSOToken ssoToken) 
        throws Exception {
        // check SAML2
        String entityID = hostedSPEntitySuffix + SingleLogoutManager.SAML2
            + SP_SUFFIX;
        SAML2MetaManager saml2Manager = new SAML2MetaManager();
        List spEntityList = 
            saml2Manager.getAllHostedServiceProviderEntities(defaultRealm);
        boolean spExists =  
            ((spEntityList != null && !spEntityList.isEmpty()) 
              && spEntityList.contains(entityID)) ;
        if (spExists) {
            throw new Exception("A SAML2 Service Provider " + entityID + " had already been configured in this instance.");
        }
        
        // check ID-FF
        entityID = hostedSPEntitySuffix + SingleLogoutManager.IDFF
            + SP_SUFFIX;
        IDFFMetaManager idffManager = new IDFFMetaManager(ssoToken);
        spEntityList = 
            idffManager.getAllHostedServiceProviderEntities(defaultRealm);
        spExists = ((spEntityList != null && !spEntityList.isEmpty()) 
              && spEntityList.contains(entityID)) ;
        if (spExists) {
            throw new Exception("An ID-FF Service Provider " + entityID + " had already been configured in this instance.");
        }
        
        // handle WS-Fed
        entityID = hostedSPEntitySuffix + SingleLogoutManager.WS_FED
            + SP_SUFFIX;
        spEntityList = 
            WSFederationMetaManager.getAllHostedServiceProviderEntities(defaultRealm);
        spExists = ((spEntityList != null && !spEntityList.isEmpty()) 
              && spEntityList.contains(entityID)) ;
        if (spExists) {
            throw new Exception("A WS-Federation Service Provider " + entityID + " had already been configured in this instance.");
        }
    }
    
    public void configureSAML2ServiceProvider(String remoteIDPEntityID,
        String hostedSPEntityID, HttpServletRequest request) throws Exception {

        // [START] create an instance of CommandManager
        StringOutputWriter outputWriter = new StringOutputWriter();
        Map env = new HashMap();
        env.put(CLIConstants.SYS_PROPERTY_OUTPUT_WRITER, outputWriter);
        env.put(CLIConstants.ARGUMENT_LOCALE, request.getLocale());
        env.put(CLIConstants.SYS_PROPERTY_DEFINITION_FILES,
            "com.sun.identity.federation.cli.FederationManager");
        env.put(CLIConstants.SYS_PROPERTY_COMMAND_NAME, "famadm");
        CommandManager cmdManager = new CommandManager(env);
        // [END] create an instance of CommandManager

        URL url = new URL(remoteIDPEntityID);
        String proto = url.getProtocol();
        String host = url.getHost();
        String port = "" + url.getPort();
        String deploymenturi = getDeploymentUri(url);

        // [START] Make a call to CLI to get the meta data template
        SAML2MetaManager metaManager = new SAML2MetaManager();
        List spEntityList = 
            metaManager.getAllHostedServiceProviderEntities(defaultRealm);
        boolean spExists =  
            ((spEntityList != null && !spEntityList.isEmpty()) 
              && spEntityList.contains(hostedSPEntityID)) ;
              
        CLIRequest req = null;
        String result = null;
        int metaStartIdx=0;
        int extendedStartIdx=0;
        int extendedEndIdx = 0;
        int metaEndIdx=0;
        String endEntityDescriptorTag=null;
        String metaXML =  null;
        if (!spExists) {
            String[] args = {"create-metadata-templ", 
                "--entityid", hostedSPEntityID,
                "--serviceprovider", "/multiprotosaml2sp"};
            req = new CLIRequest(null, args, ssoToken);
            cmdManager.addToRequestQueue(req);
            cmdManager.serviceRequestQueue();
            result = outputWriter.getMessages();
            // [END] Make a call to CLI to get the meta data template

            // [START] Parse the output of CLI to get metadata XML
            endEntityDescriptorTag = "</EntityDescriptor>";
            metaStartIdx = result.indexOf("<EntityDescriptor");
            metaEndIdx = result.indexOf(endEntityDescriptorTag,
                metaStartIdx);
            metaXML = result.substring(metaStartIdx, metaEndIdx +
                endEntityDescriptorTag.length() +1);
            // handle LB case
            if (!realBaseURL.equals(baseURL)) {
                metaXML = metaXML.replaceAll(realBaseURL, baseURL);
            }
            // [END] Parse the output of CLI to get metadata XML

        
            // [START] Parse the output of CLI to get extended data XML
            String endEntityConfigTag = "</EntityConfig>";
            int extendStartIdx = result.indexOf("<EntityConfig ");
            int extendEndIdx = result.indexOf(endEntityConfigTag, 
                extendStartIdx);
            String extendedXML = result.substring(extendStartIdx,
                extendEndIdx + endEntityConfigTag.length() + 1);
            // handle LB case
            if (!realBaseURL.equals(baseURL)) {
                extendedXML = 
                    extendedXML.replaceAll(realBaseURL, baseURL);
            }
            // [END] Parse the output of CLI to get extended data XML
       
            // [START] Import these XMLs
            com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement 
                descriptor = (com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement)
                SAML2MetaUtils.convertStringToJAXB(metaXML);
            hostedSPEntityID = descriptor.getEntityID();
            metaManager.createEntityDescriptor(defaultRealm, descriptor);

            EntityConfigElement extendConfigElm = (EntityConfigElement)
                SAML2MetaUtils.convertStringToJAXB(extendedXML);
            metaManager.createEntityConfig(defaultRealm, extendConfigElm);
            // [END] Import these XMLs
        }

        List idpEntityList = 
            metaManager.getAllRemoteIdentityProviderEntities(defaultRealm);
        boolean idpExists =  ((idpEntityList != null 
            && !idpEntityList.isEmpty()) 
            && idpEntityList.contains(remoteIDPEntityID));
        if (!idpExists) {
            // [START] Make a call to CLI to get IDP meta data template
            String[] args2 = {"create-metadata-templ", 
                "--entityid", remoteIDPEntityID,
                "--identityprovider", "/multiprotosaml2idp"};
            outputWriter = new StringOutputWriter();
            env.put(CLIConstants.SYS_PROPERTY_OUTPUT_WRITER, outputWriter);
            cmdManager = new CommandManager(env);
            req = new CLIRequest(null, args2, ssoToken);
            cmdManager.addToRequestQueue(req);
            cmdManager.serviceRequestQueue();
            result = outputWriter.getMessages();
            // [END] Make a call to CLI to get the meta data template


            // [START] Parse the output of CLI to get metadata XML
            metaStartIdx = result.indexOf("<EntityDescriptor");
            metaEndIdx = result.indexOf(endEntityDescriptorTag, metaStartIdx);
            metaXML = result.substring(metaStartIdx, 
                metaEndIdx + endEntityDescriptorTag.length() +1);
            // [END] Parse the output of CLI to get metadata XML

            // [START] Swap protocol, host, port and deployment URI
            //         to form IDP metadata XML and import it
            String idpMetaXML = metaXML.replaceAll(realBaseURL,
                proto + "://"+ host + ":" + port + deploymenturi);
            EntityDescriptorElement idpDescriptor = (EntityDescriptorElement)
                SAML2MetaUtils.convertStringToJAXB(idpMetaXML);
            remoteIDPEntityID = idpDescriptor.getEntityID();
            metaManager.createEntityDescriptor(defaultRealm, idpDescriptor);
            // [END] Swap protocol, host, port and deployment URI
            //       to form IDP metadata XML and import it
        } 

        // [START] Create Circle of Trust
        createCircleOfTrust(SAMPLE_COT_NAME,hostedSPEntityID, 
             remoteIDPEntityID, SingleLogoutManager.SAML2);
    } 

    public void configureIDFFServiceProvider(String remoteIDPEntityID,
        String hostedSPEntityID, HttpServletRequest request) throws Exception {

        // [START] create an instance of CommandManager
        StringOutputWriter outputWriter = new StringOutputWriter();
        Map env = new HashMap();
        env.put(CLIConstants.SYS_PROPERTY_OUTPUT_WRITER, outputWriter);
        env.put(CLIConstants.ARGUMENT_LOCALE, request.getLocale());
        env.put(CLIConstants.SYS_PROPERTY_DEFINITION_FILES,
            "com.sun.identity.federation.cli.FederationManager");
        env.put(CLIConstants.SYS_PROPERTY_COMMAND_NAME, "famadm");
        CommandManager cmdManager = new CommandManager(env);
        // [END] create an instance of CommandManager

        URL url = new URL(remoteIDPEntityID);
        String proto = url.getProtocol();
        String host = url.getHost();
        String port = "" + url.getPort();
        String deploymenturi = getDeploymentUri(url);

        IDFFMetaManager metaManager = new IDFFMetaManager(ssoToken);
        List spEntityList = 
            metaManager.getAllHostedServiceProviderEntities(defaultRealm);
        boolean spExists = ((spEntityList != null && !spEntityList.isEmpty()) 
              && spEntityList.contains(hostedSPEntityID)) ;
        if (!spExists) {
            // [START] Make a call to CLI to get the meta data template
            String[] args = {"create-metadata-templ",
                "--spec", "idff",
                "--entityid", hostedSPEntityID,
                "--serviceprovider", "/multiprotoidffsp"};
            CLIRequest req = new CLIRequest(null, args, ssoToken);
            cmdManager.addToRequestQueue(req);
            cmdManager.serviceRequestQueue();
            String result = outputWriter.getMessages();
            // [END] Make a call to CLI to get the meta data template
              
            // [START] Parse the output of CLI to get metadata XML
            String endEntityDescriptorTag = "</EntityDescriptor>";
            int metaStartIdx = result.indexOf("<EntityDescriptor");
            int metaEndIdx = result.indexOf(endEntityDescriptorTag,
                metaStartIdx);
            String metaXML = result.substring(metaStartIdx, metaEndIdx +
                endEntityDescriptorTag.length() +1);
            if (!realBaseURL.equals(baseURL)) {
                metaXML = metaXML.replaceAll(realBaseURL, baseURL);
            }
            // [END] Parse the output of CLI to get metadata XML
            
            // [START] Parse the output of CLI to get extended data XML
            String endEntityConfigTag = "</EntityConfig>";
            int extendStartIdx = result.indexOf("<EntityConfig ");
            int extendEndIdx = result.indexOf(endEntityConfigTag,
                extendStartIdx);
            String extendedXML = result.substring(extendStartIdx,
                extendEndIdx + endEntityConfigTag.length() + 1);
            // handle LB case
            if (!realBaseURL.equals(baseURL)) {
                extendedXML = 
                    extendedXML.replaceAll(realBaseURL, baseURL);
            }
            // [END] Parse the output of CLI to get extended data XML
        
            // [START] modify extended config to set providerHomePageURL
            int exStartIdx = extendedXML.indexOf(
                "<Attribute name=\"providerHomePageURL\">");
            int exValueIdx = extendedXML.indexOf("<Value>",
                exStartIdx);
            extendedXML = extendedXML.substring(0, exValueIdx + 7) +
                baseURL + "/samples/multiprotocol/demo/home.jsp" +
                extendedXML.substring(exValueIdx + 7);
            // [END] modify extended config to set providerHomePageURL

            // [START] Import these XMLs
            com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement
                descriptor = (com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement)
                IDFFMetaUtils.convertStringToJAXB(metaXML);
            metaManager.createEntityDescriptor(defaultRealm, descriptor);
        
            com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement 
                extendConfigElm = (com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement)
                IDFFMetaUtils.convertStringToJAXB(extendedXML);
            metaManager.createEntityConfig(defaultRealm, extendConfigElm);
            // [END] Import these XMLs
        }
        
        List idpEntityList = 
            metaManager.getAllRemoteIdentityProviderIDs(defaultRealm);
        boolean idpExists = ((idpEntityList != null && !idpEntityList.isEmpty()) 
              && idpEntityList.contains(remoteIDPEntityID)) ;
        if (!idpExists) {
            // [START] Make a call to CLI to get IDP meta data template
            String[] args2 = {"create-metadata-templ",
                "--spec", "idff",
                "--entityid", remoteIDPEntityID,
                "--identityprovider", "/multiprotoidffidp"};
            outputWriter = new StringOutputWriter();
            env.put(CLIConstants.SYS_PROPERTY_OUTPUT_WRITER,
                outputWriter);
            cmdManager = new CommandManager(env);
            CLIRequest req = new CLIRequest(null, args2, ssoToken);
            cmdManager.addToRequestQueue(req);
            cmdManager.serviceRequestQueue();
            String result = outputWriter.getMessages();
            // [END] Make a call to CLI to get the meta data template       
        
            // [START] Parse the output of CLI to get metadata XML
            String endEntityDescriptorTag = "</EntityDescriptor>";
            int metaStartIdx = result.indexOf("<EntityDescriptor");
            int metaEndIdx = result.indexOf(endEntityDescriptorTag,
                metaStartIdx);
            String metaXML = result.substring(metaStartIdx,
                metaEndIdx + endEntityDescriptorTag.length() +1);
            // [END] Parse the output of CLI to get metadata XML
        
            // [START] Swap protocol, host, port and deployment URI
            //         to form IDP metadata XML and import it
            String idpMetaXML = metaXML.replaceAll(realBaseURL,
                proto + "://"+ host + ":" + port + deploymenturi);
            com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement 
                idpDescriptor = (com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement)
                IDFFMetaUtils.convertStringToJAXB(idpMetaXML);
            remoteIDPEntityID = idpDescriptor.getProviderID();
            metaManager.createEntityDescriptor(defaultRealm, idpDescriptor);
            // [END] Swap protocol, host, port and deployment URI
            //       to form IDP metadata XML and import it
        }
        
        createCircleOfTrust(SAMPLE_COT_NAME, remoteIDPEntityID,
                hostedSPEntityID, SingleLogoutManager.IDFF);
    }
    
    public void configureWSFedServiceProvider(String remoteIDPEntityID,
        String hostedSPEntityID, HttpServletRequest request) throws Exception {

        // [START] create an instance of CommandManager
        StringOutputWriter outputWriter = new StringOutputWriter();
        Map env = new HashMap();
        env.put(CLIConstants.SYS_PROPERTY_OUTPUT_WRITER, outputWriter);
        env.put(CLIConstants.ARGUMENT_LOCALE, request.getLocale());
        env.put(CLIConstants.SYS_PROPERTY_DEFINITION_FILES,
            "com.sun.identity.federation.cli.FederationManager");
        env.put(CLIConstants.SYS_PROPERTY_COMMAND_NAME, "famadm");
        CommandManager cmdManager = new CommandManager(env);
        // [END] create an instance of CommandManager

        URL url = new URL(remoteIDPEntityID);
        String proto = url.getProtocol();
        String host = url.getHost();
        String port = "" + url.getPort();
        String deploymenturi = getDeploymentUri(url);

        List spEntityList = WSFederationMetaManager.getAllHostedServiceProviderEntities(defaultRealm);
        boolean spExists = ((spEntityList != null && !spEntityList.isEmpty()) 
              && spEntityList.contains(hostedSPEntityID)) ;
        if (!spExists) {
            // [START] Make a call to CLI to get the meta data template
            String[] args = {"create-metadata-templ",
                "--spec", "wsfed",
                "--entityid", hostedSPEntityID,
                "--serviceprovider", "/multiprotowsfedsp"};
            CLIRequest req = new CLIRequest(null, args, ssoToken);
            cmdManager.addToRequestQueue(req);
            cmdManager.serviceRequestQueue();
            String result = outputWriter.getMessages();
            // [END] Make a call to CLI to get the meta data template
              
            // [START] Parse the output of CLI to get metadata XML
            String endEntityDescriptorTag = "</Federation>";
            int metaStartIdx = result.indexOf("<Federation ");
            int metaEndIdx = result.indexOf(endEntityDescriptorTag,
                metaStartIdx);
            String metaXML = result.substring(metaStartIdx, metaEndIdx +
                endEntityDescriptorTag.length() +1);
            if (!realBaseURL.equals(baseURL)) {
                metaXML = metaXML.replaceAll(realBaseURL, baseURL);
            }
            // [END] Parse the output of CLI to get metadata XML
            
            // [START] Parse the output of CLI to get extended data XML
            String endEntityConfigTag = "</FederationConfig>";
            int extendStartIdx = result.indexOf("<FederationConfig ");
            int extendEndIdx = result.indexOf(endEntityConfigTag,
                extendStartIdx);
            String extendedXML = result.substring(extendStartIdx,
                extendEndIdx + endEntityConfigTag.length() + 1);
            // handle LB case
            if (!realBaseURL.equals(baseURL)) {
                extendedXML = 
                    extendedXML.replaceAll(realBaseURL, baseURL);
            }
            // [END] Parse the output of CLI to get extended data XML
        
            // [START] modify extended config to set defaultRelayState 
            int exStartIdx = extendedXML.indexOf(
                "<Attribute name=\"defaultRelayState\">");
            int exValueIdx = extendedXML.indexOf("<Value/>",
                exStartIdx);
            extendedXML = extendedXML.substring(0, exValueIdx) + "<Value>" +
                baseURL + "/samples/multiprotocol/demo/home.jsp</Value>" +
                extendedXML.substring(exValueIdx + 8);
            // [END] modify extended config to set defaultRelayState 

            // [START] modify extended config to set wantAssertionSigned=false
            exStartIdx = extendedXML.indexOf(
                "<Attribute name=\"wantAssertionSigned\">");
            exValueIdx = extendedXML.indexOf("<Value>true</Value>",
                exStartIdx);
            extendedXML = extendedXML.substring(0, exValueIdx) + 
                "<Value>false</Value>" + extendedXML.substring(exValueIdx + 19);
            // [END] modify extended config to set defaultRelayState 

            // [START] Import these XMLs
            com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement
                descriptor = (com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement)
                WSFederationMetaUtils.convertStringToJAXB(metaXML);
            WSFederationMetaManager.createFederation(defaultRealm, descriptor);
        
            com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement
                extendConfigElm = (com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement)
                WSFederationMetaUtils.convertStringToJAXB(extendedXML);
            WSFederationMetaManager.createEntityConfig(defaultRealm, extendConfigElm);
            // [END] Import these XMLs
        }
        
        List idpEntityList = 
            WSFederationMetaManager.getAllRemoteIdentityProviderEntities(defaultRealm);
        boolean idpExists = ((idpEntityList != null && !idpEntityList.isEmpty())
              && idpEntityList.contains(remoteIDPEntityID)) ;
        if (!idpExists) {
            // [START] Make a call to CLI to get IDP meta data template
            String[] args2 = {"create-metadata-templ",
                "--spec", "wsfed",
                "--entityid", remoteIDPEntityID,
                "--identityprovider", "/multiprotowsfedidp"};
            outputWriter = new StringOutputWriter();
            env.put(CLIConstants.SYS_PROPERTY_OUTPUT_WRITER,
                outputWriter);
            cmdManager = new CommandManager(env);
            CLIRequest req = new CLIRequest(null, args2, ssoToken);
            cmdManager.addToRequestQueue(req);
            cmdManager.serviceRequestQueue();
            String result = outputWriter.getMessages();
            // [END] Make a call to CLI to get the meta data template       
        
            // [START] Parse the output of CLI to get metadata XML
            String endEntityDescriptorTag = "</Federation>";
            int metaStartIdx = result.indexOf("<Federation ");
            int metaEndIdx = result.indexOf(endEntityDescriptorTag,
                metaStartIdx);
            String metaXML = result.substring(metaStartIdx,
                metaEndIdx + endEntityDescriptorTag.length() +1);
            // [END] Parse the output of CLI to get metadata XML
        
            // [START] Swap protocol, host, port and deployment URI
            //         to form IDP metadata XML and import it
            String idpMetaXML = metaXML.replaceAll(realBaseURL,
                proto + "://"+ host + ":" + port + deploymenturi);
            // [END] Swap protocol, host, port and deployment URI
            //         to form IDP metadata XML and import it

            // [START] Construct a dummy IDP metadata with null cotlist
            String extendedXML = "<FederationConfig FederationID=\"" + 
                 remoteIDPEntityID + "\" hosted=\"false\" xmlns=\"urn:sun:fm:wsfederation:1.0:federationconfig\">" + "\n" + "    <IDPSSOConfig>\n" +
                 "        <Attribute name=\"cotlist\">\n" +
                 "        </Attribute>\n" +  
                 "    </IDPSSOConfig>\n" +
                 "</FederationConfig>\n";
            // [END] Construct a dummy IDP metadata

            // [START] Import these XMLs
            com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement
                descriptor = (com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement)
                WSFederationMetaUtils.convertStringToJAXB(idpMetaXML);
            WSFederationMetaManager.createFederation(defaultRealm, descriptor);

            com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement
                extendConfigElm2 = (com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement)
                WSFederationMetaUtils.convertStringToJAXB(extendedXML);

            WSFederationMetaManager.createEntityConfig(defaultRealm, extendConfigElm2);
            // [END] importing XMLs
        }

        createCircleOfTrust(SAMPLE_COT_NAME, remoteIDPEntityID,
            hostedSPEntityID, SingleLogoutManager.WS_FED);
    }
    
    private String getDeploymentUri(URL url) {
        String deploymenturi = url.getPath();
        int loc = deploymenturi.indexOf("/", 1);
        if (loc != -1) {
            deploymenturi = deploymenturi.substring(0, loc);
        }
        return deploymenturi;
    }
    
    public void configureSAML2IdentityProvider(String hostedIDPEntityID,
        String remoteSPEntityID, HttpServletRequest request) throws Exception {

        // [START] create an instance of CommandManager
        StringOutputWriter outputWriter = new StringOutputWriter();
        Map env = new HashMap();
        env.put(CLIConstants.SYS_PROPERTY_OUTPUT_WRITER, outputWriter);
        env.put(CLIConstants.ARGUMENT_LOCALE, request.getLocale());
        env.put(CLIConstants.SYS_PROPERTY_DEFINITION_FILES,
            "com.sun.identity.federation.cli.FederationManager");
        env.put(CLIConstants.SYS_PROPERTY_COMMAND_NAME, "famadm");
        CommandManager cmdManager = new CommandManager(env);
        // [END] create an instance of CommandManager

        URL url = new URL(remoteSPEntityID);
        String proto = url.getProtocol();
        String host = url.getHost();
        String port = "" + url.getPort();
        String deploymenturi = getDeploymentUri(url);
        
        // [START] Make a call to CLI to get the meta data template
        SAML2MetaManager metaManager = new SAML2MetaManager();
        List idpEntityList =
                metaManager.getAllHostedIdentityProviderEntities(defaultRealm);
        boolean idpExists =  ((idpEntityList != null &&
                !idpEntityList.isEmpty()) &&
                idpEntityList.contains(hostedIDPEntityID)) ;
        CLIRequest req = null;
        int metaStartIdx = 0;
        int metaEndIdx = 0;
        String metaXML = null;
        String endEntityDescriptorTag=null;
        String result = null;
        int extendStartIdx = 0;
        int extendEndIdx = 0;
        if (!idpExists) {
            String[] args = {"create-metadata-templ",
            "--entityid", hostedIDPEntityID,
            "--identityprovider", "/multiprotosaml2idp"};
            req = new CLIRequest(null, args, ssoToken);
            cmdManager.addToRequestQueue(req);
            cmdManager.serviceRequestQueue();
            result = outputWriter.getMessages();
            // [END] Make a call to CLI to get the meta data template
            
            
            // [START] Parse the output of CLI to get metadata XML
            endEntityDescriptorTag = "</EntityDescriptor>";
            metaStartIdx = result.indexOf("<EntityDescriptor");
            metaEndIdx = result.indexOf(endEntityDescriptorTag,
                    metaStartIdx);
            metaXML = result.substring(metaStartIdx, metaEndIdx +
                    endEntityDescriptorTag.length() +1);
            if (!realBaseURL.equals(baseURL)) {
                metaXML = metaXML.replaceAll(realBaseURL, baseURL);
            }
            // [END] Parse the output of CLI to get metadata XML
            
            
            // [START] Parse the output of CLI to get extended data XML
            String endEntityConfigTag = "</EntityConfig>";
            extendStartIdx = result.indexOf("<EntityConfig ");
            extendEndIdx = result.indexOf(endEntityConfigTag,
                    extendStartIdx);
            String extendedXML = result.substring(extendStartIdx,
                    extendEndIdx + endEntityConfigTag.length() + 1);
            // handle LB case
            if (!realBaseURL.equals(baseURL)) {
                extendedXML = 
                    extendedXML.replaceAll(realBaseURL, baseURL);
            }
            // [END] Parse the output of CLI to get extended data XML
            
            
            // [START] Import these XMLs
            EntityDescriptorElement descriptor =
                    (EntityDescriptorElement)
                    SAML2MetaUtils.convertStringToJAXB(metaXML);
            metaManager.createEntityDescriptor(defaultRealm, descriptor);
            
            EntityConfigElement extendConfigElm = (EntityConfigElement)
            SAML2MetaUtils.convertStringToJAXB(extendedXML);
            metaManager.createEntityConfig(defaultRealm, extendConfigElm);
            // [END] Import these XMLs
        }
        
        // [START] Make a call to CLI to get SP meta data template
        List spEntityList =
                metaManager.getAllRemoteServiceProviderEntities(defaultRealm);
        boolean spExists =
                ((spEntityList != null && !spEntityList.isEmpty())
                && spEntityList.contains(remoteSPEntityID));
        if (!spExists) {
            String[] args2 = {"create-metadata-templ",
            "--entityid", remoteSPEntityID,
            "--serviceprovider", "/multiprotosaml2sp"};
            outputWriter = new StringOutputWriter();
            env.put(CLIConstants.SYS_PROPERTY_OUTPUT_WRITER,
                    outputWriter);
            cmdManager = new CommandManager(env);
            req = new CLIRequest(null, args2, ssoToken);
            cmdManager.addToRequestQueue(req);
            cmdManager.serviceRequestQueue();
            result = outputWriter.getMessages();
            // [END] Make a call to CLI to get the meta data template
            
            // [START] Parse the output of CLI to get metadata XML
            metaStartIdx = result.indexOf("<EntityDescriptor");
            metaEndIdx = result.indexOf(endEntityDescriptorTag,
                    metaStartIdx);
            metaXML = result.substring(metaStartIdx, metaEndIdx +
                    endEntityDescriptorTag.length() +1);
            // [END] Parse the output of CLI to get metadata XML
            
            // [START] Swap protocol, host, port and deployment URI
            //         to form IDP metadata XML and import it
            String spMetaXML = metaXML.replaceAll(realBaseURL,
                proto + "://"+ host + ":" + port + deploymenturi);
            EntityDescriptorElement spDescriptor =
                    (EntityDescriptorElement)
                    SAML2MetaUtils.convertStringToJAXB(spMetaXML);
            remoteSPEntityID = spDescriptor.getEntityID();
            metaManager.createEntityDescriptor(defaultRealm, spDescriptor);
            // [END] Swap protocol, host, port and deployment URI
            //       to form IDP metadata XML and import it
        }
        // [START] Create Circle of Trust
        createCircleOfTrust(SAMPLE_COT_NAME, hostedIDPEntityID,
                remoteSPEntityID, SingleLogoutManager.SAML2);
    }
    
    public void configureIDFFIdentityProvider(String hostedIDPEntityID,
        String remoteSPEntityID, HttpServletRequest request) throws Exception {

        // [START] create an instance of CommandManager
        StringOutputWriter outputWriter = new StringOutputWriter();
        Map env = new HashMap();
        env.put(CLIConstants.SYS_PROPERTY_OUTPUT_WRITER, outputWriter);
        env.put(CLIConstants.ARGUMENT_LOCALE, request.getLocale());
        env.put(CLIConstants.SYS_PROPERTY_DEFINITION_FILES,
            "com.sun.identity.federation.cli.FederationManager");
        env.put(CLIConstants.SYS_PROPERTY_COMMAND_NAME, "famadm");
        CommandManager cmdManager = new CommandManager(env);
        // [END] create an instance of CommandManager

        URL url = new URL(remoteSPEntityID);
        String proto = url.getProtocol();
        String host = url.getHost();
        String port = "" + url.getPort();
        String deploymenturi = getDeploymentUri(url);

        IDFFMetaManager metaManager = new IDFFMetaManager(ssoToken);
        List idpEntityList =
                metaManager.getAllHostedIdentityProviderIDs(defaultRealm);
        boolean idpExists = ((idpEntityList != null && !idpEntityList.isEmpty())
            && idpEntityList.contains(hostedIDPEntityID)) ;
        if (!idpExists) {
            // [START] Make a call to CLI to get the meta data template
            String[] args = {"create-metadata-templ",
            "--spec", "idff",
            "--entityid", hostedIDPEntityID,
            "--identityprovider", "/multiprotoidffidp"};
            CLIRequest req = new CLIRequest(null, args, ssoToken);
            cmdManager.addToRequestQueue(req);
            cmdManager.serviceRequestQueue();
            String result = outputWriter.getMessages();
            // [END] Make a call to CLI to get the meta data template
            
            // [START] Parse the output of CLI to get metadata XML
            String endEntityDescriptorTag = "</EntityDescriptor>";
            int metaStartIdx = result.indexOf("<EntityDescriptor");
            int metaEndIdx = result.indexOf(endEntityDescriptorTag,
                    metaStartIdx);
            String metaXML = result.substring(metaStartIdx, metaEndIdx +
                    endEntityDescriptorTag.length() +1);
            if (!realBaseURL.equals(baseURL)) {
                metaXML = metaXML.replaceAll(realBaseURL, baseURL);
            }
            // [END] Parse the output of CLI to get metadata XML
            
            // [START] Parse the output of CLI to get extended data XML
            String endEntityConfigTag = "</EntityConfig>";
            int extendStartIdx = result.indexOf("<EntityConfig ");
            int extendEndIdx = result.indexOf(endEntityConfigTag,
                    extendStartIdx);
            String extendedXML = result.substring(extendStartIdx,
                    extendEndIdx + endEntityConfigTag.length() + 1);
            // handle LB case
            if (!realBaseURL.equals(baseURL)) {
                extendedXML = 
                    extendedXML.replaceAll(realBaseURL, baseURL);
            }
            // [END] Parse the output of CLI to get extended data XML
            
            // [START] modify extended config to set providerHomePageURL
            int exStartIdx = extendedXML.indexOf(
                    "<Attribute name=\"providerHomePageURL\">");
            int exValueIdx = extendedXML.indexOf("<Value>",
                    exStartIdx);
            extendedXML = extendedXML.substring(0, exValueIdx + 7) +
                    baseURL + "/samples/multiprotocol/demo/home.jsp" +
                    extendedXML.substring(exValueIdx + 7);
            // [END] modify extended config to set providerHomePageURL
           
            // [START] Import these XMLs
            com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement 
                descriptor = (com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement)
                    IDFFMetaUtils.convertStringToJAXB(metaXML);
            metaManager.createEntityDescriptor(defaultRealm, descriptor);
            
            com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement extendConfigElm = (com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement)
            IDFFMetaUtils.convertStringToJAXB(extendedXML);
            metaManager.createEntityConfig(defaultRealm, extendConfigElm);
            // [END] Import these XMLs
        }
        
        List spEntityList =
                metaManager.getAllRemoteServiceProviderEntities(defaultRealm);
        boolean spExists = ((spEntityList != null && !spEntityList.isEmpty())
        && spEntityList.contains(remoteSPEntityID)) ;
        if (!idpExists) {
            // [START] Make a call to CLI to get SP meta data template
            String[] args2 = {"create-metadata-templ",
                "--spec", "idff",
                "--entityid", remoteSPEntityID,
                "--serviceprovider", "/multiprotoidffsp"};
            outputWriter = new StringOutputWriter();
            env.put(CLIConstants.SYS_PROPERTY_OUTPUT_WRITER,
                outputWriter);
            cmdManager = new CommandManager(env);
            CLIRequest req = new CLIRequest(null, args2, ssoToken);
            cmdManager.addToRequestQueue(req);
            cmdManager.serviceRequestQueue();
            String result = outputWriter.getMessages();
            // [END] Make a call to CLI to get the meta data template
            
            // [START] Parse the output of CLI to get metadata XML
            String endEntityDescriptorTag = "</EntityDescriptor>";
            int metaStartIdx = result.indexOf("<EntityDescriptor");
            int metaEndIdx = result.indexOf(endEntityDescriptorTag,
                metaStartIdx);
            String metaXML = result.substring(metaStartIdx, metaEndIdx +
                endEntityDescriptorTag.length() +1);
            // [END] Parse the output of CLI to get metadata XML
            
            // [START] Swap protocol, host, port and deployment URI
            //         to form IDP metadata XML and import it
            String spMetaXML = metaXML.replaceAll(realBaseURL,
                proto + "://"+ host + ":" + port + deploymenturi);
            com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement spDescriptor = (com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement)
                IDFFMetaUtils.convertStringToJAXB(spMetaXML);
            remoteSPEntityID = spDescriptor.getProviderID();
            metaManager.createEntityDescriptor(defaultRealm, spDescriptor);
            // [END] Swap protocol, host, port and deployment URI
            //       to form IDP metadata XML and import it
        }
        
        // [START] Create Circle of Trust
        createCircleOfTrust(SAMPLE_COT_NAME, hostedIDPEntityID,
            remoteSPEntityID, SingleLogoutManager.IDFF);
        // [END] Create Circle of Trust
    }
    
    public void configureWSFedIdentityProvider(String hostedIDPEntityID,
        String remoteSPEntityID, HttpServletRequest request) throws Exception {
        // [START] create an instance of CommandManager
        StringOutputWriter outputWriter = new StringOutputWriter();
        Map env = new HashMap();
        env.put(CLIConstants.SYS_PROPERTY_OUTPUT_WRITER, outputWriter);
        env.put(CLIConstants.ARGUMENT_LOCALE, request.getLocale());
        env.put(CLIConstants.SYS_PROPERTY_DEFINITION_FILES,
            "com.sun.identity.federation.cli.FederationManager");
        env.put(CLIConstants.SYS_PROPERTY_COMMAND_NAME, "famadm");
        CommandManager cmdManager = new CommandManager(env);
        // [END] create an instance of CommandManager

        URL url = new URL(remoteSPEntityID);
        String proto = url.getProtocol();
        String host = url.getHost();
        String port = "" + url.getPort();
        String deploymenturi = getDeploymentUri(url);

        List idpEntityList = 
            WSFederationMetaManager.getAllHostedIdentityProviderEntities(defaultRealm);
        boolean idpExists = ((idpEntityList != null && !idpEntityList.isEmpty())
              && idpEntityList.contains(hostedIDPEntityID)) ;
        if (!idpExists) {
            // [START] Make a call to CLI to get the meta data template
            String[] args = {"create-metadata-templ",
                "--spec", "wsfed",
                "--entityid", hostedIDPEntityID,
                "--identityprovider", "/multiprotowsfedidp"};
            CLIRequest req = new CLIRequest(null, args, ssoToken);
            cmdManager.addToRequestQueue(req);
            cmdManager.serviceRequestQueue();
            String result = outputWriter.getMessages();
            // [END] Make a call to CLI to get the meta data template
              
            // [START] Parse the output of CLI to get metadata XML
            String endEntityDescriptorTag = "</Federation>";
            int metaStartIdx = result.indexOf("<Federation ");
            int metaEndIdx = result.indexOf(endEntityDescriptorTag,
                metaStartIdx);
            String metaXML = result.substring(metaStartIdx, metaEndIdx +
                endEntityDescriptorTag.length() +1);
            if (!realBaseURL.equals(baseURL)) {
                metaXML = metaXML.replaceAll(realBaseURL, baseURL);
            }
            // [END] Parse the output of CLI to get metadata XML
            
            // [START] Parse the output of CLI to get extended data XML
            String endEntityConfigTag = "</FederationConfig>";
            int extendStartIdx = result.indexOf("<FederationConfig ");
            int extendEndIdx = result.indexOf(endEntityConfigTag,
                extendStartIdx);
            String extendedXML = result.substring(extendStartIdx,
                extendEndIdx + endEntityConfigTag.length() + 1);
            // handle LB case
            if (!realBaseURL.equals(baseURL)) {
                extendedXML = 
                    extendedXML.replaceAll(realBaseURL, baseURL);
            }
            // [END] Parse the output of CLI to get extended data XML
        
            // [START] Import these XMLs
            com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement
                descriptor = (com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement)
                WSFederationMetaUtils.convertStringToJAXB(metaXML);
            WSFederationMetaManager.createFederation(defaultRealm, descriptor);
        
            com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement
                extendConfigElm = (com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement)
                WSFederationMetaUtils.convertStringToJAXB(extendedXML);
            WSFederationMetaManager.createEntityConfig(defaultRealm, extendConfigElm);
            // [END] Import these XMLs
        }
        
        List spEntityList = WSFederationMetaManager.getAllRemoteServiceProviderEntities(defaultRealm);
        boolean spExists = ((spEntityList != null && !spEntityList.isEmpty()) 
              && spEntityList.contains(remoteSPEntityID)) ;
        if (!spExists) {
            // [START] Make a call to CLI to get IDP meta data template
            String[] args2 = {"create-metadata-templ",
                "--spec", "wsfed",
                "--entityid", remoteSPEntityID,
                "--serviceprovider", "/multiprotowsfedsp"};
            outputWriter = new StringOutputWriter();
            env.put(CLIConstants.SYS_PROPERTY_OUTPUT_WRITER,
                outputWriter);
            cmdManager = new CommandManager(env);
            CLIRequest req = new CLIRequest(null, args2, ssoToken);
            cmdManager.addToRequestQueue(req);
            cmdManager.serviceRequestQueue();
            String result = outputWriter.getMessages();
            // [END] Make a call to CLI to get the meta data template       
        
            // [START] Parse the output of CLI to get metadata XML
            String endEntityDescriptorTag = "</Federation>";
            int metaStartIdx = result.indexOf("<Federation ");
            int metaEndIdx = result.indexOf(endEntityDescriptorTag,
                metaStartIdx);
            String metaXML = result.substring(metaStartIdx,
                metaEndIdx + endEntityDescriptorTag.length() +1);
            // [END] Parse the output of CLI to get metadata XML
        
            // [START] Swap protocol, host, port and deployment URI
            //         to form IDP metadata XML and import it
            String spMetaXML = metaXML.replaceAll(realBaseURL,
                proto + "://"+ host + ":" + port + deploymenturi);
            // [END] Swap protocol, host, port and deployment URI
            //         to form IDP metadata XML and import it

            // [START] Construct a dummy IDP metadata with null cotlist
            String extendedXML = "<FederationConfig FederationID=\"" + 
                 remoteSPEntityID + "\" hosted=\"false\" xmlns=\"urn:sun:fm:wsfederation:1.0:federationconfig\">" + "\n" + "    <SPSSOConfig>\n" +
                 "        <Attribute name=\"cotlist\">\n" +
                 "        </Attribute>\n" +  
                 "        <Attribute name=\"wantAssertionSigned\">\n" +
                 "            <Value>false</Value>\n" + 
                 "        </Attribute>\n" +
                 "    </SPSSOConfig>\n" +
                 "</FederationConfig>\n";
            // [END] Construct a dummy IDP metadata

            // [START] Import these XMLs
            com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement
                descriptor = (com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement)
                WSFederationMetaUtils.convertStringToJAXB(spMetaXML);
            WSFederationMetaManager.createFederation(defaultRealm, descriptor);

            com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement
                extendConfigElm2 = (com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement)
                WSFederationMetaUtils.convertStringToJAXB(extendedXML);

            WSFederationMetaManager.createEntityConfig(defaultRealm, extendConfigElm2);
            // [END] importing XMLs
        }

        createCircleOfTrust(SAMPLE_COT_NAME, hostedIDPEntityID,
            remoteSPEntityID, SingleLogoutManager.WS_FED);
    }
%>

<%
    baseHost = request.getServerName();
    baseURL = request.getRequestURI().toString();
    int idx = baseURL.indexOf('/', 1);
    baseURI = baseURL.substring(idx);
    localProto = request.getScheme();
    localHost =  request.getServerName();
    localPort = "" + request.getServerPort();
    localDeploymentURI = baseURL.substring(0, idx);
    baseURL = localProto + "://" + localHost +
        ":" + localPort + localDeploymentURI;
    realBaseURL = 
        SystemPropertiesManager.get(Constants.AM_SERVER_PROTOCOL) + "://" +
        SystemPropertiesManager.get(Constants.AM_SERVER_HOST) + ":" + 
        SystemPropertiesManager.get(Constants.AM_SERVER_PORT) + 
        SystemPropertiesManager.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
    try {
        SessionProvider provider = SessionManager.getProvider();
        Object sess = provider.getSession(request);
        if (sess != null) {
            loggedIn = provider.isValid(sess);
        }
    } catch (SessionException e) {
        //ignored
    }

    SSOTokenManager manager = SSOTokenManager.getInstance();
    ssoToken = null;
    try {
        ssoToken = manager.createSSOToken(request);
    } catch (SSOException se) {
        // do nothing
    }
%>
