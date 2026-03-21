package org.example.orbit.Config;

import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Configuration
public class OrekitConfig {

    @Bean
    public RestClient celestrakRestClient() {
        return RestClient.builder()
                .baseUrl("https://celestrak.org")
                .build();
    }

    @Bean
    public DataContext orekitInit() throws Exception {
        URL url = getClass().getClassLoader().getResource("orekit-data");
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();

        // Если мы запускаем код локально из IDE
        if (url != null && url.getProtocol().equals("file")) {
            File orekitData = new File(url.toURI());
            manager.addProvider(new DirectoryCrawler(orekitData));
            return DataContext.getDefault();
        }

        // Если мы работаем внутри Docker (запуск из JAR-архива) пока не тестил
        Path tempDir = Files.createTempDirectory("orekit-data-temp");
        tempDir.toFile().deleteOnExit();

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:orekit-data/**");

        for (Resource resource : resources) {
            if (resource.isReadable()) {
                String urlStr = resource.getURL().toString();
                int index = urlStr.lastIndexOf("orekit-data");
                if (index != -1) {
                    String relativePath = urlStr.substring(index);
                    Path destPath = tempDir.resolve(relativePath);
                    Files.createDirectories(destPath.getParent());

                    try (InputStream is = resource.getInputStream()) {
                        Files.copy(is, destPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    destPath.toFile().deleteOnExit();
                }
            }
        }

        File orekitDataFile = tempDir.resolve("orekit-data").toFile();
        manager.addProvider(new DirectoryCrawler(orekitDataFile));

        return DataContext.getDefault();
    }
}