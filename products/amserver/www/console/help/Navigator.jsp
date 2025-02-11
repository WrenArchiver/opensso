<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
  
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

   $Id: Navigator.jsp,v 1.1 2009-08-04 21:54:49 veiming Exp $

--%>

<%@page language="java" %>
<%@page import="java.lang.String" %>
<%@page import="com.sun.web.ui.common.CCI18N" %>
<%@page import="com.sun.identity.console.help.Help2ViewBean" %>

<%@taglib uri="/WEB-INF/tld/com_iplanet_jato/jato.tld" prefix="jato"%>
<%@taglib uri="/WEB-INF/tld/com_sun_web_ui/cc.tld" prefix="cc"%>

<%
    // Get query parameters.
    String windowTitle = (request.getParameter("windowTitle") != null)
	? request.getParameter("windowTitle") : "";
    String appName = (request.getParameter("appName") != null)
	? request.getParameter("appName") : request.getContextPath().substring(1);
    String helpFile = (request.getParameter("helpFile") != null)
	? request.getParameter("helpFile") : "";
    String firstLoad = (request.getParameter("firstLoad") != null)
	? request.getParameter("firstLoad") : "false";

    windowTitle = Help2ViewBean.escapeHTML(windowTitle);
%>

<jato:useViewBean className="com.sun.identity.console.help.NavigatorViewBean">

<!-- Header -->
<cc:header name="Header"
 pageTitle="<%=windowTitle %>"
 baseName="com.sun.web.ui.resources.Resources"
 bundleID="help2Bundle"
 onLoad="performLoadUtilities();">

<script type="text/javascript">
    function performLoadUtilities() {
	// If this is the first time loading this page, clear the Index tab's
	// cookie cache. The TOC gets reset on first load in the view bean via
	// javascript that calls the yokeToAndLoad function of the client side
	// tree. If the cookie cache for the Index isn't cleared on first load,
	// the last selection from the previous window load will be selected.
	// 
	// Note that state is maintained via the 'isPersistent' ctree attribute
	// when users switch between tabs and/or submit the page via the search
	// button, manually reloading, etc. State should not be maintained once
	// the user has closed the window, or selected to view new context help
	// via a link in the console or web apps.
	var firstLoadValue = <%=firstLoad %>;
	if (firstLoadValue != null && firstLoadValue == true) {
	    document.cookie = "cctree_Navigator_IndexTree=null";
	}
    }

    function onKeyPressed() {
	if (document.treeForm.elements[0].value != "sunnav4") {
	    document.treeForm.submit();
	    return false;
	}
    }
</script>

<cc:form name="treeForm" method="post" defaultCommandChild="/SearchButton" onSubmit="return onKeyPressed();">

<!-- Hidden BrowserType field -->
<cc:hidden name="BrowserType" />

<!-- Left Navigation Tabs: TOC, Index, and Search -->
<div class="HlpStpTab">
<cc:tabs name="Tabs" bundleID="help2Bundle" type="mini" />
</div>

<jato:content name="toc">
<!-- Table of Contents Tab -->
<cc:ctree name="TOCTree" targetFrame="contentFrame" type="2" isPersistent="false" top="40" />
</jato:content>

<jato:content name="index">
<!-- Index Tab -->
<cc:ctree name="IndexTree" targetFrame="contentFrame" type="2" isPersistent="false" top="40" />
</jato:content>

<jato:content name="search">
<!-- Search Tab -->
<div class="HlpSchDiv">

<table border="0" cellspacing="0" cellpadding="0">
<tr><td nowrap="nowrap">
<cc:textfield name="SearchField" bundleID="help2Bundle"
 title="help2.searchFieldTitle"
 autoSubmit="true" />
<cc:button name="SearchButton" bundleID="help2Bundle"
 defaultValue="help2.searchButton"
 alt="help2.searchButtonAlt"
 title="help2.searchButtonTitle"
 type="primary"
 onKeyPress="return onKeyPressed();" />
</td></tr></table>

<div class="HlpFldTxt">
<cc:href name="TipsHref" bundleID="help2Bundle" styleClass="HlpFldLnk">
  <cc:text name="TipsText" bundleID="help2Bundle" defaultValue="help2.tips" />
</cc:href>
</div>

<table border="0" cellspacing="0" cellpadding="0" width="98%">
  <tr><td><cc:spacer name="Spacer1" width="1" height="5" /></td></tr>
  <tr><td class="TtlLin"><cc:spacer name="Spacer2" width="1" height="1" /></td></tr>
  <tr><td><cc:spacer name="Spacer3" width="1" height="5" /></td></tr>
</table>

<jato:content name="searchResults">
<jato:tiledView name="SearchResultsTiledView">
<table border="0" cellspacing="0" cellpadding="0" width="98%">
  <tr>
    <td nowrap="nowrap">
      <div class="HlpRltDiv">
      <jato:content name="searchResultsFound">
      <cc:href name="SearchResultsHref" bundleID="help2Bundle">
        <cc:text name="SearchResultsText" bundleID="help2Bundle"
         defaultValue="help2.noResultsFound"/>
      </cc:href>
      </jato:content>
      <jato:content name="noSearchResultsFound">
      <cc:text name="SearchResultsText" bundleID="help2Bundle"
       defaultValue="help2.noResultsFound"/>
      </jato:content>
      </div>
    </td>
  </tr>
</table>
</jato:tiledView>
</jato:content>

<!-- Set the search text field focus. -->
<script type="text/javascript">
    document.treeForm.elements[1].focus();
</script>
</div>
</jato:content>

</cc:form>
</cc:header>
</jato:useViewBean>
