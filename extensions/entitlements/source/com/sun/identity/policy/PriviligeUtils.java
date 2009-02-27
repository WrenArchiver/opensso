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
 * $Id: PriviligeUtils.java,v 1.6 2009-02-27 06:05:42 dillidorai Exp $
 */

package com.sun.identity.policy;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.ECondition;
import com.sun.identity.entitlement.EResourceAttributes;
import com.sun.identity.entitlement.ESubject;
import com.sun.identity.entitlement.UserESubject;
import com.sun.identity.entitlement.GroupESubject;
import com.sun.identity.entitlement.RoleESubject;
import com.sun.identity.entitlement.OrESubject;
import com.sun.identity.entitlement.NotESubject;
import com.sun.identity.entitlement.Privilige;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.policy.interfaces.ResponseProvider;
import com.sun.identity.policy.interfaces.Subject;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONObject;

/**
 * Class with utility methods to map from
 * <code>com.sun.identity.entity.Privilige</code>
 * to
 * </code>com.sun.identity.policy.Policy</code>
 */
public class PriviligeUtils {

    /**
     * Constructs PriviligeUtils
     */
    private PriviligeUtils() {
    }

    /**
     * Maps a OpenSSO Policy to entitlement Privilige
     * @param policy OpenSSO Policy object
     * @return entitlement Privilige object
     * @throws com.sun.identity.policy.PolicyException if the mapping fails
     */
    public static Privilige policyToPrivilige(Policy policy) 
            throws PolicyException {

        if (policy == null) {
            return null;
        }

        String policyName = policy.getName();

        Set ruleNames = policy.getRuleNames();
        Set<Entitlement> entitlements = new HashSet<Entitlement>();
        for (Object ruleNameObj : ruleNames) {
            String ruleName = (String)ruleNameObj;
            Rule rule = policy.getRule(ruleName);
            Entitlement entitlement = ruleToEntitlement(rule);
            entitlements.add(entitlement);
        }

        Set subjectNames = policy.getSubjectNames();
        Set nqSubjects = new HashSet();
        for (Object subjectNameObj : subjectNames) {
            String subjectName = (String)subjectNameObj;
            Subject subject = policy.getSubject(subjectName);
            Set subjectValues = subject.getValues();
            boolean exclusive = policy.isSubjectExclusive(subjectName);
            Object[] nqSubject = new Object[3];
            nqSubject[0] = subjectName;
            nqSubject[1] = subject;
            nqSubject[2] = exclusive;
            nqSubjects.add(nqSubject);
        }
        ESubject eSubject = nqSubjectsToESubject(nqSubjects);
        

        Set conditionNames = policy.getConditionNames();
        Set nConditions = new HashSet();
        for (Object conditionNameObj : conditionNames) {
            String conditionName = (String)conditionNameObj;
            Condition condition = policy.getCondition(conditionName);
            Object[] nCondition = new Object[2];
            nCondition[0] = conditionName;
            nCondition[1] = condition;
            nConditions.add(nCondition);
        }
        ECondition eCondition = nConditionsToECondition(nConditions);
        
        Set rpNames = policy.getResponseProviderNames();
        Set nrps = new HashSet();
        for (Object rpNameObj : nrps) {
            String rpName = (String)rpNameObj;
            ResponseProvider rp = policy.getResponseProvider(rpName);
            Object[] nrp = new Object[2];
            nrp[0] = rpName;
            nrp[1] = rp;
            nrps.add(nrp);
        }
        EResourceAttributes eResourceAttributes = nrpsToEResourceAttributes(nrps);
        Set eResourceAttributesSet = null;
        
        eCondition = null;
        eResourceAttributes = null;
        Privilige privilige = new Privilige(policyName, entitlements, eSubject,
                eCondition, eResourceAttributesSet);
        return privilige;
    }

    static Entitlement ruleToEntitlement(Rule rule)
            throws PolicyException {
        String serviceName = rule.getServiceTypeName();
        String resourceName = rule.getResourceName();
        Map<String, Object> actionMap 
                = new HashMap<String, Object>();
        Set actionNames = rule.getActionNames();
        for (Object actionNameObj : actionNames) {
            String actionName = (String)actionNameObj;
            Set actionValues = rule.getActionValues(actionName);
            actionMap.put(actionName, actionValues);
        }
        return new Entitlement(serviceName, resourceName, actionMap);
    }

    static ESubject nqSubjectsToESubject(Set nqSubjects) {
        Set esSet = new HashSet();
        ESubject es = null;
        for (Object nqSubjectObj : nqSubjects) {
            Object[] nqSubject = (Object[])nqSubjectObj;
            Set orSubjects = new HashSet();
            String subjectName = (String)nqSubject[0];
            Subject subject = (Subject)nqSubject[1];
            if (subject instanceof
                    com.sun.identity.policy.plugins.AMIdentitySubject) {
                es = mapAMIdentitySubjectToESubject(nqSubject);
            }
            esSet.add(es);
        }
        if (esSet.size() == 1) {
            es = (ESubject)esSet.iterator().next();
        } else if (esSet.size() > 1) {
            es = new OrESubject(esSet);
        }
        return es;
    }

    static ESubject mapAMIdentitySubjectToESubject(Object[] nqSubject) {
        Set esSet = new HashSet();
        ESubject es = null;
        String subjectName = (String)nqSubject[0];
        Subject subject = (Subject)nqSubject[1];
        Set values = subject.getValues();
        if (values == null || values.isEmpty()) {
            es = new UserESubject(null, subjectName);
            Boolean exclusive  = (Boolean)nqSubject[2];
            if (exclusive) {
                es = new NotESubject(es, subjectName);
            }
            return es;
        }
        for (Object valueObj : values) {
            String value = (String)valueObj;
            AMIdentity amIdentity = null;
            SSOToken ssoToken = null;
            amIdentity = null;
            IdType idType = null;
            try {
                ssoToken = ServiceTypeManager.getSSOToken();
                amIdentity = IdUtils.getIdentity(ssoToken, value);
                if (amIdentity != null) {
                    idType = amIdentity.getType();
                }
            } catch (SSOException ssoe) {
            } catch (IdRepoException idre) {
            }
            es = null;
            if (IdType.USER.equals(idType)) {
                es = new UserESubject(value, subjectName);
            } else if (IdType.GROUP.equals(idType)) {
                es = new GroupESubject(value, subjectName);
            } else if (IdType.ROLE.equals(idType)) {
                es = new RoleESubject(value, subjectName);
            }
            if (es != null) {
                esSet.add(es);
            }
        }
        es = null;
        if (esSet.size() == 1) {
            es = (ESubject)esSet.iterator().next();
        } else if (esSet.size() > 1) {
            es = new OrESubject(esSet, subjectName);
        }
        Boolean exclusive  = (Boolean)nqSubject[2];
        if (exclusive) {
           es = new NotESubject(es, subjectName); 
        }
        return es;
    }
    

    static ECondition nConditionsToECondition(Set nConditions) {
        JSONObject jo = new JSONObject();
        try {
            Set joys = new HashSet();
            for (Object nConditionObj : nConditions) {
                Object[] nCondition = (Object[])nConditionObj;
                JSONObject joy = new JSONObject();
                String conditionName = (String)nCondition[0];
                Condition condition = (Condition)nCondition[1];
                String className = condition.getClass().getName();
                jo.put("conditionName", conditionName);
                joy.put("className", className);
                joy.put("properties", condition.getProperties());
                joys.add(joy);
            }
            jo.put("pconditions", joys);
        } catch (Exception e) {
        }
        jo.toString();
        /*
        PolicyECondition peCondition = new PolicyECondition();
        peCondition.setState(jo.toString());
        peCondition.setPConditions(nConditions);
        return peCondition;
        */
        return null;
    }

    static EResourceAttributes nrpsToEResourceAttributes(Set nprs) {
        //EResourceAttributes era = nrpsToEResourceAttributes(nrps);
     return null;
    }


    static Policy priviligeToPolicy(Privilige privilige) 
            throws PolicyException {
        Policy policy = null;
        policy = new Policy(privilige.getName());
        Set<Entitlement> entitlements = privilige.getEntitlements();
        for(Entitlement entitlement: entitlements) {
            Rule rule = entitlementToRule(entitlement);
            policy.addRule(rule);
        }
        return policy;
    }

    static Rule entitlementToRule(Entitlement entitlement)
            throws PolicyException {
        return null;
    }

    static Set<Subject> eSubjectToPSubjects() {
        return null;
    }

    static Set<Condition> eConditionToPConditions() {
     return null;
    }

    static Set<EResourceAttributes> ResponseProvidersToEResourceAttributes() {
     return null;
    }

    static Set<ResponseProvider> EResourceAttributesToResponseProviders() {
     return null;
    }

 
    
}
