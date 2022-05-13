package com.dongholab.kafka.config.webclient

import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.resolver.DefaultAddressResolverGroup
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Configuration
class WebClientConfig {
    companion object {
        private val defaultHttpClient = HttpClient.create()
            .compress(true)
            .resolver(DefaultAddressResolverGroup.INSTANCE)
            .secure {
                val sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build()
                it.sslContext(sslContext)
            }

        fun defaultWebClient(
            baseUrl: String?, httpClient: HttpClient = defaultHttpClient
        ): WebClient {
            val strategy = ExchangeStrategies.builder().codecs {
                it.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)
            }.build()

            val webClientBuilder = WebClient.builder()
                .clientConnector(ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategy)
            if (baseUrl != null) {
                webClientBuilder.baseUrl(baseUrl)
            }

            return webClientBuilder.build()
        }
    }

    @Bean
    @Primary
    fun webClient(): WebClient = defaultWebClient(null)
}