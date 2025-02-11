/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: MockCLIManager.java,v 1.4 2009-06-05 19:33:42 veiming Exp $
 *
 */

package com.sun.identity.cli;

/**
 * This is mock CLI Manager class which is used to test the CLI Framework.
 */
public class MockCLIManager extends CLIDefinitionBase {
    private static String DEFINITION_CLASS =
        "com.sun.identity.cli.defintion.MockCLI";

    /**
     * Constructs an instance of this class.
     */
    public MockCLIManager()
        throws CLIException {
        super(DEFINITION_CLASS);
    }

    /**
     * Returns product name.
     *
     * @return product name.
     */
    public String getProductName() {
        return rb.getString(AccessManagerConstants.I18N_PRODUCT_NAME);
    }

    /**
     * Returns <code>true</code> if the option is an authentication related
     * option such as user ID and password.
     *
     * @param opt Name of option.
     * @returns <code>true</code> if the option is an authentication related
     *         option such as user ID and password.
     */
    public boolean isAuthOption(String opt) {
        return false;
    }
}
