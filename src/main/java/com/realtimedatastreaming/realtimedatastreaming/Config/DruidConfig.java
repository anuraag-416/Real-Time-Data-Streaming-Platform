package com.datastream.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@Configuration
public class DruidConfig {

    @Value("${druid.coordinator.url}")
    private String coordinatorUrl;

    @Value("${druid.overlord.url}")
    private String overlordUrl;

    @Value("${druid.router.url}")
    private String routerUrl;

    @Value("${druid.datasource}")
    private String datasource;

    @Bean
    public RestTemplate druidRestTemplate() {
        HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(100)
                .setMaxConnPerRoute(20)
                .setValidateAfterInactivity(Timeout.ofSeconds(10))
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(5))
                .setResponseTimeout(Timeout.ofSeconds(30))
                .build();

        HttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(requestFactory);
    }

    @Bean
    public String druidIngestionSpec() {
        // This would typically load from a file or resource, but for simplicity,
        // we're returning a basic spec template as a String
        return """
        {
          "type": "kafka",
          "dataSchema": {
            "dataSource": "%s",
            "parser": {
              "type": "string",
              "parseSpec": {
                "format": "json",
                "timestampSpec": {
                  "column": "timestamp",
                  "format": "auto"
                },
                "dimensionsSpec": {
                  "dimensions": [
                    "id",
                    "eventType",
                    "source",
                    "processingStatus",
                    { "name": "processingTimeMs", "type": "long" },
                    { "name": "payload", "type": "json" }
                  ]
                }
              }
            },
            "metricsSpec": [
              { "type": "count", "name": "count" },
              { "type": "longSum", "name": "totalProcessingTime", "fieldName": "processingTimeMs" }
            ],
            "granularitySpec": {
              "type": "uniform",
              "segmentGranularity": "HOUR",
              "queryGranularity": "MINUTE",
              "rollup": true
            }
          },
          "tuningConfig": {
            "type": "kafka",
            "maxRowsPerSegment": 5000000,
            "maxBytesInMemory": 1073741824,
            "maxRowsInMemory": 100000,
            "maxPendingPersists": 0,
            "indexSpec": {
              "bitmap": { "type": "roaring" },
              "dimensionCompression": "lz4",
              "metricCompression": "lz4"
            }
          },
          "ioConfig": {
            "topic": "%s",
            "consumerProperties": {
              "bootstrap.servers": "%s"
            },
            "taskCount": 4,
            "replicas": 2,
            "taskDuration": "PT1H",
            "useEarliestOffset": true
          }
        }
        """.formatted(datasource, eventsProcessedTopic, bootstrapServers);
    }
}