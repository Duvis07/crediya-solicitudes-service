package co.com.crediya.solicitudes;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;
import java.util.stream.Collectors;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MainApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MainApplication.class);
        app.addInitializers(applicationContext -> {
            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            Map<String, Object> dotenvMap = dotenv.entries()
                .stream()
                .collect(Collectors.toMap(
                        DotenvEntry::getKey,
                        DotenvEntry::getValue
                ));
            environment.getPropertySources().addLast(
                new MapPropertySource("dotenv", dotenvMap)
            );
        });
        app.run(args);
    }
}
