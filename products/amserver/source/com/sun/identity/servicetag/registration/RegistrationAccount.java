/*
 * RegistrationAccount.java
 *
 * Created on November 2, 2007, 10:41 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.identity.servicetag.registration;

public interface RegistrationAccount {
    /*
    public void setAttribute(String name, String value);
    public String getAttributeValue(String name);
    */
    
    public static final String COUNTRY = "country";
    public static final String EMAIL = "email";
    public static final String FIRSTNAME = "firstName";
    public static final String LASTNAME = "lastName";
    public static final String PASSWORD = "password";
    public static final String USERID = "userID";
    
}
