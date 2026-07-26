package com.jdoor.security;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public final class PinnedTlsContext {
    private PinnedTlsContext() {}

    public static SSLContext create(CertificateFingerprint expectedFingerprint) {
        try {
            TrustManager[] trustManagers = {new FingerprintTrustManager(expectedFingerprint)};
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trustManagers, new SecureRandom());
            return context;
        } catch (Exception failure) {
            throw new IllegalStateException("Could not create the pinned TLS context", failure);
        }
    }

    private static final class FingerprintTrustManager implements X509TrustManager {
        private final CertificateFingerprint expectedFingerprint;

        private FingerprintTrustManager(CertificateFingerprint expectedFingerprint) {
            this.expectedFingerprint = expectedFingerprint;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            throw new CertificateException("Client certificates are not accepted");
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            if (chain == null || chain.length == 0) {
                throw new CertificateException("The host did not present a certificate");
            }
            chain[0].checkValidity();
            if (!expectedFingerprint.matches(chain[0])) {
                throw new CertificateException("The host certificate does not match the pairing link");
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
