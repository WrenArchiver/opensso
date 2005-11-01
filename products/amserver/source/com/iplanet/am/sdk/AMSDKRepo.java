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
 * $Id: AMSDKRepo.java,v 1.1 2005-11-01 00:29:17 arvindp Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.iplanet.am.sdk;

import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import netscape.ldap.util.DN;

import com.iplanet.am.util.Debug;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdRepo;
import com.sun.identity.idm.IdRepoBundle;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdRepoUnsupportedOpException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.idm.RepoSearchResults;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

public class AMSDKRepo extends IdRepo {

    protected static Set listeners = new HashSet();

    private Map supportedOps = new HashMap();

    private IdRepoListener myListener = null;

    // private Map configMap = new AMHashMap();
    private String orgDN = "";

    private Debug debug;

    private static final String PC_ATTR = "iplanet-am-admin-console-default-pc";

    private static final String AC_ATTR = "iplanet-am-admin-console-default-ac";

    private static final String GC_ATTR = "iplanet-am-admin-console-default-gc";

    private static final String ADMIN_SERVICE = "iPlanetAMAdminConsoleService";

    private static final String CLASS_NAME = "com.iplanet.am.sdk.AMSDKRepo";

    private static SSOToken adminToken = null;

    private static AMStoreConnection sc = null;

    public AMSDKRepo() {
        loadSupportedOps();
        debug = Debug.getInstance("amsdkRepo");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#addListener(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.AMObjectListener, java.util.Map)
     */
    public int addListener(SSOToken token, IdRepoListener listnr)
            throws IdRepoException, SSOException {
        // TODO Auto-generated method stub
        // listnr.setConfigMap(configMap);
        listeners.add(listnr);
        myListener = listnr;
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#removeListener()
     */
    public void removeListener() {
        listeners.remove(myListener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#create(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String, java.util.Map)
     */
    public String create(SSOToken token, IdType type, String name, Map attrMap)
            throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug
                    .message("AMSDKIdRepo: Create called on " + type + ": "
                            + name);
        }
        String dn = null;
        AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(token)
                : sc;
        try {
            int orgType = amsc.getAMObjectType(orgDN);
            if (orgType != AMObject.ORGANIZATION) {
                debug.error("AMSDKRepo.create(): Incorrectly configured "
                        + " plugin: Org DN is wrong = " + orgDN);
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "303", null);
            }
        } catch (AMException ame) {
            debug.error("AMSDKRepo.create(): An exception occured while "
                    + " initializing AM SDK ", ame);
            Object[] args = { CLASS_NAME, IdOperation.CREATE.getName() };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "304", args);
        }
        AMOrganization amOrg = amsc.getOrganization(orgDN);
        Map entityNamesAndAttrs = new HashMap();
        entityNamesAndAttrs.put(name, attrMap);
        try {
            if (type.equals(IdType.USER)) {
                Set res = amOrg.createEntities(AMObject.USER,
                        entityNamesAndAttrs);
                AMEntity entity = (AMEntity) res.iterator().next();
                dn = entity.getDN();
            } else if (type.equals(IdType.AGENT)) {
                Set res = amOrg.createEntities(100, entityNamesAndAttrs);
                AMEntity entity = (AMEntity) res.iterator().next();
                dn = entity.getDN();
            } else if (type.equals(IdType.GROUP)) {
                String gcDN = AMNamingAttrManager
                        .getNamingAttr(AMObject.GROUP_CONTAINER)
                        + "=" + getDefaultGroupContainerName() + "," + orgDN;
                AMGroupContainer amgc = amsc.getGroupContainer(gcDN);
                Set groups = amgc.createStaticGroups(entityNamesAndAttrs);
                AMStaticGroup group = (AMStaticGroup) groups.iterator().next();
                dn = group.getDN();
            } else if (type.equals(IdType.ROLE)) {
                Set roles = amOrg.createRoles(entityNamesAndAttrs);
                AMRole role = (AMRole) roles.iterator().next();
                dn = role.getDN();
            } else if (type.equals(IdType.FILTEREDROLE)) {
                Set roles = amOrg.createFilteredRoles(entityNamesAndAttrs);
                AMFilteredRole role = (AMFilteredRole) roles.iterator().next();
                dn = role.getDN();
            }
        } catch (AMException ame) {
            debug.error("AMSDKRepo.create(): Caught AMException..", ame);
            throw IdUtils.convertAMException(ame);
        }

        return dn;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#delete(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String)
     */
    public void delete(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug
                    .message("AMSDKIdRepo: Delete called on " + type + ": "
                            + name);
        }
        AMOrganization amOrg = checkAndGetOrg(token);
        Set entitySet = new HashSet();

        try {
            String eDN = getDN(type, name);
            entitySet.add(eDN);
            if (type.equals(IdType.USER)) {

                amOrg.deleteUsers(entitySet);
            } else if (type.equals(IdType.AGENT)) {

                amOrg.deleteEntities(100, entitySet);
            } else if (type.equals(IdType.GROUP)) {
                amOrg.deleteStaticGroups(entitySet);
            } else if (type.equals(IdType.ROLE)) {
                amOrg.deleteRoles(entitySet);
            } else if (type.equals(IdType.FILTEREDROLE)) {
                amOrg.deleteFilteredRoles(entitySet);
            }
        } catch (AMException ame) {
            debug.error("AMSDKRepo.delete(): Caught AMException...", ame);
            throw IdUtils.convertAMException(ame);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#getAttributes(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String, java.util.Set)
     */
    public Map getAttributes(SSOToken token, IdType type, String name,
            Set attrNames) throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("AMSDKIdRepo: getAttributes called" + ": " + type
                    + ": " + name);
        }
        AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(token)
                : sc;
        String dn = getDN(type, name);
        int profileType = getProfileType(type);
        // Use adminToken if present
        if (adminToken != null) {
            token = adminToken;
        }
        try {
            if (amsc.isValidEntry(dn)) {
                AMDirectoryManager dm = AMDirectoryWrapper.getInstance();
                return dm.getAttributes(token, dn, attrNames, false, false,
                        profileType);
            } else {
                Object[] args = { name };
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "202", args);
            }
        } catch (AMException ame) {
            debug.error("AMSDKRepo.getAttributes(): AMException ", ame);
            throw IdUtils.convertAMException(ame);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#getAttributes(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String)
     */
    public Map getAttributes(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("AMSDKIdRepo: getAttributes called" + ": " + type
                    + ": " + name);
        }

        AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(token)
                : sc;
        String dn = getDN(type, name);
        int profileType = getProfileType(type);
        // Use adminToken if present
        if (adminToken != null) {
            token = adminToken;
        }
        try {
            if (amsc.isValidEntry(dn)) {
                AMDirectoryManager dm = AMDirectoryWrapper.getInstance();
                return dm.getAttributes(token, dn, false, false, profileType);
            } else {
                Object[] args = { name };
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "202", args);
            }
        } catch (AMException ame) {
            debug.error("AMSDKRepo.getAttributes(): AMException ", ame);
            throw IdUtils.convertAMException(ame);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getBinaryAttributes(
     *      com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
     *      java.lang.String, java.util.Set)
     */
    public Map getBinaryAttributes(SSOToken token, IdType type, String name,
            Set attrNames) throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("AMSDKIdRepo: getBinaryAttributes called" + ": "
                    + type + ": " + name);
        }

        AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(token)
                : sc;
        String dn = getDN(type, name);
        int profileType = getProfileType(type);
        // Use adminToken if present
        if (adminToken != null) {
            token = adminToken;
        }
        try {
            if (amsc.isValidEntry(dn)) {
                AMDirectoryManager dm = AMDirectoryWrapper.getInstance();
                return dm.getAttributesByteValues(token, dn, attrNames,
                        profileType);
            } else {
                Object[] args = { name };
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "202", args);
            }
        } catch (AMException ame) {
            debug.error("AMSDKRepo.getBinaryAttributes(): AMException ", ame);
            throw IdUtils.convertAMException(ame);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#getConfiguration()
     */
    public Map getConfiguration() {
        return super.getConfiguration();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#getMembers(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String,
     *      com.iplanet.am.sdk.IdType)
     */
    public Set getMembers(SSOToken token, IdType type, String name,
            IdType membersType) throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("AMSDKRepo: getMembers called" + type + ": " + name
                    + ": " + membersType);
        }
        Set results;
        AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(token)
                : sc;
        String dn = null;
        if (type.equals(IdType.USER) || type.equals(IdType.AGENT)) {
            debug.error("AMSDKRepo: Membership operation is not supported "
                    + " for Users or Agents");
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "203", null);

        } else if (type.equals(IdType.GROUP)) {
            dn = getDN(type, name);
            AMStaticGroup group = amsc.getStaticGroup(dn);
            if (membersType.equals(IdType.USER)) {
                try {
                    results = group.getUserDNs();
                } catch (AMException ame) {
                    debug.error("AMSDKRepo: Unable to get user memberships " +
                            "for group"+ dn, ame);
                    Object[] args = { CLASS_NAME, membersType.getName(),
                            type.getName(), name };
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "205",
                            args);
                }
            } else {
                debug.error("AMSDKRepo: Groups do not supported membership for "
                                + membersType.getName());
                Object[] args = { CLASS_NAME, membersType.getName(),
                        type.getName() };
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "204", args);
            }

        } else if (type.equals(IdType.ROLE)) {
            dn = getDN(type, name);
            AMRole role = amsc.getRole(dn);
            if (membersType.equals(IdType.USER)) {
                try {
                    results = role.getUserDNs();
                } catch (AMException ame) {
                    debug.error("AMSDKRepo: Unable to get user memberships " +
                            "for role " + dn, ame);
                    throw IdUtils.convertAMException(ame);
                }
            } else {
                Object[] args = { CLASS_NAME, membersType.getName(),
                        type.getName() };
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "204", args);
            }

        } else if (type.equals(IdType.FILTEREDROLE)) {
            dn = getDN(type, name);
            AMFilteredRole role = amsc.getFilteredRole(dn);
            if (membersType.equals(IdType.USER)) {
                try {
                    results = role.getUserDNs();
                } catch (AMException ame) {
                    debug.error("AMSDKRepo: Unable to get user memberships " +
                            "for role " + dn, ame);
                    throw IdUtils.convertAMException(ame);
                }
            } else {
                Object[] args = { CLASS_NAME, membersType.getName(),
                        type.getName() };
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "204", args);
            }

        } else {
            Object[] args = { CLASS_NAME, IdOperation.READ.getName(),
                    type.getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "305", args);
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#getMemberships(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String,
     *      com.iplanet.am.sdk.IdType)
     */
    public Set getMemberships(SSOToken token, IdType type, String name,
            IdType membershipType) throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("AMSDKRepo: getMemberships called" + type + ": "
                    + name + ": " + membershipType);
        }
        Set results;
        AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(token)
                : sc;
        String dn = null;
        if (!type.equals(IdType.USER)) {
            debug.error("AMSDKRepo: Membership for identities other than "
                    + " Users is not allowed ");
            Object[] args = { CLASS_NAME };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "206", args);

        } else {
            dn = getDN(type, name);
            AMUser user = amsc.getUser(dn);
            if (membershipType.equals(IdType.GROUP)) {
                try {
                    results = user.getStaticGroupDNs();
                } catch (AMException ame) {
                    debug.error(
                            "AMSDKRepo: Unable to get user's group memberships "
                                    + dn, ame);
                    Object[] args = { CLASS_NAME, membershipType.getName(),
                            type.getName(), name };
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "207",
                            args);
                }
            } else if (membershipType.equals(IdType.ROLE)) {

                try {
                    results = user.getRoleDNs();
                } catch (AMException ame) {
                    debug.error("AMSDKRepo: Unable to get roles of a user "
                            + dn, ame);
                    throw IdUtils.convertAMException(ame);

                }
            } else if (membershipType.equals(IdType.FILTEREDROLE)) {

                try {
                    results = user.getFilteredRoleDNs();
                } catch (AMException ame) {
                    debug.error("AMSDKRepo: Unable to get user memberships " +
                            "for role "+ dn, ame);
                    throw IdUtils.convertAMException(ame);
                }
            } else { // Memberships of any other types not supported for
                // users.
                debug.error("AMSDKRepo: Membership for other types of entities "
                                + " not supported for Users");
                Object args[] = { CLASS_NAME, type.getName(),
                        membershipType.getName() };
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "204", args);
            }
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#getSupportedOperations(
     *      com.iplanet.am.sdk.IdType)
     */
    public Set getSupportedOperations(IdType type) {
        if (debug.messageEnabled()) {
            debug.message("AMSDKRepo: getSupportedOps on " + type + " called");
            debug.message("AMSDKRepo: supportedOps Map = "
                    + supportedOps.toString());
        }
        return (Set) supportedOps.get(type);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getSupportedTypes()
     */
    public Set getSupportedTypes() {

        return supportedOps.keySet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#initialize(java.util.Map)
     */
    public void initialize(Map configParams) {

        super.initialize(configParams);
        if (debug.messageEnabled()) {
            debug.message("AMSDKRepo: Initializing configuration: "
                    + configMap.toString());
        }
        Set orgs = (Set) configMap.get("amSDKOrgName");
        if (orgs != null && !orgs.isEmpty()) {
            orgDN = (String) orgs.iterator().next();
        } else {
            orgDN = AMStoreConnection.rootSuffix;
        }
        if (adminToken == null) {
            adminToken = (SSOToken) AccessController
                    .doPrivileged(AdminTokenAction.getInstance());
            try {
                sc = new AMStoreConnection(adminToken);
            } catch (SSOException ssoe) {
                // do nothing ... but log the error
                debug.error("AMSDKRepo:Initialize..Failed to initialize "
                        + " AMStoreConnection...", ssoe);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#isExists(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String)
     */
    public boolean isExists(SSOToken token, IdType type, String name) {
        // TODO Auto-generated method stub
        if (debug.messageEnabled()) {
            debug.message("AMSDKRepo: isExists called " + type + ": " + name);
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#isActive( com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String)
     */
    public boolean isActive(SSOToken token, IdType type, String name)
            throws SSOException {
        AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(token)
                : sc;
        try {
            String dn = getDN(type, name);
            AMEntity entity = amsc.getEntity(dn);
            return entity.isActivated();

        } catch (AMException ame) {
            return false;
        } catch (IdRepoException ide) {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#modifyMemberShip(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String, java.util.Set,
     *      com.iplanet.am.sdk.IdType, int)
     */
    public void modifyMemberShip(SSOToken token, IdType type, String name,
            Set members, IdType membersType, int operation)
            throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("AMSDKRepo: modifyMemberShip called " + type + ": "
                    + name + ": " + members + ": " + membersType);
        }
        if (members == null || members.isEmpty()) {
            debug.error("AMSDKRepo.modifyMemberShip: Members set is empty");
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "201", null);
        }
        if (type.equals(IdType.USER) || type.equals(IdType.AGENT)) {
            debug.error("AMSDKRepo.modifyMembership: Memberhsip to users and"
                    + " agents is not supported");
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "203", null);
        }
        if (!membersType.equals(IdType.USER)) {
            debug.error("AMSDKRepo.modifyMembership: A non-user type cannot "
                    + " be made a member of any identity"
                    + membersType.getName());
            Object[] args = { CLASS_NAME };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "206", args);
        }
        Set usersSet = new HashSet();
        Iterator it = members.iterator();
        while (it.hasNext()) {
            String curr = (String) it.next();
            String dn = getDN(membersType, curr);
            usersSet.add(dn);
        }
        AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(token)
                : sc;
        if (type.equals(IdType.GROUP)) {
            String gdn = getDN(type, name);
            AMStaticGroup group = amsc.getStaticGroup(gdn);
            try {
                switch (operation) {
                case ADDMEMBER:
                    group.addUsers(usersSet);
                    break;
                case REMOVEMEMBER:
                    group.removeUsers(usersSet);
                }
            } catch (AMException ame) {
                debug.error(
                        "AMSDKRepo.modifyMembership: Caught exception while "
                                + " adding users to groups", ame);
                throw IdUtils.convertAMException(ame);
            }
        } else if (type.equals(IdType.ROLE)) {
            String gdn = getDN(type, name);
            AMRole role = amsc.getRole(gdn);
            try {
                switch (operation) {
                case ADDMEMBER:
                    role.addUsers(usersSet);
                    break;
                case REMOVEMEMBER:
                    role.removeUsers(usersSet);
                }
            } catch (AMException ame) {
                debug.error(
                        "AMSDKRepo.modifyMembership: Caught exception while "
                                + " adding/removing users to roles", ame);
                throw IdUtils.convertAMException(ame);
            }
        } else {
            // throw an exception
            debug.error("AMSDKRepo.modifyMembership: Memberships cannot be"
                    + "modified for type= " + type.getName());
            Object[] args = { CLASS_NAME, type.getName() };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "209", args);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#removeAttributes(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String, java.util.Set)
     */
    public void removeAttributes(SSOToken token, IdType type, String name,
            Set attrNames) throws IdRepoException, SSOException {
        // TODO Auto-generated method stub
        if (debug.messageEnabled()) {
            debug.message("AMSDKRepo: removeAttributes called " + type + ": "
                    + name + attrNames);
        }
        // FIXME - Need to be evaluated for further development
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#search(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String, java.util.Map, boolean,
     *      int, int, java.util.Set)
     */
    public RepoSearchResults search(SSOToken token, IdType type,
            String pattern, Map avPairs, boolean recursive, int maxResults,
            int maxTime, Set returnAttrs) throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("AMSDKRepo: search called" + type + ": " + pattern
                    + ": " + avPairs);
        }
        String searchDN = orgDN;
        int profileType = getProfileType(type);
        if (type.equals(IdType.USER)) {
            searchDN = "ou=" + getDefaultPeopleContainerName() + "," + orgDN;
        } else if (type.equals(IdType.AGENT)) {
            searchDN = "ou=" + getDefaultAgentContainerName() + "," + orgDN;
        } else if (type.equals(IdType.GROUP)) {
            searchDN = "ou=" + getDefaultGroupContainerName() + "," + orgDN;
        }
        // String avFilter = AMObjectImpl.constructFilter(avPairs);
        AMSearchControl ctrl = new AMSearchControl();
        ctrl.setMaxResults(maxResults);
        ctrl.setTimeOut(maxTime);
        ctrl.setSearchScope(AMConstants.SCOPE_ONE);
        if (returnAttrs == null || returnAttrs.isEmpty()) {
            ctrl.setAllReturnAttributes(true);
        } else {
            ctrl.setReturnAttributes(returnAttrs);
        }
        AMSearchResults results;
        try {
            AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(token)
                    : sc;
            switch (profileType) {
            case AMObject.USER:
                AMPeopleContainer pc = amsc.getPeopleContainer(searchDN);
                if (avPairs == null || avPairs.isEmpty()) {
                    results = pc.searchUsers(pattern, avPairs, ctrl);
                } else {
                    // avPairs is being passed. Create an OR condition
                    // filter.
                    String avFilter = constructFilter(IdRepo.OR_MOD, avPairs);
                    results = pc.searchUsers(pattern, ctrl, avFilter);
                }
                if (recursive) {
                    // It could be an Auth
                    // search and if no matching user found then we need
                    // to do a scope-sub search
                    Set usersFound = results.getSearchResults();
                    if (usersFound == null || usersFound.isEmpty()) {
                        // SCOPE_SUB search to find exactly one user.
                        // Throw an exception if more than one
                        // matching is found.
                        if (avPairs == null || avPairs.isEmpty()) {
                            AMOrganization org = amsc.getOrganization(orgDN);
                            ctrl.setSearchScope(AMConstants.SCOPE_SUB);
                            results = org.searchUsers(pattern, ctrl);
                        } else {
                            String avFilter = constructFilter(IdRepo.OR_MOD,
                                    avPairs);
                            AMOrganization org = amsc.getOrganization(orgDN);
                            ctrl.setSearchScope(AMConstants.SCOPE_SUB);
                            results = org.searchUsers("*", ctrl, avFilter);
                        }
                    }
                }
                break;
            case 100:
                AMOrganizationalUnit ou = amsc.getOrganizationalUnit(searchDN);
                results = ou.searchEntities(pattern, avPairs, null, ctrl);
                // results = ou.searchEntities(pattern, ctrl, avFilter, null);
                break;
            case AMObject.GROUP:
            case AMObject.STATIC_GROUP:
                AMGroupContainer gc = amsc.getGroupContainer(searchDN);
                results = gc.searchGroups(pattern, avPairs, ctrl);
                break;
            case AMObject.ROLE:
                AMOrganization org = amsc.getOrganization(searchDN);
                results = org.searchRoles(pattern, ctrl);
                break;
            case AMObject.FILTERED_ROLE:
                org = amsc.getOrganization(searchDN);
                results = org.searchFilteredRoles(pattern, ctrl);
                break;
            default:
                Object[] args = { CLASS_NAME, type.getName() };
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "210", args);
            }
        } catch (AMException ame) {
            debug.error("AMSDKRepo.search: Unable to perform search operation",
                    ame);
            throw IdUtils.convertAMException(ame);
        }
        return new RepoSearchResults(results.getSearchResults(), results
                .getErrorCode(), results.getResultAttributes(), type);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#search(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, int, int,
     *      java.util.Set, boolean, int, java.util.Map)
     */
    public RepoSearchResults search(SSOToken token, IdType type,
            String pattern, int maxTime, int maxResults, Set returnAttrs,
            boolean returnAllAttrs, int filterOp, Map avPairs, 
            boolean recursive)
            throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("AMSDKRepo: search called" + type + ": " + pattern
                    + ": " + avPairs);
        }
        String searchDN = orgDN;
        int profileType = getProfileType(type);
        if (type.equals(IdType.USER)) {
            searchDN = "ou=" + getDefaultPeopleContainerName() + "," + orgDN;
        } else if (type.equals(IdType.AGENT)) {
            searchDN = "ou=" + getDefaultAgentContainerName() + "," + orgDN;
        } else if (type.equals(IdType.GROUP)) {
            searchDN = "ou=" + getDefaultGroupContainerName() + "," + orgDN;
        }
        // String avFilter = AMObjectImpl.constructFilter(avPairs);
        AMSearchControl ctrl = new AMSearchControl();
        ctrl.setMaxResults(maxResults);
        ctrl.setTimeOut(maxTime);
        ctrl.setSearchScope(AMConstants.SCOPE_ONE);
        if (returnAllAttrs) {
            ctrl.setAllReturnAttributes(true);
        } else {
            if (returnAttrs != null && !returnAttrs.isEmpty()) {
                ctrl.setReturnAttributes(returnAttrs);
            }
        }
        AMSearchResults results;
        try {
            AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(token)
                    : sc;
            switch (profileType) {
            case AMObject.USER:
                AMPeopleContainer pc = amsc.getPeopleContainer(searchDN);
                if (avPairs == null || avPairs.isEmpty()) {
                    results = pc.searchUsers(pattern, avPairs, ctrl);
                } else {
                    // avPairs is being passed. Create an OR condition
                    // filter.
                    String avFilter = constructFilter(filterOp, avPairs);
                    results = pc.searchUsers(pattern, ctrl, avFilter);
                }
                if (recursive) {
                    // It could be an Auth
                    // search and if no matching user found then we need
                    // to do a scope-sub search
                    Set usersFound = results.getSearchResults();
                    if (usersFound == null || usersFound.isEmpty()) {
                        // SCOPE_SUB search to find exactly one user.
                        // Throw an exception if more than one
                        // matching is found.
                        if (avPairs == null || avPairs.isEmpty()) {
                            AMOrganization org = amsc.getOrganization(orgDN);
                            ctrl.setSearchScope(AMConstants.SCOPE_SUB);
                            results = org.searchUsers(pattern, ctrl);
                        } else {
                            String avFilter = constructFilter(
                                                    filterOp, avPairs);
                            AMOrganization org = amsc.getOrganization(orgDN);
                            ctrl.setSearchScope(AMConstants.SCOPE_SUB);
                            results = org.searchUsers("*", ctrl, avFilter);
                        }
                        /*
                         * usersFound = results.getSearchResults(); if
                         * (usersFound.size() > 1){ throw new
                         * IdRepoException(IdRepoBundle. getString("216"),
                         * "216"); }
                         */
                    }
                }
                break;
            case 100:
                AMOrganizationalUnit ou = amsc.getOrganizationalUnit(searchDN);
                results = ou.searchEntities(pattern, avPairs, null, ctrl);
                // results = ou.searchEntities(pattern, ctrl, avFilter, null);
                break;
            case AMObject.GROUP:
            case AMObject.STATIC_GROUP:
                AMGroupContainer gc = amsc.getGroupContainer(searchDN);
                results = gc.searchStaticGroups(pattern, avPairs, ctrl);
                break;
            case AMObject.ROLE:
                AMOrganization org = amsc.getOrganization(searchDN);
                results = org.searchRoles(pattern, ctrl);
                break;
            case AMObject.FILTERED_ROLE:
                org = amsc.getOrganization(searchDN);
                results = org.searchFilteredRoles(pattern, ctrl);
                break;
            default:
                Object[] args = { CLASS_NAME, type.getName() };
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "210", args);
            }
        } catch (AMException ame) {
            String amErrorCode = ame.getErrorCode();
            if (!amErrorCode.equals("341")) {
                debug.error(
                        "AMSDKRepo.search: Unable to perform search operation",
                        ame);
            }
            if (profileType == 100 && amErrorCode.equals("341")) {
                // Agent profile type...if container does not exist
                // then return empty results
                return new RepoSearchResults(new HashSet(),
                        RepoSearchResults.SUCCESS, Collections.EMPTY_MAP, type);
            }
            throw IdUtils.convertAMException(ame);
        }
        return new RepoSearchResults(results.getSearchResults(), results
                .getErrorCode(), results.getResultAttributes(), type);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#setAttributes(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String, java.util.Map, boolean)
     */
    public void setAttributes(SSOToken token, IdType type, String name,
            Map attributes, boolean isAdd) 
            throws IdRepoException, SSOException 
    {
        if (debug.messageEnabled()) {
            if (attributes.containsKey("userpassword")) {
                AMHashMap removedPasswd = new AMHashMap();
                removedPasswd.copy(attributes);
                removedPasswd.remove("userpassword");
                removedPasswd.put("userpassword", "xxx...");
                debug.message("AMSDKRepo: setAttributes called" + type + ": "
                        + name + ": " + removedPasswd);
            } else {
                debug.message("AMSDKRepo: setAttributes called" + type + ": "
                        + name + ": " + attributes);
            }
        }
        if (attributes == null || attributes.isEmpty()) {
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "201", null);
        }

        String dn = getDN(type, name);
        int profileType = getProfileType(type);
        AMDirectoryManager amdm = AMDirectoryWrapper.getInstance();
        try {
            if (adminToken != null) {
                token = adminToken;
            }
            amdm.setAttributes(token, dn, profileType, attributes, null, false);
        } catch (AMException ame) {
            debug.error("AMSDKRepo.setAttributes: Unable to set attributes",
                    ame);
            throw IdUtils.convertAMException(ame);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#setBinaryAttributes(
     *      com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
     *      java.lang.String, java.util.Map, boolean)
     */
    public void setBinaryAttributes(SSOToken token, IdType type, String name,
            Map attributes, boolean isAdd) throws IdRepoException, SSOException 
    {
        if (debug.messageEnabled()) {
            debug.message("AMSDKRepo: setBinaryAttributes called" + type + ": "
                    + name + ": " + attributes);
        }
        if (attributes == null || attributes.isEmpty()) {
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "201", null);
        }

        String dn = getDN(type, name);
        int profileType = getProfileType(type);
        AMDirectoryManager amdm = AMDirectoryWrapper.getInstance();
        try {
            if (adminToken != null) {
                token = adminToken;
            }
            amdm.setAttributes(token, dn, profileType, new AMHashMap(false),
                    attributes, false);
        } catch (AMException ame) {
            debug.error(
                    "AMSDKRepo.setBinaryAttributes: Unable to set attributes",
                    ame);
            throw IdUtils.convertAMException(ame);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#assignService(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.lang.String,
     *      java.util.Map)
     */
    public void assignService(SSOToken token, IdType type, String name,
            String serviceName, SchemaType sType, Map attrMap)
            throws IdRepoException, SSOException {
        if (type.equals(IdType.AGENT) || type.equals(IdType.GROUP)) {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        }
        // Use adminToken if present
        if (adminToken != null) {
            token = adminToken;
        }
        if (type.equals(IdType.USER)) {
            Set OCs = (Set) attrMap.get("objectclass");
            Set attrName = new HashSet(1);
            attrName.add("objectclass");
            Map tmpMap = getAttributes(token, type, name, attrName);
            Set oldOCs = (Set) tmpMap.get("objectclass");
            // Set oldOCs = getAttribute("objectclass");
            OCs = AMCommonUtils.combineOCs(OCs, oldOCs);
            attrMap.put("objectclass", OCs);
            if (sType.equals(SchemaType.USER)) {

                setAttributes(token, type, name, attrMap, false);
            } else if (sType.equals(SchemaType.DYNAMIC)) {
                // Map tmpMap = new HashMap();
                // tmpMap.put("objectclass", (Set) attrMap.get("objectclass"));
                setAttributes(token, type, name, attrMap, false);
            }
        } else if (type.equals(IdType.ROLE) || type.equals(IdType.FILTEREDROLE)
                || type.equals(IdType.REALM)) {
            AMDirectoryManager amdm = AMDirectoryWrapper.getInstance();
            try {

                amdm.registerService(token, orgDN, serviceName);
            } catch (AMException ame) {
                if (ame.getErrorCode().equals("464")) {
                    // do nothing. Definition already exists. That's OK.
                } else {
                    throw IdUtils.convertAMException(ame);
                }
            }
            String dn = getDN(type, name);
            try {
                // Remove OCs. Those are needed only when setting service
                // for users, not roles.
                attrMap.remove("objectclass");
                amdm.createAMTemplate(token, dn, getProfileType(type),
                        serviceName, attrMap, 0);
            } catch (AMException ame) {
                debug.error("AMSDKRepo.assignService: Caught AMException", ame);
                throw IdUtils.convertAMException(ame);
            }
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#unassignService(
     *      com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
     *      java.lang.String, java.lang.String, java.util.Set)
     */
    public void unassignService(SSOToken token, IdType type, String name,
            String serviceName, Map attrMap) throws IdRepoException,
            SSOException {
        if (type.equals(IdType.AGENT) || type.equals(IdType.GROUP)) {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        }
        // Use adminToken if present
        if (adminToken != null) {
            token = adminToken;
        }
        if (type.equals(IdType.USER)) {

            // Get the object classes that need to be remove from Service Schema
            Set removeOCs = (Set) attrMap.get("objectclass");
            Set attrNameSet = new HashSet();
            attrNameSet.add("objectclass");
            Map objectClassesMap = getAttributes(
                                            token, type, name, attrNameSet);
            Set OCValues = (Set) objectClassesMap.get("objectclass");
            removeOCs = AMCommonUtils.updateAndGetRemovableOCs(OCValues,
                    removeOCs);

            // Get the attributes that need to be removed
            Set removeAttrs = new HashSet();
            Iterator iter1 = removeOCs.iterator();
            while (iter1.hasNext()) {
                String oc = (String) iter1.next();
                AMDirectoryManager dsManager = AMDirectoryWrapper.getInstance();
                Set attrs = dsManager.getAttributesForSchema(oc);
                Iterator iter2 = attrs.iterator();
                while (iter2.hasNext()) {
                    String attrName = (String) iter2.next();
                    removeAttrs.add(attrName.toLowerCase());
                }
            }

            // Will be AMHashMap, So the attr names will be in lower case
            Map avPair = getAttributes(token, type, name);
            Iterator itr = avPair.keySet().iterator();

            while (itr.hasNext()) {
                String attrName = (String) itr.next();

                if (removeAttrs.contains(attrName)) {
                    try {
                        // remove attribute one at a time, so if the first
                        // one fails, it will keep continue to remove
                        // other attributes.
                        Map tmpMap = new AMHashMap();
                        tmpMap.put(attrName, Collections.EMPTY_SET);
                        setAttributes(token, type, name, tmpMap, false);
                    } catch (Exception ex) {
                        if (debug.messageEnabled()) {
                            debug.message(
                                    "AMUserImpl.unassignServices() Error " +
                                    "occured while removing attribute: "
                                    + attrName);
                        }
                    }
                }
            }

            // Now update the object class attribute
            Map tmpMap = new AMHashMap();
            tmpMap.put("objectclass", OCValues);
            setAttributes(token, type, name, tmpMap, false);
        } else if (type.equals(IdType.ROLE)) {
            try {
                AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(
                        token) : sc;
                String roleDN = getDN(type, name);
                AMRole role = amsc.getRole(roleDN);
                AMTemplate templ = role.getTemplate(serviceName,
                        AMTemplate.DYNAMIC_TEMPLATE);
                if (templ != null && templ.isExists()) {
                    templ.delete();
                }
                // FIXME may need additional functionality
                // like unregistering the service
            } catch (AMException ame) {
                debug.error("AMSDKRepo.unassignService: Caught AMException",
                        ame);
                throw IdUtils.convertAMException(ame);
            }
        } else if (type.equals(IdType.FILTEREDROLE)
                || type.equals(IdType.REALM)) {
            try {
                AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(
                        token) : sc;
                String roleDN = getDN(type, name);
                AMFilteredRole role = amsc.getFilteredRole(roleDN);
                AMTemplate templ = role.getTemplate(serviceName,
                        AMTemplate.DYNAMIC_TEMPLATE);
                if (templ != null && templ.isExists()) {
                    templ.delete();
                }
                // FIXME may need additional functionality
                // like unregistering the service
            } catch (AMException ame) {
                debug.error("AMSDKRepo.unassignService: Caught AMException",
                        ame);
                throw IdUtils.convertAMException(ame);
            }
        } else {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getAssignedServices(
     *      com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
     *      java.lang.String)
     */
    public Set getAssignedServices(SSOToken token, IdType type, String name,
            Map mapOfServiceNamesandOCs) throws IdRepoException, SSOException {
        Set resultsSet = new HashSet();

        if (type.equals(IdType.AGENT) || type.equals(IdType.GROUP)) {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        }
        if (mapOfServiceNamesandOCs == null
                || mapOfServiceNamesandOCs.isEmpty()) {
            return resultsSet;
        }
        if (type.equals(IdType.USER)) {
            Set OCs = readObjectClass(token, type, name);
            OCs = convertToLowerCase(OCs);
            Iterator iter = mapOfServiceNamesandOCs.keySet().iterator();
            while (iter.hasNext()) {
                String sname = (String) iter.next();
                Set ocSet = (Set) mapOfServiceNamesandOCs.get(sname);
                ocSet = convertToLowerCase(ocSet);
                if (OCs.containsAll(ocSet)) {
                    resultsSet.add(sname);
                }
            }
        } else if (type.equals(IdType.ROLE)) {
            // Check to see if COS template exists.
            Iterator iter = mapOfServiceNamesandOCs.keySet().iterator();
            while (iter.hasNext()) {
                String serviceName = (String) iter.next();
                try {
                    AMStoreConnection amsc = (sc == null) ? 
                            new AMStoreConnection(token)
                            : sc;
                    String roleDN = getDN(type, name);
                    AMRole role = amsc.getRole(roleDN);
                    AMTemplate templ = role.getTemplate(serviceName,
                            AMTemplate.DYNAMIC_TEMPLATE);
                    if (templ != null && templ.isExists()) {
                        resultsSet.add(serviceName);
                    }
                } catch (AMException ame) {
                    // throw IdUtils.convertAMException(ame);
                    // Ignore this exception..the service might not have
                    // dynamic attributes. Continue iterating.
                }
            }

        } else if (type.equals(IdType.FILTEREDROLE)
                || type.equals(IdType.REALM)) {
            // Check to see if COS template exists.
            Iterator iter = mapOfServiceNamesandOCs.keySet().iterator();
            while (iter.hasNext()) {
                String serviceName = (String) iter.next();
                try {
                    AMStoreConnection amsc = (sc == null) ? 
                            new AMStoreConnection(token)
                            : sc;
                    String roleDN = getDN(type, name);
                    AMFilteredRole role = amsc.getFilteredRole(roleDN);
                    AMTemplate templ = role.getTemplate(serviceName,
                            AMTemplate.DYNAMIC_TEMPLATE);
                    if (templ != null && templ.isExists()) {
                        resultsSet.add(serviceName);
                    }
                } catch (AMException ame) {
                    // throw IdUtils.convertAMException(ame);
                    // ignore this exception
                }
            }

        } else {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        }
        return resultsSet;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getServiceAttributes(
     *      com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
     *      java.lang.String, java.lang.String, java.util.Set)
     */
    public Map getServiceAttributes(SSOToken token, IdType type, String name,
            String serviceName, Set attrNames) throws IdRepoException,
            SSOException {
        if (type.equals(IdType.AGENT) || type.equals(IdType.GROUP)) {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        } else if (type.equals(IdType.USER)) {
            return getAttributes(token, type, name, attrNames);
        } else if (type.equals(IdType.ROLE) || type.equals(IdType.FILTEREDROLE)
                || type.equals(IdType.REALM)) {
            try {
                AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(
                        token) : sc;
                String roleDN = getDN(type, name);
                AMRole role = amsc.getRole(roleDN);
                AMTemplate templ = role.getTemplate(serviceName,
                        AMTemplate.DYNAMIC_TEMPLATE);
                if (templ != null && templ.isExists()) {
                    return templ.getAttributes(attrNames);
                } else {
                    if (debug.messageEnabled()) {
                        debug.message("AMSDKRepo::getServiceAttributes "
                                + "Service: " + serviceName
                                + " is not assigned to DN: " + roleDN);
                    }
                    return (Collections.EMPTY_MAP);
                }
            } catch (AMException ame) {
                throw IdUtils.convertAMException(ame);
            }
        } else if (type.equals(IdType.FILTEREDROLE)
                || type.equals(IdType.REALM)) {
            try {
                AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(
                        token) : sc;
                String roleDN = getDN(type, name);
                AMRole role = amsc.getRole(roleDN);
                AMTemplate templ = role.getTemplate(serviceName,
                        AMTemplate.DYNAMIC_TEMPLATE);
                if (templ != null && templ.isExists()) {
                    return templ.getAttributes(attrNames);
                } else {
                    Object args[] = { serviceName };
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "101",
                            args);
                }
            } catch (AMException ame) {
                throw IdUtils.convertAMException(ame);
            }
        } else {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#modifyService(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.lang.String,
     *      java.util.Map)
     */
    public void modifyService(SSOToken token, IdType type, String name,
            String serviceName, SchemaType sType, Map attrMap)
            throws IdRepoException, SSOException {

        if (type.equals(IdType.AGENT) || type.equals(IdType.GROUP)) {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        } else if (type.equals(IdType.USER)) {
            if (sType.equals(SchemaType.DYNAMIC)) {
                Object args[] = { this.getClass().getName(), sType.toString(),
                        type.getName() };
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "214", args);
            } else {
                setAttributes(token, type, name, attrMap, false);
            }
        } else if (type.equals(IdType.ROLE)) {
            // Need to modify COS definition and COS template.
            if (sType.equals(SchemaType.USER)) {
                Object args[] = { this.getClass().getName(), sType.toString(),
                        type.getName() };
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "214", args);
            }
            try {
                AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(
                        token) : sc;
                String roleDN = getDN(type, name);
                AMRole role = amsc.getRole(roleDN);
                AMTemplate templ = role.getTemplate(serviceName,
                        AMTemplate.DYNAMIC_TEMPLATE);
                if (templ != null && templ.isExists()) {
                    templ.setAttributes(attrMap);
                    templ.store();
                } else {
                    Object args[] = { serviceName };
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "101",
                            args);
                }
            } catch (AMException ame) {
                throw IdUtils.convertAMException(ame);
            }
        } else if (type.equals(IdType.FILTEREDROLE)
                || type.equals(IdType.REALM)) {
            // Need to modify COS definition and COS template.
            if (sType.equals(SchemaType.USER)) {
                Object args[] = { this.getClass().getName(), sType.toString(),
                        type.getName() };
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "214", args);
            }
            try {
                AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(
                        token) : sc;
                String roleDN = getDN(type, name);
                AMFilteredRole role = amsc.getFilteredRole(roleDN);
                AMTemplate templ = role.getTemplate(serviceName,
                        AMTemplate.DYNAMIC_TEMPLATE);
                if (templ != null && templ.isExists()) {
                    templ.setAttributes(attrMap);
                    templ.store();
                } else {
                    Object args[] = { serviceName };
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "101",
                            args);
                }
            } catch (AMException ame) {
                throw IdUtils.convertAMException(ame);
            }
        }
    }

    private void loadSupportedOps() {
        Set opSet = new HashSet();
        opSet.add(IdOperation.CREATE);
        opSet.add(IdOperation.DELETE);
        opSet.add(IdOperation.EDIT);
        opSet.add(IdOperation.READ);
        opSet.add(IdOperation.SERVICE);

        supportedOps.put(IdType.USER, Collections.unmodifiableSet(opSet));
        supportedOps.put(IdType.ROLE, Collections.unmodifiableSet(opSet));
        supportedOps.put(IdType.FILTEREDROLE, Collections
                .unmodifiableSet(opSet));
        Set op2Set = new HashSet(opSet);

        op2Set.remove(IdOperation.SERVICE);
        supportedOps.put(IdType.GROUP, Collections.unmodifiableSet(op2Set));
        supportedOps.put(IdType.AGENT, Collections.unmodifiableSet(op2Set));
        Set op3Set = new HashSet(opSet);
        op3Set.remove(IdOperation.CREATE);
        op3Set.remove(IdOperation.DELETE);
        op3Set.remove(IdOperation.EDIT);
        supportedOps.put(IdType.REALM, Collections.unmodifiableSet(op3Set));
    }

    private String getDefaultPeopleContainerName() {
        String gcName = "People";
        try {
            ServiceSchemaManager ssm = new ServiceSchemaManager(ADMIN_SERVICE,
                    adminToken);
            if (ssm != null) {
                ServiceSchema ss = ssm.getGlobalSchema();
                if (ss != null) {
                    Map attrs = ss.getAttributeDefaults();
                    Set vals = (Set) attrs.get(PC_ATTR);
                    if (vals != null && !vals.isEmpty())
                        gcName = (String) vals.iterator().next();
                }

            }
        } catch (SMSException smse) {
            debug.error("AMSDKRepo.getDefaultGC: SMSException: ", smse);
        } catch (SSOException ssoe) {
            debug.error("AMSDKRepo.getDefaultGC: SSOException", ssoe);
        }
        return gcName;
    }

    private String getDefaultGroupContainerName() {
        String gcName = "Groups";
        try {
            ServiceSchemaManager ssm = new ServiceSchemaManager(ADMIN_SERVICE,
                    adminToken);
            if (ssm != null) {
                ServiceSchema ss = ssm.getGlobalSchema();
                if (ss != null) {
                    Map attrs = ss.getAttributeDefaults();
                    Set vals = (Set) attrs.get(GC_ATTR);
                    if (vals != null && !vals.isEmpty())
                        gcName = (String) vals.iterator().next();
                }

            }
        } catch (SMSException smse) {
            debug.error("AMSDKRepo.getDefaultAC: SMSException: ", smse);
        } catch (SSOException ssoe) {
            debug.error("AMSDKRepo.getDefaultAC: SSOException", ssoe);
        }
        return gcName;
    }

    private String getDefaultAgentContainerName() {
        String gcName = "Agent";
        try {
            ServiceSchemaManager ssm = new ServiceSchemaManager(ADMIN_SERVICE,
                    adminToken);
            if (ssm != null) {
                ServiceSchema ss = ssm.getGlobalSchema();
                if (ss != null) {
                    Map attrs = ss.getAttributeDefaults();
                    Set vals = (Set) attrs.get(AC_ATTR);
                    if (vals != null && !vals.isEmpty())
                        gcName = (String) vals.iterator().next();
                }

            }
        } catch (SMSException smse) {
            debug.error("AMSDKRepo.getDefaultAC: SMSException: ", smse);
        } catch (SSOException ssoe) {
            debug.error("AMSDKRepo.getDefaultAC: SSOException", ssoe);
        }
        return gcName;
    }

    private AMOrganization checkAndGetOrg(SSOToken token)
            throws IdRepoException, SSOException {
        AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(token)
                : sc;
        try {
            int orgType = amsc.getAMObjectType(orgDN);
            if (orgType != AMObject.ORGANIZATION) {
                debug.error("AMSDKRepo.create(): Incorrectly configured "
                        + " plugin: Org DN is wrong = " + orgDN);
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "303", null);
            }
        } catch (AMException ame) {
            debug.error("AMSDKRepo.create(): An exception occured while "
                    + " initializing AM SDK ", ame);
            Object[] args = { "com.iplanet.am.sdk.AMSDKRepo",
                    IdOperation.CREATE.getName() };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "304", args);
        }
        return amsc.getOrganization(orgDN);

    }

    private String getDN(IdType type, String name) throws IdRepoException,
            SSOException {
        if (DN.isDN(name) && (name.indexOf(",") > -1)) {
            // If should contain at least one comma for it to be a DN
            return name;
        }
        String dn;
        if (sc == null) {
            // initialization error. Throw an exception
            throw new IdRepoException(AMSDKBundle.BUNDLE_NAME, "301", null);
        }
        if (type.equals(IdType.USER)) {
            dn = AMNamingAttrManager.getNamingAttr(AMObject.USER) + "=" + name
                    + ",ou=" + getDefaultPeopleContainerName() + "," + orgDN;
            try {
                int sdkType = sc.getAMObjectType(dn);
                if (sdkType != AMObject.USER) {
                    Object[] args = { AMStoreConnection
                            .getAMObjectName(sdkType) };
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "217",
                            args);
                }
            } catch (AMException ame) {
                throw IdUtils.convertAMException(ame);
            }
        } else if (type.equals(IdType.AGENT)) {
            dn = AMNamingAttrManager.getNamingAttr(100) + "=" + name + ",ou="
                    + getDefaultAgentContainerName() + "," + orgDN;
            try {
                int sdkType = sc.getAMObjectType(dn);
                if (sdkType != 100) {
                    Object[] args = { AMStoreConnection
                            .getAMObjectName(sdkType) };
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "217",
                            args);
                }
            } catch (AMException ame) {
                throw IdUtils.convertAMException(ame);
            }
        } else if (type.equals(IdType.GROUP)) {
            dn = AMNamingAttrManager.getNamingAttr(AMObject.GROUP) + "=" + name
                    + ",ou=" + getDefaultGroupContainerName() + "," + orgDN;
            try {
                int sdkType = sc.getAMObjectType(dn);
                if (sdkType != AMObject.GROUP
                        && sdkType != AMObject.STATIC_GROUP) {
                    Object[] args = { AMStoreConnection
                            .getAMObjectName(sdkType) };
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "217",
                            args);
                }
            } catch (AMException ame) {
                throw IdUtils.convertAMException(ame);
            }
        } else if (type.equals(IdType.ROLE)) {
            dn = AMNamingAttrManager.getNamingAttr(AMObject.ROLE) + "=" + name
                    + "," + orgDN;
            try {
                int sdkType = sc.getAMObjectType(dn);
                if (sdkType != AMObject.ROLE) {
                    Object[] args = { AMStoreConnection
                            .getAMObjectName(sdkType) };
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "217",
                            args);
                }
            } catch (AMException ame) {
                throw IdUtils.convertAMException(ame);
            }
        } else if (type.equals(IdType.FILTEREDROLE)) {
            dn = AMNamingAttrManager.getNamingAttr(AMObject.FILTERED_ROLE)
                    + "=" + name + "," + orgDN;
            try {
                int sdkType = sc.getAMObjectType(dn);
                if (sdkType != AMObject.FILTERED_ROLE) {
                    Object[] args = { AMStoreConnection
                            .getAMObjectName(sdkType) };
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "217",
                            args);
                }
            } catch (AMException ame) {
                throw IdUtils.convertAMException(ame);
            }
        } else if (type.equals(IdType.REALM)) {
            // Hidden filtered role. No check should be done here
            dn = dn = AMNamingAttrManager.getNamingAttr(AMObject.FILTERED_ROLE)
                    + "=" + name + "," + orgDN;
        } else {
            Object[] args = { CLASS_NAME, IdOperation.READ.getName(),
                    type.getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "305", args);
        }
        return dn;
    }

    private int getProfileType(IdType type) throws IdRepoException {
        int profileType;
        if (type.equals(IdType.USER)) {
            profileType = AMObject.USER;
        } else if (type.equals(IdType.AGENT)) {
            profileType = 100;
        } else if (type.equals(IdType.GROUP)) {
            profileType = AMObject.GROUP;
        } else if (type.equals(IdType.ROLE)) {
            profileType = AMObject.ROLE;
        } else if (type.equals(IdType.FILTEREDROLE)) {
            profileType = AMObject.FILTERED_ROLE;
        } else if (type.equals(IdType.REALM) || type.equals(IdType.REALM)) {
            profileType = AMObject.FILTERED_ROLE;
        } else {
            Object[] args = { CLASS_NAME, IdOperation.READ.getName(),
                    type.getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "305", args);
        }
        return profileType;
    }

    private Set readObjectClass(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException {
        Set attrNameSet = new HashSet();
        attrNameSet.add("objectclass");
        Map objectClassesMap = getAttributes(token, type, name, attrNameSet);
        Set OCValues = (Set) objectClassesMap.get("objectclass");
        return OCValues;
    }

    private Set convertToLowerCase(Set vals) {
        if (vals == null || vals.isEmpty()) {
            return vals;
        } else {
            Set tSet = new HashSet();
            Iterator it = vals.iterator();
            while (it.hasNext()) {
                tSet.add(((String) it.next()).toLowerCase());
            }
            return tSet;
        }
    }

    protected static String constructFilter(int filterModifier, Map avPairs) {
        StringBuffer filterSB = new StringBuffer();
        if (filterModifier == IdRepo.NO_MOD) {
            return null;
        } else if (filterModifier == IdRepo.OR_MOD) {
            filterSB.append("(|");
        } else if (filterModifier == IdRepo.AND_MOD) {
            filterSB.append("(&");
        }

        Iterator iter = avPairs.keySet().iterator();

        while (iter.hasNext()) {
            String attributeName = (String) (iter.next());
            Iterator iter2 = ((Set) (avPairs.get(attributeName))).iterator();

            while (iter2.hasNext()) {
                String attributeValue = (String) iter2.next();
                filterSB.append("(").append(attributeName).append("=").append(
                        attributeValue).append(")");
            }
        }
        filterSB.append(")");
        return filterSB.toString();
    }
}
