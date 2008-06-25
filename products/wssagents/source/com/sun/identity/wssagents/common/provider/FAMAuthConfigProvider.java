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
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: FAMAuthConfigProvider.java,v 1.2 2008-06-25 05:54:47 qcheng Exp $
 *
 */

package com.sun.identity.wssagents.common.provider;

import java.util.Map;
import java.util.WeakHashMap;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.config.AuthConfigProvider;
import javax.security.auth.message.config.ClientAuthConfig;
import javax.security.auth.message.config.ServerAuthConfig;

public class FAMAuthConfigProvider implements AuthConfigProvider {

    String id = null;
    String description = "FAM AuthConfigProvider";
    
    WeakHashMap clientConfigMap = new WeakHashMap();
    WeakHashMap serverConfigMap = new WeakHashMap();
    
    /** Creates a new instance of FAMAuthConfigProvider */
    public FAMAuthConfigProvider(Map props, AuthConfigFactory factory) {
        
        if (factory != null) {
            factory.registerConfigProvider(this, "SOAP", null, description);
        }
    }

    public ClientAuthConfig getClientAuthConfig(String layer, String appContext, 
        CallbackHandler callbackHandler) throws AuthException {
        System.out.println("FAMAuthConfigProvider getClientAuthConfig " + 
            "appContext : " + appContext);
        System.out.println("FAMAuthConfigProvider getClientAuthConfig " + 
            "callbackHandler : " + callbackHandler);
        
        ClientAuthConfig clientConfig = null;

        clientConfig = 
            (ClientAuthConfig)this.clientConfigMap.get(appContext);
        if (clientConfig != null) {
            return clientConfig;
        } else if (clientConfig == null) {
            clientConfig = 
                new FAMClientAuthConfig(layer, appContext, callbackHandler);
            this.clientConfigMap.put(appContext, clientConfig);
        }
        
        return clientConfig;
    }
    
    public ServerAuthConfig getServerAuthConfig(String layer, String appContext, 
        CallbackHandler callbackHandler) throws AuthException {
        System.out.println("FAMAuthConfigProvider getServerAuthConfig " + 
            "appContext : " + appContext);
        System.out.println("FAMAuthConfigProvider getServerAuthConfig " + 
            "callbackHandler : " + callbackHandler);
        
        ServerAuthConfig serverConfig = null;

        serverConfig = 
            (ServerAuthConfig)this.serverConfigMap.get(appContext);
        if (serverConfig != null) {
            return serverConfig;
        } else if (serverConfig == null) {
            serverConfig = 
                new FAMServerAuthConfig(layer, appContext, callbackHandler);
            this.serverConfigMap.put(appContext,serverConfig);
        }
        
        return serverConfig;
    }

    public void refresh() {
    }
    
    
}
