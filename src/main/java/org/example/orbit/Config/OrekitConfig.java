package org.example.orbit.Config;

import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;


@Configuration
public class OrekitConfig {

    @Bean
    public RestClient celestrakRestClient() {
        return RestClient.builder()
                .baseUrl("https://celestrak.org")
                .build();
    }

    @Bean
    public DataContext orekitInit() throws URISyntaxException {
        URL url = getClass().getClassLoader().getResource("orekit-data");
        File orekitData = new File(url.toURI());
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.addProvider(new DirectoryCrawler(orekitData));
        return DataContext.getDefault();
    }
}
