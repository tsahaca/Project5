package com.test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class AppConfig {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Value("${client.default-uri}")
    private String defaultUri;

    @Value("${client.user.name}")
    private String userName;

    @Value("${client.user.password}")
    private String userPassword;

    @Bean
    public Jaxb2Marshaller marshaller () {
        final Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
        jaxb2Marshaller.setPackagesToScan("com.test.eagleapi.domain");
        // jaxb2Marshaller.setContextPath("com.test.eagleapi.domain");

        jaxb2Marshaller.setPackagesToScan("com.test.eagleapi.domain");
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("jaxb.formatted.output", true);
        jaxb2Marshaller.setMarshallerProperties(map);

        return jaxb2Marshaller;
    }

    @Bean
    public SOAPConnector soapConnector (Jaxb2Marshaller marshaller) {
        SOAPConnector client = new SOAPConnector();
        client.setWebServiceTemplate(webServiceTemplate());
        return client;
    }

    //////

    @Bean
    public WebServiceTemplate webServiceTemplate () {
        WebServiceTemplate webServiceTemplate = new WebServiceTemplate();
        webServiceTemplate.setMarshaller(marshaller());
        webServiceTemplate.setUnmarshaller(marshaller());
        webServiceTemplate.setDefaultUri(defaultUri);

        // set a HttpComponentsMessageSender which provides support for basic
        // authentication

        webServiceTemplate.setMessageSender(httpComponentsMessageSender());

        return webServiceTemplate;
    }

    @Bean
    public HttpComponentsMessageSender httpComponentsMessageSender () {

        final CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY, usernamePasswordCredentials());

        HttpClient httpClient = HttpClients.custom()
                .addInterceptorFirst(
                        new HttpComponentsMessageSender.RemoveSoapHeadersInterceptor())
                .addInterceptorFirst(new HttpResponseInterceptor() {
                    @Override
                    public void process (HttpResponse httpResponse,
                            HttpContext httpContext)
                            throws HttpException, IOException {
                        boolean htmlResponse = Arrays
                                .stream(httpResponse.getAllHeaders())
                                .anyMatch(header -> header.getName()
                                        .equalsIgnoreCase(
                                                "Content-Type")
                                        && header.getValue()
                                                .contains("text/plain"));
                        if (htmlResponse) {
                            httpResponse.removeHeaders("Content-Type");
                        }
                    }
                })
                // .setDefaultRequestConfig(requestConfig)
                .setDefaultCredentialsProvider(provider).build();
        HttpComponentsMessageSender httpComponentsMessageSender = new HttpComponentsMessageSender(
                httpClient);
        return httpComponentsMessageSender;
    }

    @Bean
    public UsernamePasswordCredentials usernamePasswordCredentials () {
        // pass the user name and password to be used
        return new UsernamePasswordCredentials(userName, userPassword);
    }

}
