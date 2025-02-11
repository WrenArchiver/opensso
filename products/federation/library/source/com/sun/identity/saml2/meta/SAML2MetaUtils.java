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
 * $Id: SAML2MetaUtils.java,v 1.9 2009-09-21 17:28:12 exu Exp $
 *
 */

package com.sun.identity.saml2.meta;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.jaxb.entityconfig.AttributeType;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.AttributeAuthorityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.AuthnAuthorityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.XACMLPDPDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.XACMLAuthzDecisionQueryDescriptorElement;
import com.sun.identity.saml2.jaxb.metadataextquery.AttributeQueryDescriptorElement;
/**
 * The <code>SAML2MetaUtils</code> provides metadata related util methods.
 */
public final class SAML2MetaUtils {
    protected static final String RESOURCE_BUNDLE_NAME = "libSAML2Meta";
    protected static ResourceBundle resourceBundle =
                         Locale.getInstallResourceBundle(RESOURCE_BUNDLE_NAME);
    public static Debug debug = Debug.getInstance("libSAML2");
    private static final String JAXB_PACKAGES =
        "com.sun.identity.saml2.jaxb.xmlenc:" +
        "com.sun.identity.saml2.jaxb.xmlsig:" +
        "com.sun.identity.saml2.jaxb.assertion:" +
        "com.sun.identity.saml2.jaxb.metadata:" +
        "com.sun.identity.saml2.jaxb.entityconfig:" +
        "com.sun.identity.saml2.jaxb.schema";
    private static final String JAXB_PACKAGE_LIST_PROP =
        "com.sun.identity.liberty.ws.jaxb.packageList";
    private static JAXBContext jaxbContext = null;
    private static final String PROP_JAXB_FORMATTED_OUTPUT =
                                        "jaxb.formatted.output";
    private static final String PROP_NAMESPACE_PREFIX_MAPPER =
                                    "com.sun.xml.bind.namespacePrefixMapper";

    private static NamespacePrefixMapperImpl nsPrefixMapper =
                                            new NamespacePrefixMapperImpl();
    static String jaxbPackages = null;

    static {
        try {
            String tmpJaxbPkgs = SystemPropertiesManager.get(
                JAXB_PACKAGE_LIST_PROP);
            if (tmpJaxbPkgs != null && tmpJaxbPkgs.length() > 0) {
                jaxbPackages = JAXB_PACKAGES + ":" + tmpJaxbPkgs;
            } else {
                jaxbPackages = JAXB_PACKAGES;
            }
            if (debug.messageEnabled()) {
                debug.message("SAML2MetaUtils.static: " + 
                    "jaxbPackages = " + jaxbPackages);
            }
            jaxbContext = JAXBContext.newInstance(jaxbPackages);
        } catch (JAXBException jaxbe) {
            debug.error("SAML2MetaUtils.static:", jaxbe);
        }
    }

    private SAML2MetaUtils() {
    }

    /**
     * Returns <code>JAXB</code> context for the metadata service.
     * @return <code>JAXB</code> context object.
     */
    public static JAXBContext getMetaJAXBContext() {

        return jaxbContext;
    }

    /**
     * Converts a <code>String</code> object to a JAXB object.
     * @param str a <code>String</code> object
     * @return a JAXB object converted from the <code>String</code> object.
     * @exception JAXBException if an error occurs while converting
     *                          <code>String</code> object
     */
    public static Object convertStringToJAXB(String str)
        throws JAXBException {

       Unmarshaller u = jaxbContext.createUnmarshaller();
       return u.unmarshal(new StreamSource(new StringReader(str)));
    }

    /**
     * Reads from the <code>InputStream</code> and converts to a JAXB object.
     * @param is a <code>InputStream</code> object
     * @return a JAXB object converted from the <code>InputStream</code> object.
     * @exception JAXBException if an error occurs while converting
     *                          <code>InputStream</code> object
     */
    public static Object convertInputStreamToJAXB(InputStream is)
        throws JAXBException {

       Unmarshaller u = jaxbContext.createUnmarshaller();
       return u.unmarshal(is);
    }

    /**
     * Converts a <code>Node</code> object to a JAXB object.
     * @param node a <code>Node</code> object
     * @return a JAXB object converted from the <code>Node</code> object.
     * @exception JAXBException if an error occurs while converting
     *                          <code>Node</code> object
     */
    public static Object convertNodeToJAXB(Node node)
        throws JAXBException {

       Unmarshaller u = jaxbContext.createUnmarshaller();
       return u.unmarshal(node);
    }

    /**
     * Converts a JAXB object to a <code>String</code> object.
     * @param jaxbObj a JAXB object
     * @return a <code>String</code> representing the JAXB object.
     * @exception JAXBException if an error occurs while converting JAXB object
     */
    public static String convertJAXBToString(Object jaxbObj)
        throws JAXBException {

        StringWriter sw = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(PROP_JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(PROP_NAMESPACE_PREFIX_MAPPER, nsPrefixMapper);
        marshaller.marshal(jaxbObj, sw);
        return sw.toString();
    }

    /**
     * Converts a JAXB object and writes to an <code>OutputStream</code> object.
     * @param jaxbObj a JAXB object
     * @param os an <code>OutputStream</code> object
     * @exception JAXBException if an error occurs while converting JAXB object
     */
    public static void convertJAXBToOutputStream(Object jaxbObj,
                                                 OutputStream os)
        throws JAXBException {

        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(PROP_JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(PROP_NAMESPACE_PREFIX_MAPPER, nsPrefixMapper);
        marshaller.marshal(jaxbObj, os);
    }

    /**
     * Converts a JAXB object to a <code>String</code> object and creates a
     * <code>Map</code>. The key is 'attrName' and the value is a
     * <code>Set</code> contains the <code>String</code> object.
     * @param attrName attribute name
     * @param jaxbObj a JAXB object
     * @return a <code>Map</code>. The key is 'attrName' and the value is a
     *         <code>Set</code> contains the <code>String</code> object
     *         converted from the JAXB object.
     * @exception JAXBException if an error occurs while converting JAXB object
     */
    protected static Map convertJAXBToAttrMap(String attrName, Object jaxbObj)
        throws JAXBException {

        String xmlString = convertJAXBToString(jaxbObj);
        Map attrs = new HashMap();
        Set values = new HashSet();
        values.add(xmlString);
        attrs.put(attrName, values);

        return attrs;
    }

    /**
     * Gets attribute value pairs from <code>BaseConfigType</code> and
     * put in a <code>Map</code>. The key is attribute name and the value is
     * a <code>List</code> of attribute values;
     * @param config the <code>BaseConfigType</code> object
     * @return a attrbute value <code>Map</code>
     */
    public static Map getAttributes(BaseConfigType config) {
        Map attrMap = new HashMap();
        List list = config.getAttribute();
        for(Iterator iter = list.iterator(); iter.hasNext();) {
            AttributeType avp = (AttributeType)iter.next();
            attrMap.put(avp.getName(), avp.getValue());
        }

        return attrMap;
    }

    /**
     * Returns the realm by parsing the metaAlias. MetaAlias format is
     * <pre>
     * &lt;realm>/&lt;any string without '/'> for non-root realm or
     * /&lt;any string without '/'> for root realm.
     * </pre>
     * @param metaAlias The metaAlias.
     * @return the realm associated with the metaAlias.
     */
    public static String getRealmByMetaAlias(String metaAlias) {
        if (metaAlias == null) {
            return null;
        }

        int index = metaAlias.lastIndexOf("/");
        if (index == -1 || index == 0) {
            return "/";
        }

        return metaAlias.substring(0, index);
    }

    /**
     * Returns metaAlias embedded in uri.
     * @param uri The uri string.
     * @return the metaAlias embedded in uri or null if not found.
     */
    public static String getMetaAliasByUri(String uri) {
        if (uri == null) {
            return null;
        }

        int index = uri.indexOf(SAML2MetaManager.NAME_META_ALIAS_IN_URI);
        if (index == -1 || index + 9 == uri.length()) {
            return null;
        }

        return uri.substring(index + 9);
    }

    /**
     * Returns first policy decision point descriptor in an entity descriptor.
     *
     * @param eDescriptor The entity descriptor.
     * @return policy decision point descriptor or null if it is not found. 
     */
    public static XACMLPDPDescriptorElement getPolicyDecisionPointDescriptor(
        EntityDescriptorElement eDescriptor)
    {
        XACMLPDPDescriptorElement descriptor = null;
        
        if (eDescriptor != null) {
            List list = 
            eDescriptor.getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();
            
            for (Iterator i = list.iterator(); 
                i.hasNext() && (descriptor == null);
            ) {
                Object obj = i.next();
                if (obj instanceof XACMLPDPDescriptorElement) {
                    descriptor = (XACMLPDPDescriptorElement)obj;
                }
            }
        }

        return descriptor;
    }


    /**
     * Returns first policy enforcement point descriptor in an entity 
     * descriptor.
     *
     * @param eDescriptor The entity descriptor.
     * @return policy enforcement point descriptor or null if it is not found. 
     */
    public static XACMLAuthzDecisionQueryDescriptorElement 
        getPolicyEnforcementPointDescriptor(
        EntityDescriptorElement eDescriptor)
    {
        XACMLAuthzDecisionQueryDescriptorElement descriptor = null;
        
        if (eDescriptor != null) {
            List list = 
            eDescriptor.getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();
            
            for (Iterator i = list.iterator(); 
                i.hasNext() && (descriptor == null);
            ) {
                Object obj = i.next();
                if (obj instanceof XACMLAuthzDecisionQueryDescriptorElement) {
                    descriptor = (XACMLAuthzDecisionQueryDescriptorElement)obj;
                }
            }
        }

        return descriptor;
    }
    
    /**
     * Returns first service provider's SSO descriptor in an entity
     * descriptor.
     * @param eDescriptor The entity descriptor.
     * @return <code>SPSSODescriptorElement</code> for the entity or null if
     *         not found. 
     */
    public static SPSSODescriptorElement getSPSSODescriptor(
        EntityDescriptorElement eDescriptor)
    {
        if (eDescriptor == null) {
            return null;
        }

        List list =
            eDescriptor.getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();
        for(Iterator iter = list.iterator(); iter.hasNext();) {
            Object obj = iter.next();
            // TODO: may need to cache to avoid using instanceof
            if (obj instanceof SPSSODescriptorElement) {
                return (SPSSODescriptorElement)obj;
            }
        }

        return null;
    }

    /**
     * Returns first identity provider's SSO descriptor in an entity
     * descriptor.
     * @param eDescriptor The entity descriptor.
     * @return <code>IDPSSODescriptorElement</code> for the entity or null if
     *         not found. 
     */
    public static IDPSSODescriptorElement getIDPSSODescriptor(
        EntityDescriptorElement eDescriptor)
    {
        if (eDescriptor == null) {
            return null;
        }

        List list =
            eDescriptor.getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();
        for(Iterator iter = list.iterator(); iter.hasNext();) {
            Object obj = iter.next();
            if (obj instanceof IDPSSODescriptorElement) {
                return (IDPSSODescriptorElement)obj;
            }
        }

        return null;
    }
    
    /**
     * Returns attribute authority descriptor in an entity descriptor.
     *
     * @param eDescriptor The entity descriptor.
     * @return an <code>AttributeAuthorityDescriptorElement</code> object for
     *     the entity or null if not found. 
     */
    public static AttributeAuthorityDescriptorElement
        getAttributeAuthorityDescriptor(EntityDescriptorElement eDescriptor)
    {
        if (eDescriptor == null) {
            return null;
        }

        List list =
            eDescriptor.getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();

        for(Iterator iter = list.iterator(); iter.hasNext();) {
            Object obj = iter.next();
            if (obj instanceof AttributeAuthorityDescriptorElement) {
                return (AttributeAuthorityDescriptorElement)obj;
            }
        }

        return null;
    }

    /**
     * Returns attribute query descriptor in an entity descriptor.
     *
     * @param eDescriptor The entity descriptor.
     * @return an <code>AttributeQueryDescriptorElement</code> object for
     *     the entity or null if not found. 
     */
    public static AttributeQueryDescriptorElement
        getAttributeQueryDescriptor(EntityDescriptorElement eDescriptor)
    {
        if (eDescriptor == null) {
            return null;
        }

        List list =
            eDescriptor.getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();

        for(Iterator iter = list.iterator(); iter.hasNext();) {
            Object obj = iter.next();
            if (obj instanceof AttributeQueryDescriptorElement) {
                return (AttributeQueryDescriptorElement)obj;
            }
        }

        return null;
    }

    /**
     * Returns authentication authority descriptor in an entity descriptor.
     *
     * @param eDescriptor The entity descriptor.
     * @return an <code>AuthnAuthorityDescriptorElement</code> object for
     *     the entity or null if not found. 
     */
    public static AuthnAuthorityDescriptorElement
        getAuthnAuthorityDescriptor(EntityDescriptorElement eDescriptor)
    {
        if (eDescriptor == null) {
            return null;
        }

        List list =
            eDescriptor.getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();

        for(Iterator iter = list.iterator(); iter.hasNext();) {
            Object obj = iter.next();
            if (obj instanceof AuthnAuthorityDescriptorElement) {
                return (AuthnAuthorityDescriptorElement)obj;
            }
        }

        return null;
    }

    /**
     * Get the first value of set by given key searching in the given map. 
     * return null if <code>attrMap</code> is null or <code>key</code> 
     * is null.
     *
     * @param attrMap Map of which set is to be added.
     * @param key Key of the entry to be added.
     * @return the first value of a matching set by the given key.
     */
    public static String getFirstEntry(Map attrMap, String key) {
        String retValue = null;

        if ((attrMap != null) && !attrMap.isEmpty()) {
            Set valueSet = (Set)attrMap.get(key);

            if ((valueSet != null) && !valueSet.isEmpty()) {
                retValue = (String)valueSet.iterator().next();
            }
        }

        return retValue;
    }
    
     /**
     * Adds a set of a given value to a map. Set will not be added if
     * <code>attrMap</code> is null or <code>value</code> is null or
     * <code>key</code> is null.
     *
     * @param attrMap Map of which set is to be added.
     * @param key Key of the entry to be added.
     * @param value Value to be added to the Set.
     */
    public static void fillEntriesInSet(Map attrMap, String key, String value) {
        if ((key != null) && (value != null) && (attrMap != null)) {
            Set valueSet = new HashSet(); 
            valueSet.add(value);
            attrMap.put(key, valueSet);
        }
    }
    
    /**
     * Returns first service provider's SSO configuration in an entity.
     * @param eConfig <code>EntityConfigElement</code> of the entity to 
     * be retrieved.
     * @return <code>SPSSOConfigElement</code> for the entity or null if not
     *         found.
     * @throws SAML2MetaException if unable to retrieve the first service
     *                            provider's SSO configuration.
     */
    public static SPSSOConfigElement getSPSSOConfig(EntityConfigElement eConfig)
        throws SAML2MetaException {

        if (eConfig == null) {
            return null;
        }

        List list =
            eConfig.getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
        for(Iterator iter = list.iterator(); iter.hasNext();) {
            Object obj = iter.next();
            if (obj instanceof SPSSOConfigElement) {
                return (SPSSOConfigElement)obj;
            }
        }

        return null;
    }

    /**
     * Returns first identity provider's SSO configuration in an entity
     * @param eConfig <code>EntityConfigElement</code> of the entity to 
     * be retrieved.
     * @return <code>IDPSSOConfigElement</code> for the entity or null if not
     *         found.
     * @throws SAML2MetaException if unable to retrieve the first identity
     *                            provider's SSO configuration.
     */
    public static IDPSSOConfigElement getIDPSSOConfig(
        EntityConfigElement eConfig) throws SAML2MetaException {
        if (eConfig == null) {
            return null;
        }

        List list =
            eConfig.getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
        for(Iterator iter = list.iterator(); iter.hasNext();) {
            Object obj = iter.next();
            if (obj instanceof IDPSSOConfigElement) {
                return (IDPSSOConfigElement)obj;
            }
        }

        return null;
    }

    public static String exportStandardMeta(String realm, String entityID,
	boolean sign)
	throws SAML2MetaException {

	try {
	    SAML2MetaManager metaManager = new SAML2MetaManager();
	    EntityDescriptorElement descriptor =
		metaManager.getEntityDescriptor(realm, entityID);

	    String xmlstr = null;
	    if (descriptor == null) {
		return null;
	    }

	    if (sign) {
		SPSSOConfigElement spConfig = metaManager.getSPSSOConfig(
		    realm, entityID);
		IDPSSOConfigElement idpConfig = metaManager.getIDPSSOConfig(
                    realm, entityID);
		Document doc = SAML2MetaSecurityUtils.sign(descriptor,
                    spConfig, idpConfig);
		if (doc != null) {
                    xmlstr = XMLUtils.print(doc);
		}
            }
            if (xmlstr == null) {
		xmlstr = convertJAXBToString(descriptor);
		xmlstr = SAML2MetaSecurityUtils.formatBase64BinaryElement(
                    xmlstr);
            }
            xmlstr = workaroundAbstractRoleDescriptor(xmlstr);
            return xmlstr;
	} catch (JAXBException e) {
            throw new SAML2MetaException(e.getMessage());
	}
    }

    private static String workaroundAbstractRoleDescriptor(String xmlstr) {
	int index =
	    xmlstr.indexOf(":" +SAML2MetaConstants.ATTRIBUTE_QUERY_DESCRIPTOR);
	if (index == -1) {
            return xmlstr;
	}

        int index2 = xmlstr.lastIndexOf("<", index);
	if (index2 == -1) {
            return xmlstr;
	}

        String prefix = xmlstr.substring(index2 + 1, index);
	String type =  prefix + ":" +
            SAML2MetaConstants.ATTRIBUTE_QUERY_DESCRIPTOR_TYPE;

	xmlstr = xmlstr.replaceAll("<" + prefix + ":" +
            SAML2MetaConstants.ATTRIBUTE_QUERY_DESCRIPTOR,
            "<" + SAML2MetaConstants.ROLE_DESCRIPTOR + " " +
            SAML2Constants.XSI_DECLARE_STR + " xsi:type=\"" + type + "\"");
	xmlstr = xmlstr.replaceAll("</" + prefix + ":" +
           SAML2MetaConstants.ATTRIBUTE_QUERY_DESCRIPTOR,
            "</" + SAML2MetaConstants.ROLE_DESCRIPTOR);
	return xmlstr;
    }
}
