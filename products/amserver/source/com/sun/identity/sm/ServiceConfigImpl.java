/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ServiceConfigImpl.java,v 1.8 2008-06-25 05:44:05 qcheng Exp $
 *
 */

package com.sun.identity.sm;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * The class <code>ServiceConfigImpl</code> provides interfaces to read the
 * configuration information of a service configuration. It provides methods to
 * get configuration parameters for this service configuration.
 */
class ServiceConfigImpl implements ServiceListener {

    private ServiceConfigManagerImpl scm;

    private ServiceSchemaImpl ss;

    private SSOToken token;

    private boolean globalConfig;

    private boolean newEntry;

    private String serviceComponentName;

    private String orgName;

    private String groupName;

    private String compName;

    private String configID;

    private int priority;

    private Map attributes;

    private Map attributesWithoutDefaults;

    private CachedSMSEntry smsEntry;

    private CachedSubEntries subEntries = null;

    private String labeledUri = null;

    /**
     * Private constructor
     */
    private ServiceConfigImpl(ServiceConfigManagerImpl scm,
            ServiceSchemaImpl ss, CachedSMSEntry entry, String orgName,
            String groupName, String compName, boolean globalConfig,
            SSOToken token) throws SMSException, SSOException {
        this.scm = scm;
        this.ss = ss;
        smsEntry = entry;
        smsEntry.addServiceListener(this);
        // Add this object as a listener to schema changes in this service.
        ServiceSchemaManagerImpl ssmi = scm.getServiceSchemaManagerImpl();
        ssmi.addListener(this);
        this.orgName = (orgName == null) ? SMSEntry.baseDN : orgName;
        this.groupName = groupName;
        if (compName == null || compName.equals("")) {
            this.compName = "";
            serviceComponentName = "/";
        } else {
            this.compName = compName.substring(compName.lastIndexOf('/') + 1);
            serviceComponentName = compName;
        }
        this.globalConfig = globalConfig;

        // Read the attributes
        update();

        // Cache the SSOToken to constrct cached-sub-entries
        this.token = token;
    }

    /**
     * Returns the component name
     */
    String getComponentName() {
        return (serviceComponentName);
    }

    /**
     * Returns the configuration's schema ID
     */
    String getSchemaID() {
        return (configID);
    }

    /**
     * Returns the group name
     */
    String getGroupName() {
        return (groupName);
    }

    /**
     * Returns the organization name
     */
    String getOrganizationName() {
        return (orgName);
    }

    /**
     * Returns the priority assigned to the service configuration.
     */
    int getPriority() {
        return (priority);
    }

    /**
     * Returns the labeled uri assigned to the service configuration.
     */
    String getLabeledUri() {
        return (labeledUri);
    }

    /**
     * Returns the names of all service's sub-configurations.
     */
    Set getSubConfigNames(SSOToken t) throws SMSException, SSOException {
        if (subEntries == null)
            subEntries = CachedSubEntries.getInstance(token, smsEntry.getDN());
        return (subEntries.getSubEntries(t));
    }

    /**
     * Returns the names of service's sub-configurations that match the given
     * pattern.
     */
    Set getSubConfigNames(SSOToken token, String pattern) throws SMSException,
            SSOException {
        if (subEntries == null)
            subEntries = CachedSubEntries.getInstance(token, smsEntry.getDN());
        return (subEntries.getSubEntries(token, pattern));
    }

    /**
     * Returns the names of service's sub-configurations that match the given
     * pattern.
     */
    Set getSubConfigNames(String pattern, String serviceidPattern)
            throws SMSException, SSOException {
        if (subEntries == null)
            subEntries = CachedSubEntries.getInstance(token, smsEntry.getDN());
        return (subEntries.getSchemaSubEntries(pattern, serviceidPattern));
    }

    /**
     * Returns the service's sub-configuration given the service's
     * sub-configuration name.
     */
    ServiceConfigImpl getSubConfig(SSOToken token, String subConfigName)
            throws SSOException, SMSException {
        // Construct subconfig DN
        subConfigName = SMSSchema.unescapeName(subConfigName);
        String sdn = "ou=" + subConfigName + "," + smsEntry.getDN();

        // Construct ServiceConfigImpl
        return (ServiceConfigImpl.getInstance(token, scm, null, sdn, orgName,
                groupName, (serviceComponentName + "/" + SMSSchema
                        .escapeSpecialCharacters(subConfigName)), globalConfig,
                ss));
    }

    /**
     * Returns the service configuration parameters. The keys in the
     * <code>Map</code> contains the attribute names and their corresponding
     * values in the <code>Map</code> is a <code>Set</code> that contains
     * the values for the attribute.
     */
    Map getAttributes() {
        if (!SMSEntry.cacheSMSEntries) {
            // Read the entry, since it should not be cached
            update();
        }
        return (SMSUtils.copyAttributes(attributes));
    }

    /**
     * Returns the service configuration parameters for read only.
     * The keys in the <code>Map</code> contains the attribute names and
     * their corresponding values in the <code>Map</code> is a
     * <code>Set</code> that contains the values for the attribute.
     */
    Map getAttributesForRead() {
        if (!SMSEntry.cacheSMSEntries) {
            // Read the entry, since it should not be cached
            update();
        }
        return (attributes);
    }

    /**
     * Returns the service configuration parameters. The keys in the
     * <code>Map</code> contains the attribute names and their corresponding
     * values in the <code>Map</code> is a <code>Set</code> that contains
     * the values for the attribute. attributes returned by this method are the
     * ones which do not include default values from the service schema
     */
    Map getAttributesWithoutDefaults() {
        if (!SMSEntry.cacheSMSEntries) {
            // Read the entry, since it should not be cached
            update();
        }
        return (SMSUtils.copyAttributes(attributesWithoutDefaults));
    }

    /**
     * Returns the service configuration parameters for read only.
     * The keys in the
     * <code>Map</code> contains the attribute names and their
     * corresponding values in the <code>Map</code> is a
     * <code>Set</code> that contains the values for the attribute.
     * attributes returned by this method are the ones which do not
     * include default values from the service schema
     */
    Map getAttributesWithoutDefaultsForRead() {
        if (!SMSEntry.cacheSMSEntries) {
            // Read the entry, since it should not be cached
            update();
        }
        return (attributesWithoutDefaults);
    }

    /**
     * Returns the DN associated with this entry
     */
    String getDN() {
        return (smsEntry.getDN());
    }

    /**
     * Returns the SMSEntry associated with this object
     */
    SMSEntry getSMSEntry() {
        return (smsEntry.getClonedSMSEntry());
    }

    /**
     * Updates the SMSEntry with the new changes
     */
    void refresh(SMSEntry e) throws SMSException {
        smsEntry.refresh(e);
    }

    /**
     * Returns the ServiceSchemaImpl assicated with this object
     */
    ServiceSchemaImpl getServiceSchemaImpl() {
        return (ss);
    }

    /**
     * Checks if the entry exists in the directory
     */
    boolean isNewEntry() {
        return (newEntry);
    }

    void updateAndNotifyListeners() {
        update();
    }

    void update() {
        // Get the SMSEntry
        SMSEntry entry = smsEntry.getSMSEntry();
        newEntry = entry.isNewEntry();

        // Read the attributes
        Map origAttributes = SMSUtils.getAttrsFromEntry(entry);
        Map origAttributesWithoutDefaults = SMSUtils.getAttrsFromEntry(entry);
        // Add default values, if attribute not present
        // and decrypt password attributes
        String validate = ss.getValidate();
        if ((validate == null) || validate.equalsIgnoreCase("yes")) {
            Set asNames = ss.getAttributeSchemaNames();
            // Remove attributes that do not exist in the schema.
            Set oldSet = origAttributes.keySet();
            Set removeAttrs = new HashSet();
            Iterator it = oldSet.iterator();
            while (it.hasNext()) {
                String tName = (String) it.next();
                if (!asNames.contains(tName)) {
                    // attributes.remove(tName);
                    // attributesWithoutDefaults.remove(tName);
                    removeAttrs.add(tName);
                }
            }
            it = removeAttrs.iterator();
            while (it.hasNext()) {
                String t = (String) it.next();
                origAttributes.remove(t);
                origAttributesWithoutDefaults.remove(t);
            }
            Iterator ass = asNames.iterator();
            while (ass.hasNext()) {
                AttributeValidator av = ss.getAttributeValidator((String) ass
                        .next());
                origAttributes = av.inheritDefaults(origAttributes);
                origAttributesWithoutDefaults = av
                        .decodeEncodedAttrs(origAttributesWithoutDefaults);
            }
        } // if (validate....)

        // Read the priority
        priority = 0;
        String priorities[] = smsEntry.getSMSEntry().getAttributeValues(
                SMSEntry.ATTR_PRIORITY);
        if (priorities != null) {
            try {
                priority = Integer.parseInt(priorities[0]);
            } catch (NumberFormatException nfe) {
                SMSEntry.debug.error("ServiceConfig::getPriority() " + nfe);
            }
        }

        // Read the service ID
        String[] ids = entry.getAttributeValues(SMSEntry.ATTR_SERVICE_ID);
        if (ids != null) {
            configID = ids[0];
        } else {
            configID = compName;
        }

        // Read the labeled uri
        String[] lUri = entry.getAttributeValues(SMSEntry.ATTR_LABELED_URI);
        if (lUri != null) {
            labeledUri = lUri[0];
        }

        // Replace the class instance attribute Maps
        attributes = Collections.unmodifiableMap(origAttributes);
        attributesWithoutDefaults = Collections.unmodifiableMap(
            origAttributesWithoutDefaults);
    }

    // ------------------------------------------------------------------
    // Static Protected method to get an instance of ServiceConfigImpl
    // ------------------------------------------------------------------
    // Method called by ServiceConfigManagerImpl to Global or Organization
    // service configuration
    static ServiceConfigImpl getInstance(SSOToken token,
            ServiceConfigManagerImpl scm, ServiceSchemaImpl ss, String dn,
            String oName, String groupName, String compName,
            boolean globalConfig) throws SSOException, SMSException {
        return getInstance(token, scm, ss, dn, oName, groupName, compName,
                globalConfig, null);
    }

    // Method called by ServiceConfigImpl to get sub-service configuration
    static ServiceConfigImpl getInstance(SSOToken token,
        ServiceConfigManagerImpl scm, ServiceSchemaImpl ss, String dn,
        String oName, String groupName, String compName,
        boolean globalConfig, ServiceSchemaImpl parentSS)
        throws SSOException, SMSException {
        if (debug.messageEnabled()) {
            debug.message("ServiceConfigImpl::getInstance: called: " + dn);
        }

        // Get required parameters
        String orgName = DNMapper.orgNameToDN(oName);
        String cacheName = getCacheName(scm.getName(), scm.getVersion(),
            orgName, groupName, compName, globalConfig);

        // Check cache for the object, if present return
        ServiceConfigImpl answer = null;
        synchronized(configMutex) {
            answer = getFromCache(cacheName, token);
        }
        if (answer != null ) {
            // Check if the entry has to be updated
            if (!SMSEntry.cacheSMSEntries) {
                // Read the entry, since it should not be cached
                answer.update();
            }
            return (answer);
        }

        // Since entry not in cache, first check if the orgName exists
        if (!SMSEntry.checkIfEntryExists(DNMapper.orgNameToDN(orgName), token)) {
            if (debug.warningEnabled()) {
                debug.warning("ServiceConfigImpl::getInstance called with " +
                    "non existant organization name: " + orgName);
            }
            // Object [] args = { orgName };
            // throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                // "sms-invalid-org-name", args));
            return (null);
        }

        // Not in cache, construct SMSEntry object
        CachedSMSEntry entry = checkAndUpdatePermission(cacheName, dn, token);

        // Since entry not in cache, check if service schema exists
        if (ss == null) {
            // Need to get the sub-schema name
            String subConfigId = null;
            SMSEntry sentry = entry.getSMSEntry();
            String[] ids = sentry.getAttributeValues(
                SMSEntry.ATTR_SERVICE_ID);
            if (ids != null) {
                subConfigId = ids[0];
            } else {
                // Get configId from sub config name
                int index = compName.lastIndexOf('/');
                subConfigId = compName.substring(index + 1);
            }
            // Get the schema from the parent
            if (parentSS != null) {
                ss = parentSS.getSubSchema(subConfigId);
            }
            // Return null if schema is not defined
            if (ss == null) {
                return (null);
            }
        }

        // Not in cache, construct service config impl
        answer = new ServiceConfigImpl(scm, ss, entry, orgName,
            groupName, compName, globalConfig, token);

        // Add to cache
        synchronized (configMutex) {
            // Check if already added by another thread
            ServiceConfigImpl tmp = getFromCache(cacheName, null);
            if (tmp == null) {
                configImpls.put(cacheName, answer);
            } else {
                answer = tmp;
            }
        }

        if (debug.messageEnabled()) {
            debug.message("ServiceConfigImpl::getInstance: return: " + dn);
        }
        return (answer);
    }

    // Method called by ServiceConfig to delete sub-service configuration
    // key from the cache when removeSubConfig is called.
    static void deleteInstance(SSOToken token,
        ServiceConfigManagerImpl scm, ServiceSchemaImpl ss, String dn,
        String oName, String groupName, String compName,
        boolean globalConfig, ServiceSchemaImpl parentSS)
        throws SSOException, SMSException {

        if (debug.messageEnabled()) {
            debug.message("ServiceConfigImpl::deleteInstance: called: dn: " 
                + dn + " Org Name: " + oName + " Group Name: " + groupName
                + " Component Name: "+ compName);
        }

        // Get required parameters
        String orgName = DNMapper.orgNameToDN(oName);
        String cacheName = getCacheName(scm.getName(), scm.getVersion(),
            orgName, groupName, compName, globalConfig);

        if (debug.messageEnabled()) {
            debug.message("ServiceConfigImpl::deleteInstance: cacheName: " + 
                    cacheName);
        }
        synchronized(configMutex) {
            // Check if the cachename/key to be removed is in the configImpls 
            // cache and delete from cache if it exists.
            if (configImpls.containsKey(cacheName)) {
                configImpls.remove(cacheName);
            }
        }
        if (debug.messageEnabled()) {
            debug.message("ServiceConfigImpl::deleteInstance: deleted: " + dn);
        }
    }

    // This function is executed after obtaining "configMutex" lock
    static ServiceConfigImpl getFromCache(String cacheName, SSOToken t)
        throws SMSException, SSOException {
        ServiceConfigImpl answer =
            (ServiceConfigImpl) configImpls.get(cacheName);
        if ((answer != null) && ((!answer.smsEntry.isValid()) ||
           (answer.smsEntry.isNewEntry()))) {
            answer = null;
        }
        if ((answer != null) && (t != null)) {
            // Check if the user has permissions
            Set principals = (Set) userPrincipals.get(cacheName);
            if (!principals.contains(t.getPrincipal().getName())) {
                // Principal name not in cache, need to check perm
                answer = null;
            }
        }
        return (answer);        
    }

    static CachedSMSEntry checkAndUpdatePermission(
        String cacheName, String dn, SSOToken t)
        throws SMSException, SSOException {
        CachedSMSEntry answer = CachedSMSEntry.getInstance(t, dn, null);
        synchronized (configMutex) {
            Set sudoPrincipals = (Set) userPrincipals.get(cacheName);
            if (sudoPrincipals == null) {
                sudoPrincipals = new HashSet(2);
                userPrincipals.put(cacheName, sudoPrincipals);
            }
            sudoPrincipals.add(t.getPrincipal().getName());
        }
        return (answer);
    }

    // Clears the cache
    static void clearCache() {
        configImpls = new HashMap();
        userPrincipals = new HashMap();
    }

    static String getCacheName(String sName, String version, String oName,
            String gName, String cName, boolean global) {
        StringBuffer sb = new StringBuffer(100);
        sb.append(sName).append(version).append(oName).append(gName).append(
                cName).append(global);
        return (sb.toString().toLowerCase());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.sm.ServiceListener#globalConfigChanged(
     *      java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String, int)
     */
    public void globalConfigChanged(String serviceName, String version,
            String groupName, String serviceComponent, int type) {
        // do nothing, the update method for this is called by the internal
        // notification mechanism in CachedSMSEntry

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.sm.ServiceListener#organizationConfigChanged(
     *      java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String,
     *      java.lang.String, int)
     */
    public void organizationConfigChanged(String serviceName, String version,
            String orgName, String groupName, String serviceComponent, int type)
    {
        // do nothing. Same as "globalConfigChanged"
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.sm.ServiceListener#schemaChanged(java.lang.String,
     *      java.lang.String)
     */
    public void schemaChanged(String serviceName, String version) {
        if (serviceName.equalsIgnoreCase(scm.getName())) {
            update();
        }
    }
    
    public String toXML(String nodeName)
        throws SMSException, SSOException {
        return toXML(nodeName, null);
    }
    
    public String toXML(String nodeName, String organizationName)
        throws SMSException, SSOException {
        Set serviceConfigNames = getSubConfigNames(token);
        Map orgAttributes = null;

        if (nodeName.equals(SMSUtils.ORG_CONFIG)) {
            OrganizationConfigManager ocm = new OrganizationConfigManager(
                token, organizationName);
            orgAttributes = ocm.getAttributes(scm.getName());
        }

        if (((attributesWithoutDefaults == null) ||
            attributesWithoutDefaults.isEmpty()) &&
            ((serviceConfigNames == null) ||
                serviceConfigNames.isEmpty()) &&
            ((orgAttributes == null) || orgAttributes.isEmpty())
        ) {
            return "";
        }

        StringBuffer buff = new StringBuffer();
        buff.append("<").append(nodeName);
        
        if (nodeName.equals(SMSUtils.GLOBAL_CONFIG) ||
            nodeName.equals(SMSUtils.ORG_CONFIG) ||
            nodeName.equals(SMSUtils.INSTANCE)
        ) {
            if ((groupName != null) && (groupName.length() > 0)) {
                buff.append(" ").append(SMSUtils.GROUP).append("=\"")
                    .append(groupName).append("\"");
            }
        }
        
        if (nodeName.equals(SMSUtils.ORG_CONFIG) ||
            nodeName.equals(SMSUtils.INSTANCE)
        ) {
            if ((organizationName != null) && 
                (organizationName.length() > 0)
            ) {
                String oName = (organizationName.indexOf('/') == -1) ?
                    DNMapper.orgNameToRealmName(organizationName) :
                    organizationName;
                buff.append(" ").append(SMSUtils.NAME).append("=\"")
                    .append(oName).append("\"");
            }
        }
        
        if (nodeName.equals(SMSUtils.SUB_CONFIG)) {
            if ((compName != null) && (compName.length() > 0)) {
                buff.append(" ").append(SMSUtils.NAME).append("=\"")
                    .append(compName).append("\"");
            }
        
            if ((configID != null) && (configID.length() > 0)) {
                buff.append(" ").append(SMSUtils.SERVICE_ID).append("=\"")
                    .append(configID).append("\"");
            }
            if (priority > 0) {
                buff.append(" ").append(SMSUtils.PRIORITY).append("=\"")
                    .append(Integer.toString(priority)).append("\"");
            }
        }
        
        buff.append(">");
        
        buff.append(SMSUtils.toAttributeValuePairXML(
            attributesWithoutDefaults));
        
        for (Iterator i = serviceConfigNames.iterator(); i.hasNext(); ) {
            String scName = (String)i.next();
            ServiceConfigImpl sci = this.getSubConfig(token, scName);
            buff.append(sci.toXML(SMSUtils.SUB_CONFIG));
        }
        
        if ((orgAttributes != null) && !orgAttributes.isEmpty()) {
            buff.append("<").append(SMSUtils.ORG_ATTRIBUTE_VALUE_PAIR)
                .append(">\n");
            buff.append(SMSUtils.toAttributeValuePairXML(orgAttributes));
            buff.append("</").append(SMSUtils.ORG_ATTRIBUTE_VALUE_PAIR)
                .append(">\n");
        }

        buff.append("</").append(nodeName).append(">");
        return buff.toString();
    }

    // Static variables
    private static Map configImpls = new HashMap();

    private static Map userPrincipals = new HashMap();

    private static Debug debug = SMSEntry.debug;

    private static final String configMutex = "ConfigMutex";
}
