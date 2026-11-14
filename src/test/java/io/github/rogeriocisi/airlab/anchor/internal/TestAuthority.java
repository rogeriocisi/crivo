package io.github.rogeriocisi.airlab.anchor.internal;

import io.github.rogeriocisi.airlab.support.Hashes;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.cmp.PKIFailureInfo;
import org.bouncycastle.asn1.cmp.PKIStatus;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampResponseGenerator;
import org.bouncycastle.tsp.TimeStampTokenGenerator;

/**
 * A time stamp authority of the tests' own: a root and a signing certificate made on the spot, and
 * real RFC 3161 answers signed with them. The tests check the same signatures, nonces and imprints a
 * real authority's answer goes through, without the network.
 */
final class TestAuthority {

    private static final AtomicLong SERIALS = new AtomicLong(1000);

    private final KeyPair tsaKeys;
    private final X509Certificate tsa;
    private final X509Certificate ca;
    private final byte[] tsaPem;
    private final byte[] caPem;

    private TestAuthority(KeyPair tsaKeys, X509Certificate tsa, X509Certificate ca) {
        this.tsaKeys = tsaKeys;
        this.tsa = tsa;
        this.ca = ca;
        this.tsaPem = pem(tsa);
        this.caPem = pem(ca);
    }

    /** A root and a signing certificate with the critical time stamping usage. */
    static TestAuthority create(String name) {
        return create(name, true);
    }

    static TestAuthority create(String name, boolean timeStampingUsage) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair caKeys = generator.generateKeyPair();
            KeyPair tsaKeys = generator.generateKeyPair();
            Date from = Date.from(Instant.parse("2020-01-01T00:00:00Z"));
            Date to = Date.from(Instant.parse("2060-01-01T00:00:00Z"));
            JcaX509CertificateConverter converter = new JcaX509CertificateConverter();

            X500Name caName = new X500Name("CN=" + name + " Root");
            X509v3CertificateBuilder caBuilder = new JcaX509v3CertificateBuilder(
                    caName, BigInteger.ONE, from, to, caName, caKeys.getPublic());
            caBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
            X509Certificate ca = converter.getCertificate(caBuilder.build(
                    new JcaContentSignerBuilder("SHA256withECDSA").build(caKeys.getPrivate())));

            X509v3CertificateBuilder tsaBuilder = new JcaX509v3CertificateBuilder(
                    caName, BigInteger.TWO, from, to, new X500Name("CN=" + name + " TSA"), tsaKeys.getPublic());
            tsaBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
            if (timeStampingUsage) {
                tsaBuilder.addExtension(Extension.extendedKeyUsage, true,
                        new ExtendedKeyUsage(KeyPurposeId.id_kp_timeStamping));
            }
            X509Certificate tsa = converter.getCertificate(tsaBuilder.build(
                    new JcaContentSignerBuilder("SHA256withECDSA").build(caKeys.getPrivate())));
            return new TestAuthority(tsaKeys, tsa, ca);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** A granted answer to the request, attested at {@code genTime}. */
    byte[] grant(byte[] requestDer, Instant genTime) {
        try {
            TimeStampTokenGenerator tokens = new TimeStampTokenGenerator(
                    new JcaSimpleSignerInfoGeneratorBuilder().build("SHA256withECDSA", tsaKeys.getPrivate(), tsa),
                    new JcaDigestCalculatorProviderBuilder().build()
                            .get(new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256)),
                    new ASN1ObjectIdentifier("1.3.6.1.4.1.99999.1"));
            tokens.addCertificates(new JcaCertStore(List.of(tsa)));
            TimeStampResponseGenerator responses = new TimeStampResponseGenerator(tokens, TSPAlgorithms.ALLOWED);
            return responses.generate(new TimeStampRequest(requestDer),
                    BigInteger.valueOf(SERIALS.incrementAndGet()), Date.from(genTime)).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** A refusal, as an authority sends for a request it will not stamp. */
    byte[] reject() {
        try {
            TimeStampTokenGenerator tokens = new TimeStampTokenGenerator(
                    new JcaSimpleSignerInfoGeneratorBuilder().build("SHA256withECDSA", tsaKeys.getPrivate(), tsa),
                    new JcaDigestCalculatorProviderBuilder().build()
                            .get(new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256)),
                    new ASN1ObjectIdentifier("1.3.6.1.4.1.99999.1"));
            return new TimeStampResponseGenerator(tokens, TSPAlgorithms.ALLOWED)
                    .generateFailResponse(PKIStatus.REJECTION, PKIFailureInfo.badAlg, "recusado no teste")
                    .getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** This authority's pins, as {@link TrustAnchors} loads them. */
    TrustAnchors trust() {
        return TrustAnchors.load(new org.springframework.core.io.ByteArrayResource(tsaPem), tsaSha256(),
                new org.springframework.core.io.ByteArrayResource(caPem), caSha256());
    }

    /** Writes {@code tsa.crt} and {@code cacert.pem} into a directory, for Spring properties. */
    Path write(Path directory) {
        try {
            Files.createDirectories(directory);
            Files.write(directory.resolve("tsa.crt"), tsaPem);
            Files.write(directory.resolve("cacert.pem"), caPem);
            return directory;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    String tsaSha256() {
        return Hashes.toHex(Hashes.sha256(tsaPem));
    }

    String caSha256() {
        return Hashes.toHex(Hashes.sha256(caPem));
    }

    byte[] tsaPem() {
        return tsaPem.clone();
    }

    byte[] caPem() {
        return caPem.clone();
    }

    static Instant at(String iso) {
        return Instant.parse(iso);
    }

    static Duration minutes(long n) {
        return Duration.ofMinutes(n);
    }

    private static byte[] pem(X509Certificate certificate) {
        StringWriter out = new StringWriter();
        try (JcaPEMWriter writer = new JcaPEMWriter(out)) {
            writer.writeObject(certificate);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return out.toString().getBytes(StandardCharsets.US_ASCII);
    }
}
