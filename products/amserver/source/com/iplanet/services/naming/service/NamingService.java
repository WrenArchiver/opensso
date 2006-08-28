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
 * $Id: NamingService.java,v 1.3 2006-08-28 18:50:41 veiming Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.iplanet.services.naming.service;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.services.comm.server.RequestHandler;
import com.iplanet.services.comm.share.Request;
import com.iplanet.services.comm.share.Response;
import com.iplanet.services.comm.share.ResponseSet;
import com.iplanet.services.naming.share.NamingRequest;
import com.iplanet.services.naming.share.NamingResponse;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.internal.AuthPrincipal;
import com.sun.identity.security.AdminDNAction;
import com.sun.identity.security.AdminPasswordAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.net.URL;
import java.security.AccessController;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class NamingService implements RequestHandler, ServiceListener {

    public static final int SERVICE_REV_NUMBER_70 = 20;

    public static int serviceRevNumber;

    private static Debug namingDebug = null;

    public static final String NAMING_SERVICE = "com.iplanet.am.naming";

    private static Hashtable namingTable = null;

    private static Properties platformProperties = null;

    private static String server_proto = null;

    private static String server_host = null;

    private static String server_port = null;

    private static SSOToken sso = null;

    private static ServiceSchemaManager scmNaming = null;

    private static ServiceSchemaManager scmPlatform = null;

    private static ServiceConfig sessionServiceConfig = null;

    private static Set sessionConfig = null;

    private static String delimiter = "|";

    /*
     * Initialize SSO, schema managers statically, and add listener for schema
     * change events for platform service so that the naming table gets updated
     * if a new platform server is added or gets deleted
     */
    static {
        namingDebug = Debug.getInstance("amNaming");
        platformProperties = SystemProperties.getAll();
        server_proto = platformProperties.getProperty(
            "com.iplanet.am.server.protocol", "");
        server_host = platformProperties.getProperty(
            "com.iplanet.am.server.host", "");
        server_port = platformProperties.getProperty(
            Constants.AM_SERVER_PORT, "");

        try {
            SSOTokenManager mgr = SSOTokenManager.getInstance();
            String adminDN = (String) AccessController
                    .doPrivileged(new AdminDNAction());
            String adminPassword = (String) AccessController
                    .doPrivileged(new AdminPasswordAction());
            sso = mgr.createSSOToken(new AuthPrincipal(adminDN), adminPassword);
            scmNaming = new ServiceSchemaManager("iPlanetAMNamingService", sso);
            scmPlatform = new ServiceSchemaManager("iPlanetAMPlatformService",
                    sso);
            serviceRevNumber = scmPlatform.getRevisionNumber();
            if (serviceRevNumber < SERVICE_REV_NUMBER_70) {
                ServiceConfigManager scm = new ServiceConfigManager(
                        "iPlanetAMSessionService", sso);
                sessionServiceConfig = scm.getGlobalConfig(null);
                sessionConfig = sessionServiceConfig.getSubConfigNames();
            }

            // Add Listener to the platform service
            scmPlatform.addListener(new NamingService());
        } catch (Exception ne) {
            namingDebug.error("Naming Initialization failed.", ne);
        }

    }

    public NamingService() {

    }

    /**
     * This function returns the naming table that consists of service urls,
     * platform servers and key/value mappings for platform servers Each server
     * instance needs to be updated in the platform server list to reflect that
     * server in the naming table
     */
    public static Hashtable getNamingTable(boolean forClient)
            throws SMSException {
        return updateNamingTable(forClient);
    }

    public static Hashtable getNamingTable() throws SMSException {
        try {
            if (namingTable != null) {
                return namingTable;
            }
            updateNamingTable();
        } catch (Exception ex) {
            throw new SMSException(ex.getMessage());
        }
        return namingTable;
    }

    /**
     * This method updates the naming table especially whenever a new server
     * added/deleted into platform server list
     */
    private static void updateNamingTable() throws SMSException {
        namingTable = updateNamingTable(false);
    }

    /**
     * This method updates the naming table especially whenever a new server
     * added/deleted into platform server list
     */
    private static Hashtable updateNamingTable(boolean forClient)
            throws SMSException {
        Hashtable nametable = null;

        try {
            ServiceSchema sc = scmNaming.getGlobalSchema();
            Map namingAttrs = sc.getAttributeDefaults();
            sc = scmPlatform.getGlobalSchema();
            Map platformAttrs = sc.getAttributeDefaults();
            Set sites = getSites(platformAttrs);
            Set servers = getServers(platformAttrs, sites);

            if ((sites != null) && !sites.isEmpty()) {
                if (!forClient) {
                    registFQDNMapping(sites);
                }
                sites.addAll(servers);
            } else {
                sites = servers;
            }

            if (forClient) {
                storeServerListForClient(sites, namingAttrs);
            } else {
                storeServerList(sites, namingAttrs);
            }

            // To reduce risk convert from a Map to a Hastable since the rest
            // of the naming code expects it in this format. Note there is
            // tradeoff based on whether or not short circuiting is being used.

            nametable = convertToHash(namingAttrs);
            if (forClient && (namingTable != null)) {
                String siteList = (String) namingTable
                        .get(Constants.SITE_ID_LIST);
                nametable.put(Constants.SITE_ID_LIST, siteList);
            }
        } catch (Exception ex) {
            namingDebug.error("Can't get naming table", ex);
            throw new SMSException(ex.getMessage());
        }

        return nametable;
    }

    /**
     * This will convert updated naming attributes map into naming hashtable
     */
    static Hashtable convertToHash(Map m) {
        Hashtable retHash = new Hashtable();
        Set s = m.keySet();
        Iterator iter = s.iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            Set val = (Set) m.get(key);
            retHash.put(key, setToString(val));
        }
        return retHash;
    }

    /**
     * This function stores the server list by parsing platform server list that
     * are stored in <code>iPlanetAMPlatformService</code>. This would expect
     * the servers from the platform service are in the following format
     * protocol://server.domain:port|serverId e.g.
     * http://shivalik.red.iplanet.com:58080|01
     * http://solpuppy.red.iplanet.com:58081|02 The serverId can be anything and
     * does not need to be a number If the platform server is not in the correct
     * format, that entry will be ignored. Note: This server id should be unique
     * if it's participating in load balancing mode.
     */

    static void storeServerList(Set servers, Map namingAttrs) {
        Set serverList = new HashSet();
        Set siteList = new HashSet();
        Iterator iter = servers.iterator();
        while (iter.hasNext()) {
            String serverEntry = (String) iter.next();
            int index = serverEntry.indexOf(delimiter);
            if (index != -1) {
                String server = serverEntry.substring(0, index);
                String serverId = serverEntry.substring(index + 1, serverEntry
                        .length());

                siteList.add(serverId);
                index = serverId.indexOf(delimiter);
                if (index != -1) {
                    serverId = serverId.substring(0, 2);
                }

                HashSet serverSet = new HashSet();
                serverSet.add(server);
                serverList.add(server);
                namingAttrs.put(serverId, serverSet);
            } else {
                namingDebug.error("Platform Server List entry is invalid:"
                        + serverEntry);
            }
        }
        namingAttrs.put(Constants.PLATFORM_LIST, serverList);
        namingAttrs.put(Constants.SITE_ID_LIST, siteList);
    }

    static void storeServerListForClient(Set servers, Map namingAttrs) {
        Set serverList = new HashSet();
        Iterator iter = servers.iterator();
        while (iter.hasNext()) {
            String serverEntry = (String) iter.next();
            int index = serverEntry.indexOf(delimiter);
            if (index != -1) {
                String server = serverEntry.substring(0, index);
                String serverId = serverEntry.substring(index + 1, serverEntry
                        .length());
                index = serverId.indexOf(delimiter);
                if (index != -1) {
                    continue;
                }
                HashSet serverSet = new HashSet();
                serverSet.add(server);
                serverList.add(server);
                namingAttrs.put(serverId, serverSet);
            } else {
                namingDebug.error("Platform Server List entry is invalid:"
                        + serverEntry);
            }
        }
        namingAttrs.put(Constants.PLATFORM_LIST, serverList);
    }

    static String setToString(Set s) {
        StringBuffer sb = new StringBuffer(100);
        Iterator iter = s.iterator();
        while (iter.hasNext()) {
            sb.append((String) iter.next());
            if (iter.hasNext()) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    public ResponseSet process(Vector requests,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse, ServletContext servletContext)
    {
        ResponseSet rset = new ResponseSet(NAMING_SERVICE);
        for (int i = 0; i < requests.size(); i++) {
            Request req = (Request) requests.elementAt(i);
            Response res = processRequest(req);
            rset.addResponse(res);
        }
        return rset;
    }

    private Response processRequest(Request req) {
        String content = req.getContent();
        NamingRequest nreq = NamingRequest.parseXML(content);
        NamingResponse nres = new NamingResponse(nreq.getRequestID());

        // get the version from nreq and check old
        float reqVersion = Float.valueOf(nreq.getRequestVersion()).floatValue();
        boolean limitNametable = (reqVersion > 1.0) ? true : false;

        // get the sesisonId from nreq
        String sessionId = nreq.getSessionId();
        try {
            if (sessionId == null) {
                nres.setNamingTable(NamingService
                        .getNamingTable(limitNametable));
            } else {
                Hashtable tempHash = new Hashtable();
                tempHash = transferTable(NamingService
                        .getNamingTable(limitNametable));
                Hashtable replacedTable = replaceTable(tempHash, sessionId);
                if (replacedTable == null) {
                    nres.setException("SessionID ---" + sessionId
                            + "---is Invalid");
                } else {
                    nres.setNamingTable(replacedTable);
                }
            }
        } catch (Exception e) {
            nres.setException(e.getMessage());
        }
        return new Response(nres.toXMLString());
    }

    private Hashtable replaceTable(Hashtable namingTable, String sessionID) {
        SessionID sessID = new SessionID(sessionID);
        namingDebug.message("SessionId received is --" + sessionID);
        String protocol = sessID.getSessionServerProtocol();
        String host = sessID.getSessionServer();
        String port = sessID.getSessionServerPort();
        if (protocol.equalsIgnoreCase("") || host.equalsIgnoreCase("")
                || port.equalsIgnoreCase("")) {
            return null;
        }
        // Do validation from platform server list
        if (!(protocol.equals(server_proto) && host.equals(server_host) && port
                .equals(server_port))) {
            String cookieURL = protocol + "://" + host + ":" + port;
            String platformList = (String) namingTable
                    .get(Constants.PLATFORM_LIST);
            if (platformList.indexOf(cookieURL) == -1) {
                return null;
            }
        }
        Hashtable tempNamingTable = namingTable;
        // replace all percent here
        for (Enumeration e = tempNamingTable.keys(); e.hasMoreElements();) {
            Object obj = e.nextElement();
            String key = obj.toString();
            String url = (tempNamingTable.get(obj)).toString();
            int idx;
            if ((idx = url.indexOf("%protocol")) != -1) {
                url = url.substring(0, idx)
                        + protocol
                        + url.substring(idx + "%protocol".length(), url
                                .length());
            }
            // %host processing
            if ((idx = url.indexOf("%host")) != -1) {
                url = url.substring(0, idx) + host
                        + url.substring(idx + "%host".length(), url.length());
            }
            // %port processing
            if ((idx = url.indexOf("%port")) != -1) {
                // plugin the server name
                url = url.substring(0, idx) + port
                        + url.substring(idx + "%port".length(), url.length());
            }
            tempNamingTable.put(key, url);
        }
        return tempNamingTable;
    }

    private Hashtable transferTable(Hashtable hashTab) {
        if (hashTab == null)
            return null;
        Hashtable newTab = new Hashtable();
        for (Enumeration e = hashTab.keys(); e.hasMoreElements();) {
            Object obj = e.nextElement();
            String key = obj.toString();
            String value = (hashTab.get(obj)).toString();
            newTab.put(key, value);
        }
        return newTab;
    }

    // The following functions are the implementations of service listener
    // for schema/config change events

    /**
     * This function updates the naming table whenever it gets a schema changed
     * event.
     */
    public void schemaChanged(String serviceName, String version) {
        // Do not update if the servieName is not "iPlanetAMPlatformService"
        if ((serviceName == null)
                || (!serviceName.equals("iPlanetAMPlatformService"))) {
            return;
        }
        try {
            updateNamingTable();
        } catch (SMSException ex) {
            namingDebug.error("Error occured in updating naming table", ex);
        }
    }

    // We don't need to do anything for the following methods, we kept it
    // for the implementation sake. But, these methods will never be
    // invoked since the Platform service currently does not support
    // global/organization 'Configuration'.

    public void globalConfigChanged(String serviceName, String version,
            String groupName, String serviceComponent, int type) {
        // Do nothing
    }

    public void organizationConfigChanged(String serviceName, String version,
            String orgName, String groupName, String serviceComponent, int type)
    {
        // Do nothing
    }

    private static void registFQDNMapping(Set sites) {
        Iterator iter = null;
        MessageFormat form = null;

        if (sites == null) {
            return;
        }

        iter = sites.iterator();
        form = new MessageFormat("com.sun.identity.server.fqdnMap[{0}]");

        while (iter.hasNext()) {
            String entry = (String) iter.next();
            StringTokenizer tok = new StringTokenizer(entry, "|");
            URL url = null;
            try {
                url = new URL(tok.nextToken());
            } catch (Exception e) {
            }
            String host = url.getHost();
            if (host != null) {
                Object[] args = { host };
                form.format(args);
                SystemProperties.initializeProperties(form.format(args), host);
            }
        }
    }

    private static Set getSites(Map platformAttrs) throws Exception {
        Set sites = null;

        Set servers = (Set) platformAttrs.get(Constants.PLATFORM_LIST);
        if (serviceRevNumber < SERVICE_REV_NUMBER_70) {
            sites = getSitesFromSessionConfig(servers);
        } else {
            sites = (Set) platformAttrs.get(Constants.SITE_LIST);
        }

        if (namingDebug.messageEnabled()) {
            if (sites != null) {
                namingDebug.message("Sites : " + sites.toString());
            }
        }

        return sites;
    }

    private static Set getServers(Map platformAttrs, Set sites)
            throws Exception {
        Set servers = null;

        servers = (Set) platformAttrs.get(Constants.PLATFORM_LIST);
        if ((sites != null) && (serviceRevNumber < SERVICE_REV_NUMBER_70)) {
            servers = getServersFromSessionConfig(sites, servers);
        }

        if (namingDebug.messageEnabled()) {
            if (servers != null) {
                namingDebug.message("servers : " + servers.toString());
            }
        }

        return servers;
    }

    private static Set getSitesFromSessionConfig(Set platform) throws Exception
    {
        HashSet sites = new HashSet();
        Iterator iter = platform.iterator();

        while (iter.hasNext()) {
            String server = (String) iter.next();
            int idx = server.indexOf(delimiter);
            String serverFQDN = server.substring(0, idx);

            if (sessionConfig.contains(serverFQDN)) {
                sites.add(server);
            }
        }

        return sites.isEmpty() ? null : sites;
    }

    private static Set getServersFromSessionConfig(Set sites, Set platform)
            throws Exception {
        HashSet servers = new HashSet();

        Map clusterInfo = getClusterInfo(sites);
        Iterator serverlist = platform.iterator();

        while (serverlist.hasNext()) {
            String server = (String) serverlist.next();
            if (sites.contains(server)) {
                continue;
            }

            int idx = server.indexOf(delimiter);
            String serverid = server.substring(idx + 1, server.length());
            Iterator keys = clusterInfo.keySet().iterator();
            boolean found = false;

            while (!found && keys.hasNext()) {
                String siteid = (String) keys.next();
                String clusterlist = (String) clusterInfo.get(siteid);
                if (clusterlist.indexOf(serverid) >= 0) {
                    servers.add(server + delimiter + siteid);
                    found = true;
                }
            }

            if (found == false) {
                servers.add(server);
            }
        }

        return servers.isEmpty() ? null : servers;
    }

    private static Hashtable getClusterInfo(Set sites) throws Exception {
        Hashtable clustertbl = new Hashtable();
        Iterator iter = sites.iterator();

        while (iter.hasNext()) {
            String site = (String) iter.next();
            int idx = site.indexOf(delimiter);
            String siteid = site.substring(idx + 1, site.length());
            site = site.substring(0, idx);
            ServiceConfig subConfig = sessionServiceConfig.getSubConfig(site);
            Map sessionAttrs = subConfig.getAttributes();
            String clusterServerList = CollectionHelper.getMapAttr(
                sessionAttrs, Constants.CLUSTER_SERVER_LIST, "");
            clustertbl.put(siteid, clusterServerList);
        }

        return clustertbl;
    }
}
