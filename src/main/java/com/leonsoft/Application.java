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


//@SpringBootApplication
//public class CalApplication implements CommandLineRunner {
//
//	public static void main(String[] args) {
//		new SpringApplicationBuilder(CalApplication.class).headless(false).run(args);
//	}
//
//	@Override
//	public void run(String... args) {
//		JFrame frame = new JFrame("Spring Boot Swing App");
//		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		frame.setSize(300,300);
//		JPanel panel = new JPanel(new BorderLayout());
//		JTextField text = new JTextField("Spring Boot can be used with Swing apps");
//		panel.add(text, BorderLayout.CENTER);
//		frame.setContentPane(panel);
//		frame.setVisible(true);
//	}
//}