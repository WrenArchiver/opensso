/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SAML2AssertionValidator.java,v 1.2 2008-06-25 05:50:08 qcheng Exp $
 *
 */

package com.sun.identity.wss.security;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import org.w3c.dom.Element;

import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.impl.AssertionImpl;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.assertion.AuthnStatement;
import com.sun.identity.saml2.assertion.AttributeStatement;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.assertion.SubjectLocality;
import com.sun.identity.saml2.assertion.AuthnContext;
import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.assertion.SubjectConfirmation;
import com.sun.identity.saml2.assertion.SubjectConfirmationData;
import com.sun.identity.shared.xml.XMLUtils;
import java.security.cert.X509Certificate;

/**
 * This class validates SAML2 Assertions using local configuration.
 */
public class SAML2AssertionValidator {
    
  //  private Set trustedIssuers = null;
    private Map attributeMap = null;
    private String subjectName = null;    
    private Map config = null;
    private static final String TRUSTED_ISSUERS = "trustedIssuers";
    private static final String TRUSTED_IP_ADDRESSES = "trustedIPAddresses";
    private X509Certificate cert = null;
    
          
    public SAML2AssertionValidator(Element assertionE,
            Map config) throws SecurityException {
                
        this.config = config;
        if(config == null) {
           throw new SecurityException(
                   WSSUtils.bundle.getString("config is null"));
        }
        
        try {
            Assertion samlAssertion = new AssertionImpl(assertionE);
            if(!samlAssertion.isSigned()) {
               throw new SecurityException("assertion is not signed"); 
            }
                                    
            Issuer issuer = samlAssertion.getIssuer();
            String issuerID = issuer.getValue();
            if(issuerID == null) {
               throw new SecurityException(
                       WSSUtils.bundle.getString("issuer is null"));
            }
            Set trustedIssuers = (Set)config.get(TRUSTED_ISSUERS);
            if(trustedIssuers != null &&
                    !trustedIssuers.contains(issuerID)) {
               throw new SecurityException(
                       WSSUtils.bundle.getString("issuer is not trusted"));
            }
                       
            if(!samlAssertion.isTimeValid()) {               
               throw new SecurityException("assertion time is not valid");               
            }
            
            Subject subject = samlAssertion.getSubject(); 
            if(subject == null) {
               throw new SecurityException("subject is null");
            }
            
            subjectName = subject.getNameID().getValue();
            if(subjectName == null) {
               throw new SecurityException("subject is null"); 
            }
            Element keyInfo = getKeyInfo(subject);
            if(keyInfo != null) {
               cert = WSSUtils.getCertificate(keyInfo);
            }            
                  
            List authnStatements = samlAssertion.getAuthnStatements();            
            if(authnStatements != null && !authnStatements.isEmpty()) {
               validateAuthnStatement((AuthnStatement)authnStatements.get(0));
            }
            
            List attributeStatements = samlAssertion.getAttributeStatements();
            if(attributeStatements != null && !attributeStatements.isEmpty()) {
               validateAttributeStatement(
                       (AttributeStatement)attributeStatements.get(0)); 
            }
                        
        } catch (SAML2Exception se) {
            throw new SecurityException(se.getMessage());  
        }                
    }
    
    private void validateAuthnStatement(AuthnStatement authnStatement)
            throws SecurityException {
        
        SubjectLocality subjectLocality = authnStatement.getSubjectLocality();
        /* TODO validate using valid IP Addresses
        if(subjectLocality != null) {
           String ipAddress = subjectLocality.getAddress();                 
           if(ipAddress != null && 
                !config.getTrustedIPAddresses().contains(ipAddress)) {
              throw new SecurityException("invalid ip address");           
           }
        }*/
        
        // TODO  we need to have valid auth context refs and the corresponding
        // authmethod/authlevel mappings .
        AuthnContext authnContext = authnStatement.getAuthnContext();
        if(authnContext != null) {
           String authnContextRef = authnContext.getAuthnContextClassRef();
        }
       
        
    }
    
    private void validateAttributeStatement(
             AttributeStatement attributeStatement) throws SecurityException {
                  
        List<Attribute> attributes = attributeStatement.getAttribute();
        if(!attributes.isEmpty()) {
           attributeMap = new HashMap(); 
        }
        for (Iterator iter = attributes.iterator(); iter.hasNext();) {
             Attribute attribute = (Attribute)iter.next();                
             String attrName = attribute.getName();             
             List <java.lang.String>  values = 
                        attribute.getAttributeValue();
             if(values != null && !values.isEmpty()) {
                String value = (String)values.get(0);
                Element attrElement = XMLUtils.toDOMDocument(value, 
                        WSSUtils.debug).getDocumentElement();
                String attrValue = XMLUtils.getElementValue(attrElement);
                if(attrValue != null && attrValue.length() != 0) {
                   attributeMap.put(attrName, value);
                }
             }            
        }       
    }
    
    public Map getAttributes() {
        return attributeMap;
    }
    
    public String getSubjectName() {
        return subjectName;
    }
    
    public X509Certificate getKeyInfoCert() {
        return cert;
    }
    private Element getKeyInfo(Subject subject) {
        
         List list = subject.getSubjectConfirmation();
         if(list == null || list.isEmpty()) {
            return null;
         }
         SubjectConfirmation subjConfirmation =
                           (SubjectConfirmation)list.get(0);
         SubjectConfirmationData confirmationData = 
                 subjConfirmation.getSubjectConfirmationData();
         if(confirmationData == null) {
            return null;
         }
         List content =  confirmationData.getContent();
         if(content == null || content.isEmpty()) {
               return null;
         }            
         return (Element)content.get(0);        
    }

}
