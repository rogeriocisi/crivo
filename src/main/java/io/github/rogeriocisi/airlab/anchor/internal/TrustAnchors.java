package io.github.rogeriocisi.airlab.anchor.internal;

import io.github.rogeriocisi.airlab.support.Hashes;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;
import org.springframework.core.io.Resource;

/**
 * The two certificates a stamp is checked against: the authority's signing certificate and the root
 * that issued it, each pinned by the SHA-256 of the file as the authority publishes it.
 *
 * <p>Pinning the file, not trusting a system store, is deliberate: a stamp is only as good as the
 * certificate it is checked against, and the published hash is what an outside auditor compares too
 * (FreeTSA lists both values on its home page). A mismatch stops the start up.
 */
final class TrustAnchors {

    private static final String TIME_STAMPING_EKU = "1.3.6.1.5.5.7.3.8";

    private final X509Certificate tsa;
    private final X509Certificate ca;
    private final String tsaSha256;
    private final String caSha256;

    private TrustAnchors(X509Certificate tsa, X509Certificate ca, String tsaSha256, String caSha256) {
        this.tsa = tsa;
        this.ca = ca;
        this.tsaSha256 = tsaSha256;
        this.caSha256 = caSha256;
    }

    static TrustAnchors load(Resource tsaFile, String tsaPin, Resource caFile, String caPin) {
        byte[] tsaBytes = read(tsaFile);
        byte[] caBytes = read(caFile);
        String tsaHash = Hashes.toHex(Hashes.sha256(tsaBytes));
        String caHash = Hashes.toHex(Hashes.sha256(caBytes));
        if (!tsaHash.equals(normalise(tsaPin))) {
            throw new IllegalStateException("o certificado da autoridade de carimbo nao confere com o SHA-256 fixado: "
                    + tsaHash);
        }
        if (!caHash.equals(normalise(caPin))) {
            throw new IllegalStateException("o certificado raiz da autoridade de carimbo nao confere com o SHA-256 fixado: "
                    + caHash);
        }
        X509Certificate tsa = parse(tsaBytes);
        X509Certificate ca = parse(caBytes);
        try {
            ca.verify(ca.getPublicKey());
            tsa.verify(ca.getPublicKey());
            List<String> usages = tsa.getExtendedKeyUsage();
            java.util.Set<String> critical = tsa.getCriticalExtensionOIDs();
            if (usages == null || !usages.contains(TIME_STAMPING_EKU)
                    || critical == null || !critical.contains("2.5.29.37")) {
                throw new IllegalStateException("o certificado da autoridade nao e de carimbo do tempo (EKU critico)");
            }
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("o certificado da autoridade nao foi emitido pela raiz fixada", e);
        }
        return new TrustAnchors(tsa, ca, tsaHash, caHash);
    }

    X509Certificate tsa() {
        return tsa;
    }

    X509Certificate ca() {
        return ca;
    }

    String tsaSha256() {
        return tsaSha256;
    }

    String caSha256() {
        return caSha256;
    }

    private static byte[] read(Resource resource) {
        try (InputStream in = resource.getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("certificado de carimbo do tempo nao encontrado: " + resource.getDescription(), e);
        }
    }

    private static X509Certificate parse(byte[] bytes) {
        try {
            return (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(bytes));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("certificado de carimbo do tempo ilegivel", e);
        }
    }

    private static String normalise(String pin) {
        return pin == null ? "" : pin.strip().toLowerCase(Locale.ROOT);
    }
}
