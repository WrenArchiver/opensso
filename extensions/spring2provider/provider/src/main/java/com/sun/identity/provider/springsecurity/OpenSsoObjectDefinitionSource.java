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
 * 
 * "Portions Copyrighted 2008 Miguel Angel Alonso Negro <miguelangel.alonso@gmail.com>"
 *
 * $Id: OpenSsoObjectDefinitionSource.java,v 1.3 2009-02-21 01:43:03 malonso Exp $
 *
 */
 package com.sun.identity.provider.springsecurity;

import com.iplanet.sso.SSOToken;
import com.sun.identity.policy.ActionDecision;
import com.sun.identity.policy.PolicyDecision;
import com.sun.identity.policy.client.PolicyEvaluator;
import com.sun.identity.policy.client.PolicyEvaluatorFactory;
import org.springframework.security.AccessDeniedException;
import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.security.InsufficientAuthenticationException;
import org.springframework.security.intercept.web.FilterInvocation;
import org.springframework.security.intercept.web.FilterInvocationDefinitionSource;

import java.util.*;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.SecurityConfig;
import org.springframework.security.util.AntUrlPathMatcher;
import org.springframework.security.util.UrlMatcher;

/**
 * It is in charge of getting the security policies, <code>PolicyDecision</code>, 
 * defined for a resource and an user by web service of opensso.war application.
 */
public class OpenSsoObjectDefinitionSource implements FilterInvocationDefinitionSource, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(OpenSsoObjectDefinitionSource.class);

    /**
     * Environment params. Not used
     */
    private Map envParams = new HashMap();
    /**
     * Matcher to compile URL patterns
     */
    private UrlMatcher urlMatcher = new AntUrlPathMatcher();
    /**
     * compiled patterns of URLs which are out of authentication policies
     */
    private Collection anonymousPatterns;
    /**
     * URL patterns defined in spring configuration which are out of authentication policies
     */
    private Collection<String> anonymousUrls = new ArrayList();

    /**
     * Set the URLs defined in spring configuration which are out of authentication policies
     * @param anonymousUrls anonymous URLs
     */
    public void setAnonymousUrls(Collection<String> anonymousUrls) {
        this.anonymousUrls = anonymousUrls;
    }

    /**
     * @inheritDoc
     */
    public void afterPropertiesSet() throws Exception {
        anonymousPatterns = new ArrayList(anonymousUrls.size());
        for (Iterator<String> it = anonymousUrls.iterator(); it.hasNext();) {
            String url = it.next();
            anonymousPatterns.add(urlMatcher.compile(url));
        }
    }
    
    /**
     * @inheritDoc
     */
    public ConfigAttributeDefinition getAttributes(Object object) throws IllegalArgumentException {
        FilterInvocation filterInvocation = (FilterInvocation) object;
        if(isAnonymousUrl(filterInvocation.getRequestUrl())){
            return null;
        }
        
        SSOToken token = OpenSsoProcessingFilter.getToken(filterInvocation.getHttpRequest());
        if(token == null){
            throw new InsufficientAuthenticationException("SSOToken does not exist");
        }
        String resource = filterInvocation.getRequestUrl();
        Set actions = new HashSet();
        actions.add(filterInvocation.getHttpRequest().getMethod());

        try {
            PolicyEvaluator policyEvaluator = PolicyEvaluatorFactory.getInstance().getPolicyEvaluator("iPlanetAMWebAgentService");
            PolicyDecision policyDecision = policyEvaluator.getPolicyDecision(token, resource, actions, envParams);
            Map actionDecisions = policyDecision.getActionDecisions();
            if(actionDecisions == null || actionDecisions.isEmpty()){
                return new ConfigAttributeDefinition(new ArrayList());
            } else {
                ActionDecision actionDecision = (ActionDecision) actionDecisions.values().iterator().next();
                List configAtributes = new ArrayList();
                for (Iterator it = actionDecision.getValues().iterator(); it.hasNext();) {
                    configAtributes.add(new SecurityConfig((String)it.next()));
                }
                return new ConfigAttributeDefinition(configAtributes);
            }
        } catch (Exception e) {
            logger.error("Error", e);
            throw new AccessDeniedException("Error accessing to Opensso",e);
        }
    }

    /**
     * @inheritDoc
     * There are not validations
     */
    public Collection getConfigAttributeDefinitions() {
        return null;
    }

    /**
     * @inheritDoc
     */
    public boolean supports(Class clazz) {
        return FilterInvocation.class.isAssignableFrom(clazz);
    }

    private boolean isAnonymousUrl(String requestUrl) {
        for (Iterator it = anonymousPatterns.iterator(); it.hasNext();) {
            if(urlMatcher.pathMatchesUrl(it.next(), requestUrl)){
                return true;
            }
        }
        return false;
    }
}
