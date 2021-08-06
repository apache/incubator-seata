/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.core.rpc.netty.tls;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

/**
 * The enum server certificate type
 */
public enum ServerCertificateType {
    /**
     * The certificate type JKS.
     */
    JKS("JKS") {
        @Override
        public SslContext getSslContext(String tlsVersion, String certificatePath,
                                        String certificatePassword, String keyFilePath) {
            return doGetSslContextWithKeyStore(tlsVersion, certificatePath, certificatePassword);
        }
    },

    /**
     * The certificate type PKCS12.
     */
    PKCS12("PKCS12") {
        @Override
        public SslContext getSslContext(String tlsVersion, String certificatePath,
                                        String certificatePassword, String keyFilePath) {
            return doGetSslContextWithKeyStore(tlsVersion, certificatePath, certificatePassword);
        }
    },

    /**
     * The certificate type PEM.
     */
    PEM("PEM") {
        @Override
        public SslContext getSslContext(String tlsVersion, String certificatePath,
                                        String certificatePassword, String keyFilePath) {
            SslContext sslContext = null;
            File certificateFile = new File(certificatePath);
            File keyFile = new File(keyFilePath);
            try {
                SslContextBuilder sslContextBuilder = SslContextBuilder.forServer(certificateFile, keyFile)
                        .clientAuth(ClientAuth.NONE);
                if (tlsVersion != null) {
                    sslContextBuilder.protocols(tlsVersion);
                }
                sslContext = sslContextBuilder.build();
            } catch (SSLException e) {
                e.printStackTrace();
            }
            return sslContext;
        }
    };

    /**
     * The certificate type.
     */
    private String type;

    ServerCertificateType(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }

    /**
     * Get the SslContext with the configuration.
     *
     * @param tlsVersion the TLS protocol version to enable.
     * @param certificatePath the certificate path.
     * @param certificatePassword the certificate password,
     *                    or {@code null} for certificate type PEM.
     * @param keyFilePath the PKCS#8 private key file path,
     *                    or {@code null} for certificate type JKS and PKCS12.
     * @return the SslContext.
     */
    public abstract SslContext getSslContext(String tlsVersion, String certificatePath,
                                             String certificatePassword, String keyFilePath);

    protected SslContext doGetSslContextWithKeyStore(String tlsVersion, String certificatePath,
                                                     String certificatePassword) {
        SslContext sslContext = null;
        InputStream certificateFile = null;
        try {
            certificateFile = new FileInputStream(certificatePath);
            KeyStore keyStore = KeyStore.getInstance(this.getType());
            keyStore.load(certificateFile, certificatePassword.toCharArray());
            KeyManagerFactory keyManagerFactory = KeyManagerFactory
                .getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, certificatePassword.toCharArray());

            SslContextBuilder sslContextBuilder = SslContextBuilder.forServer(keyManagerFactory)
                .clientAuth(ClientAuth.NONE);
            if (tlsVersion != null) {
                sslContextBuilder.protocols(tlsVersion);
            }
            sslContext = sslContextBuilder.build();
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException |
                UnrecoverableKeyException | CertificateException e) {
            e.printStackTrace();
        } finally {
            if (certificateFile != null) {
                try {
                    certificateFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sslContext;
    }
}
