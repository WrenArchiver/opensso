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
 * $Id: AttrSet.java,v 1.1 2005-11-01 00:30:17 arvindp Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.iplanet.services.ldap;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

import netscape.ldap.LDAPAttributeSet;

/**
 * Represents a set of attributes
 */
public class AttrSet implements java.io.Serializable, java.lang.Cloneable {

    private ArrayList _attrs = new ArrayList();

    /**
     * iPlanet-PUBLIC-STATIC Empty Attribute Set.
     */
    public static final AttrSet EMPTY_ATTR_SET = new AttrSet();

    /**
     * iPlanet-PUBLIC-CONSTRUCTOR No argument constructor
     */
    public AttrSet() {
    }

    /**
     * iPlanet-PUBLIC-CONSTRUCTOR Construct attribute set given an array of
     * attributes
     * 
     * @param attrs
     *            array of attributes to be defined in the attribute set
     */
    public AttrSet(Attr[] attrs) {
        int size = attrs.length;
        _attrs = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            _attrs.add(attrs[i]);
        }
    }

    /**
     * iPlanet-PUBLIC-CONSTRUCTOR Construct attribute set given an attribute
     * 
     * @param attr
     *            attribute to be defined in the attribute set
     */
    public AttrSet(Attr attr) {
        add(attr);
    }

    /**
     * Construct AttrSet from LDAPAttributeSet
     * 
     * @param ldapAttrSet
     *            LDAP attribute set
     * 
     */
    public AttrSet(LDAPAttributeSet ldapAttrSet) {
        int size = ldapAttrSet.size();
        _attrs = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            _attrs.add(new Attr(ldapAttrSet.elementAt(i)));
        }
    }

    /**
     * iPlanet-PUBLIC-METHOD Add one attribute to the AttrSet The attribute
     * should have only string values
     * 
     * @param attr
     *            attribute to be added to the set
     */
    public void add(Attr attr) {
        if (attr == null)
            return;
        Attr attr1 = findAttribute(attr.getName());
        if (attr1 == null) {
            _attrs.add(attr);
        } else {
            // attribute already exists,
            // add new values to existing attribute
            attr1.addValues(attr.getStringValues());
        }
    }

    /**
     * iPlanet-PUBLIC-METHOD Add one attribute to the AttrSet The attribute
     * should have only byte values
     * 
     * @param attr
     *            attribute to be added to the set
     */
    public void addBinaryAttr(Attr attr) {
        Attr attr1 = findAttribute(attr.getName());
        if (attr1 == null) {
            _attrs.add(attr);
        } else {
            // attribute already exists,
            // add new values to existing attribute
            attr1.addValues(attr.getByteValues());
        }
    }

    /**
     * iPlanet-PUBLIC-METHOD Removes an exisiting attribute
     * 
     * @param attr
     *            attribute to be removed
     */
    public void remove(String name) {
        int index = indexOf(name);
        if (index != -1) {
            _attrs.remove(index);
        }
    }

    /**
     * iPlanet-PUBLIC-METHOD Remove a specified value for an attribute in the
     * set
     * 
     * @param attrName
     *            attribute name to be looked up
     * @param delValue
     *            value to be deleted for the specified attribute
     */
    public void remove(String attrName, String delValue) {
        int index = indexOf(attrName);
        if (index != -1) {
            Attr attr = (Attr) _attrs.get(index);
            attr.removeValue(delValue);
            if (attr.size() == 0) {
                _attrs.remove(index);
            }
        }
    }

    /**
     * iPlanet-PUBLIC-METHOD Replace an existing attribute
     * 
     * @param attr
     *            attribute to be replaced
     */
    public void replace(Attr attr) {
        int index = indexOf(attr.getName());
        if (index != -1) {
            _attrs.set(index, attr);
        } else {
            _attrs.add(attr);
        }
    }

    /**
     * iPlanet-PUBLIC-METHOD Get names of attributes
     * 
     * @return Names of attributes in the set
     */
    public String[] getAttributeNames() {
        int size = size();
        String[] names = new String[size];
        for (int i = 0; i < size; i++) {
            names[i] = ((Attr) _attrs.get(i)).getName();
        }
        return names;
    }

    /**
     * iPlanet-PUBLIC-METHOD Gets the attribute contained in the set. If not
     * found returns null object
     * 
     * @param name
     *            name of the attribute to get
     * @return attribute found
     */
    public Attr getAttribute(String name) {
        // We may probably want to clone. Not cloning now.
        return findAttribute(name);
    }

    /**
     * iPlanet-PUBLIC-METHOD Enumerate the attributes contained in the attribute
     * set
     * 
     * @return enmeration of attributes in the set
     */
    public Enumeration getAttributes() {
        // iterator would be preferred; returning Enumeration for backward
        // compatibility
        return new IterEnumeration(_attrs.iterator());
    }

    /**
     * iPlanet-PUBLIC-METHOD Gets the first string value right from a specified
     * attribute
     * 
     * @param attrName
     *            name of the attribute to be queried in the set
     * @return the first string value found
     */
    public String getValue(String attrName) {
        String value = null;
        Attr attr = findAttribute(attrName);
        if (attr != null) {
            value = attr.getValue();
        }
        return value;
    }

    /**
     * iPlanet-PUBLIC-METHOD Check if attrSet has this attribute
     * 
     * @param attrName
     *            name of the attribute to be checked against the set
     * @return true if found and false otherwise
     */
    public boolean contains(String attrName) {
        boolean containsTheValue = false;
        int index = indexOf(attrName);
        if (index != -1) {
            containsTheValue = true;
        }
        return containsTheValue;
    }

    /**
     * iPlanet-PUBLIC-METHOD Check if this attrSet has the attribute with the
     * given value
     * 
     * @param attrName
     *            name of the attribute to be checked against the set
     * @param value
     *            value of the attribute the attribute should contain
     * @return true if found and false otherwise
     */
    public boolean contains(String attrName, String value) {
        boolean containsTheValue = false;
        Attr attr = findAttribute(attrName);
        if (attr != null) {
            containsTheValue = attr.contains(value);
        }
        return containsTheValue;
    }

    /**
     * iPlanet-PUBLIC-METHOD Get the number of attributes in the Attribute Set
     * 
     * @return number of attributes in the set
     */
    public int size() {
        return _attrs.size();
    }

    /**
     * Get the attribute at an index that starts from 0
     * 
     * @return the attribute at the given index
     */
    public Attr elementAt(int index) {
        return (Attr) _attrs.get(index);
    }

    /**
     * Gets the index for an attribute contained in the set
     * 
     * @return index that is zero based. If attrName is not found in the set,
     *         this method returns -1.
     */
    public int indexOf(String attrName) {
        attrName = attrName.toLowerCase();
        int index = -1;
        int size = _attrs.size();
        for (int i = 0; i < size; i++) {
            if (attrName.equals(((Attr) _attrs.get(i)).getName())) {
                index = i;
                break;
            }
        }
        return index;
    }

    /**
     * Find the attribute gvien the attribute name
     * 
     * @return attribute found, returns null if no such attribute exists
     */
    private Attr findAttribute(String name) {
        name = name.toLowerCase();
        Attr attr = null;
        if (_attrs != null) {
            int size = _attrs.size();
            for (int i = 0; i < size; i++) {
                Attr attr1 = (Attr) _attrs.get(i);
                if (attr1.getName().equals(name)) {
                    attr = attr1;
                    break;
                }
            }
        }
        return attr;
    }

    /**
     * iPlanet-PUBLIC-METHOD Return a copy of the object
     * 
     * @return A copy of the object
     */
    public Object clone() {
        AttrSet attrSet = new AttrSet();
        int size = _attrs.size();
        for (int i = 0; i < size; i++) {
            attrSet.add((Attr) ((Attr) _attrs.get(i)).clone());
        }
        return attrSet;
    }

    /**
     * Maps to an LDAPAttributeSet
     * 
     * @return the equivalent LDAPAttributeSet
     */
    public LDAPAttributeSet toLDAPAttributeSet() {
        LDAPAttributeSet ldapAttrSet = new LDAPAttributeSet();
        int size = size();
        for (int i = 0; i < size; i++) {
            Attr attr = (Attr) _attrs.get(i);
            if (attr.size() > 0) {
                ldapAttrSet.add(attr.toLDAPAttribute());
            }
        }
        return ldapAttrSet;
    }

    /**
     * iPlanet-PUBLIC-METHOD Retrieves the string representation of an AttrSet
     * 
     * @return string representation of the AttrSet.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer("AttrSet: ");
        int size = _attrs.size();
        for (int i = 0; i < size; i++) {
            sb.append(_attrs.get(i).toString() + "\n");
        }
        return sb.toString();
    }

}

class IterEnumeration implements Enumeration {

    private Iterator _iter;

    IterEnumeration(Iterator iterator) {
        _iter = iterator;
    }

    public boolean hasMoreElements() {
        return _iter.hasNext();
    }

    public Object nextElement() {
        return _iter.next();
    }

}
