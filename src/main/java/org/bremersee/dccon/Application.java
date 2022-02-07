package org.bremersee.dccon;

import org.bremersee.xml.JaxbContextBuilderAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The application.
 */
@SpringBootApplication(exclude = {
    JaxbContextBuilderAutoConfiguration.class
})
public class Application {

  /**
   * The entry point of the application.
   *
   * @param args the input arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

}
