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

   $Id: index.jsp,v 1.2 2006-11-01 07:03:46 hengming Exp $

   Copyright 2006 Sun Microsystems Inc. All Rights Reserved
--%>

<%@page 
import="
java.io.*, 
java.util.*,
com.iplanet.am.util.SystemProperties,
com.sun.identity.common.SystemConfigurationUtil,
com.sun.identity.liberty.ws.disco.ResourceOffering,
com.sun.identity.liberty.ws.security.SecurityAssertion,
com.sun.identity.plugin.session.SessionManager,
com.sun.identity.plugin.session.SessionProvider,
com.sun.identity.saml.common.*,
com.sun.liberty.jaxrpc.LibertyManagerClient"
%>

<html xmlns="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
    <head><title>Discovery Service Boot Strapping</title></head>
    <body bgcolor="white">
	<h1>Discovery Service Boot Strapping Resource Offering</h1>
<%!

public void jspInit() {
    try {
        String bootstrapFile = System.getProperty("java.io.tmpdir") +
            File.separator + "ClientSampleWSC.properties";
        FileInputStream fin = new FileInputStream(bootstrapFile);
        Properties props = new Properties();
        props.load(fin);
        fin.close();

        String configDir = props.getProperty("configDir");
        if (configDir != null) {
            fin = new FileInputStream(configDir + "/AMConfig.properties");
            props = new Properties();
            props.load(fin);
            SystemProperties.initializeProperties(props);
            fin.close();

            fin = new FileInputStream(configDir +
                "/FederationConfig.properties");
            props = new Properties();
            props.load(fin);
            SystemConfigurationUtil.initializeProperties(props);
            fin.close();
        }
    } catch (Exception ex) {
        ex.printStackTrace();
    }
}
%>
<%
    try {
        String bootstrapFile = System.getProperty("java.io.tmpdir") +
            File.separator + "ClientSampleWSC.properties";
        FileInputStream fin = new FileInputStream(bootstrapFile);
        Properties props = new Properties();
        props.load(fin);
        fin.close();

        // WSC-SP Provider ID
        String providerID = props.getProperty("spProviderID");

        SessionProvider sessionProvider = SessionManager.getProvider();
        Object sessionObj = sessionProvider.getSession(request);
        if(session != null && sessionProvider.isValid(sessionObj)) {
            LibertyManagerClient lmc = new LibertyManagerClient();
            ResourceOffering offering = lmc.getDiscoveryResourceOffering(
                sessionObj, providerID);

	    if(offering == null) {
                %>ERROR: no resource offering in AttributeStatement.<%
            } else {
                String remoteProvider = 
                       offering.getServiceInstance().getProviderID(); 
		String fnSuffix = remoteProvider.replace('/','_')
                    .replace(':','_');
		String fileName = System.getProperty("java.io.tmpdir") +
                    "RO_" + fnSuffix;
		PrintWriter pw = new PrintWriter(new FileWriter(fileName));
                pw.print(offering.toString());
                pw.close();
                // get reourceID
                    %>
<form method="GET" action="discovery-query.jsp">
<input type='hidden' name='providerID' value='<%= providerID %>'>
<input type='hidden' name='discoveryResourceOffering' value='<%= fileName %>'>
<input type="submit" name="Submit" value="Send Discovery Lookup" />
</form>
<p>
<form method="GET" action="discovery-modify.jsp">
<input type='hidden' name='providerID' value='<%= providerID %>'>
<input type='hidden' name='discoveryResourceOffering' value='<%= fileName %>'>
<input type="submit" name="Submit" value="Add PP Resource Offering" />
</form>
<pre><%= SAMLUtils.displayXML(offering.toString()) %></pre>
                    <%
                SecurityAssertion sa = lmc.getDiscoveryServiceCredential(
                    sessionObj, providerID);
                if (sa != null) {
                    %>
<pre><%= SAMLUtils.displayXML(sa.toString()) %></pre>
                    <%
                }
	    }
        } else {
	    %>ERROR: user not logged in.<%
        }
    } catch (Exception ex) {
        StringWriter bufex = new StringWriter();
        ex.printStackTrace(new PrintWriter(bufex));
        %>
            ERROR: caught Exception:
            <pre>
        <%
            out.println(bufex.toString());
        %>
            </pre>
        <%
    }
%>
	<hr/>
    </body>
</html>
