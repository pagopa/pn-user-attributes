package it.pagopa.pn.user.attributes;

import it.pagopa.pn.commons.configs.listeners.TaskIdApplicationListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class PnUserAttributesApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(PnUserAttributesApplication.class);
		app.addListeners(new TaskIdApplicationListener());
		app.run(args);
	}

	@RestController
	@RequestMapping("/")
	public static class RootController {

		@GetMapping("/")
		public String home() {
			return "";
		}
	}
}
