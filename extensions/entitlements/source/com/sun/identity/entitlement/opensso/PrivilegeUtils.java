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
 * at opensso/legal/CDDLv1.0.txt
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: PrivilegeUtils.java,v 1.43 2009-08-14 22:46:20 veiming Exp $
 */
package com.sun.identity.entitlement.opensso;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.OrSubject;
import com.sun.identity.entitlement.OrCondition;
import com.sun.identity.entitlement.AndCondition;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationManager;
import com.sun.identity.entitlement.AuthenticatedESubject;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.IPrivilege;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.ReferralPrivilege;
import com.sun.identity.entitlement.ResourceAttribute;
import com.sun.identity.entitlement.RoleSubject;
import com.sun.identity.entitlement.StaticAttributes;
import com.sun.identity.entitlement.UserAttributes;
import com.sun.identity.entitlement.UserSubject;
import com.sun.identity.entitlement.xacml3.XACMLPrivilegeUtils;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.policy.ActionSchema;
import com.sun.identity.policy.InvalidNameException;
import com.sun.identity.policy.NameNotFoundException;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.PolicyConfig;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.ReferralTypeManager;
import com.sun.identity.policy.Rule;
import com.sun.identity.policy.ServiceType;
import com.sun.identity.policy.ServiceTypeManager;
import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.policy.interfaces.Referral;
import com.sun.identity.policy.interfaces.ResponseProvider;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.identity.policy.plugins.AMIdentitySubject;
import com.sun.identity.policy.plugins.AuthenticatedUsers;
import com.sun.identity.policy.plugins.IDRepoResponseProvider;
import com.sun.identity.policy.plugins.PrivilegeCondition;
import com.sun.identity.policy.plugins.PrivilegeSubject;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.DNMapper;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Class with utility methods to map from
 * <code>com.sun.identity.entity.Privilege</code>
 * to
 * </code>com.sun.identity.policy.Policy</code>
 */
public class PrivilegeUtils {

    private static Random random = new Random();
    private static ServiceTypeManager svcTypeManager;

    static {
        try {
            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            svcTypeManager = new ServiceTypeManager(adminToken);
        } catch (SSOException ex) {
            PrivilegeManager.debug.error("PrivilegeUtils.<init>", ex);
        }
    }

    /**
     * Constructs XACMLPrivilegeUtils
     */
    private PrivilegeUtils() {
    }

    /**
     * Maps a OpenSSO Policy to entitlement Privilege
     * @param policy OpenSSO Policy object
     * @return entitlement Privilege object
     * @throws com.sun.identity.policy.PolicyException if the mapping fails
     */
    public static Set<IPrivilege> policyToPrivileges(Object policyObject)
        throws SSOException, PolicyException, EntitlementException {
        if (policyObject == null) {
            return Collections.EMPTY_SET;
        }

        Set<IPrivilege> privileges = new HashSet<IPrivilege>();
        if (policyObject instanceof
                com.sun.identity.entitlement.xacml3.core.Policy) {
             Privilege p = XACMLPrivilegeUtils.policyToPrivilege(
                (com.sun.identity.entitlement.xacml3.core.Policy)policyObject);
             privileges.add(p);
        } else if (policyObject instanceof Policy) {
            policyToPrivileges((Policy) policyObject, privileges);
        } else { //TODO: log error, unsupported policy type

        }
        
        return privileges;
    }

    public static void policyToPrivileges(Policy policy,
        Set<IPrivilege> privileges)
        throws SSOException, PolicyException, EntitlementException {
        String policyName = policy.getName();

        if (policy.isReferralPolicy()) {
            Map<String, Set<String>> resources = getResources(policy);
            Set<String> referredRealms = getReferrals(policy);
            ReferralPrivilege rp = new ReferralPrivilege(policyName,
                resources, referredRealms);
            rp.setDescription(policy.getDescription());
            rp.setCreationDate(policy.getCreationDate());
            rp.setCreatedBy(policy.getCreatedBy());
            rp.setLastModifiedBy(policy.getLastModifiedBy());
            rp.setLastModifiedDate(policy.getLastModifiedDate());
            rp.setActive(policy.isActive());
            privileges.add(rp);
        } else {
            Set<Entitlement> entitlements = rulesToEntitlement(policy);
            EntitlementSubject eSubject = toEntitlementSubject(policy);
            EntitlementCondition eCondition = toEntitlementCondition(policy);
            Set<ResourceAttribute> resourceAttributesSet =
                toResourceAttributes(policy);

            if (entitlements.size() == 1) {
                privileges.add(createPrivilege(policyName, policyName,
                    entitlements.iterator().next(), eSubject,
                    eCondition, resourceAttributesSet, policy));
            } else {
                for (Entitlement e : entitlements) {
                    String pName = policyName + "_" + e.getName();
                    privileges.add(createPrivilege(pName, policyName, e,
                        eSubject,
                        eCondition, resourceAttributesSet, policy));
                }
            }
        }
    }

    private static Privilege createPrivilege(
        String name,
        String policyName,
        Entitlement e,
        EntitlementSubject eSubject,
        EntitlementCondition eCondition,
        Set<ResourceAttribute> resourceAttributesSet,
        Policy policy
    ) throws EntitlementException {
        OpenSSOPrivilege privilege = new OpenSSOPrivilege();
        privilege.setName(name);
        privilege.setEntitlement(e);
        privilege.setSubject(eSubject);
        privilege.setCondition(eCondition);
        privilege.setResourceAttributes(resourceAttributesSet);
        privilege.setPolicyName(policyName);
        privilege.setDescription(policy.getDescription());
        privilege.setCreatedBy(policy.getCreatedBy());
        privilege.setLastModifiedBy(policy.getLastModifiedBy());
        privilege.setCreationDate(policy.getCreationDate());
        privilege.setLastModifiedDate(policy.getLastModifiedDate());
        privilege.setActive(policy.isActive());
        return privilege;
    }

    private static EntitlementSubject toEntitlementSubject(Policy policy)
        throws PolicyException {
        Set<String> subjectNames = policy.getSubjectNames();
        Set<EntitlementSubject> entitlementSubjects =
            new HashSet<EntitlementSubject>();

        if (subjectNames != null) {
            for (String subjectName : subjectNames) {
                Subject subject = policy.getSubject(subjectName);
                boolean exclusive = policy.isSubjectExclusive(subjectName);
                boolean dealtWith = false;
                /*
                if (subject instanceof AMIdentitySubject) {
                    AMIdentitySubject sbj = (AMIdentitySubject) subject;
                    Set<EntitlementSubject> eSubjects = toEntitlementSubject(
                        sbj, exclusive);
                    if (!eSubjects.isEmpty()) {
                        entitlementSubjects.addAll(eSubjects);
                        dealtWith = true;
                    }
                } else if (subject instanceof AuthenticatedUsers) {
                    AuthenticatedUsers sbj = (AuthenticatedUsers)subject;
                    Set<EntitlementSubject> eSubjects = toEntitlementSubject(
                        sbj, exclusive);
                    if (!eSubjects.isEmpty()) {
                        entitlementSubjects.addAll(eSubjects);
                        dealtWith = true;
                    }
                } */

                if (!dealtWith) {
                    EntitlementSubject sbj = mapGenericSubject(subjectName,
                        subject, exclusive);
                    if (sbj != null) {
                        entitlementSubjects.add(sbj);
                    }
                }
            }
        }

        if (entitlementSubjects.isEmpty()) {
            return null;
        }
        return (entitlementSubjects.size() == 1) ?
            entitlementSubjects.iterator().next() :
            new OrSubject(entitlementSubjects);
    }

    private static Set<EntitlementSubject> toEntitlementSubject(
        AMIdentitySubject sbj,
        boolean exclusive) {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());

        try {
            Set<EntitlementSubject> result = new HashSet<EntitlementSubject>();
            Set<String> values = sbj.getValues();
            
            for (String uuid : values) {
                AMIdentity amid = IdUtils.getIdentity(adminToken, uuid);
                IdType type = amid.getType();
                if (type.equals(IdType.GROUP)) {
                    OpenSSOGroupSubject grp = new OpenSSOGroupSubject(uuid);
                    grp.setExclusive(exclusive);
                    result.add(grp);
                } else if (type.equals(IdType.ROLE)) {
                    RoleSubject role = new RoleSubject(uuid);
                    role.setExclusive(exclusive);
                    result.add(role);
                } else if (type.equals(IdType.USER)) {
                    UserSubject user = new UserSubject(uuid);
                    user.setExclusive(exclusive);
                    result.add(user);
                } else {
                    return Collections.EMPTY_SET;
                }
            }
            return result;
        } catch (IdRepoException ex) {
            return Collections.EMPTY_SET;
        }
    }

    private static Set<EntitlementSubject> toEntitlementSubject(
        AuthenticatedUsers sbj,
        boolean exclusive) {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());

        Set<EntitlementSubject> result = new HashSet<EntitlementSubject>();
        result.add(new AuthenticatedESubject());
        return result;
    }


    private static EntitlementCondition toEntitlementCondition(Policy policy)
        throws PolicyException {
        Set conditionNames = policy.getConditionNames();
        Set nConditions = new HashSet();
        for (Object conditionNameObj : conditionNames) {
            String conditionName = (String) conditionNameObj;
            Condition condition = policy.getCondition(conditionName);
            Object[] nCondition = new Object[2];
            nCondition[0] = conditionName;
            nCondition[1] = condition;
            nConditions.add(nCondition);
        }
        return nConditionsToECondition(nConditions);
    }

    private static Set<ResourceAttribute> toResourceAttributes(Policy policy)
        throws PolicyException, EntitlementException {
        Set rpNames = policy.getResponseProviderNames();
        Set nrps = new HashSet();
        for (Object rpNameObj : rpNames) {
            String rpName = (String) rpNameObj;
            ResponseProvider rp = policy.getResponseProvider(rpName);
            Object[] nrp = new Object[2];
            nrp[0] = rpName;
            nrp[1] = rp;
            nrps.add(nrp);
        }
        return nrpsToResourceAttributes(nrps);
    }

    private static Set<Rule> getRules(Policy policy)
        throws NameNotFoundException {
        Set ruleNames = policy.getRuleNames();
        Set<Rule> rules = new HashSet<Rule>();
        for (Object ruleNameObj : ruleNames) {
            String ruleName = (String) ruleNameObj;
            Rule rule = policy.getRule(ruleName);
            rules.add(rule);
        }
        return rules;
    }

    private static Map<String, Set<String>> getResources(Policy policy) 
        throws NameNotFoundException {
        Map<String, Set<String>> results = new HashMap<String, Set<String>>();
        Set<Rule> rules = getRules(policy);
        for (Rule rule : rules) {
            Set<String> ruleResources = rule.getResourceNames();
            if (ruleResources != null) {
                Set<String> resourceNames = results.get(
                    rule.getApplicationName());
                if (resourceNames == null) {
                    resourceNames = new HashSet<String>();
                    results.put(rule.getApplicationName(), resourceNames);
                }
                resourceNames.addAll(ruleResources);
            }
        }
        return results;
    }

    private static Set<String> getReferrals(Policy policy) 
        throws NameNotFoundException {
        Set<String> results = new HashSet<String>();
        Set<String> names = policy.getReferralNames();
        for (String name : names) {
            Referral r = policy.getReferral(name);
            Set<String> values = r.getValues();
            for (String s : values) {
                if (DN.isDN(s)) {
                    results.add(DNMapper.orgNameToRealmName(s));
                } else {
                    results.add(s);
                }
            }
        }
        return results;
    }

    private static Set<Entitlement> rulesToEntitlement(Policy policy)
        throws PolicyException, SSOException, EntitlementException {
        Set<Rule> rules = getRules(policy);
        Set<Entitlement> entitlements = new HashSet<Entitlement>();

        for (Rule rule : rules) {
            String serviceName = rule.getServiceTypeName();
            Map<String, Boolean> actionMap = pavToPrav(rule.getActionValues(), 
                serviceName);
            String entitlementName = rule.getName();
           
            Set<String> resourceNames = new HashSet<String>();
            Set<String> ruleResources = rule.getResourceNames();
            if (ruleResources != null) {
                resourceNames.addAll(ruleResources);
            }

            Entitlement entitlement = new Entitlement(rule.getApplicationName(),
                resourceNames, actionMap);
            entitlement.setName(entitlementName);
            Set<String> excludedResourceNames1 = rule.getExcludedResourceNames();
            if (excludedResourceNames1 != null) {
                Set<String> excludedResourceNames = new HashSet<String>();
                excludedResourceNames.addAll(excludedResourceNames1);
                entitlement.setExcludedResourceNames(excludedResourceNames);
            }
            entitlements.add(entitlement);
        }

        return entitlements;
    }

    private static EntitlementCondition nConditionsToECondition(Set nConditons)
    {
        Set<EntitlementCondition> ecSet = new HashSet<EntitlementCondition>();
        for (Object nConditionObj : nConditons) {
            Object[] nCondition = (Object[]) nConditionObj;
            EntitlementCondition ec = mapGenericCondition(nCondition);
            ecSet.add(ec);
        }

        if (ecSet.isEmpty()) {
            return null;
        }
        if (ecSet.size() == 1) {
            return ecSet.iterator().next();
        }

        Map<String, Set<EntitlementCondition>> cnEntcMap =
            new HashMap<String, Set<EntitlementCondition>>();
        for (EntitlementCondition ec : ecSet) {
            String key = (ec instanceof PolicyCondition) ?
                ((PolicyCondition)ec).getClassName()
                : ec.getClass().getName();

            Set<EntitlementCondition> values = cnEntcMap.get(key);
            if (values == null) {
                values = new HashSet<EntitlementCondition>();
                cnEntcMap.put(key, values);
            }
            values.add(ec);
        }
        
        Set<String> keySet = cnEntcMap.keySet();
        if (keySet.size() == 1) {
            Set<EntitlementCondition> values =
                cnEntcMap.get(keySet.iterator().next());
            return (values.size() == 1) ? values.iterator().next() :
                new OrCondition(values);
        }

        Set andSet = new HashSet();
        for (String key : keySet) {
            Set values = (Set)cnEntcMap.get(key);
            if (values.size() == 1) {
                andSet.add(values.iterator().next());
            } else {
                andSet.add(new OrCondition(values));
            }
        }
        return new AndCondition(andSet);
    }

    private static EntitlementSubject mapGenericSubject(
        String subjectName,
        Subject objSubject,
        boolean exclusive) {
        try {
            if (objSubject instanceof
                com.sun.identity.policy.plugins.PrivilegeSubject) {
                com.sun.identity.policy.plugins.PrivilegeSubject pips =
                    (com.sun.identity.policy.plugins.PrivilegeSubject)
                    objSubject;
                Set<String> values = pips.getValues();
                String val = values.iterator().next();
                int idx = val.indexOf("=");
                String className = val.substring(0, idx);
                String state = val.substring(idx+1);
                EntitlementSubject es =
                    (EntitlementSubject)Class.forName(className).newInstance();
                es.setState(state);
                return es;
            } else {
                Subject sbj = (Subject)objSubject;
                Set<String> val = sbj.getValues();
                String className = sbj.getClass().getName();
                return new PolicySubject(subjectName,
                    className, val, exclusive);
            }
        } catch (ClassNotFoundException e) {
            PrivilegeManager.debug.error("PrivilegeUtils.mapGenericSubject", e);
        } catch (InstantiationException e) {
            PrivilegeManager.debug.error("PrivilegeUtils.mapGenericSubject", e);
        } catch (IllegalAccessException e) {
            PrivilegeManager.debug.error("PrivilegeUtils.mapGenericSubject", e);
        }
        return null;
    }

    private static EntitlementCondition mapGenericCondition(
        Object[] nCondition) {
        try {
            Object objCondition = nCondition[1];
            if (objCondition instanceof
                com.sun.identity.policy.plugins.PrivilegeCondition) {
                com.sun.identity.policy.plugins.PrivilegeCondition pipc =
                    (com.sun.identity.policy.plugins.PrivilegeCondition)
                    objCondition;
                Map<String, Set<String>> props = pipc.getProperties();
                String className = props.keySet().iterator().next();
                EntitlementCondition ec =
                    (EntitlementCondition)Class.forName(className).newInstance();
                Set<String> setValues = props.get(className);
                ec.setState(setValues.iterator().next());
                return ec;
            } else if (objCondition instanceof Condition) {
                Condition cond = (Condition)objCondition;
                Map<String, Set<String>> props = cond.getProperties();
                String className = cond.getClass().getName();
                return new PolicyCondition((String)nCondition[0], className,
                    props);
            }
        } catch (ClassNotFoundException e) {
            PrivilegeManager.debug.error(
                "PrivilegeUtils.mapGenericCondition", e);
        } catch (InstantiationException e) {
            PrivilegeManager.debug.error(
                "PrivilegeUtils.mapGenericCondition", e);
        } catch (IllegalAccessException e) {
            PrivilegeManager.debug.error(
                "PrivilegeUtils.mapGenericCondition", e);
        }
        return null;
    }

    public static Object privilegeToPolicyObject(
        String realm,
        Privilege privilege
    ) throws PolicyException, SSOException, EntitlementException {
        Object policyObject = null;
        if (PolicyPrivilegeManager.xacmlPrivilegeEnabled()) {
            policyObject = XACMLPrivilegeUtils.privilegeToPolicy(privilege);
        } else {
             policyObject = privilegeToPolicy(realm, privilege);
        }
        return policyObject;
    }

    public static Policy referralPrivilegeToPolicy(String realm,
        ReferralPrivilege referralPrivilege) throws PolicyException,
        SSOException, EntitlementException {

        Policy policy = new Policy(referralPrivilege.getName(),
            referralPrivilege.getDescription(), true);
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        javax.security.auth.Subject adminSubject =
            SubjectUtils.createSubject(adminToken);
        PolicyManager pm = new PolicyManager(adminToken, realm);
        ReferralTypeManager rm = pm.getReferralTypeManager();

        policy.setCreatedBy(referralPrivilege.getCreatedBy());
        policy.setCreationDate(referralPrivilege.getCreationDate());
        policy.setLastModifiedBy(referralPrivilege.getLastModifiedBy());
        policy.setLastModifiedDate(referralPrivilege.getLastModifiedDate());

        int count = 1;
        for (String r : referralPrivilege.getRealms()) {
            Referral referral = rm.getReferral("SubOrgReferral");
            Set<String> tmp = new HashSet<String>();
            tmp.add(r);
            referral.setValues(tmp);
            policy.addReferral("referral" + count++, referral);
        }

        Map<String, Set<String>> map =
            referralPrivilege.getOriginalMapApplNameToResources();
        count = 1;
        String realmName = (DN.isDN(realm)) ?
            DNMapper.orgNameToRealmName(realm) : realm;

        for (String appName : map.keySet()) {
            Set<String> res = map.get(appName);
            Application appl = ApplicationManager.getApplication(
                adminSubject, realmName, appName);
            if (appl == null) {
                Object[] params = {appName, realm};
                throw new EntitlementException(105, params);
            }
            String serviceName = appl.getApplicationType().getName();

            for (String r : res) {
                Rule rule = new Rule("rule" + count++, serviceName, r,
                    Collections.EMPTY_MAP);
                rule.setApplicationName(appName);
                policy.addRule(rule);
            }
        }
        return policy;
    }
    
    public static Policy privilegeToPolicy(String realm, Privilege privilege)
            throws PolicyException, SSOException, EntitlementException {
        Policy policy = new Policy(privilege.getName());
        policy.setDescription(privilege.getDescription());
        if (privilege.getEntitlement() != null) {
            Entitlement entitlement = privilege.getEntitlement();
            Set<Rule> rules = entitlementToRule(realm, entitlement);
            for (Rule rule : rules) {
                policy.addRule(rule);
            }
        }

        EntitlementSubject es = privilege.getSubject();
        if ((es != null) && (es != Privilege.NOT_SUBJECT)) {
            Subject sbj = eSubjectToEPSubject(es);
            policy.addSubject(randomName(), sbj, false);
        }

        EntitlementCondition ec = privilege.getCondition();
        if (ec != null) {
            Condition cond = eConditionToEPCondition(ec);
            policy.addCondition(randomName(), cond);
        }

        if (privilege.getResourceAttributes() != null) {
            Map<String, ResponseProvider> nrps =
                resourceAttributesToResponseProviders(
                    privilege.getResourceAttributes());
            for (String rpName : nrps.keySet()) {
                ResponseProvider responseProvider = nrps.get(rpName);
                policy.addResponseProvider(rpName, responseProvider);
            }
        }

        policy.setCreatedBy(privilege.getCreatedBy());
        policy.setCreationDate(privilege.getCreationDate());
        policy.setLastModifiedBy(privilege.getLastModifiedBy());
        policy.setLastModifiedDate(privilege.getLastModifiedDate());
        return policy;
    }

    private static Set<Rule> entitlementToRule(
        String realm,
        Entitlement entitlement
    ) throws PolicyException, SSOException, EntitlementException {
        Set<Rule> rules = new HashSet<Rule>();
        String appName = entitlement.getApplicationName();

        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance()); //TODO - who added?
        String realmName = (DN.isDN(realm)) ?
            DNMapper.orgNameToRealmName(realm) : realm;

        Application appl = ApplicationManager.getApplication(
            SubjectUtils.createSubject(adminToken), realmName, appName);
        if (appl == null) {
            Object[] params = {appName, realm};
            throw new EntitlementException(105, params);
        }
        String serviceName = appl.getApplicationType().getName();

        Set<String> resourceNames = entitlement.getResourceNames();
        Map<String, Boolean> actionValues = entitlement.getActionValues();
        Map av = pravToPav(actionValues, serviceName);

        if (resourceNames != null) {
            String entName = entitlement.getName();
            if (entName == null) {
                entName = "entitlement";
            }

            Rule rule = new Rule(entName, serviceName, null, av);
            rule.setResourceNames(resourceNames);
            rule.setExcludedResourceNames(
                entitlement.getExcludedResourceNames());
            rule.setApplicationName(appName);
            rules.add(rule);
        }
        return rules;
    }


    private static Subject eSubjectToEPSubject(EntitlementSubject es) {
        PrivilegeSubject ps = new PrivilegeSubject();
        Set<String> values = new HashSet<String>();
        values.add(es.getClass().getName() + "=" + es.getState());
        ps.setValues(values);
        return ps;
    }

    private static Condition eConditionToEPCondition(
        EntitlementCondition ec
    ) throws PolicyException {
        PrivilegeCondition pc = new PrivilegeCondition();
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>(2);
        set.add(ec.getState());
        map.put(ec.getClass().getName(), set);
        pc.setProperties(map);
        return pc;
    }

    private static Set<ResourceAttribute> nrpsToResourceAttributes(
        Set nrps) throws EntitlementException {
        Set<ResourceAttribute> resourceAttributesSet = new HashSet();
        if (nrps != null && !nrps.isEmpty()) {
            for (Object nrpObj : nrps) {
                Object[] nrpa = (Object[]) nrpObj;
                String nrpName = (String) nrpa[0];
                ResponseProvider rp = (ResponseProvider) nrpa[1];
                if (rp instanceof IDRepoResponseProvider) {
                    resourceAttributesSet.addAll(nrpsToResourceAttributes(
                        (IDRepoResponseProvider) rp, nrpName));
                }
            }
        }
        return resourceAttributesSet;
    }

    private static Set<ResourceAttribute> nrpsToResourceAttributes(
        IDRepoResponseProvider irp,
        String nrpName) throws EntitlementException {
        Map<String, ResourceAttribute> map = new
            HashMap<String, ResourceAttribute>();
        Map props = irp.getProperties();

        if ((props != null) && !props.isEmpty()) {
            Set<String> sas = (Set<String>) props.get(
                IDRepoResponseProvider.STATIC_ATTRIBUTE);

            if (sas != null && !sas.isEmpty()) {
                for (String sat : sas) {
                    int i = sat.indexOf("=");
                    String name = (i != -1) ? sat.substring(0, i) : sat;
                    String value = (i != -1) ? sat.substring(i + 1) : null;

                    String k = name + "_" +
                        IDRepoResponseProvider.STATIC_ATTRIBUTE;
                    StaticAttributes sa = (StaticAttributes) map.get(k);
                    if (sa == null) {
                        sa = new StaticAttributes();
                        sa.setPropertyName(name);
                        map.put(k, sa);
                    }
                    if (value != null) {
                        sa.getPropertyValues().add(value);
                    }
                    sa.setPResponseProviderName(nrpName);
                }
            }

            Set<String> uas = (Set<String>) props.get(
                IDRepoResponseProvider.DYNAMIC_ATTRIBUTE);

            if (uas != null && !uas.isEmpty()) {
                for (String uat : uas) {
                    int i = uat.indexOf("=");
                    String name = (i != -1) ? uat.substring(0, i) : uat;
                    String value = (i != -1) ? uat.substring(i + 1) : null;

                    String k = name + "_" +
                        IDRepoResponseProvider.DYNAMIC_ATTRIBUTE;
                    UserAttributes ua = (UserAttributes) map.get(k);
                    if (ua == null) {
                        ua = new UserAttributes();
                        ua.setPropertyName(name);
                        map.put(k, ua);
                    }
                    if (value != null) {
                        ua.getPropertyValues().add(value);
                    }
                    ua.setPResponseProviderName(nrpName);
                }
            }
        }
        Set<ResourceAttribute> results = new HashSet<ResourceAttribute>();
        results.addAll(map.values());
        return results;
    }



    private static Map<String, ResponseProvider>
        resourceAttributesToResponseProviders(
            Set<ResourceAttribute> resourceAttributes
    ) throws PolicyException {
        Map<String, ResponseProvider> results = new
            HashMap<String, ResponseProvider>();

        if (resourceAttributes != null) {
            Map<String, Map<String, Set<String>>> map = new
                HashMap<String, Map<String, Set<String>>>();

            for (ResourceAttribute ra : resourceAttributes) {
                if (ra instanceof StaticAttributes) {
                    resourceAttributesToResponseProviders(
                        (StaticAttributes)ra, map);
                } else if (ra instanceof UserAttributes) {
                    resourceAttributesToResponseProviders(
                        (UserAttributes)ra, map);
                }
            }

            for (String n : map.keySet()) {
                ResponseProvider rp = new IDRepoResponseProvider();
                Map<String, Set<String>> values = map.get(n);
                Set<String> dynValues = values.get(
                    IDRepoResponseProvider.DYNAMIC_ATTRIBUTE);

                if ((dynValues != null) && !dynValues.isEmpty()) {
                    Map<String, Set<String>> configParams = new
                        HashMap<String, Set<String>>();
                    configParams.put(PolicyConfig.SELECTED_DYNAMIC_ATTRIBUTES,
                        dynValues);
                    rp.initialize(configParams);
                }

                rp.setProperties(values);

                results.put(n, rp);
            }
        }

        return results;
    }

    private static void resourceAttributesToResponseProviders(
        StaticAttributes sa,
        Map<String, Map<String, Set<String>>> results) throws PolicyException {
        String pluginName = sa.getPResponseProviderName();

        Map<String, Set<String>> map = results.get(pluginName);
        if (map == null) {
            map = new HashMap<String, Set<String>>();
            results.put(pluginName, map);
        }

        String propertyName = sa.getPropertyName();
        Set<String> propertyValues = sa.getPropertyValues();

        Set<String> values = map.get(
            IDRepoResponseProvider.STATIC_ATTRIBUTE);
        if (values == null) {
            values = new HashSet<String>();
            map.put(IDRepoResponseProvider.STATIC_ATTRIBUTE, values);
        }

        getResponseAttributeValues(propertyName, propertyValues, values);
    }

    private static void resourceAttributesToResponseProviders(
        UserAttributes ua,
        Map<String, Map<String, Set<String>>> results) throws PolicyException {
        String pluginName = ua.getPResponseProviderName();
        
        Map<String, Set<String>> map = results.get(pluginName);
        if (map == null) {
            map = new HashMap<String, Set<String>>();
            results.put(pluginName, map);
        }

        String propertyName = ua.getPropertyName();
        Set<String> propertyValues = ua.getPropertyValues();

        Set<String> values = map.get(
            IDRepoResponseProvider.DYNAMIC_ATTRIBUTE);
        if (values == null) {
            values = new HashSet<String>();
            map.put(IDRepoResponseProvider.DYNAMIC_ATTRIBUTE, values);
        }

        getResponseAttributeValues(propertyName, propertyValues, values);
    }

    private static void getResponseAttributeValues(
        String propertyName,
        Set<String> propertyValues,
        Set<String> results) {
        if ((propertyValues != null) && !propertyValues.isEmpty()) {
            for (String v : propertyValues) {
                results.add(propertyName + "=" + v);
            }
        } else {
            results.add(propertyName);
        }
    }

    private static String randomName() {
        return "" + random.nextInt(10000);
    }

    static Map pravToPav(Map<String, Boolean> actionValues,
            String serviceName) throws PolicyException, SSOException  {
        if (actionValues == null) {
            return null;
        }

        ServiceType serviceType = null;
        try {
            serviceType = svcTypeManager.getServiceType(serviceName);
        } catch (NameNotFoundException e) {
            //ignore
        }

        Map av = new HashMap();
        Set<String> keySet = actionValues.keySet();
        for (String action : keySet) {
            try {
                Set values = new HashSet();
                Boolean value = actionValues.get(action);

                if (serviceType != null) {
                    ActionSchema as = serviceType.getActionSchema(action);
                    String trueValue = as.getTrueValue();
                    String falseValue = as.getFalseValue();
                    if (value.equals(Boolean.TRUE)) {
                        values.add(trueValue);
                    } else {
                        values.add(falseValue);
                    }
                } else {
                    values.add(value.toString());
                }

                av.put(action, values);
            } catch (InvalidNameException e) {
                Boolean value = actionValues.get(action);
                Set values = new HashSet();
                values.add(value.toString());
                av.put(action, values);
            }
        }
        return av;
    }

    static Map<String, Boolean> pavToPrav(Map actionValues,
            String serviceName) throws PolicyException, SSOException {
        if (actionValues == null) {
            return null;
        }

        ServiceType serviceType = null;
        if (serviceName != null) {
            try {
                serviceType = svcTypeManager.getServiceType(serviceName);
            } catch (NameNotFoundException e) {
                //ignore
            }
        }

        Map av = new HashMap();
        Set keySet = (Set) actionValues.keySet();
        for (Object actionObj : keySet) {
            String action = (String) actionObj;
            Set values = (Set) actionValues.get(action);
            
            if ((values == null) || values.isEmpty()) {
                av.put(action, Boolean.FALSE);
            } else {
                if (serviceType != null) {
                    try {
                        ActionSchema as = serviceType.getActionSchema(action);
                        if (as.getSyntax().equals(
                            AttributeSchema.Syntax.BOOLEAN)) {
                            String trueValue = as.getTrueValue();

                            if (values.contains(trueValue)) {
                                av.put(action, Boolean.TRUE);
                            } else {
                                av.put(action, Boolean.FALSE);
                            }
                        } else {
                            // Append action value to action name
                            String value = values.iterator().next().toString();
                            av.put(action + "_" + value, Boolean.TRUE);
                        }
                    } catch (InvalidNameException e) {
                        av.put(action, Boolean.parseBoolean(
                            (String) values.iterator().next()));
                    }
                } else {
                    if (!values.isEmpty()) {
                        av.put(action, Boolean.parseBoolean(
                            (String)values.iterator().next()));
                    }
                }
            }

        }
        return av;
    }

    static public String policyToXML(Object policy) {
        String xmlString = "";
        if (policy instanceof com.sun.identity.entitlement.xacml3.core.Policy) {
            com.sun.identity.entitlement.xacml3.core.Policy xacmlPolicy =
                    (com.sun.identity.entitlement.xacml3.core.Policy) policy;
            xmlString = com.sun.identity.entitlement.xacml3.XACMLPrivilegeUtils.toXML(
                    xacmlPolicy);
        } else if (policy instanceof com.sun.identity.policy.Policy) {
            xmlString = ((com.sun.identity.policy.Policy) policy).toXML();
        } else {
            //TODO: log error, unsupported policy type
        }
        return xmlString;
    }

    static public String getPolicyName(Object policy) {
        String name = "";
        if (policy instanceof com.sun.identity.entitlement.xacml3.core.Policy) {
            name = ((com.sun.identity.entitlement.xacml3.core.Policy)policy).getPolicyId();
        } else if (policy instanceof Policy) {
            name = ((Policy)policy).getName();
        } else {
            //TODO: log error, unsupported policy type
        }
        return name;
    }

    static public Set<IPrivilege> policyObjectToPrivileges(Object policy)
            throws EntitlementException, PolicyException, SSOException {
        //TODO: implement method, objectToPrivileges(Object object)
        Set<IPrivilege> privileges = null;
        if (policy instanceof com.sun.identity.entitlement.xacml3.core.Policy) {
            Privilege privilege = XACMLPrivilegeUtils.policyToPrivilege(
                (com.sun.identity.entitlement.xacml3.core.Policy)policy);
            privileges = new HashSet<IPrivilege>();
            privileges.add(privilege);
        } else if (policy instanceof Policy) {
             privileges = policyToPrivileges((Policy)policy);
        } else {
            //TODO: log error, unsupported policy type
        }
        return privileges;
    }

}
