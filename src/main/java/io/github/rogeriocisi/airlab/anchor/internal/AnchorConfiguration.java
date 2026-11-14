package io.github.rogeriocisi.airlab.anchor.internal;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the authority. The certificates are loaded and checked against their pins when the
 * application starts, so a wrong file fails at once rather than at the first stamp.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TimestampProperties.class)
class AnchorConfiguration {

    @Bean
    TrustAnchors trustAnchors(TimestampProperties properties) {
        return TrustAnchors.load(properties.tsaCertificate(), properties.tsaCertificateSha256(),
                properties.caCertificate(), properties.caCertificateSha256());
    }

    @Bean
    TsaClient tsaClient(TimestampProperties properties) {
        return new HttpTsaClient(properties.url(), properties.timeout());
    }
}
