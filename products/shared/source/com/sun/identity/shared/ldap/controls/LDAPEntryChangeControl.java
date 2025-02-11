/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * The contents of this file are subject to the Netscape Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/NPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is mozilla.org code.
 *
 * The Initial Developer of the Original Code is Netscape
 * Communications Corporation.  Portions created by Netscape are
 * Copyright (C) 1999 Netscape Communications Corporation. All
 * Rights Reserved.
 *
 * Contributor(s): 
 */
package com.sun.identity.shared.ldap.controls;

import java.io.*;
import com.sun.identity.shared.ldap.ber.stream.*;
import com.sun.identity.shared.ldap.client.JDAPBERTagDecoder;
import com.sun.identity.shared.ldap.*;

/**
 * Represents an LDAP v3 server control that specifies information
 * about a change to an entry in the directory.  (The OID for this
 * control is 2.16.840.1.113730.3.4.7.)  You need to use this control in
 * conjunction with a "persistent search" control (represented
 * by <CODE>LDAPPersistentSearchControl</CODE> object.
 * <P>
 *
 * To use persistent searching for change notification, you create a
 * "persistent search" control that specifies the types of changes that
 * you want to track.  When an entry is changed, the server sends that
 * entry back to your client and may include an "entry change notification"
 * control that specifies additional information about the change.
 * <P>
 *
 * Typically, you use the <CODE>getResponseControls</CODE> method of
 * the <CODE>LDAPConnection</CODE> object to get any 
 * <CODE>LDAPEntryChangeControl</CODE> objects returned by the server.
 * <P>
 *
 * Once you retrieve an <CODE>LDAPEntryChangeControl</CODE> object from
 * the server, you can get the following additional information about
 * the change made to the entry:
 * <P>
 *
 * <UL>
 * <LI>The type of change made (add, modify, delete, or modify DN)
 * <LI>The change number identifying the record of the change in the
 * change log (if the server supports change logs)
 * <LI>If the entry was renamed, the old DN of the entry
 * </UL>
 * <P>
 *
 * @see com.sun.identity.shared.ldap.controls.LDAPPersistSearchControl
 * @see com.sun.identity.shared.ldap.LDAPConnection#getResponseControls
 */

public class LDAPEntryChangeControl extends LDAPControl {

    /**
     * Constructs a new <CODE>LDAPEntryChangeControl</CODE> object.
     * @see com.sun.identity.shared.ldap.LDAPControl
     * @see com.sun.identity.shared.ldap.controls.LDAPPersistSearchControl
     */
    public LDAPEntryChangeControl() {
        super(ENTRYCHANGED, false, null);
    }

    /**
     * Contructs an <CODE>LDAPEntryChangedControl</CODE> object. 
     * This constructor is used by <CODE>LDAPControl.register</CODE> to 
     * instantiate entry change controls.
     * @param oid this parameter must be 
     * <CODE>LDAPEntryChangeControl.ENTRYCHANGED</CODE>
     * or an <CODE>LDAPException</CODE> is thrown
     * @param critical <code>true</code> if this control is critical
     * @param value the value associated with this control
     * @exception com.sun.identity.shared.ldap.LDAPException If oid is not 
     * <CODE>LDAPEntryChangeControl.ENTRYCHANGED</CODE>.
     * @exception java.io.IOException If value is not a valid BER sequence.
     * @see com.sun.identity.shared.ldap.LDAPControl#register
     */
    public LDAPEntryChangeControl(String oid, boolean critical, byte[] value) 
        throws LDAPException, IOException {
	super(ENTRYCHANGED, false, value);
	
	if (!oid.equals( ENTRYCHANGED )) {
	    throw new LDAPException("oid must be LDAPEntryChangeControl." +
				    "ENTRYCHANGED", LDAPException.PARAM_ERROR);
	}

        
        ByteArrayInputStream inStream = new ByteArrayInputStream(m_value);
        BERSequence seq = new BERSequence();
        JDAPBERTagDecoder decoder = new JDAPBERTagDecoder();
        int[] numRead = new int[1];
        numRead[0] = 0;

	BERSequence s = (BERSequence)BERElement.getElement(decoder, inStream,
							   numRead);
	
	BEREnumerated itr = (BEREnumerated)s.elementAt(0);
	    
	m_changeTypes = itr.getValue();

	if (s.size() > 1) {            
            BERElement ele = (BERElement) s.elementAt(1);
            if (ele != null) {
                if (ele.getType() == BERElement.OCTETSTRING) {
                    BEROctetString str = (BEROctetString)ele;
                    try {
                        m_previousDN = new String(str.getValue(), "UTF8");
                    } catch (UnsupportedEncodingException e) {
                    }
                } else if (ele.getType() == BERElement.INTEGER) {
                    BERInteger num = (BERInteger)ele;
                    m_changeNumber = num.getValue();
                }
            }            
	}
	if (s.size() > 2) {
	    BERInteger num = (BERInteger)s.elementAt(2);
  	    m_changeNumber = num.getValue();
	}
    }  

    public int getType() {
        return LDAPControl.LDAP_ENTRY_CHANGE_CONTROL;
    }
    
    /**
     * Sets the change number (which identifies the record of the change
     * in the server's change log) in this "entry change notification"
     * control.
     * @param num change number to set
     * @see com.sun.identity.shared.ldap.controls.LDAPEntryChangeControl#getChangeNumber
     */
    public void setChangeNumber(int num) {
        m_changeNumber = num;
    }

    /**
     * Sets the change type (which identifies the type of change
     * that occurred) in this "entry change notification" control.
     * @param num change type to set. This can be one of
     * the following values:
     * <P>
     *
     * <UL>
     * <LI><CODE>LDAPPersistSearchControl.ADD</CODE> (a new entry was
     * added to the directory)
     * <LI><CODE>LDAPPersistSearchControl.DELETE</CODE> (an entry was
     * removed from the directory)
     * <LI><CODE>LDAPPersistSearchControl.MODIFY</CODE> (an entry was
     * modified)
     * <LI><CODE>LDAPPersistSearchControl.MODDN</CODE> (an entry was
     * renamed)
     * </UL>
     * <P>
     *
     * @see com.sun.identity.shared.ldap.controls.LDAPEntryChangeControl#getChangeType
     */
    public void setChangeType(int num) {
        m_changeTypes = num;
    }

    /**
     * Sets the previous DN of the entry (if the entry was renamed)
     * in the "entry change notification control".
     * @param dn the previous distinguished name of the entry
     * @see com.sun.identity.shared.ldap.controls.LDAPEntryChangeControl#getPreviousDN
     */
    public void setPreviousDN(String dn) {
        m_previousDN = dn;
    }

    /**
     * Gets the change number, which identifies the record of the change
     * in the server's change log.
     * @return change number identifying the change made.
     * @see com.sun.identity.shared.ldap.controls.LDAPEntryChangeControl#setChangeNumber
     */
    public int getChangeNumber() {
        return m_changeNumber;
    }

    /**
     * Gets the change type, which identifies the type of change
     * that occurred.
     * @return change type identifying the type of change that
     * occurred.  This can be one of the following values:
     * <P>
     *
     * <UL>
     * <LI><CODE>LDAPPersistSearchControl.ADD</CODE> (a new entry was
     * added to the directory)
     * <LI><CODE>LDAPPersistSearchControl.DELETE</CODE> (an entry was
     * removed from the directory)
     * <LI><CODE>LDAPPersistSearchControl.MODIFY</CODE> (an entry was
     * modified)
     * <LI><CODE>LDAPPersistSearchControl.MODDN</CODE> (an entry was
     * renamed)
     * </UL>
     * <P>
     *
     * @see com.sun.identity.shared.ldap.controls.LDAPEntryChangeControl#setChangeType
     */
    public int getChangeType() {
        return m_changeTypes;
    }

    /**
     * Gets the previous DN of the entry (if the entry was renamed).
     * @return the previous distinguished name of the entry.
     * @see com.sun.identity.shared.ldap.controls.LDAPEntryChangeControl#setPreviousDN
     */
    public String getPreviousDN() {
        return m_previousDN;
    }

    public String toString() {
         StringBuffer sb = new StringBuffer("{EntryChangedCtrl:");
        
        sb.append(" isCritical=");
        sb.append(isCritical());
        
        sb.append(" changeTypes=");
        sb.append(LDAPPersistSearchControl.typesToString(m_changeTypes));
        
        sb.append(" previousDN=");
        sb.append(m_previousDN);
        
        sb.append(" changeNumber=");
        sb.append(m_changeNumber);

        sb.append("}");

        return sb.toString();
    }

    private int m_changeNumber = -1;
    private int m_changeTypes = -1;
    private String m_previousDN = null;
    public final static String ENTRYCHANGED = "2.16.840.1.113730.3.4.7";

}

