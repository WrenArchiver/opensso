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
 * $Id: JCEEncryption.java,v 1.1 2005-11-01 00:30:26 arvindp Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.iplanet.services.util;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import com.iplanet.am.util.Debug;

/**
 * <p>
 * This class provides encryption and decryption facility for the SDK based on
 * the existence of a JCE provider in the runtime. Unlike
 * <code>JSSEncryption</code>, this class can only handle a fixed algorithm
 * for key generation and encryption which is <code>PBEWithMD5AndDES</code>.
 * Since different JCE providers such as IAIK use slightly different names for
 * this algorithm, this class provides the facility to over-ride this hardcoded
 * value by setting the system properties for each of these algorithms. The
 * property name for specifying the key generation algorithm is
 * <code>amKeyGenDescriptor</code> and that for specifying encryption
 * algorithm is <code>amCryptoDescriptor</code>.
 * </p>
 * <p>
 * <b>NOTE:</b> The facility of overriding key generation and encryption
 * algorithms must be used very carefully. In particular, this facility is not
 * meant to force the use of an algorithm different from the specified default
 * algorithm <code>PBEWithMD5AndDES</code> since that will result in
 * incompatibility between the <code>JSSEncryption</code> if it is being used
 * by any peer entity such as agent or server. This would not be a problem if
 * all entities in the network were configured to use this encryption provider
 * and all had the same implementation of the specified algorithms available.
 */
public class JCEEncryption implements AMEncryption, ConfigurableKey {

    private static final byte VERSION = 1;

    private static final String CRYPTO_DESCRIPTOR;

    private static final String CRYPTO_DESCRIPTOR_PROPERTY_NAME = 
        "amCryptoDescriptor";

    private static final String CRYPTO_DESCRIPTOR_DEFAULT_VALUE = 
        "PBEWithMD5AndDES";

    private static final String CRYPTO_DESCRIPTOR_PROVIDER;

    private static final String CRYPTO_DESCRIPTOR_PROVIDER_PROPERTY_NAME = 
        "amCryptoDescriptor.provider";

    private static final String CRYPTO_DESCRIPTOR_PROVIDER_DEFAULT_VALUE = 
        "SunJCE";

    private static final String KEYGEN_ALGORITHM;

    private static final String KEYGEN_ALGORITHM_PROPERTY_NAME = 
        "amKeyGenDescriptor";

    private static final String KEYGEN_ALGORITHM_DEFAULT_VALUE = 
        "PBEWithMD5AndDES";

    private static final String KEYGEN_ALGORITHM_PROVIDER;

    private static final String KEYGEN_ALGORITHM_PROVIDER_PROPERTY_NAME = 
        "amKeyGenDescriptor.provider";

    private static final String KEYGEN_ALGORITHM_PROVIDER_DEFAULT_VALUE = 
        "SunJCE";

    private static final int DEFAULT_KEYGEN_ALG_INDEX = 2;

    private static final int DEFAULT_ENC_ALG_INDEX = 2;

    private static final int ITERATION_COUNT = 5;

    private static Debug debug = Debug.getInstance("amSDK");

    static {
        CRYPTO_DESCRIPTOR = System.getProperty(CRYPTO_DESCRIPTOR_PROPERTY_NAME,
                CRYPTO_DESCRIPTOR_DEFAULT_VALUE);
        KEYGEN_ALGORITHM = System.getProperty(KEYGEN_ALGORITHM_PROPERTY_NAME,
                KEYGEN_ALGORITHM_DEFAULT_VALUE);
        CRYPTO_DESCRIPTOR_PROVIDER = System.getProperty(
                CRYPTO_DESCRIPTOR_PROVIDER_PROPERTY_NAME,
                CRYPTO_DESCRIPTOR_PROVIDER_DEFAULT_VALUE);
        KEYGEN_ALGORITHM_PROVIDER = System.getProperty(
                KEYGEN_ALGORITHM_PROVIDER_PROPERTY_NAME,
                KEYGEN_ALGORITHM_PROVIDER_DEFAULT_VALUE);
    }

    /**
     * Method declaration
     * 
     * @param clearText
     */
    public byte[] encrypt(byte[] clearText) {
        return pbeEncrypt(clearText);
    }

    /**
     * Method declaration
     * 
     * @param encText
     */
    public byte[] decrypt(byte[] encText) {
        return pbeDecrypt(encText);
    }

    /**
     * This method attempts to dynamically register the SunJCE provider when
     * needed. This is a work-around for a known problem with WebLogic 7.0 sp2
     * server which does not allow static registration of Sun JCE provider.
     */
    private static void registerSunJCEProvider() {
        String sunJCEProviderClassName = "com.sun.crypto.provider.SunJCE";
        Provider[] providers = Security.getProviders();
        boolean providerRegistered = false;
        if (providers != null && providers.length > 0) {
            for (int i = 0; i < providers.length; i++) {
                if (providers[i].getClass().getName().equals(
                        sunJCEProviderClassName)) {
                    providerRegistered = true;
                    break;
                }
            }
        }
        if (!providerRegistered) {
            if (debug != null && debug.warningEnabled()) {
                debug.warning("JCEEncryption: SunJCE provider not " +
                        "registered. Attempting to register...");
            }
            Provider sunJCEProvider = null;
            try {
                Class sunJCEProviderClass = Class
                        .forName(sunJCEProviderClassName);
                sunJCEProvider = (Provider) sunJCEProviderClass.newInstance();
                Security.addProvider(sunJCEProvider);
                if (debug != null && debug.messageEnabled()) {
                    debug.message("JCEEncryption: registered SunJCE provider");
                }
            } catch (Exception ex) {
                if (debug != null) {
                    debug.error("JCEEncryption: exception while " +
                            "registering provider", ex);
                }
            }
        } else if (debug != null && debug.messageEnabled()) {
            debug.message("JCEEncryption: SunJCE provider is already " +
                    "registered.");
        }
    }

    /**
     * Method declaration
     * 
     * @param clearText
     */
    private byte[] pbeEncrypt(byte[] clearText) {
        byte[] result = null;
        if (clearText == null || clearText.length == 0) {
            return null;
        }

        if (_initialized) {
            try {
                byte type[] = new byte[2];
                type[1] = (byte) DEFAULT_ENC_ALG_INDEX;
                type[0] = (byte) DEFAULT_KEYGEN_ALG_INDEX;

                Cipher pbeCipher = null;
                try {
                    pbeCipher = Cipher.getInstance(CRYPTO_DESCRIPTOR,
                            CRYPTO_DESCRIPTOR_PROVIDER);
                } catch (Exception ex) {
                    if (ex instanceof NoSuchAlgorithmException
                            || ex instanceof NoSuchPaddingException) {
                        // Best effort try dynamically registring the SunJCE
                        // provider
                        if (debug != null) {
                            debug
                                    .error("JCEEncryption: Exception caught: ",
                                            ex);
                        }
                        registerSunJCEProvider();
                        pbeCipher = Cipher.getInstance(CRYPTO_DESCRIPTOR);
                    } else {
                        throw ex;
                    }
                }

                if (pbeCipher != null) {
                    pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey,
                            pbeParameterSpec);

                    result = pbeCipher.doFinal(clearText);
                    byte[] iv = pbeCipher.getIV();

                    result = addPrefix(type, iv, result);
                } else if (debug != null) {
                    debug.error("JCEEncryption: Failed to obtain Cipher");
                }
            } catch (Exception ex) {
                if (debug != null) {
                    debug.error("JCEEncryption:: failed to encrypt data", ex);
                }
            }
        } else {
            if (debug != null) {
                debug.error("JCEEncryption:: not yet initialized");
            }
        }

        return result;
    }

    /**
     * Method declaration
     * 
     * @param type
     * @param iv
     * @param share
     */
    private static byte[] addPrefix(byte type[], byte iv[], byte share[]) {
        byte data[] = new byte[share.length + 11];

        data[0] = VERSION;
        data[1] = type[0];
        data[2] = type[1];

        for (int i = 0; i < 8; i++) {
            data[3 + i] = iv[i];
        }

        for (int i = 0; i < share.length; i++) {
            data[11 + i] = share[i];
        }

        return data;
    }

    /**
     * Method declaration
     * 
     * @param cipherText
     */
    private byte[] pbeDecrypt(byte[] cipherText) {
        byte[] result = null;
        if (_initialized) {
            try {
                byte share[] = cipherText;

                if (share[0] != VERSION) {
                    if (debug != null) {
                        debug.error("JCEEncryption:: Unsported version: "
                                + share[0]);
                    }

                    return null;
                }

                byte raw[] = getRaw(share);

                Cipher pbeCipher = null;
                try {
                    pbeCipher = Cipher.getInstance(CRYPTO_DESCRIPTOR,
                            CRYPTO_DESCRIPTOR_PROVIDER);
                } catch (Exception ex) {
                    if (ex instanceof NoSuchAlgorithmException
                            || ex instanceof NoSuchPaddingException) {
                        // Best effort try dynamically registring the SunJCE
                        // provider
                        if (debug != null) {
                            debug
                                    .error("JCEEncryption: Exception caught: ",
                                            ex);
                        }
                        registerSunJCEProvider();
                        pbeCipher = Cipher.getInstance(CRYPTO_DESCRIPTOR);
                    } else {
                        throw ex;
                    }
                }

                if (pbeCipher != null) {
                    pbeCipher.init(Cipher.DECRYPT_MODE, pbeKey,
                            pbeParameterSpec);

                    result = pbeCipher.doFinal(raw);
                } else if (debug != null) {
                    debug.error("JCEEncryption: Failed to obtain Cipher");
                }
            } catch (Exception ex) {
                if (debug != null) {
                    debug.error("JCEEncryption:: failed to decrypt data", ex);
                }
            }
        } else {
            if (debug != null) {
                debug.error("JCEEncryption:: not yet initialized");
            }
        }

        return result;
    }

    /**
     * Method declaration
     * 
     * @param share
     */
    private static byte[] getRaw(byte share[]) {
        byte data[] = new byte[share.length - 11];

        for (int i = 11; i < share.length; i++) {
            data[i - 11] = share[i];
        }

        return data;
    }

    /**
     * Sets password-based key to use
     */
    public void setPassword(String password) throws Exception {
        pbeKey = SecretKeyFactory.getInstance(KEYGEN_ALGORITHM,
                KEYGEN_ALGORITHM_PROVIDER).generateSecret(
                new PBEKeySpec(password.toCharArray()));
        _initialized = true;
    }

    private static final byte[] ___y = { 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
            0x01, 0x01 };

    private SecretKey pbeKey;

    private boolean _initialized = false;

    private static PBEParameterSpec pbeParameterSpec = new PBEParameterSpec(
            ___y, ITERATION_COUNT);
}
