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
 * $Id: AuthUtils.java,v 1.15 2007-11-14 01:43:35 veiming Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */


package com.sun.identity.authentication.service;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;

import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Iterator;
import java.util.ResourceBundle;

import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;

import java.security.AccessController;
import java.security.Principal;

import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.callback.Callback;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Cookie;

import netscape.ldap.util.DN;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.sso.SSOException;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.share.SessionEncodeURL;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.session.util.SessionUtils;

import com.iplanet.am.util.Debug;
import com.iplanet.am.util.AMClientDetector;
import com.iplanet.am.util.Locale;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.am.util.Stats;
import com.iplanet.am.util.Misc;
import com.sun.identity.common.Constants;

import com.iplanet.services.cdm.Client;
import com.iplanet.services.cdm.AuthClient;
import com.iplanet.services.cdm.ClientsManager;
import com.iplanet.services.util.Crypt;
import com.iplanet.services.util.CookieUtils;
import com.iplanet.services.naming.WebtopNaming;

import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.security.EncodeAction;

import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.config.AMAuthLevelManager;
import com.sun.identity.authentication.config.AMAuthConfigUtils;
import com.sun.identity.authentication.server.AuthContextLocal;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.authentication.util.AMAuthUtils;
import com.sun.identity.authentication.client.AuthClientUtils;

import com.sun.identity.common.ResourceLookup;
import com.sun.identity.common.Constants;
import com.sun.identity.common.DNUtils;
import com.sun.identity.common.FQDNUtils;
import com.sun.identity.common.RequestUtils;
import com.sun.identity.common.ISLocaleContext;

import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.DNMapper;

import com.sun.identity.policy.PolicyUtils;
import com.sun.identity.policy.plugins.AuthLevelCondition;
import com.sun.identity.policy.plugins.AuthSchemeCondition;
import com.sun.identity.policy.plugins.AuthenticateToServiceCondition;
import com.sun.identity.policy.plugins.AuthenticateToRealmCondition;

public class AuthUtils extends AuthClientUtils {
    
    public static final String BUNDLE_NAME="amAuth";
    
    /**
     * Authentication type for Realm based authentication after
     * Composite Advices
     */
    public static final int REALM = 1;
    
    /**
     * Authentication type for Service based authentication after 
     * Composite Advices
     */
    public static final int SERVICE = 2;
    
    /**
     * Authentication type for Module based authentication after 
     * Composite Advices
     */
    public static final int MODULE = 3;
    
    
    private static ArrayList pureJAASModuleClasses = new ArrayList();
    private static ArrayList ISModuleClasses = new ArrayList();
    private static Hashtable moduleService = new Hashtable();
    private static ResourceBundle bundle;
    static Debug utilDebug = Debug.getInstance("amAuthUtils");    
   
    public AuthUtils() {
        utilDebug.message("AuthUtil: constructor");
    }
    
    /* retrieve session */
    public com.iplanet.dpro.session.service.InternalSession
    getSession(AuthContextLocal authContext) {
        
        com.iplanet.dpro.session.service.InternalSession sess =
        getLoginState(authContext).getSession();
        if (utilDebug.messageEnabled()) {
            utilDebug.message("returning session : " + sess);
        }
        return sess;
    }
    
    /* this method does the following
     * 1. initializes authService (AuthD) if not already done.
     * 2. parses the request parameters and stores in dataHash
     * 3. Retrieves the AuthContext object from the global table
     * 4. if this is found then updates the loginState request
     *    type to false and updates the parameter hash table in
     *   loginstate object.
     
     * on error throws AuthException
     */
    
    /**
     * Returns the authentication context for a request.
     *
     * @param request HTTP Servlet Request.
     * @param response HTTP Servlet Response.
     * @param sid SessionID for this request.
     * @param isSessionUpgrade <code>true</code> if session upgrade.
     * @param isBackPost <code>true</code> if back posting.
     * @return authentication context.
     */
    public static AuthContextLocal getAuthContext(
        HttpServletRequest request,
        HttpServletResponse response,
        SessionID sid,
        boolean isSessionUpgrade,
        boolean isBackPost) throws AuthException {
        return getAuthContext(request,response,sid,
            isSessionUpgrade,isBackPost,false);
    }
    
    /**
     * Returns the authentication context for a request.
     *
     * @param request HTTP Servlet Request.
     * @param response HTTP Servlet Response.
     * @param sid SessionID for this request.
     * @param isSessionUpgrade <code>true</code> if session upgrade.
     * @param isBackPost <code>true</code> if back posting.
     * @param isLogout <code>true</code> for logout.
     * @return authentication context.
     */
    public static AuthContextLocal getAuthContext(
        HttpServletRequest request,
        HttpServletResponse response,
        SessionID sid,
        boolean isSessionUpgrade,
        boolean isBackPost,
        boolean isLogout) throws AuthException {
        utilDebug.message("In AuthUtils:getAuthContext");
        Hashtable dataHash;
        AuthContextLocal authContext = null;
        LoginState loginState = null;
        // initialize auth service.
        AuthD ad = AuthD.getAuth();
        
        try {
            dataHash = parseRequestParameters(request);
            // commented this since it debug file
            // has too many messages and making the file large
            // in size.
            authContext = retrieveAuthContext(request, sid);
            if (utilDebug.messageEnabled()) {
                utilDebug.message("AuthUtil:getAuthContext:sid is.. .: " 
                                  + sid);
                utilDebug.message("AuthUtil:getAuthContext:authContext is..: "
                + authContext);
            }
            
            if (utilDebug.messageEnabled()) {
                utilDebug.message("isSessionUpgrade  :" + isSessionUpgrade);
                utilDebug.message("BACK with Request method POST : " 
                                  + isBackPost);
            }
            
            if ((authContext == null)  && (isLogout)) {
                return null;
            }
            
            if ((authContext == null) || (isSessionUpgrade) || (isBackPost)) {
                try {
                    loginState = new LoginState();
                    if (isSessionUpgrade) {
                        loginState.setPrevAuthContext(authContext);
                        loginState.setSessionUpgrade(isSessionUpgrade);
                    } else if (isBackPost) {
                        loginState.setPrevAuthContext(authContext);
                    }
                    
                    authContext =
                    loginState.createAuthContext(request,response,sid,dataHash);
                    authContext.setLoginState(loginState);
                    String queryOrg =
                    getQueryOrgName(request,getOrgParam(dataHash));
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message("query org is .. : "+ queryOrg);
                    }
                    loginState.setQueryOrg(queryOrg);
                } catch (AuthException ae) {
                    utilDebug.message("Error creating AuthContextLocal : ");
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message("Exception " , ae);
                    }
                    throw new AuthException(ae);
                }
            } else {
                utilDebug.message("getAuthContext: found existing request.");
                
                authContext = processAuthContext(authContext,request,
				response,dataHash,sid);
				loginState = getLoginState(authContext);
				loginState.setRequestType(false);
            }
            
        } catch (Exception ee) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error creating AuthContextLocal : " 
                                  + ee.getMessage());
            }
            
            throw new AuthException(ee);
        }
        return authContext;
        
    }
    
    
    // processAuthContext checks for arg=newsession in the HttpServletRequest
    // if request has arg=newsession then destroy session and create a new
    // AuthContextLocal object.
    
    static AuthContextLocal processAuthContext(AuthContextLocal authContext,
    HttpServletRequest request,
    HttpServletResponse response,
    Hashtable dataHash,
    SessionID sid) throws AuthException {
        // initialize auth service.
        AuthD ad = AuthD.getAuth();
        
        LoginState loginState = getLoginState(authContext);
        com.iplanet.dpro.session.service.InternalSession sess = null;
        
        if (utilDebug.messageEnabled()) {
            utilDebug.message("in processAuthContext authcontext : " 
                + authContext );
            utilDebug.message("in processAuthContext request : " + request);
            utilDebug.message("in processAuthContext response : " + response);
            utilDebug.message("in processAuthContext sid : " + sid);
        }
        
        if (newSessionArgExists(dataHash, sid) &&
        (loginState.getLoginStatus() == LoginStatus.AUTH_SUCCESS)) {
            // destroy auth context and create new one.
            utilDebug.message("newSession arg exists");
            destroySession(loginState);
            try{
                loginState = new LoginState();
                authContext = loginState.createAuthContext(request,response,
                sid,dataHash);
                authContext.setLoginState(loginState);
                String queryOrg =
                getQueryOrgName(request,getOrgParam(dataHash));
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("query org is .. : "+ queryOrg);
                }
                loginState.setQueryOrg(queryOrg);
            } catch (AuthException ae) {
                utilDebug.message("Error creating AuthContextLocal");
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("Exception " , ae);
                }
                throw new AuthException(AMAuthErrorCode.AUTH_ERROR, null);
            }
        } else {
            if (authContext.submittedRequirements()) {
                ad.debug.error("Currently processing submit Requirements");
                throw new AuthException(
                         AMAuthErrorCode.AUTH_TOO_MANY_ATTEMPTS, null);
            }
            // update loginState - requestHash , sess
            utilDebug.message("new session arg does not exist");
            loginState.setHttpServletRequest(request);
            loginState.setHttpServletResponse(response);
            loginState.setParamHash(dataHash);
            sess = ad.getSession(sid);
            if (utilDebug.messageEnabled()) {
                utilDebug.message("AuthUtil :Session is .. : " + sess);
            }
            loginState.setSession(sess);
            loginState.persistentCookieArgExists();
            loginState.setRequestLocale(request);
            if (checkForCookies(request)) {
                loginState.setCookieDetect(false);
            }
        }
        return authContext;
    }
    
    public static LoginState getLoginState(AuthContextLocal authContext) {
        
        LoginState loginState = null;
        if (authContext != null) {
            loginState = authContext.getLoginState();
        }
        return loginState;
    }       
   
    public Hashtable getRequestParameters(AuthContextLocal authContext) {
        LoginState loginState = getLoginState(authContext);
        if (loginState != null) {
            return loginState.getRequestParamHash();
        } else {
            return new Hashtable();
        }
    }
    
    // retrieve the sid from the LoginState object
    public static String getSidString(AuthContextLocal authContext)
    throws AuthException {
        com.iplanet.dpro.session.service.InternalSession sess = null;
        String sidString = null;
        try {
            if (authContext != null) {
                LoginState loginState = authContext.getLoginState();
                if (loginState != null) {
                    SessionID sid = loginState.getSid();
                    if (sid != null) {
                        sidString = sid.toString();
                    }
                }
            }
        } catch (Exception  e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error retreiving sid.. :" + e.getMessage());
            }
            // no need to have error code since the method where this is called
            // generates AUTH_ERROR
            throw new AuthException("noSid", new Object[] {e.getMessage()});
        }
        return sidString;
    }
    
    /**
     * Returns the Cookie object created based on the cookie name,
     * Session ID and cookie domain. If Session is in invalid State then
     * cookie is created with authentication cookie name , if
     * Active/Inactive Session state AM Cookie Name will be used to create
     * cookie.
     *
     * @param ac the AuthContext object
     *@param cookieDomain the cookie domain for creating cookie
     * @return Cookie object.
     */
    public Cookie getCookieString(AuthContextLocal ac,String cookieDomain) {
        
        Cookie cookie=null;
        String cookieName = getCookieName();
        try {
            String sidString= getSidString(ac);
            LoginState loginState = getLoginState(ac);
            if (loginState != null && loginState.isSessionInvalid()) {
                cookieName = getAuthCookieName();
                utilDebug.message("Create AM AUTH cookie");
            }
            cookie = createCookie(cookieName,sidString,cookieDomain);
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error getting sid : " + e.getMessage());
            }
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("Cookie is : " + cookie);
        }
        return cookie;
    }
    
    /**
     * Returns the Logout cookie.
     *
     * @param ac the AuthContextLocal object
     * @param cookieDomain the cookieDomain
     * @return Logout cookie .
     */
    public Cookie getLogoutCookie(AuthContextLocal ac, String cookieDomain) {
        LoginState loginState = getLoginState(ac);
        SessionID sid = loginState.getSid();
        String logoutCookieString = getLogoutCookieString(sid);
        Cookie logoutCookie = createCookie(logoutCookieString,cookieDomain);
        logoutCookie.setMaxAge(0);
        return logoutCookie;
    }
    
    // returns true if request is new else false.    
    public boolean isNewRequest(AuthContextLocal ac) {
        
        LoginState loginState = getLoginState(ac);
        if (loginState.isNewRequest()) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("this is a newRequest");
            }
            return true;
        } else {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("this is an existing request");
            }
            return false;
        }
    }
    
    /* return the successful login url */
    public String getLoginSuccessURL(AuthContextLocal authContext) {
        String successURL = null;
        LoginState loginState = getLoginState(authContext);
        if (loginState == null) {
            successURL = AuthD.getAuth().defaultSuccessURL;
        } else {
            successURL = getLoginState(authContext).getSuccessLoginURL();
        }
        return successURL;
    }
    
    /* return the failed login url */
    public String getLoginFailedURL(AuthContextLocal authContext) {
        
        try {
            LoginState loginState = getLoginState(authContext);
            if (loginState == null) {
                return AuthD.getAuth().defaultFailureURL;
            }
            String loginFailedURL=loginState.getFailureLoginURL();
            if (utilDebug.messageEnabled()) {
                utilDebug.message("AuthUtils: getLoginFailedURL "
                                  + loginFailedURL);
            }
            
            // remove the loginstate/authContext from the hashtable
            //removeLoginStateFromHash(authContext);
            //	destroySession(authContext);
            return loginFailedURL;
        } catch (Exception e) {
            utilDebug.message("Exception " , e);
            return null;
        }
    }
    
    
    /* return filename  - will use FileLookUp API
     * for UI only - this returns the relative path
     */
    public String getFileName(AuthContextLocal authContext,String fileName) {
        
        LoginState loginState = getLoginState(authContext);
        String relFileName = null;
        if (loginState != null) {
            relFileName =
            getLoginState(authContext).getFileName(fileName);
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("getFileName:AuthUtilsFile name is :"
            + relFileName);
        }
        return relFileName;
    }
    
    public boolean getInetDomainStatus(AuthContextLocal authContext) {
        return getLoginState(authContext).getInetDomainStatus();
    }
    
    public static boolean newSessionArgExists(
        Hashtable dataHash, SessionID sid) {

        String arg = (String)dataHash.get("arg");
        if (arg != null && arg.equals("newsession")) {
            if (retrieveAuthContext(sid) != null) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
    
    public String encodeURL(String url,
    AuthContextLocal authContext,
    HttpServletResponse response) {
        if (utilDebug.messageEnabled()) {
            utilDebug.message("AuthUtils:input url is :"+ url);
        }
        LoginState loginState = getLoginState(authContext);
        String encodedURL;
        
        if (loginState==null) {
            encodedURL = url;
        } else {
            encodedURL = loginState.encodeURL(url,response);
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("AuthUtils:encoded url is :"+encodedURL);
        }
        
        return encodedURL;
    }
    
    // return the locale
    public String getLocale(AuthContextLocal authContext) {
        // initialize auth service.
        AuthD ad = AuthD.getAuth();
        
        if (authContext == null) {
            return  ad.getPlatformLocale();
        }
        
        LoginState loginState = getLoginState(authContext);
        if (loginState == null) {
            return ad.getPlatformLocale();
        }
        
        return loginState.getLocale();
    }   
   
    static void destroySession(LoginState loginState) {
        try {
            if (loginState != null) {
                loginState.destroySession();
            }
        } catch (Exception e)  {
            utilDebug.message("Error destroySEssion : " , e);
        }
    }
    
    public void destroySession(AuthContextLocal authContext) {
        if (authContext != null) {
            LoginState loginState = getLoginState(authContext);
            destroySession(loginState);
        }
    }    
   
    /**
     * Returns <code>true</code> if the session has timed out or the page has
     * timed out.
     *
     * @param authContext the authentication context object for the request.
     * @return <code>true</code> if timed out else false.
     */
    public boolean sessionTimedOut(AuthContextLocal authContext) {
        boolean timedOut = false;
        
        LoginState loginState = getLoginState(authContext);
        
        if (loginState != null) {
            timedOut = loginState.isTimedOut();
            
            if (!timedOut) {
                com.iplanet.dpro.session.service.InternalSession sess =
                    loginState.getSession();
                if ((sess == null) && AuthD.isHttpSessionUsed()) {
                    HttpSession hsess = loginState.getHttpSession();
                    timedOut = (hsess == null);
                } else if (sess != null) {
                    timedOut = sess.isTimedOut();
                }
                loginState.setTimedOut(timedOut);
            }
            
            if (utilDebug.messageEnabled()) {
                utilDebug.message("AuthUtils.sessionTimedOut: " + timedOut);
            }
        }
        return timedOut;
    }    
   
    /* return the value of argument iPSPCookie entered on the URL */
    public boolean isPersistentCookieOn(AuthContextLocal authContext) {
        return getLoginState(authContext).isPersistentCookieOn();
    }
    
    /* retrieve persistent cookie setting from core auth profile */
    public boolean getPersistentCookieMode(AuthContextLocal authContext) {
        return getLoginState(authContext).getPersistentCookieMode();
    }
    
    /* return persistent cookie */
    public Cookie getPersistentCookieString(AuthContextLocal authContext,
    String cookieDomain ) {
        return null;
    }
    
    /* returns the username from the persistent cookie */
    public String searchPersistentCookie(AuthContextLocal authContext) {
        LoginState loginState = getLoginState(authContext);
        return loginState.searchPersistentCookie();
    }
    
    public Cookie createPersistentCookie(AuthContextLocal authContext,
    String cookieDomain) throws AuthException {
        Cookie pCookie=null;
        try {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("cookieDomain : " + cookieDomain);
            }
            LoginState loginState = getLoginState(authContext);
            pCookie = loginState.setPersistentCookie(cookieDomain);
            return pCookie;
        } catch (Exception e) {
            utilDebug.message("Unable to create persistent Cookie");
            throw new AuthException(AMAuthErrorCode.AUTH_ERROR, null);
        }
    }
    
    public Cookie createlbCookie(AuthContextLocal authContext,
    String cookieDomain, boolean persist) throws AuthException {
        Cookie lbCookie=null;
        try {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("cookieDomain : " + cookieDomain);
            }
            LoginState loginState = getLoginState(authContext);
            lbCookie = loginState.setlbCookie(cookieDomain, persist);
            return lbCookie;
        } catch (Exception e) {
            utilDebug.message("Unable to create Load Balance Cookie");
            throw new AuthException(AMAuthErrorCode.AUTH_ERROR, null);
        }
        
    }
    
    public void setlbCookie(AuthContextLocal authContext,
    HttpServletResponse response) throws AuthException {
        String cookieName = getlbCookieName();
        if (cookieName != null && cookieName.length() != 0) {
            Set domains = getCookieDomains();
            if (!domains.isEmpty()) {
                for (Iterator it = domains.iterator(); it.hasNext(); ) {
                    String domain = (String)it.next();
                    Cookie cookie = createlbCookie(authContext, domain, false);
                    response.addCookie(cookie);
                }
            } else {
                response.addCookie(createlbCookie(authContext, null, false));
            }
        }
    }     
  
    /**
     * called by UI if the username returned by
     * searchPersistentCookie is null
     * clear persistent cookie  in the request
     */
    public Cookie clearPersistentCookie(String cookieDomain,
    AuthContextLocal authContext) {
        String pCookieValue = LoginState.encodePCookie();
        int maxAge = 0;
        
        Cookie clearPCookie = createPersistentCookie(getPersistentCookieName(),
        pCookieValue,maxAge,cookieDomain);
        
        return clearPCookie;
    }    
   
    /* return the indexType for this request */
    public int getCompositeAdviceType(AuthContextLocal authContext) {
        int type = 0;
        try {            
            LoginState loginState = getLoginState(authContext);            
            if (loginState != null) {
                type = loginState.getCompositeAdviceType();
            }
            if (utilDebug.messageEnabled()) {
                utilDebug.message("in getCompositeAdviceType, type : " + type);
            }            
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error in getCompositeAdviceType : " 
                    + e.toString());
            }
        }
        return type;
    }
    
    /* return the indexType for this request */
    public AuthContext.IndexType getIndexType(AuthContextLocal authContext) {
        
        try {
            AuthContext.IndexType indexType = null;
            LoginState loginState = getLoginState(authContext);
            
            if (loginState != null) {
                indexType = loginState.getIndexType();
            }
            if (utilDebug.messageEnabled()) {
                utilDebug.message("in getIndexType, index type : " + indexType);
            }
            return indexType;
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error in getIndexType : " + e.toString());
            }
            return null;
        }
    }
    
    /* return the indexName for this request */
    public String getIndexName(AuthContextLocal authContext) {
        
        try {
            String indexName = null;
            LoginState loginState = getLoginState(authContext);
            
            if (loginState != null) {
                indexName = loginState.getIndexName();
            }
            if (utilDebug.messageEnabled()) {
                utilDebug.message("in getIndexName, index Name : " + indexName);
            }
            return indexName;
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error in getIndexName : " + e.toString());
            }
            return null;
        }
    }
    
    public Callback[] getRecdCallback(AuthContextLocal authContext) {
        LoginState loginState = getLoginState(authContext);
        Callback[] recdCallback = null;
        if (loginState != null) {
            recdCallback = loginState.getRecdCallback();
        }
        
        if ( recdCallback != null ) {
            if (utilDebug.messageEnabled()) {
                for (int i = 0; i < recdCallback.length; i++) {
                    utilDebug.message("in getRecdCallback, recdCallback[" 
                                      + i + "] :" + recdCallback[i]);
                }
            }
        }
        else {
            utilDebug.message("in getRecdCallback, recdCallback is null");
        }
        
        return recdCallback;
    }    
    
    /**
     * Returns the resource based on the default values.
     *
     * @param request HTTP Servlet Request.
     * @param fileName name of the file
     * @return Path to the resource.
     */
    public String getDefaultFileName(
        HttpServletRequest request,
        String fileName) {
        // initialize auth service.
        AuthD ad = AuthD.getAuth();
        
        String locale = ad.getPlatformLocale();
        String filePath = getFilePath(getClientType(request));
        String fileRoot = ISAuthConstants.DEFAULT_DIR;
        
        String templateFile = null;
        try {
            templateFile = ResourceLookup.getFirstExisting(
            ad.getServletContext(),
            fileRoot,locale,null,filePath,fileName,
            templatePath,true);
        } catch (Exception e) {
            templateFile = new StringBuffer().append(templatePath)
            .append(fileRoot).append(Constants.FILE_SEPARATOR)
            .append(fileName).toString();
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("getDefaultFileName:templateFile is :" +
            templateFile);
        }
        return templateFile;
    }
    
    /* returns the orgDN for the request */
    public String getOrgDN(AuthContextLocal authContext) {
        String orgDN = null;
        LoginState loginState = getLoginState(authContext);
        if (loginState != null) {
            orgDN = loginState.getOrgDN();
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("orgDN is : " + orgDN);
        }
        return orgDN;
    }
    
    /* create auth context for org */
    public static AuthContextLocal getAuthContext(String orgName)
    throws AuthException {
        return getAuthContext(orgName,"0",false, null);
    }
    
    public static AuthContextLocal getAuthContext(String orgName,
    String sessionID) throws AuthException {
        return getAuthContext(orgName,sessionID,false, null);
    }
    
    public static AuthContextLocal getAuthContext(String orgName,
    HttpServletRequest req) throws AuthException {
        return getAuthContext(orgName, "0", false, req);
    }
    
    public static AuthContextLocal getAuthContext(String orgName,
    String sessionID, boolean logout) throws AuthException {
        return getAuthContext(orgName, sessionID, logout, null);
    }
    
    public static AuthContextLocal getAuthContext(HttpServletRequest req,
    String sessionID) throws AuthException {
        return getAuthContext(null, sessionID, false, req);
    }
    
    /** Returns the AuthContext Handle for the Request.
     *  @param orgName OrganizationName in request
     *  @param sessionID Session ID for this request
     *  @param isLogout a boolean which is true if it is a Logout request
     *  @param req HttpServletRequest
     *  @return AuthContextLocal object
     */
    public static AuthContextLocal getAuthContext(String orgName,
    String sessionID, boolean isLogout, HttpServletRequest req)
    throws AuthException {
        return getAuthContext(orgName, sessionID, false, req, null, null);
    }
    
    /* create auth context for org  and sid, if sessionupgrade then
     * save the previous authcontext and create new authcontext
     * orgName - organization name to login too
     * sessionId - sessionID of the request - "0" if new request
     * isLogout - is this a logout request - if yes then no session
     * upgrade  - this is the case where session is VALID so need
     * to use this flag to determine if session upgrade is needed.
     * this is used mainly for Logout/Abort.
     *  @param orgName OrganizationName in request
     *  @param sessionID Session ID for this request
     *  @param isLogout a boolean which is true if it is a Logout request
     *  @param req HttpServletRequest
     *  @param indexType Index Type
     *  @param indexName Index Name
     *  @return AuthContextLocal object
     */
    public static AuthContextLocal getAuthContext(String orgName,
    String sessionID, boolean isLogout, HttpServletRequest req,
    String indexType, String indexName)
    throws AuthException {
        AuthContextLocal authContext = null;
        SessionID sid = null;
        LoginState loginState = null;
        boolean sessionUpgrade = false;
        AuthD ad = AuthD.getAuth();
        
        if (utilDebug.messageEnabled()) {
            utilDebug.message("orgName : " + orgName);
            utilDebug.message("sessionID is " + sessionID);
            utilDebug.message("sessionID is " + sessionID.length());
            utilDebug.message("isLogout : " + isLogout);
        }
        try {
            if ((sessionID != null) && (!sessionID.equals("0"))) {
                sid = new SessionID(sessionID);
                authContext = retrieveAuthContext(req, sid);
                
                // check if this sesson id is active, if yes then it
                // is a session upgrade case.
                LoginState prevLoginState = getLoginState(authContext);
                com.iplanet.dpro.session.service.InternalSession sess = null;
                if (prevLoginState != null) {
                    sess = prevLoginState.getSession();
                }
                if (sess == null) {
                    sessionUpgrade = false;
                } else {
                    int sessionState = sess.getState();
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message("sid from sess is : " + sess.getID());
                        utilDebug.message("sess is : " + sessionState);
                    }
                    if (!((sessionState == Session.INVALID)  || (isLogout))) {
                        if ((indexType != null) && (indexName != null)) {
                            Hashtable indexTable = new Hashtable();
                            indexTable.put(indexType, indexName);
                            if (authContext != null) {
                                SSOToken ssot = prevLoginState.getSSOToken();
                                sessionUpgrade = checkSessionUpgrade(ssot,
                                    indexTable);
                            }
                        } else {
                            sessionUpgrade = true;
                        }
                    }
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message("session upgrade is : "+ sessionUpgrade);
                    }
                }
            }
            
            if (utilDebug.messageEnabled()) {
                utilDebug.message("AuthUtil:getAuthContext:sid is.. .: " + sid);
                utilDebug.message("AuthUtil:getAuthContext:authContext is.. .: "
                + authContext);
                utilDebug.message("AuthUtil:getAuthContext:sessionUpgrade is.. .: "
                + sessionUpgrade);
            }
            
            if ((orgName == null) && (authContext == null)) {
                utilDebug.error("Cannot create authcontext with null org " );
                throw new AuthException(AMAuthErrorCode.AUTH_ERROR, null);
            }
            
            if ((orgName != null) && ((authContext ==null) || (sessionUpgrade))) {
                try {
                    loginState = new LoginState();
                    if (sessionUpgrade) {
                        loginState.setPrevAuthContext(authContext);
                        loginState.setSessionUpgrade(sessionUpgrade);
                    }
                    
                    authContext = loginState.createAuthContext(sid,orgName,req);
                    authContext.setLoginState(loginState);
                    String queryOrg = getQueryOrgName(null,orgName);
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message("query org is .. : "+ queryOrg);
                    }
                    loginState.setQueryOrg(queryOrg);
                } catch (AuthException ae) {
                    utilDebug.message("Error creating AuthContextLocal 2: ");
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message("Exception " , ae);
                    }
                    throw new AuthException(ae);
                }
            } else {
                // update loginState
                try {
                    com.iplanet.dpro.session.service.InternalSession
                    requestSess = ad.getSession(sessionID);
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message("AuthUtil :Session is .. : " + requestSess);
                    }
                    loginState = getLoginState(authContext);
                    loginState.setSession(requestSess);
                    loginState.setRequestType(false);
                } catch (Exception ae) {
                    utilDebug.message("Error Retrieving AuthContextLocal" );
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message("Exception " , ae);
                    }
                    throw new AuthException(AMAuthErrorCode.AUTH_ERROR, null);
                }
                
            }
            
            
        } catch (Exception ee) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Creating AuthContextLocal 2: ", ee);
            }
            
            throw new AuthException(ee);
        }
        return authContext;
    }
    
    /**
     * Returns a set of authentication modules whose authentication
     * level equals to or greater than the specified authLevel. If no such
     * module exists, an empty set will be returned.
     *
     * @param authLevel authentication level.
     * @param organizationDN DN for the organization.
     * @param clientType  Client type, e.g. "genericHTML".
     * @return Set of authentication modules whose authentication level
     *         equals to or greater that the specified authentication level.
     */
    public static Set getAuthModules(
        int authLevel,
        String organizationDN,
        String clientType) {
        return AMAuthLevelManager.getInstance().getModulesForLevel(authLevel,
        organizationDN, clientType);
    }
    
    /* return the previous authcontext */
    public AuthContextLocal getPrevAuthContext(AuthContextLocal authContext) {
        LoginState loginState = getLoginState(authContext);
        AuthContextLocal oldAuthContext = loginState.getPrevAuthContext();
        return oldAuthContext;
    }
    
    /* return the LoginState for the authconext */
    public LoginState getPrevLoginState(AuthContextLocal oldAuthContext) {
        return getLoginState(oldAuthContext);
    }
    
    /* retreive the authcontext based on the req */
    public AuthContextLocal getOrigAuthContext(SessionID sid)
    throws AuthException {
        AuthContextLocal authContext = null;
        // initialize auth service.
        AuthD ad = AuthD.getAuth();
        try {
            authContext = retrieveAuthContext(sid);
            if (utilDebug.messageEnabled()) {
                utilDebug.message("AuthUtil:getOrigAuthContext:sid is.:"+sid);
                utilDebug.message("AuthUtil:getOrigAuthContext:authContext is:"
                + authContext);
            }
            com.iplanet.dpro.session.service.InternalSession sess =
            getLoginState(authContext).getSession();
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Session is : "+ sess);
                if (sess != null) {
                    utilDebug.message("Session State is : "+ sess.getState());
                }
                utilDebug.message("Returning Orig AuthContext:"+authContext);
            }
            
            if (sess == null) {
                return null;
            } else {
                int status = sess.getState();
                if (status == Session.INVALID){
                    return null;
                }
                return authContext;
            }
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /* check if the session is active */
    public boolean isSessionActive(AuthContextLocal oldAuthContext) {
        try {
            com.iplanet.dpro.session.service.InternalSession sess =
            getSession(oldAuthContext);
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Sess is : " + sess);
            }
            boolean sessionValid = false;
            if (sess != null) {
                if (sess.getState() == Session.VALID) {
                    sessionValid = true;
                }
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("Sess State is : " + sess.getState());
                    utilDebug.message("Is Session Active : " + sessionValid);
                }
            }
            return sessionValid;
        } catch (Exception e) {
            return false;
        }
    }
    
    /* retreive session property */
    public String getSessionProperty(String property,
    AuthContextLocal oldAuthContext) {
        String value = null;
        try {
            com.iplanet.dpro.session.service.InternalSession sess =
            getSession(oldAuthContext);
            if (sess != null) {
                value = sess.getProperty(property);
            }
        } catch (Exception e) {
            utilDebug.message("Error : " ,e);
        }
        return value;
    }
    
    /* return session upgrade - true or false */
    public boolean isSessionUpgrade(AuthContextLocal authContext) {
        boolean isSessionUpgrade = false;
        LoginState loginState =  getLoginState(authContext);
        if (loginState != null) {
            isSessionUpgrade = loginState.isSessionUpgrade();
        }
        return isSessionUpgrade;
    }
    
    public void setCookieSupported(AuthContextLocal ac, boolean flag) {
        LoginState loginState =  getLoginState(ac);
        if (loginState==null) {
            return;
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("set cookieSupported to : " + flag);
            utilDebug.message("set cookieDetect to false");
        }
        loginState.setCookieSupported(flag);
    }
    
    public boolean isCookieSupported(AuthContextLocal ac) {
        LoginState loginState =  getLoginState(ac);
        if (loginState==null) {
            return false;
        }
        return loginState.isCookieSupported();
    }
    
    public boolean isCookieSet(AuthContextLocal ac) {
        LoginState loginState =  getLoginState(ac);
        if (loginState==null) {
            return false;
        }
        return loginState.isCookieSet();
    }
    
    /**
     * Returns true if cookies found in the request.
     *
     * @param req  HTTP Servlet Request.
     * @param ac authentication context.
     * @return <code>true</code> if cookies found in request.
     */
    public boolean checkForCookies(HttpServletRequest req, AuthContextLocal ac){
        LoginState loginState =  getLoginState(ac);
        if (loginState!=null) {
            utilDebug.message("set cookieSet to false.");
            loginState.setCookieSet(false);
            loginState.setCookieDetect(false);
        }
        // came here if cookie not found , return false
        return (
        (CookieUtils.getCookieValueFromReq(req,getAuthCookieName()) != null)
        ||
        (CookieUtils.getCookieValueFromReq(req,getCookieName()) !=null));
    }    
   
    public String getLoginURL(AuthContextLocal authContext) {
        LoginState loginState =  getLoginState(authContext);
        if (loginState==null) {
            return null;
        }
        return loginState.getLoginURL();
    }
    
    public static AuthContextLocal getAuthContextFromHash(SessionID sid) {
        AuthContextLocal authContext = null;
        if (sid != null) {
            authContext = retrieveAuthContext(sid);
        }
        return authContext;
    }  
    
    // Gets Callbacks per Page state
    public Callback[] getCallbacksPerState(AuthContextLocal authContext, 
                                           String pageState) {
        LoginState loginState = getLoginState(authContext);
        Callback[] recdCallback = null;
        if (loginState != null) {
            recdCallback = loginState.getCallbacksPerState(pageState);
        }
        if ( recdCallback != null ) {
            if (utilDebug.messageEnabled()) {
                for (int i = 0; i < recdCallback.length; i++) {
                    utilDebug.message("in getCallbacksPerState, recdCallback[" 
                                      + i + "] :" + recdCallback[i]);
                }
            }
        }
        else {
            utilDebug.message("in getCallbacksPerState, recdCallback is null");
        }
        return recdCallback;
    }
    
    // Sets (saves) Callbacks per Page state
    public void setCallbacksPerState(AuthContextLocal authContext,
    String pageState, Callback[] callbacks) {
        LoginState loginState = getLoginState(authContext);
        
        if (loginState != null) {
            loginState.setCallbacksPerState(pageState, callbacks);
        }
        if ( callbacks != null ) {
            if (utilDebug.messageEnabled()) {
                for (int i = 0; i < callbacks.length; i++) {
                    utilDebug.message("in setCallbacksPerState, callbacks[" 
                                      + i + "] :" + callbacks[i]);
                }
            }
        }
        else {
            utilDebug.message("in setCallbacksPerState, callbacks is null");
        }
    }    
    
    /**
     * Returns the SessionID . This is required to added the
     * session server , port , protocol info to the Logout Cookie.
     * SessionID is retrieved from Auth service if a handle on
     * the authcontext object is there otherwise retrieve from
     * the request object.
     *
     * @param authContext  is the AuthContext which is
     * 	    handle to the auth service
     * @param request is the HttpServletRequest object
     * @return returns the SessionID
     */
    public SessionID getSidValue(AuthContextLocal authContext,
    HttpServletRequest request) {
        SessionID sessionId = null;
        if (authContext != null)  {
            utilDebug.message("AuthContext is not null");
            try {
                String sid = getSidString(authContext);
                if (sid != null) {
                    sessionId = new SessionID(sid);
                }
            } catch (Exception e) {
                utilDebug.message("Exception getting sid",e);
            }
        }
        
        if (sessionId == null) {
            utilDebug.message("Sid from AuthContext is null");
            sessionId = new SessionID(request);
        }
        
        if (utilDebug.messageEnabled()) {
            utilDebug.message("sid is : " + sessionId);
        }
        return sessionId;
    }
    
    /**
     * Returns true if cookie is supported otherwise false.
     * the value is retrieved from the auth service if a
     * handle on the auth context object is there otherwise
     * check the HttpServletRequest object to see if the
     * Access Manager cookie is in the request header
     *
     * @param authContext is the handle to the auth service
     *	                  for the request
     * @param request is the HttpServletRequest Object for the
     *	              request
     *
     * @return boolean value indicating whether cookie is supported
     *	       or not.
     */
    public boolean isCookieSupported(AuthContextLocal authContext,
    HttpServletRequest request) {
        boolean cookieSupported;
        if (authContext != null)  {
            utilDebug.message("AuthContext is not null");
            cookieSupported = isCookieSupported(authContext);
        } else {
            cookieSupported = checkForCookies(request,null);
        }
        
        if (utilDebug.messageEnabled()) {
            utilDebug.message("Cookie supported" + cookieSupported);
        }
        return cookieSupported;
    }
    
    /**
     * Returns the previous index type after module is selected in authlevel
     * or composite advices.
     * @param ac the is the AuthContextLocal instance.
     * @return AuthContext.IndexType.
     */
    public AuthContext.IndexType getPrevIndexType(AuthContextLocal ac) {
        LoginState loginState = getLoginState(ac);
        if (loginState != null) {
            return loginState.getPreviousIndexType();
        } else {
            return null;
        }
    }
    
    /**
     * Returns whether the auth module is or the auth chain contains pure JAAS
     * module(s).
     * @param configName a string of the configuratoin name.
     * @return 1 for pure JAAS module; -1 for module(s) provided by IS only.
     */
    public static int isPureJAASModulePresent(
    String configName, AMLoginContext amlc)
    throws AuthLoginException {
        
        if (AuthD.enforceJAASThread) {
            return 1;
        }
        int returnValue = -1;
        
        Configuration ISConfiguration = null;
        try {
            ISConfiguration = Configuration.getConfiguration();
        } catch (Exception e) {
            return 1;
        }
        
        AppConfigurationEntry[] entries =
        ISConfiguration.getAppConfigurationEntry(configName);
        if (entries == null) {
            throw new AuthLoginException("amAuth",
            AMAuthErrorCode.AUTH_CONFIG_NOT_FOUND, null);
        }
        // re-use the obtained configuration
        amlc.setConfigEntries(entries);
        
        for (int i = 0; i < entries.length; i++) {
            String className = entries[i].getLoginModuleName();
            if (utilDebug.messageEnabled()) {
                utilDebug.message("config entry: " + className);
            }
            if (pureJAASModuleClasses.contains(className)) {
                returnValue = 1;
                break;
            } else if (ISModuleClasses.contains(className)) {
                continue;
            }
            
            try {
                Object classObject = Class.forName(className,true,
                    Thread.currentThread().getContextClassLoader()
                    ).newInstance();
                if (classObject instanceof AMLoginModule) {
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message(className +
                        " is instance of AMLoginModule");
                    }
                    synchronized(ISModuleClasses) {
                        if (! ISModuleClasses.contains(className)) {
                            ISModuleClasses.add(className);
                        }
                    }
                } else {
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message(className + " is a pure jaas module");
                    }
                    synchronized(pureJAASModuleClasses) {
                        if (! pureJAASModuleClasses.contains(className)) {
                            pureJAASModuleClasses.add(className);
                        }
                    }
                    returnValue = 1;
                    break;
                }
            } catch (Exception e) {
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("fail to instantiate class for " +
                    className);
                }
                synchronized(pureJAASModuleClasses) {
                    if (! pureJAASModuleClasses.contains(className)) {
                        pureJAASModuleClasses.add(className);
                    }
                }
                returnValue = 1;
                break;
            }
        }
        return returnValue;
    }
    
    /**
     * Get the module service name in either
     * iplanet-am-auth format<module.toLowerCase()>Service(old) or
     * sunAMAuth<module>Service format(new).
     */
    public static String getModuleServiceName(String moduleName) {
        String serviceName = (String) moduleService.get(moduleName);
        if (serviceName == null) {
            serviceName = AMAuthConfigUtils.getModuleServiceName(moduleName);
            try {
                SSOToken token = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
                new ServiceSchemaManager(serviceName, token);
            } catch (Exception e) {
                serviceName = AMAuthConfigUtils.getNewModuleServiceName(
                moduleName);
            }
            moduleService.put(moduleName, serviceName);
        }
        return serviceName;
    }
    
    public static int getAuthRevisionNumber(){
        try {
            SSOToken token = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
            ServiceSchemaManager scm = new ServiceSchemaManager(
            ISAuthConstants.AUTH_SERVICE_NAME, token);
            return scm.getRevisionNumber();
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("getAuthRevisionNumber error", e);
            }
        }
        return 0;
    }       
   
    /**
     * Returns success URL for this request. If <code>goto</code> parameter is
     * in the current request then returns the <code>goto</code> parameter
     * else returns the success URL set in the valid session.
     *
     * @param request HTTP Servlet Request.
     * @param authContext authentication context for this request.
     * @return success URL.
     */
    public String getSuccessURL(
        HttpServletRequest request,
        AuthContextLocal authContext) {
        String successURL = null;
        if (request != null) {
            successURL = request.getParameter("goto");
        }
        if ((successURL == null) || (successURL.length() == 0) ||
        (successURL.equalsIgnoreCase("null")) ) {
            LoginState loginState = getLoginState(authContext);
            if (loginState == null) {
                successURL = getSessionProperty("successURL",authContext);
            } else {
                successURL =
                getLoginState(authContext).getConfiguredSuccessLoginURL();
            }
        } else {
            String encoded = request.getParameter("encoded");
            if (encoded != null && encoded.equals("true")) {
                successURL = getBase64DecodedValue(successURL);
            }
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("getSuccessURL : " + successURL);
        }
        return successURL;
    }              
    
    // Returns the set of Module instances resulting from a 'composite advice'
    public static Map processCompositeAdviceXML(String xmlCompositeAdvice,
    String orgDN, String clientType) {
        Map returnAuthInstances = null;
        Set returnModuleInstances = null;
        try {
            String decodedAdviceXML = URLEncDec.decode(xmlCompositeAdvice);
            Map adviceMap = PolicyUtils.parseAdvicesXML(decodedAdviceXML);
            if (utilDebug.messageEnabled()) {
                utilDebug.message("processCompositeAdviceXML - decoded XML : "
                + decodedAdviceXML);
                utilDebug.message("processCompositeAdviceXML - result Map : "
                + adviceMap);
            }
            if ((adviceMap != null) && (!adviceMap.isEmpty())) {
                returnAuthInstances = new HashMap();
                returnModuleInstances = new HashSet();
                Set keySet = adviceMap.keySet();
                Iterator keyIter = keySet.iterator();
                while (keyIter.hasNext()) {
                    String name = (String)keyIter.next();
                    Set values = (Set)adviceMap.get(name);
                    if (name.equals(AuthenticateToRealmCondition.
                        AUTHENTICATE_TO_REALM_CONDITION_ADVICE)) {
                        //returnAuthInstances = Collections.EMPTY_MAP;
                        returnAuthInstances.put(name, values);
                        break;
                    } else if (name.equals(AuthenticateToServiceCondition.
                        AUTHENTICATE_TO_SERVICE_CONDITION_ADVICE)) {
                        returnAuthInstances.put(name, values);                        
                    } else if (name.equals(AuthSchemeCondition.
                        AUTH_SCHEME_CONDITION_ADVICE)) {
                        returnModuleInstances.addAll(values);
                    } else if (name.equals(AuthLevelCondition.
                        AUTH_LEVEL_CONDITION_ADVICE)) {
                        Set newAuthLevelModules =
                            processAuthLevelCondition(values,orgDN,clientType);
                        returnModuleInstances.addAll(newAuthLevelModules);
                    }                    
                }
                if (returnAuthInstances.isEmpty()) {
                    returnAuthInstances.put(
                        AuthSchemeCondition.AUTH_SCHEME_CONDITION_ADVICE, 
                            returnModuleInstances);
                }
            }
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error in processCompositeAdviceXML : "
                , e);
            }
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("processCompositeAdviceXML - " + 
                "returnAuthInstances : " + returnAuthInstances);
        }
        return returnAuthInstances;
    }
    
    // Returns the set of module instances having lowest auth level from a
    // given set of auth level values
    private static Set processAuthLevelCondition(Set authLevelvalues,
    String orgDN, String clientType) {
        if (utilDebug.messageEnabled()) {
            utilDebug.message("processAuthLevelCondition - authLevelvalues : "
            + authLevelvalues);
        }
        Set returnModuleInstances = Collections.EMPTY_SET;
        try {
            if ((authLevelvalues != null) && (!authLevelvalues.isEmpty())) {
                // First get the lowest auth level value from a given set
                int minAuthlevel = Integer.MAX_VALUE;
                String qualifiedRealm = null;
                String qualifiedOrgDN = null;
                Iterator iter = authLevelvalues.iterator();
                while (iter.hasNext()) {
                    //get the Realm qualified Auth Level value
                    String realmQualifiedAuthLevel = (String) iter.next();
                    String strAuthLevel = 
                        AMAuthUtils.getDataFromRealmQualifiedData(
                            realmQualifiedAuthLevel);                    
                    try {
                        int authLevel = Integer.parseInt(strAuthLevel);                        
                        if (authLevel < minAuthlevel) {
                            minAuthlevel = authLevel;
                            qualifiedRealm = 
                                AMAuthUtils.getRealmFromRealmQualifiedData(
                                    realmQualifiedAuthLevel);
                            qualifiedOrgDN = null;
                            if ((qualifiedRealm != null) && 
                                (qualifiedRealm.length() != 0)) {
                                qualifiedOrgDN = DNMapper.orgNameToDN(
                                    qualifiedRealm);
                            }
                            if (utilDebug.messageEnabled()) {
                                utilDebug.message("qualifiedRealm : " 
                                    + qualifiedRealm);
                                utilDebug.message("qualifiedOrgDN : " 
                                    + qualifiedOrgDN);
                            }
                        }
                    } catch (Exception nex) {
                        continue;
                    }
                }

                if ((qualifiedOrgDN != null) && (qualifiedOrgDN.length() != 0)) {
                    Set moduleInstances = 
                        getAuthModules(minAuthlevel,qualifiedOrgDN,clientType);
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message("moduleInstances : " 
                            + moduleInstances);
                    }
                    if ((moduleInstances != null) && 
                        (!moduleInstances.isEmpty())) {

                        returnModuleInstances = new HashSet();
                        Iterator iterInstances = moduleInstances.iterator();
                        while (iterInstances.hasNext()) {
                            //get the module instance value
                            String moduleInstance = 
                                (String) iterInstances.next();                            
                            String realmQualifiedModuleInstance = 
                                AMAuthUtils.toRealmQualifiedAuthnData(
                                    qualifiedRealm,moduleInstance);                            
                            returnModuleInstances.add(
                                realmQualifiedModuleInstance);                            
                        }
                    }
                } else {
                    returnModuleInstances = 
                        getAuthModules(minAuthlevel,orgDN,clientType);
                }

                if (utilDebug.messageEnabled()) {
                    utilDebug.message("processAuthLevelCondition - " + 
                        "returnModuleInstances : " + returnModuleInstances + 
                            " for auth level : " + minAuthlevel);
                }
            }
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error in processAuthLevelCondition : "
                , e);
            }
        }
        return returnModuleInstances;
    }                                                                                                        
      
    // returns AuthContextLocal object from Session object identified by 'sid'.
    // if not found then check it in the HttpSession.
    private static AuthContextLocal retrieveAuthContext(
    HttpServletRequest req, SessionID sid) {
        AuthContextLocal acLocal = null;        
        if (req != null && AuthD.isHttpSessionUsed()) {
            HttpSession hs = req.getSession(false);
            if (hs != null) {
                acLocal = (AuthContextLocal)hs.getAttribute(
                    ISAuthConstants.AUTH_CONTEXT_OBJ);
                if (utilDebug.messageEnabled() && acLocal != null) {
                    utilDebug.message("authContext from httpsession: " 
                        + acLocal);
                }
            }
        } else if (sid != null) {
            acLocal = retrieveAuthContext(sid);
        }

        return acLocal;
    }
    
    // retrieve the AuthContextLocal object from the Session object.
    private static AuthContextLocal retrieveAuthContext(SessionID sid) {
        com.iplanet.dpro.session.service.InternalSession is =
            AuthD.getSession(sid);        
        AuthContextLocal localAC = null;
        if (is != null) {
            localAC = (AuthContextLocal)
            is.getObject(ISAuthConstants.AUTH_CONTEXT_OBJ);
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("retrieveAuthContext - InternalSession = " + is);
            utilDebug.message("retrieveAuthContext - aclocal = " + localAC);
        }
        return localAC;
    }
    
    /**
     * Removes the AuthContextLocal object in the Session object identified
     * by the SessionID object parameter 'sid'.
     */
    public static void removeAuthContext(SessionID sid) {
        com.iplanet.dpro.session.service.InternalSession is =
        AuthD.getSession(sid);
        if (is != null) {
            is.removeObject(ISAuthConstants.AUTH_CONTEXT_OBJ);
        }
    }           
    
    /**
     * Returns the authentication service or chain configured for the
     * given organization.
     *
     * @param orgDN organization DN.
     * @return the authentication service or chain configured for the
     * given organization.
     */
    public String getOrgConfiguredAuthenticationChain(String orgDN) {
        AuthD ad = AuthD.getAuth();
        return ad.getOrgConfiguredAuthenticationChain(orgDN);
    }

    /**
     * Returns true if remote Auth security is enabled and false otherwise
     *
     * @return the value of sunRemoteAuthSecurityEnabled attribute
     */
     public String getRemoteSecurityEnabled() throws AuthException {
         ServiceSchema schema = null;
         try {
             SSOToken dUserToken = (SSOToken) AccessController.doPrivileged (
                 AdminTokenAction.getInstance());
             ServiceSchemaManager scm = new ServiceSchemaManager(
                 "iPlanetAMAuthService", dUserToken);
             schema = scm.getGlobalSchema();
         } catch ( Exception exp) {
             utilDebug.error("Cannot get global schema",exp);
             throw new AuthException(AMAuthErrorCode.AUTH_ERROR, null);
         }
         Map attrs = null;
         if (schema != null) {
             attrs = schema.getAttributeDefaults();
         }
         String securityEnabled = (String)Misc.getMapAttr(attrs,
             ISAuthConstants.REMOTE_AUTH_APP_TOKEN_ENABLED);
         if (utilDebug.messageEnabled()) {
             utilDebug.message("Security Enabled = " + securityEnabled);
         }
         return securityEnabled;   
     }

     /**
      * Returns the flag indicating a request "forward" after
      * successful authentication.
      *
      * @param authContext AuthContextLocal object
      * @param req HttpServletRequest object
      * @return the boolean flag.
      */
     public boolean isForwardSuccess(AuthContextLocal authContext,
         HttpServletRequest req) {
         boolean isForward = forwardSuccessExists(req);
         if (!isForward) {
             LoginState loginState = getLoginState(authContext);
             if (loginState != null) {
                 isForward = loginState.isForwardSuccess();
             }
         }
         return isForward;
     }

     /**
      * Returns <code>true</code> if the request has the
      * <code>forward=true</code> query parameter.
      *
      * @param req HttpServletRequest object
      * @return <code>true</code> if this parameter is present.
      */
     public boolean forwardSuccessExists(HttpServletRequest req) {
         String forward = req.getParameter("forward");
         boolean isForward =
             (forward != null) && forward.equals("true");
         if (utilDebug.messageEnabled()) {
             utilDebug.message("forwardSuccessExists : "+ isForward);
         }
         return isForward;
     }

}
