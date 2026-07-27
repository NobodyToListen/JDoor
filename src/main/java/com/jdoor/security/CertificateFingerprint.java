package com.jdoor.security;

import java.security.MessageDigest;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record CertificateFingerprint(String value) {
    private static final Pattern SHA_256_HEX = Pattern.compile("[0-9a-f]{64}");

    public CertificateFingerprint {
        value = Objects.requireNonNull(value, "value").toLowerCase(Locale.ROOT);
        if (!SHA_256_HEX.matcher(value).matches()) {
            throw new IllegalArgumentException("Fingerprint must be a 64-character SHA-256 hex value");
        }
    }

    public static CertificateFingerprint from(X509Certificate certificate) throws CertificateEncodingException {
        Objects.requireNonNull(certificate, "certificate");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
            return new CertificateFingerprint(HexFormat.of().formatHex(digest));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    public boolean matches(X509Certificate certificate) throws CertificateEncodingException {
        byte[] expected = HexFormat.of().parseHex(value);
        byte[] actual = HexFormat.of().parseHex(from(certificate).value);
        return MessageDigest.isEqual(expected, actual);
    }

    public String grouped() {
        return value.substring(0, 4)
                + "-"
                + value.substring(4, 8)
                + "-"
                + value.substring(8, 12)
                + "-"
                + value.substring(12, 16);
    }
}
