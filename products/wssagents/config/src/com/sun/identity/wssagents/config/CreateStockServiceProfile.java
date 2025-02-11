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
 * $Id: CreateStockServiceProfile.java,v 1.1 2008-08-29 21:30:44 arunav Exp $
 *
 */
package com.sun.identity.wssagents.config;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import java.util.ArrayList;
import java.util.List;
import com.sun.identity.wss.provider.ProviderConfig;

/** Creates the profile using OpenSSO client sdk */

public class CreateStockServiceProfile {
/**
     * Default constructor. 
     */
    public CreateStockServiceProfile(){
    
    } 
    private void createProfile ()throws Exception { 

       try {
        //creates the WSP profile
        ProviderConfig wspPc = null ;
        wspPc = ProviderConfig.getProvider("wsp", ProviderConfig.WSP);

        System.out.println ("sec mechanism before modification" +
               wspPc.getSecurityMechanisms());

        List listSec = new ArrayList();
        listSec.add("urn:liberty:security:2005-02:null:Bearer");
        listSec.add("urn:liberty:security:2005-02:null:SAML");
        listSec.add("urn:liberty:security:2005-02:null:X509");
        listSec.add("urn:sun:wss:security:null:SAMLToken-HK"); 
        listSec.add("urn:sun:wss:security:null:SAML2Token-HK"); 
        listSec.add("urn:sun:wss:security:null:SAML2Token-SV"); 
        listSec.add("urn:sun:wss:security:null:SAMLToken-SV"); 
        listSec.add("urn:sun:wss:security:null:UserNameToken"); 
        listSec.add("urn:sun:wss:security:null:Anonymous"); 
        listSec.add("urn:sun:wss:security:null:X509Token"); 
        listSec.add("urn:sun:wss:security:null:UserNameToken-Plain"); 
        wspPc.setSecurityMechanisms(listSec); 
        wspPc.setRequestSignEnabled(true);
        ProviderConfig.saveProvider(wspPc);
        wspPc = null ;
        wspPc = ProviderConfig.getProvider("wsp", ProviderConfig.WSP);

        System.out.println ("sec mechanism after modification " +
               wspPc.getSecurityMechanisms()); 

        // check the provider is saved correctly
        if (!ProviderConfig.isProviderExists("wsp",
                        ProviderConfig.WSP)) {
        System.out.println (  "WSP provider config is not available");
        }
       
        //creates the WSC profile 
        ProviderConfig wscPc = null ; 
        wscPc = ProviderConfig.getProvider("StockService", ProviderConfig.WSC);
        listSec = new ArrayList(); 
        listSec.add("urn:sun:wss:security:null:SAML2Token-SV");
        wscPc.setSecurityMechanisms(listSec); 
        wscPc.setRequestSignEnabled(true);
        wscPc.setPreserveSecurityHeader(true);
        wscPc.setDefaultKeyStore(true);
        wscPc.setWSPEndpoint("default");
        ProviderConfig.saveProvider(wscPc);
        wscPc = null;
        wscPc = ProviderConfig.getProvider("StockService", ProviderConfig.WSC);   
        System.out.println ("sec mechanism for Stockservice are" +
               wscPc.getSecurityMechanisms());
        if (!ProviderConfig.isProviderExists("StockService",
                        ProviderConfig.WSC)) {
        System.out.println (  "StockService provider config is not available");
        }
        }catch (Exception e) {
           e.printStackTrace();
        }
    }//createProfile

      public static void main(String[] args) throws Exception {
        CreateStockServiceProfile agentProfile = new CreateStockServiceProfile();
        agentProfile.createProfile(); 
        System.exit(0);
    }

}
