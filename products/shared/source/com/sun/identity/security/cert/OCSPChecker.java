/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: OCSPChecker.java,v 1.3 2008-06-25 05:52:58 qcheng Exp $
 *
 */

package com.sun.identity.security.cert;

import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.Security;
import java.security.cert.*;
import java.net.*;
import javax.security.auth.x500.X500Principal;

import sun.security.util.*;
import sun.security.x509.*;
import sun.misc.HexDumpEncoder;

import com.sun.identity.common.HttpURLConnectionManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.security.SecurityDebug;

/**
 * OCSPChecker is a <code>PKIXCertPathChecker</code> that uses the 
 * Online Certificate Status Protocol (OCSP) as specified in RFC 2560
 * <a href="http://www.ietf.org/rfc/rfc2560.txt">
 * http://www.ietf.org/rfc/rfc2560.txt</a>.
 *
 * @version         1.3 12/19/03
 * @author        Ram Marti        
 */
class OCSPChecker extends PKIXCertPathChecker {
 
    public static final String OCSP_ENABLE_PROP = "ocsp.enable";
    public static final String OCSP_URL_PROP = "ocsp.responderURL";
    public static final String OCSP_CERT_SUBJECT_PROP =
        "ocsp.responderCertSubjectName";
    public static final String OCSP_CERT_ISSUER_PROP =
        "ocsp.responderCertIssuerName";
    public static final String OCSP_CERT_NUMBER_PROP =
        "ocsp.responderCertSerialNumber";

    private static final String HEX_DIGITS = "0123456789ABCDEFabcdef";
    private static final Debug debug = SecurityDebug.debug;
    private static final boolean dump = false; 

    private static final String AUTH_INFO_ACCESS_OID = "1.3.6.1.5.5.7.1.1";

    // Supported extensions
    private static final int OCSP_NONCE_DATA[] = 
        { 1, 3, 6, 1, 5, 5, 7, 48, 1, 2 };
    private static final ObjectIdentifier OCSP_NONCE_OID;
    static {
        OCSP_NONCE_OID = ObjectIdentifier.newInternal(OCSP_NONCE_DATA);
    }

    private int remainingCerts;

    private X509Certificate[] certs;

    private CertPath cp;

    private PKIXParameters pkixParams;

    /**
     * Default Constructor 
     *
     * @param certPath the X509 certification path
     * @param pkixParams the input PKIX parameter set
     * @exception CertPathValidatorException Exception thrown if cert path
     * does not validate.
     */
    OCSPChecker(CertPath certPath, PKIXParameters pkixParams) 
        throws CertPathValidatorException {

        this.cp = certPath; 
        this.pkixParams = pkixParams;
        List tmp = cp.getCertificates();
        certs =
            (X509Certificate[]) tmp.toArray(new X509Certificate[tmp.size()]);
        init(false);
    }
    
    /**
     * Initializes the internal state of the checker from parameters
     * specified in the constructor
     */
    public void init(boolean forward) throws CertPathValidatorException {
        if (!forward) {
            remainingCerts = certs.length;
        } else {
            throw new CertPathValidatorException(
                "Forward checking not supported");
        }
    }

    public boolean isForwardCheckingSupported() {
        return false;
    }

    public Set getSupportedExtensions() {
        return Collections.EMPTY_SET;
    }

    /**
     * Sends an OCSPRequest for the certificate to the OCSP Server and
     * processes the response back from the OCSP Server.
     *
     * @param cert the Certificate
     * @param unresolvedCritExts the unresolved critical extensions
     * @exception CertPathValidatorException Exception is thrown if the 
     *            certificate has been revoked.
     */
    public void check(Certificate cert, Collection unresolvedCritExts)
        throws CertPathValidatorException {

        try {
            // Examine OCSP properties
            X509Certificate responderCert = null;
            boolean seekResponderCert = false;
            X500Principal responderSubjectName = null;
            X500Principal responderIssuerName = null;
            BigInteger responderSerialNumber = null;

            /*
             * OCSP security property values, in the following order:
             *   1. ocsp.responderURL
             *   2. ocsp.responderCertSubjectName
             *   3. ocsp.responderCertIssuerName
             *   4. ocsp.responderCertSerialNumber
             */
            String[] properties = getOCSPProperties();

            // When responder's subject name is set then the issuer/serial 
            // properties are ignored
            if (properties[1] != null) {
                responderSubjectName = new X500Principal(properties[1]);

            } else if (properties[2] != null && properties[3] != null) {
                responderIssuerName = new X500Principal(properties[2]);
                // remove colon or space separators
                String value = stripOutSeparators(properties[3]);
                responderSerialNumber = new BigInteger(value, 16);

            } else if (properties[2] != null || properties[3] != null) {
                throw new CertPathValidatorException(
                    "Must specify both ocsp.responderCertIssuerName and " +
                    "ocsp.responderCertSerialNumber properties");
            }

            // If the OCSP responder cert properties are set then the 
            // identified cert must be located in the trust anchors or
            // in the cert stores.
            if (responderSubjectName != null || responderIssuerName != null) {
                seekResponderCert = true;
            }

            boolean seekIssuerCert = true;
            X509CertImpl issuerCertImpl = null;
            X509CertImpl currCertImpl =
                X509CertImpl.toImpl((X509Certificate)cert);
            remainingCerts--;

            // Set the issuer certificate
            if (remainingCerts != 0) {
                issuerCertImpl = X509CertImpl.toImpl(
                    (X509Certificate)(certs[remainingCerts]));
                seekIssuerCert = false; // done
                
                // By default, the OCSP responder's cert is the same as the 
                // issuer of the cert being validated.
                if (! seekResponderCert) {
                    responderCert = certs[remainingCerts];
                    if (debug.messageEnabled()) {
                        debug.message("OCSPChecker.check: Responder's " +
                                      "certificate is the same as the issuer" +
                                      " of the certificate being validated");
                    }
                }
            }

            // Check anchor certs for:
            //    - the issuer cert (of the cert being validated)
            //    - the OCSP responder's cert
            if (seekIssuerCert || seekResponderCert) {

                if (debug.messageEnabled()) {
                    debug.message("OCSPChecker.check: seekIssuerCert = " +
                                  seekIssuerCert + ", seekResponderCert = " +
                                  seekResponderCert);
                }

                // Extract the anchor certs
                Iterator anchors = pkixParams.getTrustAnchors().iterator();
                if (! anchors.hasNext()) {
                    throw new CertPathValidatorException(
                        "Must specify at least one trust anchor");
                }

                X500Principal certIssuerName =
                    currCertImpl.getIssuerX500Principal();
                while (anchors.hasNext() &&
                        (seekIssuerCert || seekResponderCert)) {

                    TrustAnchor anchor = (TrustAnchor)anchors.next();
                    X509Certificate anchorCert = anchor.getTrustedCert();
                    X500Principal anchorSubjectName =
                        anchorCert.getSubjectX500Principal();

                    if (dump) {
                        System.out.println("Issuer DN is " + certIssuerName);
                        System.out.println("Subject DN is " +
                            anchorSubjectName);
                    }

                    // Check if anchor cert is the issuer cert
                    if (seekIssuerCert &&
                        certIssuerName.equals(anchorSubjectName)) {

                        issuerCertImpl = X509CertImpl.toImpl(anchorCert);
                        seekIssuerCert = false; // done

                        // By default, the OCSP responder's cert is the same as
                        // the issuer of the cert being validated.
                        if (! seekResponderCert && responderCert == null) {
                            responderCert = anchorCert;
                            if (debug.messageEnabled()) {
                                debug.message("OCSPChecker.check: Responder's"+
                                    " certificate is the" +
                                    " same as the issuer of the certificate " +
                                    "being validated");
                            }
                        }
                    }

                    // Check if anchor cert is the responder cert
                    if (seekResponderCert) {
                        // Satisfy the responder subject name property only, or
                        // satisfy the responder issuer name and serial number 
                        // properties only
                        if ((responderSubjectName != null &&
                             responderSubjectName.equals(anchorSubjectName)) ||
                            (responderIssuerName != null &&
                             responderSerialNumber != null &&
                             responderIssuerName.equals(
                                anchorCert.getIssuerX500Principal()) &&
                             responderSerialNumber.equals(
                                anchorCert.getSerialNumber()))) {

                            responderCert = anchorCert;
                            seekResponderCert = false; // done
                        }
                    }
                }
                if (issuerCertImpl == null) {
                    throw new CertPathValidatorException(
                        "No trusted certificate for " + 
                        currCertImpl.getIssuerDN());
                }

                // Check cert stores if responder cert has not yet been found
                if (seekResponderCert) {
                    if (debug.messageEnabled()) {
                        debug.message("OCSPChecker.check: Searching cert " + 
                                      "stores for responder's certificate");
                    }
                    X509CertSelector filter = null;
                    if (responderSubjectName != null) {
                        filter = new X509CertSelector();
                        filter.setSubject(responderSubjectName.getName());
                    } else if (responderIssuerName != null &&
                        responderSerialNumber != null) {
                        filter = new X509CertSelector();
                        filter.setIssuer(responderIssuerName.getName());
                        filter.setSerialNumber(responderSerialNumber);
                    }
                    if (filter != null) {
                        List certStores = pkixParams.getCertStores();
                        for(Iterator iter = certStores.iterator();
                            iter.hasNext();) {
                            CertStore certStore = (CertStore)iter.next();

                            Iterator i =
                                certStore.getCertificates(filter).iterator();
                            if (i.hasNext()) {
                                responderCert = (X509Certificate) i.next();
                                seekResponderCert = false; // done
                                break;
                            }
                        }
                    }
                }
            }

            // Could not find the certificate identified in the OCSP properties
            if (seekResponderCert) {
                throw new CertPathValidatorException(
                    "Cannot find the responder's certificate " +
                    "(set using the OCSP security properties).");
            }

            // Construct an OCSP Request
            OCSPRequest ocspRequest =
                new OCSPRequest(currCertImpl, issuerCertImpl);
            URL url = getOCSPServerURL(currCertImpl, properties);
            HttpURLConnection con =
                HttpURLConnectionManager.getConnection(url); 
            if (debug.messageEnabled()) {
                debug.message("OCSPChecker.check: connecting to OCSP service" +
                              " at: " + url);
            }
            
            // Indicate that both input and output will be performed, 
            // that the method is POST, and that the content length is 
            // the length of the byte array
            
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-type", "application/ocsp-request");
            byte[] bytes = ocspRequest.encodeBytes();
            CertId certId = ocspRequest.getCertId();

            con.setRequestProperty("Content-length",
                String.valueOf(bytes.length));
            OutputStream out = con.getOutputStream();
            out.write(bytes);
            out.flush();

            // Check the response
            if (debug.messageEnabled()) {
                debug.message("OCSPChecker.check: HTTP response code = " +
                              con.getResponseCode() + ", response message = " +
                              con.getResponseMessage());
            }
            InputStream in = con.getInputStream();

            int contentLength = con.getContentLength();
            if (contentLength == -1) {
                contentLength = Integer.MAX_VALUE;
            }

            byte[] response = new byte[contentLength];
            int total = 0;
            int count = 0;
            while (count != -1 && total < contentLength) {
                count = in.read(response, total, response.length - total);
                total += count;
            }

            // clean-up
            in.close();
            out.close();

            OCSPResponse ocspResponse = new OCSPResponse(response, pkixParams,
                responderCert);
            // Check that response applies to the cert that was supplied
            if (! certId.equals(ocspResponse.getCertId())) {
                throw new CertPathValidatorException(
                    "Certificate in the OCSP response does not match the " +
                    "certificate supplied in the OCSP request.");
            }

            SerialNumber serialNumber =
                            new SerialNumber(currCertImpl.getSerialNumber());
            int certOCSPStatus = ocspResponse.getCertStatus(serialNumber);

            if (debug.messageEnabled()) {
                debug.message("OCSPChecker.check: Status of certificate " +
                              "(with serial number " + serialNumber.getNumber()
                              + ") is: " + 
                              OCSPResponse.certStatusToText(certOCSPStatus));
            }
        
            if (certOCSPStatus == OCSPResponse.CERT_STATUS_REVOKED) {
                throw  new CertificateRevokedException(
                    "Certificate has been revoked", cp, remainingCerts);

            } else if (certOCSPStatus == OCSPResponse.CERT_STATUS_UNKNOWN) {
                throw  new CertPathValidatorException(
                    "Certificate's revocation status is unknown", null, cp,
                    remainingCerts);
            } 
        } catch (CertificateRevokedException cre) {
            throw cre;
        } catch (CertPathValidatorException cpve) {
            throw cpve;
        } catch (Exception e) {
            debug.error("OCSPChecker.check: ", e);
            throw new CertPathValidatorException(e);
        }
    }

    /*
     * The OCSP security property values are in the following order:
     *   1. ocsp.responderURL
     *   2. ocsp.responderCertSubjectName
     *   3. ocsp.responderCertIssuerName
     *   4. ocsp.responderCertSerialNumber
     */
    private static URL getOCSPServerURL(X509CertImpl currCertImpl,
        String[] properties)
        throws CertificateParsingException, CertPathValidatorException {
         
        if (properties[0] != null) {
           try {
                return new URL(properties[0]);
           } catch (java.net.MalformedURLException e) {
                throw new CertPathValidatorException(e);
           }
        }

        // Examine the certificate's AuthorityInfoAccess extension
        /*
        Collection c =
            currCertImpl.getAuthorityInformationAccess(); 
            */
        Collection c = getAuthorityInformationAccess(currCertImpl);
        Iterator it = c.iterator();
        while (it.hasNext()) {
            AccessDescription ad = (AccessDescription)it.next();
            if (dump) {
                System.out.println ("ad is " + ad);
                System.out.println ("AccessDescription OID is " +
                    AccessDescription.Ad_OCSP_Id);
            }
            if (ad.getAccessMethod().equals(
                        (Object)AccessDescription.Ad_OCSP_Id)) {
                GeneralName gn = ad.getAccessLocation();
                if (gn.getType() == GeneralNameInterface.NAME_URI) {
                    try {
                        URIName uri = (URIName) gn.getName();
                        return (new URL(uri.getName()));
                    } catch (java.net.MalformedURLException e) {
                        throw new CertPathValidatorException(e);
                    }
                }
            }
        }
        return null; 
    }

    /*
     * Retrieves the values of the OCSP security properties.
     */
    private static String[] getOCSPProperties() {
        final String[] properties = new String[4];

        AccessController.doPrivileged(
            new PrivilegedAction() {
                public Object run() {
                    properties[0] = Security.getProperty(OCSP_URL_PROP);
                    properties[1] =
                        Security.getProperty(OCSP_CERT_SUBJECT_PROP);
                    properties[2] =
                        Security.getProperty(OCSP_CERT_ISSUER_PROP);
                    properties[3] =
                        Security.getProperty(OCSP_CERT_NUMBER_PROP);
                    return null;
                }
            });

        return properties;
    }

    /*
     * Removes any non-hexadecimal characters from a string.
     */
    private static String stripOutSeparators(String value) {
        char[] chars = value.toCharArray();
        StringBuffer hexNumber = new StringBuffer();
        for (int i = 0; i < chars.length; i++) {
            if (HEX_DIGITS.indexOf(chars[i]) != -1) {
                hexNumber.append(chars[i]);
            }
        }
        return hexNumber.toString();
    }

    private static Collection getAuthorityInformationAccess(
        X509Certificate cert) throws CertificateParsingException {

        try {
            byte[] extValue = cert.getExtensionValue(AUTH_INFO_ACCESS_OID);
            if (extValue == null) {
               return  Collections.EMPTY_SET;
            }
            HexDumpEncoder enc = new HexDumpEncoder();
            DerInputStream der = new DerInputStream(extValue);        
            DerInputStream  subDer = new DerInputStream(der.getOctetString()); 
            DerValue[] derVal = subDer.getSequence(5);
            Set accessDesc = new HashSet(derVal.length);
            for (int i=0; i < derVal.length; i++) {
                accessDesc.add (new AccessDescription(derVal[i]));
            }
            return Collections.unmodifiableSet(accessDesc);
        } catch (IOException ioe) { 
            // should not occur
            return Collections.EMPTY_SET;
        }
    }
}

/**
 * Indicates that the identified certificate has been revoked.
 */
final class CertificateRevokedException extends 
    CertPathValidatorException {

    CertificateRevokedException(String msg, CertPath certPath, int index) {
        super(msg, null, certPath, index);
    }
}
