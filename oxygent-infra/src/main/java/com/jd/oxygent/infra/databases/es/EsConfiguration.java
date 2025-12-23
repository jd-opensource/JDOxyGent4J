package com.jd.oxygent.infra.databases.es;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Elasticsearch configuration.
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
@Slf4j
@Data
@EnableConfigurationProperties(EsConfiguration.EsProperties.class)
@ConditionalOnProperty(name = "oxygent.database.es", havingValue = "es")
public class EsConfiguration {

    @Autowired
    private EsProperties esProperties;

    private RestHighLevelClient client;

    @PostConstruct
    public void init() {
        // Address parsing
        List<HttpHost> hostLists = new ArrayList<>();
        String[] hostList = esProperties.getClusterNodes().split(";");
        for (String addr : hostList) {
            if (StringUtils.isEmpty(addr)) {
                continue;
            }
            String[] addrDetail = addr.split(":");
            String host = addrDetail[0];
            String port = addrDetail[1];
            hostLists.add(new HttpHost(host, Integer.parseInt(port), esProperties.getSchema()));
        }
        HttpHost[] httpHost = hostLists.toArray(new HttpHost[]{});
        // Build connection object
        RestClientBuilder builder = RestClient.builder(httpHost);
        // Asynchronous connection timeout configuration
        builder.setRequestConfigCallback(requestConfigBuilder -> {
            requestConfigBuilder.setConnectTimeout(esProperties.getConnectTimeout());
            requestConfigBuilder.setSocketTimeout(esProperties.getSocketTimeout());
            requestConfigBuilder.setConnectionRequestTimeout(esProperties.getConnectionRequestTimeout());
            return requestConfigBuilder;
        });
        // Asynchronous connection pool configuration
        log.debug(esProperties.getSecurityUser() + "_" + esProperties.getSecurityPassword());
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(esProperties.getSecurityUser(), esProperties.getSecurityPassword()));

        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            httpClientBuilder.setMaxConnTotal(esProperties.getMaxConnectNum());
            httpClientBuilder.setMaxConnPerRoute(esProperties.getMaxConnectPerRoute());
            //httpClientBuilder.disableAuthCaching();
            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            return httpClientBuilder;
        });
        client = new RestHighLevelClient(builder);
    }

    @PreDestroy
    public void destory() {
        try {
            client.close();
        } catch (IOException e) {
            log.error("RestHighLevelClient close failed", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * ES configuration properties class
     */
    @ConfigurationProperties(prefix = "es")
    @Data
    public static class EsProperties {
        private String clusterNodes;
        private String clusterName;
        private String schema;
        private Boolean clientTransportSniff = false;
        private String securityUser;
        private String securityPassword;
        private Integer connectTimeout;
        private Integer socketTimeout;
        private Integer connectionRequestTimeout;
        private Integer maxConnectNum;
        private Integer maxConnectPerRoute;
    }

}

