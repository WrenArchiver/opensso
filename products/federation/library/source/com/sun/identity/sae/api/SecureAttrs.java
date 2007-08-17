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
 * $Id: SecureAttrs.java,v 1.1 2007-08-17 22:48:10 exu Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.sae.api;


import java.util.*;
import java.io.*;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import sun.misc.CharacterEncoder;
import com.sun.identity.shared.encode.Base64;
import java.security.*;
import java.security.cert.X509Certificate;

/**
 * <code>SecureAttrs</code> class forms the core api of "Secure Attributes
 * Exchange" (SAE) feature. The class uses off the shelf digital
 * signing and encryption algorithms to generate tamperproof/nonrepudiable
 * strings representing attribute maps and to verify these strings.
 * Typical SAE usage is to securely send attributes (authentication &
 * use profile data) from an asserting application (eg running on an IDP) to 
 * a relying application (eg running on an SP). In this scenario the
 * asserting party uses the "signing" interfaces to generate secure
 * data and the relying application uses "verification" interfaces
 * to ascertain the authenticity of the data.
 * Current implementation provides two mechanisms to secure attributes :
 *    Symmetric  : uses simple shared secrets between the two ends. 
 *    Asymmetric : uses PKI based signing using public-private keys.
 * Freshness is provided by a varying seed generated from the
 * current timestamp and a configurable expiry period within which
 * the relying party must validate the token.
 * @supported.all.api
 */
public class SecureAttrs
{
    /**
     *  HTTP parameter name used to send and receive secure attribute data. 
     *  IDP : sends secure attrs in this parameter.
     *  SP  : receives secure attrs in this parameter.
     */
    public static final String SAE_PARAM_DATA     = "sun.data";

    /**
     *  SAE Parameter representing a command.
     *  Currently only "logout" needs to be explicitly provided. SSO is implied.
     *  IDP  : Uses this parameter to instruct FM to issue a global logout. 
     *  SP   : Receives this parameter from FM.
     */

    public static final String SAE_PARAM_CMD      = "sun.cmd";

    /**
     *  SAE Parameter representing the authenticated user.
     *  IDP  : Uses this parameter to send authenticated userid to FM.
     *  SP   : Receives userid in this parameter.
     */
    public static final String SAE_PARAM_USERID   = "sun.userid";

    /**
     *  SAE Parameter representing the requested SP app to be invoked.
     *  IDP  : populates this parameter with SP side app to be invoked.
     *  SP   : Not Applicable.
     */
    public static final String SAE_PARAM_SPAPPURL = "sun.spappurl";

    /**
     *  SAE Parameter used to identify the IDP app (Asserting party)
     *  IDP  : populates this parameter to identify itself.
     *  SP   : Not Applicable.
     */
    public static final String SAE_PARAM_IDPAPPURL = "sun.idpappurl";

    /**
     *  SAE Parameter : Deprecated.
     */
    public static final String SAE_PARAM_APPID    = "sun.appid";

    /**
     *  SAE Parameter internally used by FM for storing token timestamp.
     */
    public static final String SAE_PARAM_TS       = "sun.ts";

    /**
     *  SAE Parameter internally used by FM for storing signature data.
     */
    public static final String SAE_PARAM_SIGN     = "sun.sign";

    /**
     *  SAE Parameter used to comunicate errors.
     */
    public static final String SAE_PARAM_ERROR     = "sun.error";

    /**
     *  SAE Parameter used to communicate to SP to return to specified url 
     *  upon Logout completion.
     *  IDP : Not applicable
     *  SP  : expected to redirect to the value upon processing logout req.
     */
    public static final String SAE_PARAM_APPSLORETURNURL  = "sun.returnurl";

    /**
     *  SAE Parameter used to comunicate to FM where to redirect after a 
     *  global logout is completed.
     *  IDP : sends this param as part of logout command.
     *  SP  : N/A.
     */
    public static final String SAE_PARAM_APPRETURN  = "sun.appreturn";

    /**
     *  SAE command <code>SAE_PARAM_CMD</code>
     */
    public static final String SAE_CMD_LOGOUT     = "logout";

    /**
     * Crypto types supported. 
     */
    public static final String SAE_CRYPTO_TYPE = "type";

    /**
     * Crypto type : Symmetric : shared secret based trust between parties.
     */
    public static final String SAE_CRYPTO_TYPE_ASYM = "asymmetric";

    /**
     * Crypto type : Asymmetric : PKI based trust.
     */
    public static final String SAE_CRYPTO_TYPE_SYM = "symmetric";

    /**
     * SAE Config : Location of the keystore to access keys from for
     *   asymmetric crypto.
     */
    public static final String SAE_CONFIG_KEYSTORE_FILE = "keystorefile";

    /**
     *  SAE Config : keystore type. Default : JKS
     */
    public static final String SAE_CONFIG_KEYSTORE_TYPE = "keystoretype";

    /**
     * SAE Config : Password to open the keystrore.
     */
    public static final String SAE_CONFIG_KEYSTORE_PASS = "keystorepass";

    /**
     * SAE Config : Private key alias for asymmetric signing. Alias
     *              is used to retrive the key from the keystore.
     */
    public static final String SAE_CONFIG_PRIVATE_KEY_ALIAS = "privatekeyalias";

    /**
     * SAE Config : Public key for asymmetric signature verification. Alias
     *              is used to retrive the key from the keystore.
     */
    public static final String SAE_CONFIG_PUBLIC_KEY_ALIAS = "pubkeyalias";

    /**
     * SAE Config : Private key for asymmetric signing.
     */
    public static final String SAE_CONFIG_PRIVATE_KEY = "privatekey";

    /**
     * SAE Config : Password to access the private key.
     */
    public static final String SAE_CONFIG_PRIVATE_KEY_PASS = "privatekeypass";

    /**
     * SAE Config : Flag to indicate whether keys should be cached in memory
     *       once retrieved from the keystore.
     */
    public static final String SAE_CONFIG_CACHE_KEYS = "cachekeys";

    /**
     * SAE Config : shared secret constant - used internally in FM.
     */
    public static final String SAE_CONFIG_SHARED_SECRET = "secret";

    /**
     * SAE Config :  Signature validity : since timetamp on signature.
     */
    public static final String SAE_CONFIG_SIG_VALIDITY_DURATION =
                                                "saesigvalidityduration";
    /**
     * Debug : true | false
     */
    public static boolean dbg = true;

    private static Certs certs = null;

    private static boolean isServer = false;
    private static HashMap instances = new HashMap();
    private static boolean initdone;
    private static int tsDuration = 120000; // 2 minutes
    private boolean asymsigning = false;

    /**
     * Returns the appropriate instance to perform crypto operations.
     *  @param type one of <code>SAE_CRYPTO_TYPE</code> values.
     *  @return <code>SecureAttrs</code> instance.
     */
    public static synchronized SecureAttrs getInstance(String type)
    {
        if(instances.get(type) == null)
            instances.put(type, new SecureAttrs(type));
        return (SecureAttrs)instances.get(type);
    }
    /**
     * Initializes SecureAttrs.
     * @param properties : please see SAE_CONFIG_* constants for configurable 
     *                     values.
     * @throws Exception rethrows underlying exception.
     */
    synchronized public static void init(Properties properties) throws Exception
    {
        String dur = properties.getProperty(SAE_CONFIG_SIG_VALIDITY_DURATION);
        if (dur != null) 
            tsDuration = Integer.parseInt(dur);

        if (isServer)
            certs = (Certs) Class.forName(
                            "com.sun.identity.sae.api.FMCerts").newInstance();
        else
            certs = new DefaultCerts();

        certs.init(properties);

        initdone = true;
    }

    /**
     * Returns true if SecureAttrs has already been initialzed via a call
     * to <code>init</code>.
     * @return true or false
     */
    public static boolean isInitialized()
    {
        return initdone;
    }
    /**
     * Returns a Base64 encoded string comprising a signed set of attributes.
     *
     *   @param attrs	Attribute Value pairs to be processed.
     *   @param secret	Shared secret (symmetric) Private key alias (asymmetric)
     *
     *   @return Base64 encoded token String to be passed to a relying party.
     */
    public String getEncodedString(Map attrs, String secret) throws Exception 
    {
        if(attrs == null || attrs.isEmpty() ){
           return null;
        }

        StringBuffer sb = new StringBuffer(200);
        Iterator iter = attrs.entrySet().iterator();
        while(iter.hasNext()) {
           Map.Entry entry = (Map.Entry)iter.next();
           String key = (String)entry.getKey();
           String value = (String)entry.getValue();
           sb.append(key).append("=").append(value).append("|");
        }

        sb.append("Signature=").append(getSignedString(attrs, secret));

        return Base64.encode(sb.toString().getBytes("UTF-8"));
    }

    /**
     * Verifies a Base64 encoded string for authenticity based on the
     * shared secret supplied.
     *   @param str	Base64 encoded string containing attribute
     *   @param secret	Shared secret (symmmetric) or Public Key (asymmetric)
     *
     *   @return	Decoded, verified and parsed attrbute name-valie pairs.
     */
    public Map verifyEncodedString(String str, String secret) throws Exception
    {
        if(str == null) {
            return null;
        }

        Map map = getRawAttributesFromEncodedData(str);
        String signatureValue = (String) map.remove("Signature");
        if(!verifyAttrs(map, signatureValue, secret)) {
            return null; 
        }
        return map; 
    }

    /**
     * Returns a decoded <code>Map</code> of attribute-value pairs. 
     * No verification is performed. Useful when retrieving data before 
     * verifying contents for authenticity.
     *   @param str	Base64 encoded string containing attribute
     *
     *   @return	Decoded and parsed attrbute name-value pairs.
     */
    public Map getRawAttributesFromEncodedData(String str) throws Exception
    {
        if(str == null) {
           return null;
        }

        byte[] bytes = Base64.decode(str); 
        String decoded = new String(bytes); // , "UTF-8");
        if(decoded.indexOf("|") == -1) {
            return null;
        }
        StringTokenizer tokenizer = new StringTokenizer(decoded, "|");

        Map map = new HashMap();
        while(tokenizer.hasMoreTokens()) {
            String st = tokenizer.nextToken(); 
            int index = st.indexOf("=");
            if(index == -1) {
               continue;
            }
            String attr = st.substring(0, index); 
            String value = st.substring(index+1, st.length());
            map.put(attr, value);
        }

        return map; 
    }

    /** 
     * This interface allows to set the private to be used for signing
     * as an alternative to passing down <code>SAE_CONFIG_PRIVATE_KEY_ALIAS</a>
     * via <code>init</code>. Use this interface if you do not want
     * SecureAttr to obtain the signing key from a configured keystore.
     * To use this key during signing, specify secret as null.
     * @param privatekey
     */
    public static void setPrivateKey(PrivateKey privatekey)
    {
        certs.setPrivatekey(privatekey);
    }

    /** 
     * This interface allows to register a public key to be used for signature
     * verification. Use this interface if you do not want SecureAttrs to
     * obtain public keys from a configured keystore.
     * @param pubkeyalias
     * @param x509certificate instance.
     */
    public static void addPublicKey(
                       String pubkeyalias, X509Certificate x509certificate)
    {
        certs.addPublicKey(pubkeyalias, x509certificate);
    }

    public static void setServerFlag(boolean flag)
    {
        isServer = flag;
    }

    private static X509Certificate getPublicKey(String alias)
    {
        return certs.getPublicKey(alias);
    }

    /**
     * Returns a String representing data in the attrs argument.
     * The String generated can be one of the following depending
     * on configuration  :
     *   SHA1 digest based on a shared secret and current timestamp.
     *   or
     *   Digital signature based on a configured certificate key.
     *
     *   @param attrs	List of attribute Value pairs to be processed.
     *   @param secret	Shared secret (symmmetric) or Private Key (asymmetric)
     *
     *   @return	token String to be passed to a relying party.
     */
    public String getSignedString(Map attrs, String secret) throws Exception
    {
        // Normalize     
        StringBuffer str = normalize(attrs);
        // Setup a fresh timestamp
        long timestamp = (new Date()).getTime();

        String signature = null;

        if(asymsigning)
        {
            PrivateKey pKey = certs.getPrivateKey(secret);
            signature = signAsym(str.append(timestamp).toString(), pKey);
        } else
        {
            // Create seed : TIMESTAMP + shared secret
            String seed = secret+timestamp;
            // Encrypt
            signature = encrypt(str+seed, seed);
        }
        if (signature == null)
            return null;
        return ("TS"+timestamp + "TS"+signature);
    }

    /**
     * Verifies the authenticity of data the attrs argument based
     * on the token presented. Both attrs and token is sent by
     * a asserting party. 
     *   @param attrs	List of attribute Value pairs to be processed.
     *   @param token	token represnting attrs provided by asserting party.
     *   @param secret	Shared secret (symmmetric) or Public Key (asymmetric)
     *
     *   @return true  if attrs and token verify okay, else returns false.
     */
    public boolean verifyAttrs(Map attrs, String token, String secret) 
                               throws Exception
    {
        // Normalize     
        StringBuffer str = normalize(attrs);
        // Retrieve timestamp
        int idx = token.indexOf("TS", 2);
        String ts = token.substring(2, idx);
        long signts = Long.parseLong(ts);
        long nowts = (new Date()).getTime();

        // Check timestamp validity
        if ((nowts - signts) > tsDuration)
            return false;

        if(asymsigning)
        {
            String signature = token.substring(idx + 2, token.length());
            return verifyAsym(str.append(ts).toString(), 
                              signature, getPublicKey(secret));
        }
        // Create seed : TIMESTAMP + shared secret
        String seed = secret + ts;
        // Encrypt
        String newstr ="TS"+ts+ "TS"+encrypt(str+seed, seed);
        if (token.equals(newstr) )
            return true;
        else
            return false;
    }


    private SecureAttrs(String type)
    {
        if (SAE_CRYPTO_TYPE_ASYM.equals(type))
            asymsigning = true;
    }

    private StringBuffer normalize(Map attrs)
    {
        // Sort the Map
        TreeMap smap = new TreeMap(attrs);
        
        // Flatten to a single String
        StringBuffer str = new StringBuffer();
        Iterator iter = smap.keySet().iterator();      
        while (iter.hasNext()) {
            String key = (String) iter.next();
            str.append(key).append("=").append(smap.get(key)).append("|");
        }
        return str;
    }  

    private synchronized String encrypt(String plaintext, 
              String seed) throws Exception
    {
        MessageDigest md = null;
        try
        {
            md = MessageDigest.getInstance("SHA"); //step 2
        } catch(NoSuchAlgorithmException e) {
            throw new Exception(e.getMessage());
        }
        try
        {
            md.update((plaintext).getBytes("UTF-8")); //step 3
        } catch(UnsupportedEncodingException e) {
            throw new Exception(e.getMessage());
        }

        byte raw[] = md.digest(); //step 4
        String hash = Base64.encode(raw);

        return hash; //step 6
    }
    private String signAsym(String s, PrivateKey privatekey)
    {
        if(s == null || s.length() == 0 || privatekey == null) {
            if (dbg)
                System.out.println("SAE : signAsym: returning since priv key null");
            return null;
        }
        String s1 = privatekey.getAlgorithm();
        Signature signature = null;
        Object obj = null;
        if(s1.equals("RSA"))
            try
            {
                signature = Signature.getInstance("SHA1withRSA");
                String s2 = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
            }
            catch(Exception exception)
            {
                System.out.println("SAE:asym sign : RSA failed ="+exception);
                return null;
            }
        else
        if(s1.equals("DSA"))
        {
            try
            {
                signature = Signature.getInstance("SHA1withDSA");
                String s3 = "http://www.w3.org/2000/09/xmldsig#dsa-sha1";
            }
            catch(Exception exception1)
            {
                System.out.println("SAE:asym sign : DSA failed ="+exception1);
                return null;
            }
        } else
        {
            System.out.println("SAE:asym sign : No Algorithm");
            return null;
        }
        try
        {
            signature.initSign(privatekey);
        }
        catch(Exception exception2)
        {
            System.out.println("SAE:asym sign : sig.initSign failed"+exception2);
            return null;
        }
        try
        {
            System.out.println("Query str:"+s);
            signature.update(s.getBytes());
        }
        catch(Exception exception3)
        {
            System.out.println("SAE:asym sign : sig.update failed"+exception3);
            return null;
        }
        byte abyte0[] = null;
        try
        {
            abyte0 = signature.sign();
        }
        catch(Exception exception4)
        {
            System.out.println("SAE:asym sign : sig.sign failed"+exception4);
            return null;
        }
        if(abyte0 == null || abyte0.length == 0)
        {
            System.out.println("SAE:asym sign : sigBytes null");
            return null;
        } else
        {
            String s4 = Base64.encode(abyte0);
            System.out.println("B64 Signature="+s4);
            return s4;
        }
    }

    private static boolean verifyAsym(String s, String s1, X509Certificate x509certificate)
    {
        if(s == null || s.length() == 0 || x509certificate == null || s1 == null)
        {
            if (dbg)
            System.out.println("SAE:asym verify: qstring or cert or signature is null");
            return false;
        }
        byte abyte0[] = Base64.decode(s1);
        System.out.println("SAE:verifyAsym:signature="+abyte0+" origstr="+s1);
        Object obj = null;
        Object obj1 = null;
        String s2 = x509certificate.getPublicKey().getAlgorithm();
        Signature signature = null;
        if(s2.equals("DSA"))
            try
            {
                signature = Signature.getInstance("SHA1withDSA");
            }
            catch(Exception exception)
            {
                System.out.println("SAE:asym verify : DSA instance"+exception);
                exception.printStackTrace();
                return false;
            }
        else
        if(s2.equals("RSA"))
        {
            try
            {
                signature = Signature.getInstance("SHA1withRSA");
            }
            catch(Exception exception1)
            {
                System.out.println("SAE:asym verify : RSA instance"+exception1);
                exception1.printStackTrace();
                return false;
            }
        } else
        {
            System.out.println("SAE:asym verify : no instance");
            return false;
        }
        try
        {
            signature.initVerify(x509certificate);
        }
        catch(Exception exception2)
        {
            System.out.println("SAE:asym verify :sig.initVerify"+exception2);
            exception2.printStackTrace();
            return false;
        }
        try
        {
            signature.update(s.getBytes());
        }
        catch(Exception exception3)
        {
            System.out.println("SAE:asym verify :sig.update:"+exception3+" sig="+abyte0);
            exception3.printStackTrace();
            return false;
        }
        boolean flag = false;
        try
        {
            flag = signature.verify(abyte0);
        }
        catch(Exception exception4)
        {
            System.out.println("SAE:asym verify :sig.verify:"+exception4+"sig="+abyte0);
            exception4.printStackTrace();
            return false;
        }
        return flag;
    }

    static public void main(String[] args)
    {
        try
        {
            Properties properties = new Properties();
            properties.setProperty("keystorefile", "mykeystore");
            properties.setProperty("keystoretype", "JKS");
            properties.setProperty("keystorepass", "22222222");
            properties.setProperty("privatekeyalias", "testcert");
            properties.setProperty("publickeyalias", "testcert");
            properties.setProperty("privatekeypass", "11111111");
            init(properties);
            System.out.println("TEST 1 START test encoded str ===========");
            SecureAttrs secureattrs = getInstance("symmetric");
            String s = "YnJhbmNoPTAwNXxtYWlsPXVzZXI1QG1haWwuY29tfHN1bi51c2VyaWQ9dXNlcjV8U2lnbmF0dXJlPVRTMTE3NDI3ODY1OTM2NlRTbzI2MkhoL3R1dDRJc0U1V3ZqWjVSLzZkM0FzPQ==";
            Map map = secureattrs.verifyEncodedString(s, "secret");
            if(map == null)
                System.out.println("    FAILED");
            else
                System.out.println("    PASSED"+map);
            System.out.println("TEST 1 END ================");
            System.out.println("TEST 2 START : encode followed by decode ===");
            HashMap hashmap = new HashMap();
            hashmap.put("branch", "bb");
            hashmap.put("mail", "mm");
            hashmap.put("sun.userid", "uu");
            hashmap.put("sun.spappurl", "apapp");
            System.out.println("  TEST 2a START : SYM KEY ===");
            secureattrs = getInstance("symmetric");
            String s1 = "secret";
            String s2 = secureattrs.getEncodedString(hashmap, s1);
            System.out.println("Encoded string: "+s2);
            Map map1 = secureattrs.verifyEncodedString(s2, s1);
            if(map1 != null)
                System.out.println("  2a PASSED "+map1);
            else
                System.out.println("  2a FAILED "+map1);
            System.out.println("  TEST 2b START : ASYM KEY ===");
            secureattrs = getInstance("asymmetric");
            s1 = "testcert";
            String s3 = secureattrs.getEncodedString(hashmap, s1);
            System.out.println("Encoded string: "+s3);
            map1 = secureattrs.verifyEncodedString(s3, s1);
            if(map1 != null)
                System.out.println("  2b PASSED "+map1);
            else
                System.out.println("  2b FAILED "+map1);
            System.out.println("TEST 2 END  ====================");
            System.out.println("TEST 3 START : decode with incorrect secret");
            System.out.println("  TEST 3a START : SYM KEY ===");
            secureattrs = getInstance("symmetric");
            map1 = secureattrs.verifyEncodedString(s2, "junk");
            if(map1 != null)
                System.out.println("  3a FAILED "+map1);
            else
                System.out.println("  3a PASSED "+map1);
            System.out.println("  TEST 3b START : ASYM KEY ===");
            secureattrs = getInstance("asymmetric");
            map1 = secureattrs.verifyEncodedString(s3, "junk");
            if(map1 != null)
                System.out.println("  3b FAILED "+map1);
            else
                System.out.println("  3b PASSED "+map1);
            System.out.println("TEST 3 END  ====================");
            System.out.println("TEST 4 START : decode with correct secret");
            System.out.println("  TEST 4a START : SYM KEY ===");
            secureattrs = getInstance("symmetric");
            s1 = "secret";
            map1 = secureattrs.verifyEncodedString(s2, s1);
            if(map1 != null)
                System.out.println("  4a PASSED "+map1);
            else
                System.out.println("  4a FAILED "+map1);
            System.out.println("  TEST 4b START : ASYM KEY ===");
            secureattrs = getInstance("asymmetric");
            s1 = "testcert";
            map1 = secureattrs.verifyEncodedString(s3, s1);
            if(map1 != null)
                System.out.println("  4a PASSED "+map1);
            else
                System.out.println("  4a FAILED "+map1);
            System.out.println("TEST 4 END  ====================");
        }
        catch(Exception exception)
        {
            System.out.println("TEST Exc : "+exception);
        }
     
    }
    public interface Certs {
        public void init(Properties props) throws Exception;
        public PrivateKey getPrivateKey(String alias);
        public X509Certificate getPublicKey(String alias);
        public void setPrivatekey(PrivateKey privatekey);
        public void addPublicKey(
                       String pubkeyalias, X509Certificate x509certificate);
    }
    static class DefaultCerts implements Certs
    {
        private PrivateKey privateKey = null;
        private KeyStore ks = null;
        private String keystoreFile = "";
        private HashMap keyTable = new HashMap();
        private boolean cacheKeys = true;
        private String pkpass = null;

        public void init(Properties properties) throws Exception
        {
            String keyfile = properties.getProperty("keystorefile");
            if(keyfile != null)
            {
                String ktype = properties.getProperty(
                                         SAE_CONFIG_KEYSTORE_TYPE, "JKS");
                ks = KeyStore.getInstance(ktype);
                FileInputStream fileinputstream = new FileInputStream(keyfile);
                String kpass = properties.getProperty(SAE_CONFIG_KEYSTORE_PASS);
                pkpass = properties.getProperty(SAE_CONFIG_PRIVATE_KEY_PASS);
                ks.load(fileinputstream, kpass.toCharArray());
                String pkeyalias = properties.getProperty(
                                           SAE_CONFIG_PRIVATE_KEY_ALIAS );
                if(pkeyalias != null) {
                    privateKey = (PrivateKey)ks.getKey(pkeyalias, 
                                           pkpass.toCharArray());
                }
                String pubkeyalias = properties.getProperty(
                                           SAE_CONFIG_PUBLIC_KEY_ALIAS );
                if ("false".equals(properties.getProperty(
                          SAE_CONFIG_CACHE_KEYS)))
                    cacheKeys = false;

                if (cacheKeys && pubkeyalias != null)
                    getPublicKeyFromKeystore(pubkeyalias);
            }
        }
        public PrivateKey getPrivateKey(String alias)
        {
            try {
                if (alias == null)
                    return privateKey;
                return (PrivateKey)ks.getKey(alias, 
                                pkpass.toCharArray());
            } catch (Exception ex) {
                return null;
            }
        }
        public X509Certificate getPublicKey(String alias)
        {
            X509Certificate x509certificate = 
                         (X509Certificate)keyTable.get(alias);
            if (x509certificate == null && ks != null) {
                try
                {
                    x509certificate = getPublicKeyFromKeystore(alias);
                }
                catch(Exception exception)
                {
                    System.out.println("SAE:getPublicKey:Exc:"+exception);
                }
            }
            return x509certificate;
        }
        public void setPrivatekey(PrivateKey privatekey)
        {
            privateKey = privatekey;
        }
        public void addPublicKey(
                       String pubkeyalias, X509Certificate x509certificate)
        {
            keyTable.put(pubkeyalias, x509certificate);
        }
        private X509Certificate getPublicKeyFromKeystore(String pubkeyalias)
            throws Exception
        {
            X509Certificate x509certificate = 
                (X509Certificate)ks.getCertificate(pubkeyalias);
            if(cacheKeys)
                keyTable.put(pubkeyalias, x509certificate);
            return x509certificate;
        }
    }
}
