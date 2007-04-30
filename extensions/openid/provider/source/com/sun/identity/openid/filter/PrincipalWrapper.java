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
 * $Id: PrincipalWrapper.java,v 1.1 2007-04-30 01:28:27 pbryan Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 * Portions Copyrighted 2007 Paul C. Bryan
 */

package com.sun.identity.openid.filter;

import java.security.Principal;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * TODO: Description.
 *
 * @author pbryan
 */
public class PrincipalWrapper extends HttpServletRequestWrapper
{
    /** TODO: Description. */
    private Principal principal;

    /** TODO: Description. */
    private HttpServletRequest request;

    /**
     * TODO: Description.
     *
     * @param request the request object to be wrapped by this class.
     * @param principal the principal to override with this class.
     */
    public PrincipalWrapper(HttpServletRequest request, Principal principal) {
        super(request);
        this.request = request;
        this.principal = principal;
    }

    /**
     * Returns a principal object containing the name of the current
     * authenticated user. If the user has not been authenticated, the method
     * returns null.
     *
     * @return a principal containing the name of the user making the request.
     */
    public Principal getUserPrincipal() {
        return principal;
    }

    /**
     * Implemented to suppress deprecation warnings during compile.
     *
     * @deprecated As of version 1.2 of the Java Servlet API.
     */
    @Deprecated public boolean isRequestedSessionIdFromUrl() {
        return request.isRequestedSessionIdFromUrl();
    }

    /**
     * Implemented to suppress deprecation warnings during compile.
     *
     * @deprecated As of version 1.2 of the Java Servlet API.
     */
    @Deprecated public String getRealPath(String path) {
        return request.getRealPath(path);
    }
}
