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
 * $Id: FAMSTSAttributeProvider.java,v 1.6 2008-03-08 03:03:19 mallas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.wss.sts.spi;

import javax.security.auth.Subject;
import com.sun.xml.ws.api.security.trust.*;
import java.security.Principal;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import javax.xml.namespace.QName;
import java.security.AccessController;
import java.security.PrivilegedAction;
import com.iplanet.security.x509.CertUtils;
import java.security.cert.X509Certificate;
import org.w3c.dom.Element;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.wss.sts.STSConstants;
import com.sun.identity.wss.sts.STSUtils;
import com.sun.identity.wss.sts.STSClientUserToken;
import com.sun.identity.wss.sts.FAMSTSException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.wss.provider.ProviderConfig;
import com.sun.identity.wss.sts.ClientUserToken;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.wss.security.WSSUtils;
import com.sun.identity.shared.datastruct.CollectionHelper;

/**
 * The STS attribute provider is used to retrieve an authenticated user or
 * profile attributes and gives it to the assertion generator so that
 * these attributes could be part of SAML attribute statements. 
 *
 * The attribute checks first if the end user's SSOToken is present in the
 * <code>OnBehalfOf</code> element in the WS-Trust request and generates
 * SAML Attributes from the user profile. This is the case usually if the STS
 * and web services client is deployed locally on the same or trusted Federal
 * Access Manager instances. If not, it tries to retrieve the web services 
 * client profile attributes if it exists.
 */ 
public class FAMSTSAttributeProvider implements STSAttributeProvider {

    private static final String FAM_TOKEN = "FAMToken";
    private static final String SAML_ATTRIBUTE_MAP = "SAMLAttributeMapping"; 
    private static final String NAMEID_MAPPER_CLASS = "NameIDMapper";
    private static final String ATTR_NAMESPACE = "AttributeNamespace";
    private static final String STS = "sts";
    private static final String MEMBERSHIPS = "Memberships";
    private static final String INCLUDE_MEMBERSHIPS = "includeMemberships";
    private static final String defaultNS = "http://example.com";
    
    private SSOToken ssoToken = null;
    
    /**
     * Returns all claimed attributes for a given subject.
     */
    public Map<QName, List<String>> getClaimedAttributes(Subject subject, 
            String appliesTo, String tokenType, Claims claims) {

        String subjectName = getAuthenticatedSubject(subject);
        if(subjectName == null) {
           subjectName = getSubjectNameFromCustomToken(subject);
           if(STSUtils.debug.messageEnabled()) {
              STSUtils.debug.message("FAMSTSAttributeProvider.getClaimed" +
              "Attributes: subject is null from authenticated subject");
           }
        }
        
        if(subjectName == null) {            
           if(STSUtils.debug.messageEnabled()) {
              STSUtils.debug.message("FAMSTSAttributeProvider.getClaimed" +
              "Attributes: subject is null from custom tokens");
           }
           Iterator iter = subject.getPublicCredentials().iterator();
           Object  object = iter.next();
           if(object instanceof X509Certificate) {
              X509Certificate cert = (X509Certificate)object;
              subjectName = CertUtils.getSubjectName(cert); 
           }
        }

        if(subjectName == null) {
           if(STSUtils.debug.messageEnabled()) {
              STSUtils.debug.message("FAMSTSAttributeProvider.getClaimed" +
              "Attributes: subject from X509certificate is null" +
              " Checking in subject principals");
           }
           Set<Principal> principals = subject.getPrincipals();
           if (principals != null){
               final Iterator iterator = principals.iterator();
               while (iterator.hasNext()){
                    String cnName = principals.iterator().next().getName();
                    int pos = cnName.indexOf("=");
                    subjectName = cnName.substring(pos+1);
                    break;
               }       
           }
        }

        if(subjectName == null) {
           STSUtils.debug.error("FAMSTSAttributeProvider.getClaimed" + 
                " Subject could not found.");
           return null;
        }
        
	Map<QName, List<String>> attrs = new HashMap<QName, List<String>>();
        Map agentConfig = STSUtils.getAgentAttributes(
                 appliesTo, null, ProviderConfig.WSP);
        if(agentConfig == null || agentConfig.isEmpty()) {
           STSUtils.debug.error("FAMSTSAttributeProvider.getClaimed" + 
                " Agent configuration not defined for " + appliesTo);           
           return attrs;
        }

        Set tmp = (Set)agentConfig.get(ATTR_NAMESPACE);
        String namespace = (String)tmp.iterator().next();
        if(namespace == null) {
           if(STSUtils.debug.messageEnabled()) {
              STSUtils.debug.message("FAMSTSAttributeProvider.getClaimed" +
              "Attributes: Namespace is not defined.");
           }
           namespace = defaultNS;
        }

	QName nameIdQName = new QName(namespace, 
                            STSAttributeProvider.NAME_IDENTIFIER);
	List<String> nameIdAttrs = new ArrayList<String>();
	nameIdAttrs.add(getUserPseduoName(subjectName, agentConfig));
	attrs.put(nameIdQName, nameIdAttrs);

        tmp = (Set)agentConfig.get(SAML_ATTRIBUTE_MAP);
        Map samlAttributeMap = null;
        if(tmp != null && !tmp.isEmpty()) {
           samlAttributeMap = getSAMLAttributes(subjectName, tmp, namespace);
        }
        if(samlAttributeMap != null) {
           attrs.putAll(samlAttributeMap);
        }

        boolean includeMemberships = Boolean.valueOf(
                CollectionHelper.getMapAttr(
                agentConfig, INCLUDE_MEMBERSHIPS, "false")).booleanValue(); 
        if(includeMemberships){
           Map memberships = getMemberships(subjectName, namespace); 
           if(memberships != null && !memberships.isEmpty()) {
              attrs.putAll(memberships);
           }
        }
       
	return attrs;
    }
    
    private String getUserPseduoName(String userName, Map agentConfig){
      
        String nameIDImpl = CollectionHelper.getMapAttr(
                            agentConfig, NAMEID_MAPPER_CLASS); 
        if(nameIDImpl == null) {
           return userName;
        }

        try {
            Class nameIDImplClass = Class.forName(nameIDImpl);
            NameIdentifierMapper niMapper = 
                    (NameIdentifierMapper)nameIDImplClass.newInstance(); 
            return niMapper.getUserPsuedoName(userName);
        } catch (Exception ex) {
            STSUtils.debug.error("FAMSTSAttributeProvider.getUserPseduoName: "+
            " Exception", ex);
        }
        return userName;
    }

    /**
     * Returns SAML Attributes for attribute statements
     * @param subjectName
     * @param attributeMap
     * @param namespace
     * @return map of saml attribute mappings
     */
    private Map<QName, List<String>> getSAMLAttributes(String subjectName,
                  Set attributeMap, String namespace) {

        Map<QName, List<String>> map = new HashMap();
        AMIdentity amId = null;
        try {
            amId = new AMIdentity(WSSUtils.getAdminToken(), subjectName);
            if(!amId.isExists()) {
               if(STSUtils.debug.messageEnabled()) {
                  STSUtils.debug.message("FAMSTSAttributeProvider.getSAML" +
                  "Attributes: Subject " + subjectName + " does not exist");
               }
            }
        } catch (Exception ex) {
            STSUtils.debug.error("FAMSTSAttributeProvider.getSAML" +
                    "Attributes: read subject error", ex);
            return null;
        }

        for (Iterator iter = attributeMap.iterator(); iter.hasNext();) {
             String attribute = (String)iter.next();
             if(attribute.indexOf("=") != -1) {
                StringTokenizer st = new StringTokenizer(attribute, "=");
                if(st.countTokens() != 2) {
                   continue;
                }
                String samlAttribute = st.nextToken();
                String realAttribute = st.nextToken();
                Set values = new HashSet();
                boolean attributeFoundInSSOToken = false;
                if(ssoToken != null) {
                   try {
                       String attributeValue = 
                               ssoToken.getProperty(realAttribute);
                       if(attributeValue != null) {
                          values.add(attributeValue);
                          attributeFoundInSSOToken = true;
                       }
                   } catch (SSOException se) {
                       if(STSUtils.debug.messageEnabled()) {
                          STSUtils.debug.message("FAMSTSAttributeProvider.get"+
                                  "SAMLAttributes: SSOException", se);
                       }
                   }
                }
                
                if(!attributeFoundInSSOToken) {
                   try {
                       values = amId.getAttribute(realAttribute);
                   } catch (Exception ex) {
                       STSUtils.debug.error("FAMSTSAttributeProvider.getSAML" +
                           "Attributes: read attributes error", ex);
                   }
                }
                
                if(values == null || values.isEmpty()) {
                   if(STSUtils.debug.messageEnabled()) {
                      STSUtils.debug.message("FAMSTSAttributeProvider.get"+
                           "SAMLAttributes: attribute value not found for" +
                           realAttribute);
                   }
                   continue;
                }
                List<String> list = new ArrayList();
                list.addAll(values);
                QName qName = new QName(namespace, samlAttribute);
                map.put(qName, list);
             }
        }
        return map; 
    }
    
    /**
     * Returns end user's principal if FAM Token is present or any other
     * custom token, otherwise returns null.
     */
    private String getSubjectNameFromCustomToken(Subject subject) {
        Iterator iter = subject.getPublicCredentials().iterator();
        while(iter.hasNext()) {
            Object  object = iter.next();
            if(object instanceof Element) {
               Element credential = (Element)object;
               if(credential.getLocalName().equals(FAM_TOKEN)) {
                  try {
                      ssoToken = (SSOToken)credential;
                      STSClientUserToken userToken =
                          new STSClientUserToken(credential);
                      String tokenId = userToken.getTokenId();
                      if(userToken.getType().equals(
                                     STSConstants.SSO_TOKEN_TYPE)) {
                         return userToken.getPrincipalName();
                      }
                  } catch (FAMSTSException fae) {
                      if(STSUtils.debug.messageEnabled()) {
                         STSUtils.debug.message("FAMSTSAttributeProvider.get" +
                         "SubjectNameFromCustomToken: FAMException", fae);
                      }
                  }
               } else {
                  String customToken = SystemConfigurationUtil.getProperty(
                             STSConstants.STS_CLIENT_USER_TOKEN_PLUGIN); 
                  if(customToken != null) {
                     try {
                         Class customTokenClass = Class.forName(customToken);  
                         ClientUserToken userToken = (ClientUserToken)
                                      customTokenClass.newInstance();
                         return userToken.getPrincipalName();
                     } catch (Exception ex) {
                         STSUtils.debug.error("FAMSTSAttributeProvider.check" +
                             "ForCustomTokens: Exception", ex);
                     }
                  }
               }
            }
        }
        return null;
    }

    private Map<QName, List<String>>  getMemberships(
               String subjectName, String namespace) {
        Map<QName, List<String>> map = new HashMap();
        List<String> roles = WSSUtils.getMemberShips(subjectName);
        if(roles == null || roles.isEmpty()) {
           return map;
        }

        QName qName = new QName(namespace, MEMBERSHIPS);
        map.put(qName, roles);
        return map;
    }
    
    /**
     *  Returns the principal from the authenticated Subject if available
     *  through private credentials
     * @param subject authenticated subject
     * @return the authenticated principal name
     */
    private String getAuthenticatedSubject(Subject subject) {        
        final Subject sub = subject;
        try {                
            AccessController.doPrivileged(new PrivilegedAction() {
                public java.lang.Object run() {
                   Set creds = sub.getPrivateCredentials();
                   if(creds != null && !creds.isEmpty()) {
                      for(Iterator iter = creds.iterator(); iter.hasNext();) {
                          Object cred = iter.next();
                          if(cred instanceof SSOToken) {
                             ssoToken = (SSOToken)cred;
                          }
                      }
                   }
                   return null;
                }
            });               
                
        } catch (Exception e) {
            STSUtils.debug.error("FAMSTSAttributeProvider.getAuthenticated" +
                   "Subject: Priveleged exception error", e);
            return null;                    
        }
        try {
            if(ssoToken != null) {
               return ssoToken.getPrincipal().getName(); 
            }
        } catch (SSOException se) {
           STSUtils.debug.error("FAMSTSAttributeProvider.getAuthenticated" +
                   "Subject: SSOException", se);
        }
        return null;
    }
}
