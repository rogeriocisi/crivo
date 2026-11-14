package io.github.rogeriocisi.airlab.anchor.internal;

import io.github.rogeriocisi.airlab.anchor.AnchorOutcome.FailureReason;
import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * RFC 3161 over HTTP (section 3.4): POST of the request with {@code application/timestamp-query},
 * answer in {@code application/timestamp-reply}. Uses the JDK client and the system proxy settings,
 * so no extra dependency and the same behaviour on Windows and Linux.
 */
final class HttpTsaClient implements TsaClient {

    /** Far above a real answer (FreeTSA's is under 5 KB); refuses anything that is not a token. */
    private static final int MAX_RESPONSE_BYTES = 64 * 1024;

    private final URI url;
    private final Duration timeout;
    private final HttpClient http;

    HttpTsaClient(String url, Duration timeout) {
        this.url = URI.create(url);
        this.timeout = timeout;
        this.http = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .proxy(ProxySelector.getDefault())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public byte[] exchange(byte[] request) throws AnchorRejected {
        HttpRequest post = HttpRequest.newBuilder(url)
                .timeout(timeout)
                .header("Content-Type", "application/timestamp-query")
                .header("Accept", "application/timestamp-reply")
                .POST(HttpRequest.BodyPublishers.ofByteArray(request))
                .build();
        try {
            HttpResponse<byte[]> response = http.send(post, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new AnchorRejected(FailureReason.UNREACHABLE, "a autoridade respondeu HTTP " + response.statusCode());
            }
            byte[] body = response.body();
            if (body.length > MAX_RESPONSE_BYTES) {
                throw new AnchorRejected(FailureReason.INVALID_RESPONSE, "resposta grande demais para um token");
            }
            return body;
        } catch (IOException e) {
            throw new AnchorRejected(FailureReason.UNREACHABLE, "autoridade inalcancavel: " + e.getClass().getSimpleName(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AnchorRejected(FailureReason.UNREACHABLE, "pedido interrompido", e);
        }
    }
}
