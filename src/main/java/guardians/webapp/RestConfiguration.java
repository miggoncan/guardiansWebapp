package guardians.webapp;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class RestConfiguration {
	@Value("${api.uri}")
	private String restUri;
	@Value("${auth.rest.username}")
	private String username;
	@Value("${auth.rest.password}")
	private String password;
	@Value("${ssl.trust-store}")
	private Resource trustStore;
	@Value("${ssl.trust-store.password}")
	private String trustStorePassword;
	
	@Bean
	public RestTemplateBuilder restTemplateBuilder() {
		return new RestTemplateBuilder(restTemplate -> {
			// Configure SSL to accept self-signed certificates
			SSLContext sslContext = null;
			try {
				sslContext = SSLContexts.custom()
						.loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();
			} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
				log.error("Unexpected exception while creating SSLContext: " + e);
			}
			SSLConnectionSocketFactory socketFactory = 
					new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
			CloseableHttpClient httpClient = HttpClients.custom()
		        .setSSLSocketFactory(socketFactory)
		        .build();
		    HttpComponentsClientHttpRequestFactory requestFactory = 
		    		new HttpComponentsClientHttpRequestFactory();
		    requestFactory.setHttpClient(httpClient);
		    restTemplate.setRequestFactory(requestFactory);
		    // Configure basic authentication
		    restTemplate.getInterceptors().add(
		    		new BasicAuthenticationInterceptor(username, password));
		    // Configure the restTemplate to use the default HAL message converter
		    restTemplate.setMessageConverters(
		    		Traverson.getDefaultMessageConverters(MediaTypes.HAL_JSON));
		});
	}
	
	@Bean
	public Traverson traverson() {
		Traverson traverson = new Traverson(URI.create(restUri), MediaTypes.HAL_JSON);
		// Configure the RestTemplates used by the Traverson
		RestTemplate restTemplate = restTemplateBuilder().build();
		traverson.setRestOperations(restTemplate);
		return traverson;
	}
}
