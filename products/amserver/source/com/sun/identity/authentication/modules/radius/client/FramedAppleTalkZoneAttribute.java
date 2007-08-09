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
 * $Id: FramedAppleTalkZoneAttribute.java,v 1.1 2007-08-09 22:25:03 pawand Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */


package com.sun.identity.authentication.modules.radius.client;

import java.util.*;
import java.math.*;
import java.security.*;
import java.net.*;
import java.io.*;

public class FramedAppleTalkZoneAttribute extends Attribute
{
	private byte _value[] = null;
	private String _str = null;

	public FramedAppleTalkZoneAttribute(byte value[])
	{
		super();
		_t = FRAMED_APPLETALK_ZONE;
		_str = new String(value, 2, value.length - 2);
		_value = value;
	}

	public String getString()
	{
		return _str;
	}

	public byte[] getValue() throws IOException
	{
		return _str.getBytes();
	}
}
