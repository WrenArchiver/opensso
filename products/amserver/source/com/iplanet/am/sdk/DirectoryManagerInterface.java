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
 * $Id: DirectoryManagerInterface.java,v 1.1 2005-11-01 00:29:24 arvindp Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.iplanet.am.sdk;

import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.ums.SearchControl;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.sm.SchemaType;

/**
 * This interface defines the methods which need to be implemented by plugins.
 * Two plugins are supported, <code> ldap </code> and <code> remote </code>.
 */
public interface DirectoryManagerInterface {

    /**
     * Returns a true if the entry exists in the directory.
     * 
     * @param token
     *            User's single sign on token
     * @param entryDN
     *            DN of the entry.
     * @return true or false
     */
    public boolean doesEntryExists(SSOToken token, String entryDN);

    /**
     * Returns the integer type of the object represented by the
     * <code> DN </code>.
     * 
     * @param token
     *            User's single sign on token
     * @param dn
     *            <code>DN</code> of the entry.
     * @return Integer type of the entry.
     * @throws AMException
     *             If entry is not a supported type or if unable to access the
     *             datastore.
     * @throws SSOException
     *             If user's single sign on token is invalid.
     */
    public int getObjectType(SSOToken token, String dn) throws AMException,
            SSOException;

    /**
     * Returns the integer type of the object represented by the
     * <code> DN </code> using the cached attributes.
     * 
     * @param token
     *            User's single sign on token
     * @param dn
     *            <code>DN</code> of the entry.
     * @param cachedAttributes
     *            cached attributes that can be used to determine the object
     *            type
     * @return Integer type of the entry.
     * @throws AMException
     *             If entry is not a supported type or if unable to access the
     *             datastore.
     * @throws SSOException
     *             If user's single sign on token is invalid.
     */
    public int getObjectType(SSOToken token, String dn, Map cachedAttributes)
            throws AMException, SSOException;

    /**
     * Returns the attributes set in the Domain Component of the organization,
     * in the <code> DC Tree Enabled mode </code>.
     * 
     * @param token
     *            User's single sign on token
     * @param entryDN
     *            <code>DN</code> of the entry.
     * @param attrNames
     *            Set of attribute names
     * @param byteValues
     *            true if trying to read binary attributes
     * @param objectType
     *            Integer representing type of the object.
     * @return Map of attribute-values.
     * @throws AMException
     *             If unable to access datastore
     * @throws SSOException
     *             If user's single sign on token is invalid.
     */
    public Map getDCTreeAttributes(SSOToken token, String entryDN,
            Set attrNames, boolean byteValues, int objectType)
            throws AMException, SSOException;

    /**
     * Returns a Map with attribute-values requested from the directory.
     * 
     * @param token
     *            User's single sign on token
     * @param entryDN
     *            <code>DN</code> of the entry.
     * @param profileType
     *            Integer representing type of the object.
     * @return Map of attribute-values.
     * @throws AMException
     *             If unable to access datastore
     * @throws SSOException
     *             If user's single sign on token is invalid.
     */
    public Map getAttributes(SSOToken token, String entryDN, int profileType)
            throws AMException, SSOException;

    /**
     * Returns a Map with attribute-values requested from the directory.
     * 
     * @param token
     *            User's single sign on token
     * @param entryDN
     *            <code>DN</code> of the entry.
     * @param attrNames
     *            Set of attributes to be read.
     * @param profileType
     *            Integer representing type of the object.
     * @return Map of attribute-values.
     * @throws AMException
     *             If unable to access datastore
     * @throws SSOException
     *             If user's single sign on token is invalid.
     */
    public Map getAttributes(SSOToken token, String entryDN, Set attrNames,
            int profileType) throws AMException, SSOException;

    /**
     * Returns a map of attribute-values for binary attributes.
     * 
     * @param token
     *            User's single sign on token
     * @param entryDN
     *            <code>DN</code> of the entry.
     * @param profileType
     *            Integer representing type of the object.
     * @return Map of attribute-values.
     * @throws AMException
     *             If unable to access datastore
     * @throws SSOException
     *             If user's single sign on token is invalid.
     */
    public Map getAttributesByteValues(SSOToken token, String entryDN,
            int profileType) throws AMException, SSOException;

    /**
     * Returns a map of attribute-values for binary attributes.
     * 
     * @param token
     *            User's single sign on token
     * @param entryDN
     *            <code>DN</code> of the entry.
     * @param attrNames
     *            Names of the attributes to be read.
     * @param profileType
     *            Integer representing type of the object.
     * @return Map of attribute-values.
     * @throws AMException
     *             If unable to access datastore
     * @throws SSOException
     *             If user's single sign on token is invalid.
     */
    public Map getAttributesByteValues(SSOToken token, String entryDN,
            Set attrNames, int profileType) throws AMException, SSOException;

    /**
     * Returns a map of attribute-values for requested attributes.
     * 
     * @param token
     *            User's single sign on token
     * @param entryDN
     *            <code>DN</code> of the entry.
     * @param ignoreCompliance
     *            Ignore compliance mode when constructing search filters.
     * @param byteValues
     *            Return binary attributes, if true.
     * @param profileType
     *            Integer representing type of the object.
     * @return Map of attribute-values.
     * @throws AMException
     *             If unable to access datastore
     * @throws SSOException
     *             If user's single sign on token is invalid.
     */
    public Map getAttributes(SSOToken token, String entryDN,
            boolean ignoreCompliance, boolean byteValues, int profileType)
            throws AMException, SSOException;

    /**
     * Returns a map of attribute-values for requested attributes.
     * 
     * @param token
     *            User's single sign on token
     * @param entryDN
     *            <code>DN</code> of the entry.
     * @param attrNames
     *            Set of attribute names to be read.
     * @param ignoreCompliance
     *            Ignore compliance mode when constructing search filters.
     * @param byteValues
     *            Return binary attributes, if true.
     * @param profileType
     *            Integer representing type of the object.
     * @return Map of attribute-values.
     * @throws AMException
     *             If unable to access datastore
     * @throws SSOException
     *             If user's single sign on token is invalid.
     */
    public Map getAttributes(SSOToken token, String entryDN, Set attrNames,
            boolean ignoreCompliance, boolean byteValues, int profileType)
            throws AMException, SSOException;

    /**
     * Returns the search filter for organization.
     * 
     * @param entryDN
     *            <code> DN </code> of the organization.
     * @return Search filter.
     */
    public String getOrgSearchFilter(String entryDN);

    /**
     * Gets the Organization DN for the specified entryDN. If the entry itself
     * is an org, then same DN is returned.
     * <p>
     * <b>NOTE:</b> This method will involve serveral directory searches, hence
     * be cautious of Performance hit
     * 
     * @param token
     *            a valid SSOToken
     * @param entryDN
     *            the entry whose parent Organization is to be obtained
     * @return the DN String of the parent Organization
     * @throws AMException
     *             if an error occured while obtaining the parent Organization
     */
    public String getOrganizationDN(SSOToken token, String entryDN)
            throws AMException;

    /**
     * Gets the Organization DN for the specified entryDN. If the entry itself
     * is an org, then same DN is returned.
     * 
     * @param token
     *            a valid SSOToken
     * @param entryDN
     *            the entry whose parent Organization is to be obtained
     * @param childDN
     *            the immediate entry whose parent Organization is to be
     *            obtained
     * @return the DN String of the parent Organization
     * @throws AMException
     *             if an error occured while obtaining the parent Organization
     */
    public String verifyAndGetOrgDN(SSOToken token, String entryDN,
            String childDN) throws AMException;

    /**
     * Returns attributes from an external data store.
     * 
     * @param token
     *            Single sign on token of user
     * @param entryDN
     *            DN of the entry user is trying to read
     * @param attrNames
     *            Set of attributes to be read
     * @param profileType
     *            Integer determining the type of profile being read
     * @return A Map of attribute-value pairs
     * @throws AMException
     *             if an error occurs when trying to read external datastore
     */
    public Map getExternalAttributes(SSOToken token, String entryDN,
            Set attrNames, int profileType) throws AMException;

    /**
     * Adds or remove static group DN to or from member attribute
     * 'iplanet-am-static-group-dn'
     * 
     * @param token
     *            SSOToken
     * @param members
     *            set of user DN's
     * @param staticGroupDN
     *            DN of the static group
     * @param toAdd
     *            true to add, false to remove
     * @throws AMException
     *             if there is an internal problem with AM Store.
     */
    public void updateUserAttribute(SSOToken token, Set members,
            String staticGroupDN, boolean toAdd) throws AMException;

    /**
     * Create an entry in the Directory
     * 
     * @param token
     *            SSOToken
     * @param entryName
     *            name of the entry (naming value), e.g. "sun.com", "manager"
     * @param objectType
     *            Profile Type, ORGANIZATION, AMObject.ROLE, AMObject.USER, etc.
     * @param parentDN
     *            the parent DN
     * @param attrSet
     *            the initial attribute set for creation
     */
    public void createEntry(SSOToken token, String entryName, int objectType,
            String parentDN, Map attributes) throws AMEntryExistsException,
            AMException;

    /**
     * Remove an entry from the directory.
     * 
     * @param token
     *            SSOToken
     * @param entryDN
     *            dn of the profile to be removed
     * @param objectType
     *            profile type
     * @param recursive
     *            if true, remove all sub entries & the object
     * @param softDelete
     *            Used to let pre/post callback plugins know that this delete is
     *            either a soft delete (marked for deletion) or a purge/hard
     *            delete itself, otherwise, remove the object only
     */
    public void removeEntry(SSOToken token, String entryDN, int objectType,
            boolean recursive, boolean softDelete) throws AMException,
            SSOException;

    /**
     * Remove group admin role
     * 
     * @param token
     *            SSOToken of the caller
     * @param dn
     *            group DN
     * @param recursive
     *            true to delete all admin roles for all sub groups or sub
     *            people container
     */
    public void removeAdminRole(SSOToken token, String dn, boolean recursive)
            throws SSOException, AMException;

    /**
     * Searches the Directory
     * 
     * @param token
     *            SSOToken
     * @param entryDN
     *            DN of the entry to start the search with
     * @param searchFilter
     *            search filter
     * @param searchScope
     *            search scope, BASE, ONELEVEL or SUBTREE
     * @return Set set of matching DNs
     */
    public Set search(SSOToken token, String entryDN, String searchFilter,
            int searchScope) throws AMException;

    /**
     * Search the Directory
     * 
     * @param token
     *            SSOToken
     * @param entryDN
     *            DN of the entry to start the search with
     * @param searchFilter
     *            search filter
     * @param SearchControl
     *            search control defining the VLV indexes and search scope
     * @return Set set of matching DNs
     */
    public AMSearchResults search(SSOToken token, String entryDN,
            String searchFilter, SearchControl searchControl,
            String attrNames[]) throws AMException;

    /**
     * Get members for roles, dynamic group or static group
     * 
     * @param token
     *            SSOToken
     * @param entryDN
     *            DN of the role or group
     * @param objectType
     *            objectType of the target object, AMObject.ROLE or
     *            AMObject.GROUP
     * @return Set Member DNs
     */
    public Set getMembers(SSOToken token, String entryDN, int objectType)
            throws AMException;

    /**
     * Renames an entry. Currently used for only user renaming
     * 
     * @param token
     *            the sso token
     * @param objectType
     *            the type of entry
     * @param entryDN
     *            the entry DN
     * @param newName
     *            the new name (i.e., if RDN is cn=John, the value passed should
     *            be "John"
     * @param deleteOldName
     *            if true the old name is deleted otherwise it is retained.
     * @return new <code>DN</code> of the renamed entry
     * @throws AMException
     *             if the operation was not successful
     */
    public String renameEntry(SSOToken token, int objectType, String entryDN,
            String newName, boolean deleteOldName) throws AMException;

    /**
     * Method Set the attributes of an entry.
     * 
     * @param token
     *            Single sign on token
     * @param entryDN
     *            DN of the profile whose template is to be set
     * @param objectType
     *            profile type
     * @param attrSet
     *            attributes to be set
     */
    public void setAttributes(SSOToken token, String entryDN, int objectType,
            Map stringAttributes, Map byteAttributes, boolean isAdd)
            throws AMException, SSOException;

    // ###### Group and Role APIs
    /**
     * Returns the dynamic groups search filter and search scope.
     * 
     * @param token
     *            Single sign on token
     * @param entryDN
     *            DN of the profile
     * @param profileType
     * @throws AMException
     * @throws SSOException
     */
    public String[] getGroupFilterAndScope(SSOToken token, String entryDN,
            int profileType) throws SSOException, AMException;

    /**
     * Set's the dynamic groups search filter.
     * 
     * @param token
     *            Single sign on token
     * @param entryDN
     *            <code> DN </code> of group entry
     * @param filter
     *            Search filter
     * @throws AMException
     *             If there is a datastore exception.
     * @throws SSOException
     *             If user's single sign on token is invalid.
     */
    public void setGroupFilter(SSOToken token, String entryDN, String filter)
            throws AMException, SSOException;

    /**
     * Modify member ship for role or static group
     * 
     * @param token
     *            SSOToken
     * @param members
     *            Set of member DN to be operated
     * @param target
     *            DN of the target object to add the member
     * @param type
     *            type of the target object, AMObject.ROLE or AMObject.GROUP
     * @param operation
     *            type of operation, ADD_MEMBER or REMOVE_MEMBER
     * @param updateUserEntry
     *            If true then call the updatUserAttribute when modifying group
     *            membership
     */
    public void modifyMemberShip(SSOToken token, Set members, String target,
            int type, int operation) throws AMException;

    /**
     * Get registered services for an organization
     * 
     * @param token
     *            SSOToken
     * @param entryDN
     *            DN of the org
     * @return Set set of service names
     */
    public Set getRegisteredServiceNames(SSOToken token, String entryDN)
            throws AMException;

    /**
     * Register a service for an org or org unit policy to a profile
     * 
     * @param token
     *            token
     * @param orgDN
     *            DN of the org
     * @param serviceName
     *            Service Name
     */
    public void registerService(SSOToken token, String orgDN, 
            String serviceName) throws AMException, SSOException;

    /**
     * Un register service for a AMro profile.
     * 
     * @param token
     *            SSOToken
     * @param entryDN
     *            DN of the profile whose service is to be removed
     * @param objectType
     *            profile type
     * @param serviceName
     *            Service Name
     * @param type
     *            Template type
     */
    public void unRegisterService(SSOToken token, String entryDN,
            int objectType, String serviceName, AMTemplate template, int type)
            throws AMException;

    /**
     * Get the AMTemplate DN (COSTemplateDN)
     * 
     * @param token
     *            SSOToken
     * @param entryDN
     *            DN of the profile whose template is to be set
     * @param serviceName
     *            Service Name
     * @param type
     *            the template type, AMTemplate.DYNAMIC_TEMPLATE
     * @return String DN of the AMTemplate
     */
    public String getAMTemplateDN(SSOToken token, String entryDN,
            int objectType, String serviceName, int type) throws AMException;

    /**
     * Create an AMTemplate (COSTemplate)
     * 
     * @param token
     *            token
     * @param entryDN
     *            DN of the profile whose template is to be set
     * @param serviceName
     *            Service Name
     * @param attrSet
     *            attributes to be set
     * @param priority
     *            template priority
     * @return String DN of the newly created template
     */
    public String createAMTemplate(SSOToken token, String entryDN,
            int objectType, String serviceName, Map attributes, int priority)
            throws AMException;

    /**
     * Returns the naming attribute
     * 
     * @param objectType
     * @param orgDN
     * @return
     */
    public String getNamingAttr(int objectType, String orgDN);

    /**
     * Return the name of the creation template for a given object type.
     * 
     * @param objectType
     *            Integere representing object type
     * @return Name of creation template
     */
    public String getCreationTemplateName(int objectType);

    /**
     * Returns the objectclass represening an object type.
     * 
     * @param objectType
     * @return
     */
    public String getObjectClassFromDS(int objectType);

    /**
     * Returns the attributes in the directory schema, associated with the given
     * objectclass.
     * 
     * @param objectclass
     * @return The set of attribute names (both required and optional) for this
     *         objectclass
     */
    public Set getAttributesForSchema(String objectclass);

    /**
     * Returns the search filter of a given search template.
     * 
     * @param objectType
     *            Integere represenintg object type.
     * @param orgDN
     *            Organization <code< DN </code>
     * @param searchTemplateName
     *            Name of search template
     * @return Search filter
     */
    public String getSearchFilterFromTemplate(int objectType, String orgDN,
            String searchTemplateName);

    /**
     * Returns the set of top level containers that can be viewed by ths user
     * 
     * @param token
     *            User's single sign on token.
     * @return The top level containers this user manages based on its'
     *         administrative roles (if any)
     * @throws AMException
     *             if a datastore access fails
     * @throws SSOException
     *             if user's single sign on token is invalid.
     */
    public Set getTopLevelContainers(SSOToken token) throws AMException,
            SSOException;

    /**
     * Add a listener object that will receive notifications when entries are
     * changed.
     * 
     * @param token
     *            SSOToken of the user adding the listner
     * @param listener
     *            listener object that will be called when entries are changed
     * @throws AMEventManagerException
     *             if a error occurs during adding listener object
     */
    public void addListener(SSOToken token, AMObjectListener listener,
            Map configMap) throws AMEventManagerException;

    public AMIdentity create(SSOToken token, IdType type, String name,
            Map attrMap, String amOrgName) throws IdRepoException, SSOException;

    public void delete(SSOToken token, IdType type, String name,
            String orgName, String amsdkDN) throws IdRepoException,
            SSOException;

    public Map getAttributes(SSOToken token, IdType type, String name,
            Set attrNames, String amOrgName, String amsdkDN)
            throws IdRepoException, SSOException;

    public Map getAttributes(SSOToken token, IdType type, String name,
            String amOrgName, String amsdkDN) throws IdRepoException,
            SSOException;

    public Set getMembers(SSOToken token, IdType type, String name,
            String amOrgName, IdType membersType, String amsdkDN)
            throws IdRepoException, SSOException;

    public Set getMemberships(SSOToken token, IdType type, String name,
            IdType membershipType, String amOrgName, String amsdkDN)
            throws IdRepoException, SSOException;

    public boolean isExists(SSOToken token, IdType type, String name,
            String amOrgName) throws SSOException, IdRepoException;

    public boolean isActive(SSOToken token, IdType type, String name,
            String amOrgName, String amsdkDN) throws SSOException,
            IdRepoException;

    public void modifyMemberShip(SSOToken token, IdType type, String name,
            Set members, IdType membersType, int operation, String amOrgName)
            throws IdRepoException, SSOException;

    public void removeAttributes(SSOToken token, IdType type, String name,
            Set attrNames, String amOrgName, String amsdkDN)
            throws IdRepoException, SSOException;

    public IdSearchResults search(SSOToken token, IdType type, String pattern,
            Map avPairs, boolean recursive, int maxResults, int maxTime,
            Set returnAttrs, String amOrgName) throws IdRepoException,
            SSOException;

    public IdSearchResults search(SSOToken token, IdType type, String pattern,
            IdSearchControl ctrl, String amOrgName) throws IdRepoException,
            SSOException;

    public void setAttributes(SSOToken token, IdType type, String name,
            Map attributes, boolean isAdd, String amOrgName, String amsdkDN)
            throws IdRepoException, SSOException;

    public Set getAssignedServices(SSOToken token, IdType type, String name,
            Map mapOfServiceNamesAndOCs, String amOrgName, String amsdkDN)
            throws IdRepoException, SSOException;

    public void assignService(SSOToken token, IdType type, String name,
            String serviceName, SchemaType stype, Map attrMap,
            String amOrgName, String amsdkDN) throws IdRepoException,
            SSOException;

    public void unassignService(SSOToken token, IdType type, String name,
            String serviceName, Map attrMap, String amOrgName, String amsdkDN)
            throws IdRepoException, SSOException;

    public Map getServiceAttributes(SSOToken token, IdType type, String name,
            String serviceName, Set attrNames, String amOrgName, String amsdkDN)
            throws IdRepoException, SSOException;

    public void modifyService(SSOToken token, IdType type, String name,
            String serviceName, SchemaType stype, Map attrMap,
            String amOrgName, String amsdkDN) throws IdRepoException,
            SSOException;

    public Set getSupportedTypes(SSOToken token, String amOrgName)
            throws IdRepoException, SSOException;

    public Set getSupportedOperations(SSOToken token, IdType type,
            String amOrgName) throws IdRepoException, SSOException;

}
