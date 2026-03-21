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
        // 1. Создаем временную папку в файловой системе ОС (внутри Docker-контейнера)
        Path tempDir = Files.createTempDirectory("orekit-data-temp");
        tempDir.toFile().deleteOnExit();

        // 2. Ищем все файлы orekit-data внутри JAR-архива с помощью Spring
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:orekit-data/**");

        for (Resource resource : resources) {
            // isReadable() отсеет папки, оставив только сами файлы с данными
            if (resource.isReadable()) {
                String urlStr = resource.getURL().toString();

                // Вырезаем относительный путь начиная с "orekit-data"
                int index = urlStr.lastIndexOf("orekit-data");
                if (index != -1) {
                    String relativePath = urlStr.substring(index);
                    Path destPath = tempDir.resolve(relativePath);

                    // Создаем подпапки, если они есть
                    Files.createDirectories(destPath.getParent());

                    // 3. Копируем файл из архива во временную папку на диск
                    try (InputStream is = resource.getInputStream()) {
                        Files.copy(is, destPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    destPath.toFile().deleteOnExit();
                }
            }
        }

        // 4. Натравливаем Orekit на нашу распакованную временную папку
        File orekitDataFile = tempDir.resolve("orekit-data").toFile();
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.addProvider(new DirectoryCrawler(orekitDataFile));

        return DataContext.getDefault();
    }
}