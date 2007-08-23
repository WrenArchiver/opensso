<!--
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
  
   $Id: isAlive.jsp,v 1.3 2007-08-23 19:35:57 jonnelson Exp $
  
   Copyright 2005 Sun Microsystems Inc. All Rights Reserved
-->

<%@ page language="java" 
    import="java.security.AccessController,
        com.sun.identity.security.AdminTokenAction,
        com.iplanet.sso.SSOToken,
        com.sun.identity.sm.ServiceManager,
        com.sun.identity.sm.SMSEntry"
%>

<html>

<head>
    <title>Sun Java(TM) System Federated Access Manager</title>
</head>

<body>

<%
    // Get valid SSOToken
    SSOToken token = (SSOToken) AccessController.doPrivileged(
        AdminTokenAction.getInstance());

    // Construct the name to lookup
    String name = "ou=services," + ServiceManager.getBaseDN();

    // Check if the configuration data store is up
    Object attributes = null;
    try {
        attributes = SMSEntry.getSMSObject().read(token, name);
    } catch (Exception e) {
        attributes = null;
    }

    if (attributes == null) {
        /**
         * Identity Server or directory is down, have failure message here
          * or throw an exception. This currently throws an exception
         * which will cause web server to return error code of 500,
         * to return an error message, comment the "throw" line
         */
        out.println("<h1>Server is DOWN</h1>");
        throw (new ServletException("directory is down"));
    } else {
        /**
         * Identity Server is alive, have success message below
         */
        out.println("<h1>Server is ALIVE: </h1>");
    }
%>

</body>

</html>


