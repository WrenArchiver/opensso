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
 * $Id: ICLIStub.java,v 1.1 2006-12-08 21:02:30 veiming Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.cli.stubs;

import java.util.List;

/**
 * This interface defines the methods that are required for CLI
 * Definition Stub which is created to move away from Java annotations.
 * As we need to support JDK 1.4
 */
public interface ICLIStub {
    /**
     * Returns resource bundle name.
     *
     * @return resource bundle name.
     */
    String getResourceBundleName();

    /**
     * Returns version string.
     *
     * @return version string.
     */
    String getVersion();

    /**
     * Returns a list of <code>SubCommandStub</code> objects.
     *
     * @return a list of <code>SubCommandStub</code> objects.
     */
    List getSubCommandStubs();
}
