package li.sebastianmueller.planner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class PlannerBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlannerBackendApplication.class, args);
    }
}
