package ml.pevgen.s5reactivetest1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class S5ReactiveTest1Application {

    public static void main(String[] args) {
        SpringApplication.run(S5ReactiveTest1Application.class, args);
    }



}
