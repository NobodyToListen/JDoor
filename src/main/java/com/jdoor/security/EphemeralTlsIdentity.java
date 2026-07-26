package com.jdoor.security;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public record EphemeralTlsIdentity(
        SSLContext serverContext, X509Certificate certificate, CertificateFingerprint fingerprint, Instant expiresAt) {
    private static final Duration VALIDITY = Duration.ofHours(24);

    public static EphemeralTlsIdentity create(Clock clock, SecureRandom random) {
        try {
            Instant now = clock.instant();
            Instant expiresAt = now.plus(VALIDITY);

            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"), random);
            KeyPair keyPair = generator.generateKeyPair();

            X500Name subject = new X500Name("CN=JDoor Assist ephemeral session");
            BigInteger serial = new BigInteger(128, random).setBit(127);
            X509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(
                    subject,
                    serial,
                    Date.from(now.minus(Duration.ofMinutes(2))),
                    Date.from(expiresAt),
                    subject,
                    keyPair.getPublic());
            JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();
            certificateBuilder.addExtension(
                    Extension.subjectKeyIdentifier,
                    false,
                    extensionUtils.createSubjectKeyIdentifier(keyPair.getPublic()));
            certificateBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
            certificateBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
            certificateBuilder.addExtension(
                    Extension.extendedKeyUsage, false, new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));

            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA")
                    .setSecureRandom(random)
                    .build(keyPair.getPrivate());
            X509Certificate certificate =
                    new JcaX509CertificateConverter().getCertificate(certificateBuilder.build(signer));
            certificate.checkValidity(Date.from(now));
            certificate.verify(keyPair.getPublic());

            char[] password = randomPassword(random);
            try {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(null, password);
                keyStore.setKeyEntry(
                        "jdoor-session", keyPair.getPrivate(), password, new java.security.cert.Certificate[] {
                            certificate
                        });

                KeyManagerFactory keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagers.init(keyStore, password);

                SSLContext context = SSLContext.getInstance("TLS");
                context.init(keyManagers.getKeyManagers(), null, random);
                return new EphemeralTlsIdentity(
                        context, certificate, CertificateFingerprint.from(certificate), expiresAt);
            } finally {
                Arrays.fill(password, '\0');
            }
        } catch (Exception failure) {
            throw new IllegalStateException("Could not create the ephemeral TLS identity", failure);
        }
    }

    private static char[] randomPassword(SecureRandom random) {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        char[] password = java.util.Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes)
                .toCharArray();
        Arrays.fill(bytes, (byte) 0);
        return password;
    }
}
