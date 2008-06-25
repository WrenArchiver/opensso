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
 * $Id: CreateMetaDataTemplate.java,v 1.33 2008-06-25 05:49:52 qcheng Exp $
 *
 */

package com.sun.identity.federation.cli;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.shared.Constants;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.workflow.CreateIDFFMetaDataTemplate;
import com.sun.identity.workflow.CreateSAML2HostedProviderTemplate;
import com.sun.identity.workflow.CreateWSFedMetaDataTemplate;
import com.sun.identity.workflow.MetaTemplateParameters;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.security.cert.CertificateEncodingException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBException;

/**
 * Create Meta Data Template.
 */
public class CreateMetaDataTemplate extends AuthenticatedCommand {
    
    private String entityID;
    private String metadata;
    private String extendedData;
    private String idpAlias;
    private String spAlias;
    private String attraAlias;
    private String attrqAlias;
    private String authnaAlias;
    private String pdpAlias;
    private String pepAlias;
    private String idpSCertAlias;
    private String idpECertAlias;
    private String attraSCertAlias;
    private String attraECertAlias;
    private String authnaSCertAlias;
    private String authnaECertAlias;
    private String pdpSCertAlias;
    private String pdpECertAlias;
    private String spSCertAlias;
    private String spECertAlias;
    private String attrqSCertAlias;
    private String attrqECertAlias;
    private String affiAlias;
    private String affiOwnerID;
    private List   affiMembers;
    private String affiSCertAlias;
    private String affiECertAlias;
    private String pepSCertAlias;
    private String pepECertAlias;
    private String protocol;
    private String host;
    private String port;
    private String deploymentURI;
    private String realm;
    private boolean isWebBased;

    /**
     * Creates Meta Data Template.
     *
     * @param rc Request Context.
     * @throws CLIException if unable to process this request.
     */
    public void handleRequest(RequestContext rc)
        throws CLIException {
        super.handleRequest(rc);
        ldapLogin();
        getOptions(rc);
        validateOptions();
        normalizeOptions();
        
        String spec = FederationManager.getIDFFSubCommandSpecification(rc);
        if (spec.equals(FederationManager.DEFAULT_SPECIFICATION)) {
            handleSAML2Request(rc);
        } else if (spec.equals(FedCLIConstants.IDFF_SPECIFICATION)) {
            handleIDFFRequest(rc);
        } else if (spec.equals(FedCLIConstants.WSFED_SPECIFICATION)) {
            handleWSFedRequest(rc);
        } else {
            throw new CLIException(
                getResourceString("unsupported-specification"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        
    }
    
    private void handleSAML2Request(RequestContext rc)
        throws CLIException {
        if (!isWebBased || (extendedData != null)) {
            buildConfigTemplate();
        }
        if (!isWebBased || (metadata != null)) {
            buildDescriptorTemplate();
        }
    }
    
    private void handleIDFFRequest(RequestContext rc)
        throws CLIException {
        if (!isWebBased || (extendedData != null)) {
            buildIDFFConfigTemplate();
        }
        if (!isWebBased || (metadata != null)) {
            buildIDFFDescriptorTemplate();
        }
    }
    
    private void handleWSFedRequest(RequestContext rc)
    throws CLIException {
        if (!isWebBased || (extendedData != null)) {
            buildWSFedConfigTemplate();
        }

        if (!isWebBased || (metadata != null)) {
            buildWSFedDescriptorTemplate();
        }
    }
    
    private void getOptions(RequestContext rc) {
        entityID = getStringOptionValue(FedCLIConstants.ARGUMENT_ENTITY_ID);
        idpAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_IDENTITY_PROVIDER);
        spAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_SERVICE_PROVIDER);
        attraAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_ATTRIBUTE_AUTHORITY);
        attrqAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_ATTRIBUTE_QUERY_PROVIDER);
        authnaAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_AUTHN_AUTHORITY);
        pdpAlias = getStringOptionValue(FedCLIConstants.ARGUMENT_PDP);
        pepAlias = getStringOptionValue(FedCLIConstants.ARGUMENT_PEP);
        affiAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_AFFILIATION);
        affiOwnerID = getStringOptionValue(
            FedCLIConstants.ARGUMENT_AFFI_OWNERID);
        affiMembers = (List)rc.getOption(
            FedCLIConstants.ARGUMENT_AFFI_MEMBERS);
        
        metadata = getStringOptionValue(FedCLIConstants.ARGUMENT_METADATA);
        extendedData = getStringOptionValue(
            FedCLIConstants.ARGUMENT_EXTENDED_DATA);
        
        idpSCertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_IDP_S_CERT_ALIAS);
        idpECertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_IDP_E_CERT_ALIAS);
        
        spSCertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_SP_S_CERT_ALIAS);
        spECertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_SP_E_CERT_ALIAS);
        
        attraSCertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_ATTRA_S_CERT_ALIAS);
        attraECertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_ATTRA_E_CERT_ALIAS);
        
        attrqSCertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_ATTRQ_S_CERT_ALIAS);
        attrqECertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_ATTRQ_E_CERT_ALIAS);

        authnaSCertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_AUTHNA_S_CERT_ALIAS);
        authnaECertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_AUTHNA_E_CERT_ALIAS);

        affiSCertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_AFFI_S_CERT_ALIAS);
        affiECertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_AFFI_E_CERT_ALIAS);

        pdpSCertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_PDP_S_CERT_ALIAS);
        pdpECertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_PDP_E_CERT_ALIAS);

        pepSCertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_PEP_S_CERT_ALIAS);
        pepECertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_PEP_E_CERT_ALIAS);

        protocol = SystemPropertiesManager.get(Constants.AM_SERVER_PROTOCOL);
        host = SystemPropertiesManager.get(Constants.AM_SERVER_HOST);
        port = SystemPropertiesManager.get(Constants.AM_SERVER_PORT);
        deploymentURI = SystemPropertiesManager.get(
            Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
        String webURL = getCommandManager().getWebEnabledURL();
        isWebBased = (webURL != null) && (webURL.trim().length() > 0);
    }
    
    private void normalizeOptions() {
        if ((idpAlias != null) && !idpAlias.startsWith("/")) {
            idpAlias = "/" + idpAlias;
        }
        if ((spAlias != null) && !spAlias.startsWith("/")) {
            spAlias = "/" + spAlias;
        }
        if ((attraAlias != null) && !attraAlias.startsWith("/")) {
            attraAlias = "/" + attraAlias;
        }
        if ((attrqAlias != null) && !attrqAlias.startsWith("/")) {
            attrqAlias = "/" + attrqAlias;
        }
        if ((authnaAlias != null) && !authnaAlias.startsWith("/")) {
            authnaAlias = "/" + authnaAlias;
        }
        if ((pdpAlias != null) && !pdpAlias.startsWith("/")) {
            pdpAlias = "/" + pdpAlias;
        }
        if ((pepAlias != null) && !pepAlias.startsWith("/")) {
            pepAlias = "/" + pepAlias;
        }
        if (entityID == null) {
            entityID = host + realm;
        }
        if (idpSCertAlias == null) {
            idpSCertAlias = "";
        }
        if (idpECertAlias == null) {
            idpECertAlias = "";
        }
        if (spSCertAlias == null) {
            spSCertAlias = "";
        }
        if (spECertAlias == null) {
            spECertAlias = "";
        }
        if (attraSCertAlias == null) {
            attraSCertAlias = "";
        }
        if (attraECertAlias == null) {
            attraECertAlias = "";
        }
        if (attrqSCertAlias == null) {
            attrqSCertAlias = "";
        }
        if (attrqECertAlias == null) {
            attrqECertAlias = "";
        }
        if (authnaSCertAlias == null) {
            authnaSCertAlias = "";
        }
        if (authnaECertAlias == null) {
            authnaECertAlias = "";
        }
        if (affiSCertAlias == null) {
            affiSCertAlias = "";
        }
        if (affiECertAlias == null) {
            affiECertAlias = "";
        }
        if (pdpSCertAlias == null) {
            pdpSCertAlias = "";
        }
        if (pdpECertAlias == null) {
            pdpECertAlias = "";
        }
        if (pepSCertAlias == null) {
            pepSCertAlias = "";
        }
        if (pepECertAlias == null) {
            pepECertAlias = "";
        }
    }
    
    private void validateOptions()
        throws CLIException {
        if ((idpAlias == null) && (spAlias == null) && (pdpAlias == null) && 
            (pepAlias == null) && (attraAlias == null) &&
            (attrqAlias == null) && (authnaAlias == null) && (affiAlias == null)
        ) {
            throw new CLIException(getResourceString(
                "create-meta-template-exception-role-null"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }

        if ((affiAlias != null) && ((idpAlias != null) ||
            (spAlias != null) || (pdpAlias != null) || (pepAlias != null) ||
            (attraAlias != null) || (attrqAlias != null) ||
            (authnaAlias != null))
        ) {
            throw new CLIException(getResourceString(
                "create-meta-template-exception-affi-conflict"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }

        if ((affiAlias != null) &&
            ((affiMembers == null) || affiMembers.isEmpty())
        ) {
            throw new CLIException(getResourceString(
                "create-meta-template-exception-affi-members-empty"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }

        if ((affiAlias != null) &&
            ((affiOwnerID == null))
        ) {
            throw new CLIException(getResourceString(
                "create-meta-template-exception-affi-ownerid-empty"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        
        if ((affiAlias == null) &&
            ((affiSCertAlias != null) || (affiECertAlias != null))
        ) {
            throw new CLIException(getResourceString(
                "create-meta-template-exception-affi-null-with-cert-alias"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }

        if ((idpAlias == null) &&
            ((idpSCertAlias != null) || (idpECertAlias != null))
        ) {
            throw new CLIException(getResourceString(
                "create-meta-template-exception-idp-null-with-cert-alias"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        
        if ((spAlias == null) &&
            ((spSCertAlias != null) || (spECertAlias != null))
        ) {
            throw new CLIException(getResourceString(
                "create-meta-template-exception-sp-null-with-cert-alias"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        
        if ((attraAlias == null) &&
            ((attraSCertAlias != null) || (attraECertAlias != null))
        ) {
            throw new CLIException(getResourceString(
                "create-meta-template-exception-attra-null-with-cert-alias"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        
        if ((attrqAlias == null) &&
            ((attrqSCertAlias != null) || (attrqECertAlias != null))
        ) {
            throw new CLIException(getResourceString(
                "create-meta-template-exception-attrq-null-with-cert-alias"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }

        if ((authnaAlias == null) &&
            ((authnaSCertAlias != null) || (authnaECertAlias != null))
        ) {
            throw new CLIException(getResourceString(
                "create-meta-template-exception-authna-null-with-cert-alias"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }

        if ((pdpAlias == null) &&
            ((pdpSCertAlias != null) || (pdpECertAlias != null))
            ) {
            throw new CLIException(getResourceString(
                    "create-meta-template-exception-pdp-null-with-cert-alias"),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        
        if ((pepAlias == null) &&
            ((pepSCertAlias != null) || (pepECertAlias != null))
        ) {
            throw new CLIException(getResourceString(
                "create-meta-template-exception-pep-null-with-cert-alias"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }

        if (protocol == null) {
            throw new CLIException(getResourceString(
                "create-meta-template-exception-protocol-not-found"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        if (host == null) {
            throw new CLIException(getResourceString(
                "create-meta-template-exception-host-not-found"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        if (port == null) {
            throw new CLIException(getResourceString(
                "create-meta-template-exception-port-not-found"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        if (deploymentURI == null) {
            throw new CLIException(getResourceString(
                "create-meta-template-exception-deploymentURI-not-found"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
    
    private void buildDescriptorTemplate()
    throws CLIException {
        Writer pw = null;
        try {
            if (!isWebBased && (metadata != null) && (metadata.length() > 0)) {
                pw = new PrintWriter(new FileWriter(metadata));
            } else {
                pw = new StringWriter();
            }

            String xml =
                CreateSAML2HostedProviderTemplate.buildMetaDataTemplate(
                    entityID, getWorkflowParamMap(), null);
            pw.write(xml);
            
            if (!isWebBased) {
                Object[] objs = { metadata, realm };
                getOutputWriter().printlnMessage(MessageFormat.format(
                    getResourceString(
                    "create-meta-template-created-descriptor-template"), objs));
            }
        } catch (SAML2MetaException e) {
            throw new CLIException(e.getMessage(),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IOException e) {
            Object[] objs = { metadata };
            throw new CLIException(MessageFormat.format(
                    getResourceString("cannot-write-to-file"), objs),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if ((pw != null) && (pw instanceof PrintWriter)) {
                ((PrintWriter)pw).close();
            } else {
                this.getOutputWriter().printlnMessage(
                        ((StringWriter)pw).toString());
            }
        }
    }
    
    private static String buildMetaAliasInURI(String alias) {
        return "/" + SAML2MetaManager.NAME_META_ALIAS_IN_URI + alias;
    }    
    
    private void buildConfigTemplate()
        throws CLIException {
        Writer pw = null;
        try {
            if (!isWebBased && (extendedData != null) &&
                (extendedData.length() > 0)
            ) {
                pw = new PrintWriter(new FileWriter(extendedData));
            } else {
                pw = new StringWriter();
            }

            String xml =
                CreateSAML2HostedProviderTemplate.createExtendedDataTemplate(
                entityID, getWorkflowParamMap(), null);
            pw.write(xml);
            
            if (!isWebBased) {
                Object[] objs = {extendedData, realm};
                getOutputWriter().printlnMessage(MessageFormat.format(
                    getResourceString(
                    "create-meta-template-created-configuration-template"),
                    objs));
            }
        } catch (IOException ex) {
            Object[] objs = { extendedData };
            throw new CLIException(MessageFormat.format(
                    getResourceString("cannot-write-to-file"), objs),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if ((pw != null) && (pw instanceof PrintWriter)) {
                ((PrintWriter)pw).close();
            } else {
                this.getOutputWriter().printlnMessage(
                        ((StringWriter)pw).toString());
            }
        }
    }
    
    private void buildIDFFConfigTemplate()
    throws CLIException {
        Writer pw = null;
        try {
            if (!isWebBased && (extendedData != null) && 
                (extendedData.length() > 0)
            ) {
                pw = new PrintWriter(new FileWriter(extendedData));
            } else {
                pw = new StringWriter();
            }
            
            String xml = CreateIDFFMetaDataTemplate.createExtendedMetaTemplate(
                entityID, getWorkflowParamMap());
            pw.write(xml);
            if (!isWebBased) {
                Object[] objs = {extendedData, realm};
                getOutputWriter().printlnMessage(MessageFormat.format(
                    getResourceString(
                    "create-meta-template-created-configuration-template"),
                    objs));
            }
        } catch (IOException ex) {
            Object[] objs = { extendedData };
            throw new CLIException(MessageFormat.format(
                    getResourceString("cannot-write-to-file"), objs),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if ((pw != null) && (pw instanceof PrintWriter)) {
                ((PrintWriter)pw).close();
            } else {
                this.getOutputWriter().printlnMessage(
                    ((StringWriter)pw).toString());
            }
        }
    }
    
    private void buildIDFFDescriptorTemplate()
        throws CLIException {
        Writer pw = null;
        try {
            if (!isWebBased && (metadata != null) && (metadata.length() > 0)) {
                pw = new PrintWriter(new FileWriter(metadata));
            } else {
                pw = new StringWriter();
            }
            
            String xml = CreateIDFFMetaDataTemplate.createStandardMetaTemplate(
                entityID, getWorkflowParamMap(), null);
            pw.write(xml);
            
            if (!isWebBased) {
                Object[] objs = { metadata, realm };
                getOutputWriter().printlnMessage(MessageFormat.format(
                    getResourceString(
                    "create-meta-template-created-descriptor-template"), objs));
            }
        } catch (IDFFMetaException e) {
            throw new CLIException(e.getMessage(),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IOException e) {
            Object[] objs = { metadata };
            throw new CLIException(MessageFormat.format(
                    getResourceString("cannot-write-to-file"), objs),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if ((pw != null) && (pw instanceof PrintWriter)) {
                ((PrintWriter)pw).close();
            } else {
                this.getOutputWriter().printlnMessage(
                        ((StringWriter)pw).toString());
            }
        }
    }
    
    private void buildWSFedDescriptorTemplate()
    throws CLIException {
        Writer pw = null;
        try {
            if (!isWebBased && (metadata != null) && (metadata.length() > 0)) {
                pw = new PrintWriter(new FileWriter(metadata));
            } else {
                pw = new StringWriter();
            }
            
            String xml = CreateWSFedMetaDataTemplate.createStandardMetaTemplate(
                entityID, getWorkflowParamMap(), null);
            pw.write(xml);
            
            if (!isWebBased) {
                Object[] objs = { metadata, realm };
                getOutputWriter().printlnMessage(MessageFormat.format(
                    getResourceString(
                    "create-meta-template-created-descriptor-template"), objs));
            }
        } catch (IOException e) {
            Object[] objs = { metadata };
            throw new CLIException(MessageFormat.format(
                    getResourceString("cannot-write-to-file"), objs),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (JAXBException e) {
            throw new CLIException(e.getMessage(),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (CertificateEncodingException e) {
            throw new CLIException(e.getMessage(),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if ((pw != null) && (pw instanceof PrintWriter)) {
                ((PrintWriter)pw).close();
            } else {
                this.getOutputWriter().printlnMessage(
                        ((StringWriter)pw).toString());
            }
        }
    }

    private void buildWSFedConfigTemplate()
    throws CLIException {
        Writer pw = null;
        try {
            if (!isWebBased && (extendedData != null) &&
                (extendedData.length() > 0)
            ) {
                pw = new PrintWriter(new FileWriter(extendedData));
            } else {
                pw = new StringWriter();
            }

            String xml = CreateWSFedMetaDataTemplate.createExtendedMetaTemplate(
                entityID, this.getWorkflowParamMap());
            pw.write(xml);
            if (!isWebBased) {
                Object[] objs = {extendedData, realm};
                getOutputWriter().printlnMessage(MessageFormat.format(
                    getResourceString(
                    "create-meta-template-created-configuration-template"),
                    objs));
            }
        } catch (IOException ex) {
            Object[] objs = { extendedData };
            throw new CLIException(MessageFormat.format(
                    getResourceString("cannot-write-to-file"), objs),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (JAXBException e) {
            throw new CLIException(e.getMessage(),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if ((pw != null) && (pw instanceof PrintWriter)) {
                ((PrintWriter)pw).close();
            } else {
                this.getOutputWriter().printlnMessage(
                        ((StringWriter)pw).toString());
            }
        }
    }




    private Map getWorkflowParamMap() {
        Map map = new HashMap();
        map.put(MetaTemplateParameters.P_IDP, idpAlias);
        map.put(MetaTemplateParameters.P_SP, spAlias);
        map.put(MetaTemplateParameters.P_ATTR_AUTHORITY, attraAlias);
        map.put(MetaTemplateParameters.P_ATTR_QUERY_PROVIDER,
            attrqAlias);
        map.put(MetaTemplateParameters.P_AUTHN_AUTHORITY, authnaAlias);
        map.put(MetaTemplateParameters.P_AFFILIATION, affiAlias);
        map.put(MetaTemplateParameters.P_AFFI_OWNERID, affiOwnerID);
        map.put(MetaTemplateParameters.P_AFFI_MEMBERS, affiMembers);
        map.put(MetaTemplateParameters.P_PDP, pdpAlias);
        map.put(MetaTemplateParameters.P_PEP, pepAlias);
        map.put(MetaTemplateParameters.P_IDP_E_CERT, idpECertAlias);
        map.put(MetaTemplateParameters.P_IDP_S_CERT, idpSCertAlias);
        map.put(MetaTemplateParameters.P_SP_E_CERT, spECertAlias);
        map.put(MetaTemplateParameters.P_SP_S_CERT, spSCertAlias);
        map.put(MetaTemplateParameters.P_ATTR_AUTHORITY_E_CERT,
            attraECertAlias);
        map.put(MetaTemplateParameters.P_ATTR_AUTHORITY_S_CERT,
            attraSCertAlias);
        map.put(MetaTemplateParameters.P_ATTR_QUERY_PROVIDER_E_CERT,
            attrqECertAlias);
        map.put(MetaTemplateParameters.P_ATTR_QUERY_PROVIDER_S_CERT,
            attrqSCertAlias);
        map.put(MetaTemplateParameters.P_AUTHN_AUTHORITY_E_CERT,
            authnaECertAlias);
        map.put(MetaTemplateParameters.P_AUTHN_AUTHORITY_S_CERT,
            authnaSCertAlias);
        map.put(MetaTemplateParameters.P_AFFI_E_CERT,
            affiECertAlias);
        map.put(MetaTemplateParameters.P_AFFI_S_CERT,
            affiSCertAlias);
        map.put(MetaTemplateParameters.P_PDP_E_CERT, pdpECertAlias);
        map.put(MetaTemplateParameters.P_PDP_S_CERT, pdpSCertAlias);
        map.put(MetaTemplateParameters.P_PEP_E_CERT, pepECertAlias);
        map.put(MetaTemplateParameters.P_PEP_S_CERT, pepSCertAlias);
        return map;
    }
}
