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
 * $Id: AuthenticationCommon.java,v 1.1 2007-03-20 22:02:28 rmisra Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.sun.identity.qatest.common.TestCommon;
import java.lang.String;
import java.net.URL;
import java.util.logging.Level;

/**
 * This class contains helper method related to Authentication
 */
public class AuthenticationCommon extends TestCommon {

    public AuthenticationCommon() {
        super("AuthenticationCommon");
    }

   /**
    * Tests zero page login for given mode. This is a positive test. If the login is 
    * unsuccessfull, an error is thrown.
    */
    public void testZeroPageLoginPositive(WebClient wc, String user, String password, String mode,
            String modeValue, String passMsg)
        throws Exception {
        Object[] params = {user, password, mode, modeValue, passMsg};
        entering("testZeroPageLoginPositive", params);
        try {
            String strTest;
            if (mode.equals("user"))
                strTest = protocol + ":" + "//" + host + ":" + port + uri +
                        "/UI/Login?IDToken1=" + user + "&IDToken2=" + password;
            else
                strTest = protocol + ":" + "//" + host + ":" + port + uri +
                        "/UI/Login?" + mode + "=" + modeValue + "&IDToken1=" +
                        user + "&IDToken2=" + password;
            log(logLevel, "testZeroPageLoginPositive", strTest);
            URL url = new URL(strTest);
            HtmlPage page = (HtmlPage)wc.getPage( url );
            log(logLevel, "testZeroPageLoginPositive", page.getTitleText());
            assert page.getTitleText().equals(passMsg);
        } catch (Exception e) {
            log(Level.SEVERE, "testZeroPageLoginPositive", e.getMessage(),
                    params);
            e.printStackTrace();
            throw e;
        }
        exiting("testZeroPageLoginPositive");
    }

   /**
    * Tests zero page login for a module. This is a negative test. If the login is
    * successfull, an error is thrown.
    */
    public void testZeroPageLoginNegative(WebClient wc, String user, String password, String mode,
            String modeValue, String failMsg)
        throws Exception {
        Object[] params = {user, password, mode, modeValue, failMsg};
        entering("testZeroPageLoginNegative", params);
        try {
            String strTest;
            if (mode.equals("user"))
                strTest = protocol + ":" + "//" + host + ":" + port + uri +
                        "/UI/Login?IDToken1=" + user + "&IDToken2=" +
                        password + "negative";
            else
                strTest = protocol + ":"  + "//" + host + ":" + port + uri +
                        "/UI/Login?" + mode + "=" + modeValue + "&IDToken1=" +
                        user + "&IDToken2=" + password + "negative";
            log(logLevel, "testZeroPageLoginNegative", strTest);
            URL url = new URL(strTest);
            HtmlPage page = (HtmlPage)wc.getPage( url );
            log(logLevel, "testZeroPageLoginNegative", page.getTitleText());
            assert page.getTitleText().equals(failMsg);
        } catch (Exception e) {
            log(Level.SEVERE, "testZeroPageLoginNegative", e.getMessage(),
                    params);
            e.printStackTrace();
            throw e;
        }
        exiting("testLoginNegative");
    }

   /**
    * Tests zero page login for given mode. This is a test for a valid user
    * but the user session has expired.
    */
    public void testZeroPageLoginFailure(WebClient wc, String user, String password, String mode,
            String modeValue, String passMsg)
        throws Exception {
        Object[] params = {user, password, mode, modeValue, passMsg};
        entering("testZeroPageLoginFailure", params);
        try {
            String strTest;
            if (mode.equals("user"))
                strTest = protocol + ":" + "//" + host + ":" + port + uri +
                        "/UI/Login?IDToken1=" + user + "&IDToken2=" + password;
            else
                strTest = protocol + ":" + "//" + host + ":" + port + uri +
                        "/UI/Login?" + mode + "=" + modeValue + "&IDToken1=" +
                        user + "&IDToken2=" + password;
            log(logLevel, "testZeroPageLoginFailure", strTest);
            URL url = new URL(strTest);
            HtmlPage page = (HtmlPage)wc.getPage( url );
            log(logLevel, "testZeroPageLoginFailure", page.getTitleText());
            assert page.getTitleText().equals(passMsg);
        } catch (Exception e) {
            log(Level.SEVERE, "testZeroPageLoginFailure", e.getMessage(),
                    params);
            e.printStackTrace();
            throw e;
        }
        exiting("testZeroPageLoginFailure");
    }
}
