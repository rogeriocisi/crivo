package io.github.rogeriocisi.airlab.anchor.internal;

import io.github.rogeriocisi.airlab.anchor.AnchorOutcome.FailureReason;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.time.Instant;
import java.util.Arrays;
import org.bouncycastle.asn1.cmp.PKIStatus;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampToken;

/**
 * RFC 3161 request and checks (SPEC E8, v0.15).
 *
 * <p>The value stamped is the hash of the head event itself: 32 bytes, already a SHA-256, sent as the
 * {@code messageImprint} with algorithm SHA-256. That is exactly what {@code openssl ts -verify
 * -digest <hash>} checks, so an auditor needs no tool from this project.
 */
final class TimestampProtocol {

    private static final SecureRandom RANDOM = new SecureRandom();

    private TimestampProtocol() {
    }

    /** A request for the given SHA-256, with a fresh 64 bit nonce and the signer certificate asked for. */
    static TimeStampRequest request(byte[] sha256) {
        if (sha256 == null || sha256.length != 32) {
            throw new IllegalArgumentException("o valor carimbado e um SHA-256 de 32 bytes");
        }
        TimeStampRequestGenerator generator = new TimeStampRequestGenerator();
        generator.setCertReq(true);
        return generator.generate(TSPAlgorithms.SHA256, sha256, new BigInteger(64, RANDOM));
    }

    /**
     * Checks an answer to {@code request} before anything is stored: granted, same imprint, same nonce,
     * signed by the pinned authority certificate, which the pinned root issued.
     */
    static TokenInfo checkResponse(byte[] der, TimeStampRequest request, TrustAnchors trust) throws AnchorRejected {
        TimeStampResponse response = parse(der);
        if (response.getStatus() != PKIStatus.GRANTED && response.getStatus() != PKIStatus.GRANTED_WITH_MODS) {
            throw new AnchorRejected(FailureReason.REJECTED,
                    "a autoridade recusou o pedido (status " + response.getStatus() + ")");
        }
        try {
            // Imprint, nonce, and the certificate when it was asked for (RFC 3161, section 2.4.2).
            response.validate(request);
        } catch (TSPException e) {
            throw new AnchorRejected(FailureReason.INVALID_RESPONSE, "a resposta nao corresponde ao pedido: " + e.getMessage(), e);
        }
        return checkToken(response.getTimeStampToken(), request.getMessageImprintDigest(), trust);
    }

    /** Checks a stored answer again, as {@code airlab verify} does: no nonce to compare any more. */
    static TokenInfo checkStored(byte[] der, byte[] expectedSha256, TrustAnchors trust) throws AnchorRejected {
        TimeStampResponse response = parse(der);
        if (response.getTimeStampToken() == null) {
            throw new AnchorRejected(FailureReason.INVALID_RESPONSE, "a resposta guardada nao tem token");
        }
        return checkToken(response.getTimeStampToken(), expectedSha256, trust);
    }

    private static TokenInfo checkToken(TimeStampToken token, byte[] expectedSha256, TrustAnchors trust)
            throws AnchorRejected {
        if (token == null) {
            throw new AnchorRejected(FailureReason.INVALID_RESPONSE, "resposta concedida sem token");
        }
        var info = token.getTimeStampInfo();
        if (!TSPAlgorithms.SHA256.equals(info.getMessageImprintAlgOID())) {
            throw new AnchorRejected(FailureReason.INVALID_RESPONSE, "o token nao e sobre um SHA-256");
        }
        if (!Arrays.equals(info.getMessageImprintDigest(), expectedSha256)) {
            throw new AnchorRejected(FailureReason.INVALID_RESPONSE, "o token carimba outro valor");
        }
        try {
            if (!token.getSID().match(new X509CertificateHolder(trust.tsa().getEncoded()))) {
                throw new AnchorRejected(FailureReason.INVALID_RESPONSE,
                        "o token nao foi assinado pelo certificado fixado da autoridade");
            }
            // Signature, signer certificate valid at the attested time, critical time stamping EKU.
            token.validate(new JcaSimpleSignerInfoVerifierBuilder().build(trust.tsa()));
        } catch (TSPException | OperatorCreationException | CertificateEncodingException | IOException e) {
            throw new AnchorRejected(FailureReason.INVALID_RESPONSE, "assinatura do token nao confere: " + e.getMessage(), e);
        }
        return new TokenInfo(info.getGenTime().toInstant(), info.getSerialNumber().toString(16));
    }

    private static TimeStampResponse parse(byte[] der) throws AnchorRejected {
        if (der == null || der.length == 0) {
            throw new AnchorRejected(FailureReason.INVALID_RESPONSE, "resposta vazia");
        }
        try {
            return new TimeStampResponse(der);
        } catch (TSPException | IOException | RuntimeException e) {
            throw new AnchorRejected(FailureReason.INVALID_RESPONSE, "resposta ilegivel", e);
        }
    }

    /** What the stored stamp records from the token. */
    record TokenInfo(Instant attestedAt, String serialHex) {
    }
}
