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
 * $Id: ResponseAttributeTests.java,v 1.1 2008-04-18 20:04:47 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.agents;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.AgentsCommon;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.SMSCommon;
import com.sun.identity.qatest.common.SMSConstants;
import com.sun.identity.qatest.common.TestCommon;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import org.testng.Reporter;

/**
 * This class tests Header attributes related to Response. 
 * Attributes are tested using a webapp or a cgi script
 * which can read the header attributes in the browser. Attributes
 * are tested for new and updated values for different profile.
 */

public class ResponseAttributeTests extends TestCommon {
    
    private boolean executeAgainstOpenSSO;
    private String logoutURL;
    private String strScriptURL;
    private String strLocRB = "HeaderAttributeTests";
    private String strGblRB = "agentsGlobal";
    private String resource;
    private URL url;
    private WebClient webClient;
    private int polIdx;
    private int resIdx;
    private int iIdx;
    private AgentsCommon mpc;
    private AMIdentity amid;
    private IDMCommon idmc;
    private SMSCommon smsc;
    private ResourceBundle rbg;
    private SSOToken usertoken;
    private SSOToken admintoken;
    private int sleepTime = 2000;
    private int pollingTime;
    private String strAgentType;
    private String strHeaderFetchMode;
    private String agentId;
    
    /**
     * Instantiated different helper class objects
     */
    public ResponseAttributeTests() 
    throws Exception {
        super("ResponseAttributeTests");
        mpc = new AgentsCommon();
        idmc = new IDMCommon();
        rbg = ResourceBundle.getBundle(strGblRB);
        executeAgainstOpenSSO = new Boolean(rbg.getString(strGblRB +
                ".executeAgainstOpenSSO")).booleanValue();
        pollingTime = new Integer(rbg.getString(strGblRB +
                ".pollingInterval")).intValue();
        admintoken = getToken(adminUser, adminPassword, basedn);
        smsc = new SMSCommon(admintoken);
        logoutURL = protocol + ":" + "//" + host + ":" + port + uri +
                "/UI/Logout";
    }

    /**
     * Two Argument constructor initialising the ScriptURL 
     * and resource being tested
     */
    public ResponseAttributeTests(String strScriptURL, String strResource) 
      throws Exception {
        this();
        url = new URL(strScriptURL);
        resource = strResource;
   }
  
    /**
     * Evaluates newly created response attribute which holds a single static
     * value
     */
    public void evaluateNewSingleValuedStaticResponseAttribute()
    throws Exception {
        entering("evaluateNewSingleValuedStaticResponseAttribute", null);
        webClient = new WebClient();
        webClient.setCookiesEnabled(true);
        try {
            HtmlPage page = consoleLogin(webClient, resource, "rauser",
                    "rauser");
            page = (HtmlPage)webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page,
                    "HTTP_RESPONSE_STATSINGLE:10");
            Reporter.log("Resource: " + url);   
            Reporter.log("Username: " + "rauser");   
            Reporter.log("Password: " + "rauser");   
            Reporter.log("Expected Result: " + "HTTP_RESPONSE_STATSINGLE:10"); 
            assert (iIdx != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateNewSingleValuedStaticResponseAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("evaluateNewSingleValuedStaticResponseAttribute");
    }

    /**
     * Evaluates newly created response attribute which holds multiple static
     * value
     */
    public void evaluateNewMultiValuedStaticResponseAttribute()
    throws Exception {
        entering("evaluateNewMultiValuedStaticResponseAttribute", null);
        webClient = new WebClient();
        try {
            HtmlPage page = consoleLogin(webClient, resource, "rauser",
                    "rauser");
            page = (HtmlPage)webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page,
                    "HTTP_RESPONSE_STATMULTIPLE:20|30");
            Reporter.log("Resource: " + url);   
            Reporter.log("Username: " + "rauser");   
            Reporter.log("Password: " + "rauser");   
            Reporter.log("Expected Result: " + 
                    "HTTP_RESPONSE_STATMULTIPLE:20|30"); 
            assert (iIdx != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateNewMultiValuedStaticResponseAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("evaluateNewMultiValuedStaticResponseAttribute");
    }

    /**
     * Evaluates newly created response attribute which holds a single dynamic
     * value
     */
    public void evaluateDynamicResponseAttribute()
    throws Exception {
        entering("evaluateDynamicResponseAttribute", null);
        webClient = new WebClient();
        try {
            HtmlPage page = consoleLogin(webClient, resource, "rauser",
                    "rauser");
            page = (HtmlPage)webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page, "HTTP_RESPONSE_CN:rauser");
            Reporter.log("Resource: " + url);   
            Reporter.log("Username: " + "rauser");   
            Reporter.log("Password: " + "rauser");   
            Reporter.log("Expected Result: " + 
                     "HTTP_RESPONSE_CN:rauser"); 
            assert (iIdx != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateDynamicResponseAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("evaluateDynamicResponseAttribute");
    }

    /**
     * Evaluates updated response attribute which holds a single dynamic value
     */
    public void evaluateUpdatedDynamicResponseAttribute()
    throws Exception {
        entering("evaluateUpdatedDynamicResponseAttribute", null);
        webClient = new WebClient();
        try {
            HtmlPage page = consoleLogin(webClient, resource, "rauser",
                    "rauser");
            page = (HtmlPage)webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page, "HTTP_RESPONSE_CN:rauser");
            assert (iIdx != -1);
            Set set = new HashSet();
            set.add("1");
            smsc.updateSvcAttribute("iPlanetAMPolicyConfigService",
                    "iplanet-am-policy-config-subjects-result-ttl", set,
                    "Organization");
            AMIdentity amid = idmc.getFirstAMIdentity(admintoken, "rauser"
                    , IdType.USER, realm);
            Set set1 = amid.getAttribute("cn");
            Iterator itr = set1.iterator();
            while (itr.hasNext()) {
                log(Level.FINEST, "evaluateUpdatedDynamicResponseAttribute",
                        " Attribute cn= " + (String)itr.next());
            }            
            Map map = new HashMap();
            set = new HashSet();
            set.add("rauserupdated");
            map.put("cn", set);
            log(Level.FINEST, "evaluateUpdatedDynamicResponseAttribute",
                    "Update Attribute List: " + map);
            set1 = amid.getAttribute("cn");
            itr = set1.iterator();
            while (itr.hasNext()) {
                log(Level.FINEST, "evaluateUpdatedDynamicResponseAttribute",
                        "Attribute cn= " + (String)itr.next());
            }            
            idmc.modifyIdentity(amid, map);
            boolean isFound = false;
            long time = System.currentTimeMillis();
            while (System.currentTimeMillis() - time < pollingTime &&
                !isFound) {
                page = (HtmlPage)webClient.getPage(url);
                iIdx = -1;
                iIdx = getHtmlPageStringIndex(page, 
                        "HTTP_RESPONSE_CN:rauserupdated", false);
                if (iIdx != -1) {
                    isFound = true;
                }
            }
            log(Level.FINEST, "evaluateUpdatedDynamicResponseAttribute",
                    "waited for : " + (System.currentTimeMillis() - time) );
            page = (HtmlPage)webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page,
                    "HTTP_RESPONSE_CN:rauserupdated");
            Reporter.log("Resource: " + url);   
            Reporter.log("Username: " + "rauser");   
            Reporter.log("Password: " + "rauser");   
            Reporter.log("Expected Result: " + 
                    "HTTP_RESPONSE_CN:rauserupdated"); 
            assert (iIdx != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateUpdatedDynamicResponseAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("evaluateUpdatedDynamicResponseAttribute");
    }
    
    /**
     * Deletes policies, identities and updates service attributes to default
     * values.
     */
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        try {
            Set set = new HashSet();
            set.add("10");
/*            smsc.updateSvcAttribute("iPlanetAMPolicyConfigService",
                    "iplanet-am-policy-config-subjects-result-ttl", set,
                    "Organization");
            smsc.removeServiceAttributeValues("iPlanetAMPolicyConfigService",
                    "sun-am-policy-dynamic-response-attributes",
                    "Organization");*/
            if (idmc.searchIdentities(admintoken, "rauser",
                    IdType.USER).size() != 0)
               idmc.deleteIdentity(admintoken, realm, IdType.USER, "rauser");
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            destroyToken(admintoken);
        }
        exiting("cleanup");
    }
}
