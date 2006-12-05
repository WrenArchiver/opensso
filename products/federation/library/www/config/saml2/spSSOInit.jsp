<%--
   The contents of this file are subject to the terms
   of the Common Development and Distribution License
   (the License). You may not use this file except in
   compliance with the License.

   You can obtain a copy of the License at
   https://opensso.dev.java.net/public/CDDLv1.0.html or
   opensso/legal/CDDLv1.0.txt
   See the License for the specific language governing
   permission and limitations under the License.

   When distributing Covered Code, include this CDDL
   Header Notice in each file and include the License file
   at opensso/legal/CDDLv1.0.txt.
   If applicable, add the following below the CDDL Header,
   with the fields enclosed by brackets [] replaced by
   your own identifying information:
   "Portions Copyrighted [year] [name of copyright owner]"

   $Id: spSSOInit.jsp,v 1.2 2006-12-05 21:55:52 weisun2 Exp $

   Copyright 2006 Sun Microsystems Inc. All Rights Reserved
--%>




<%@ page import="com.sun.identity.shared.debug.Debug" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Constants" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Utils" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Exception" %>
<%@ page import="com.sun.identity.saml2.profile.SPCache" %>
<%@ page import="com.sun.identity.saml2.profile.SPSSOFederate" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.StringTokenizer" %>

<%--
    spssoinit.jsp initiates the Single Sign-On at the Service Provider.

    Following are the list of supported query parameters :

    Required parameters to this jsp are :

    Query Parameter Name    Description

    1. metaAlias	    MetaAlias for Service Provider. The format of
			    this parameter is /realm_name/SP name.

    2. idpEntityID	    Identifier for Identity Provider

    Optional Query Parameters :

    Query Parameter Name    Description

    3. RelayState	    Target URL on successful complete of SSO/Federation

    4. NameIDFormat	    NameIDPolicy format Identifier Value.
			    The supported values are :
			    	persistent
			    	transient

			    NOTE: There are other SAML defined values for these
				  which are not supported by FM/AM.

    5. binding		    URI value that identifies a SAML protocol binding to
			    used when returning the Response message.

			    The supported values are :
				HTTP-Artifact
				HTTP-POST

    6. AssertionConsumerServiceIndex
			    An integer number indicating the location
			    to which the Response message should be returned to
			    the requester.


    7. AttributeConsumingServiceIndex
			    Indirectly specifies information associated
			    with the requester describing the SAML attributes
			    the requester desires or requires to be supplied
			    by the IDP in the generated Response message.

			    Note: This parameter may not be supported for
			    this release.

    8. isPassive	    true or false value indicating whether the IDP
			    should authenticate passively.

    9. ForceAuthN	    true or false value indicating if IDP must
			    force authentication OR false if IDP can rely on
			    reusing existing security contexts.

			    true - force authentication

    10.AllowCreate	    Value indicates if IDP is allowed to created a new
			    identifier for the principal if it does not exist.
			    Value of this parameter can be true OR false.

			    true - IDP can dynamically create user.

    12.Destination	    A URI Reference indicating the address to which the
			    request has been sent.

    13.AuthnContextDeclRef  Specifies the AuthnContext Declaration Reference.
			    The value is a pipe separated value with multiple
			    references.

    14.AuthnContextClassRef Specifies the AuthnContext Class References.
			    The value is a pipe separated value with multiple
			    references.

    15.AuthComparison       The comparison method used to evaluate the
			    requested context classes or statements.
			    Allowed values are :
				exact
				minimum
				maximum
				better

    16.Consent		    Specifies a URI a SAML defined identifier
			    known as Consent Identifiers.These are defined in
			    the SAML 2 Assertions and Protocols Document.

			    Note: This parameter may not be supported for
			    this release.

--%>
<html>

<head>
<title>SAMLv2 Service Provider SSO</title>
</head>
<body bgcolor="#FFFFFF" text="#000000">
<%
    // Retreive the Request Query Parameters
    // metaAlias and idpEntiyID are the required query parameters
    // metaAlias - Service Provider Entity Id
    // idpEntityID - Identity Provider Identifier
    // Query parameters supported will be documented.
    String idpEntityID = null;
    String metaAlias= null;
    Map paramsMap = null;
    try {
	String reqID = request.getParameter("requestID");
	if (reqID != null) {
	   //get the preferred idp
	   idpEntityID = SAML2Utils.getPreferredIDP(request);
	   paramsMap = (Map)SPCache.reqParamHash.get(reqID);
	   metaAlias = (String) paramsMap.get("metaAlias");
	   SPCache.reqParamHash.remove(reqID);
	} else {
	    // this is an original request check
	    // get the metaAlias ,idpEntityID
	    // if idpEntityID is null redirect to IDP Discovery
	    // Service to retrieve.
	    metaAlias = request.getParameter("metaAlias");
            if ((metaAlias ==  null) || (metaAlias.length() == 0)) {
                 response.sendError(response.SC_BAD_REQUEST,
		           SAML2Utils.bundle.getString("nullSPEntityID"));
	    	return;
            }
            idpEntityID = request.getParameter("idpEntityID");
	    paramsMap = SAML2Utils.getParamsMap(request);

            if ((idpEntityID == null) || (idpEntityID.length() == 0)) {
		// get reader url
		String readerURL = SAML2Utils.getReaderURL(metaAlias);
	        if (readerURL != null) {
		    String rID = SAML2Utils.generateID();
 	            String redirectURL =
			SAML2Utils.getRedirectURL(readerURL,rID,request);
		    if (redirectURL != null) {
			paramsMap.put("metaAlias",metaAlias);
			SPCache.reqParamHash.put(rID,paramsMap);
			response.sendRedirect(redirectURL);
			return;
		    }
		}
	   }
	}

	if ((idpEntityID == null) || (idpEntityID.length() == 0)) {
            response.sendError(response.SC_BAD_REQUEST,
			   SAML2Utils.bundle.getString("nullIDPEntityID"));
	    return;
	}
	// get the parameters and put it in a map.
	SPSSOFederate.initiateAuthnRequest( request,response,metaAlias,
				          idpEntityID,
                                          paramsMap);
    } catch (SAML2Exception sse) {
	SAML2Utils.debug.error("Error sending AuthnRequest " , sse);
	response.sendError(response.SC_BAD_REQUEST,
			SAML2Utils.bundle.getString("requestProcessingError"));
    } catch (Exception e) {
        SAML2Utils.debug.error("Error processing Request ",e);
	response.sendError(response.SC_BAD_REQUEST,
			SAML2Utils.bundle.getString("requestProcessingError"));
    }
%>
</body>
</html>
