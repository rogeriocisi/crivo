package io.github.rogeriocisi.airlab.anchor.internal;

/**
 * Sends a DER {@code TimeStampReq} to the authority and returns the DER {@code TimeStampResp}. An
 * interface so the tests can answer with an authority of their own, without the network.
 */
interface TsaClient {

    byte[] exchange(byte[] request) throws AnchorRejected;
}
