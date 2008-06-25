/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ThreadLocalService.java,v 1.3 2008-06-25 05:50:11 qcheng Exp $
 *
 */

package com.sun.identity.wss.security.handler;

import com.iplanet.sso.SSOToken;

/**
 *
 * ThreadLocalservice is a convenient utility class file to store
 * thread local state variables.
 */
class ThreadLocalService {
    
    private static ThreadLocal ssoToken = null;    
    private static ThreadLocal serviceName = null;
    //private static ThreadLocal threadLocal = new ThreadLocal();   
    
    static  synchronized String getServiceName() {
        
        if(true) {
           return null;
        }
        if(serviceName != null) {
           return (String) serviceName.get();
        }
        return null;
    }

    static synchronized void setServiceName(final String sName) {
        serviceName = new ThreadLocal() {
            protected synchronized Object initialValue() {
                return null;
            }
        };
        serviceName.set(sName);
    }
    
    static synchronized void removeServiceName(String sName) {
        if(serviceName != null) {
           serviceName.remove();
           serviceName = null;
        }
    }
    
    static  synchronized Object getSSOToken() {
        if(ssoToken != null) {
           return ssoToken.get();
        }
        return null;
    }

    static synchronized void setSSOToken(final Object sToken) {
        ssoToken = new ThreadLocal() {
            protected synchronized Object initialValue() {
                return sToken;
            }
        };       
    }
    
    static synchronized void removeSSOToken(Object sToken) {
        if(ssoToken != null) {
           ssoToken.remove();
           ssoToken = null;
        }
    }
}
