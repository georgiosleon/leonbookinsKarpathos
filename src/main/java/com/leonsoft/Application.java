package com.leonsoft;

import com.leonsoft.cli.DatabaseCli;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
// After (Spring Boot 4.x)
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration;

@SpringBootApplication(exclude = {ErrorMvcAutoConfiguration.class})

public class Application {

    public static void main(String[] args) {
        if (args.length > 0 && "db".equals(args[0])) {
            System.exit(DatabaseCli.run(java.util.Arrays.copyOfRange(args, 1, args.length)));
        }
        SpringApplication.run(Application.class, args);
    }

}
