package com.leonsoft;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;

@SpringBootApplication
@EnableAutoConfiguration(exclude = {ErrorMvcAutoConfiguration.class})

public class KarpathosApplication implements CommandLineRunner {

    public static void main(String[] args) {

        SpringApplication.run(KarpathosApplication.class, args);
    }

    @Override
    public void run(String... args) {
//        JFrame frame = new JFrame("Spring Boot Swing App");
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setSize(300, 300);
//        JPanel panel = new JPanel(new BorderLayout());
//        JTextField text = new JTextField("Spring Boot can be used with Swing apps");
//        panel.add(text, BorderLayout.CENTER);
//        frame.setContentPane(panel);
//        frame.setVisible(true);
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
