package it.pagopa.pn.user.attributes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.blockhound.BlockHound;

@SpringBootApplication
public class PnUserAttributesApplication {

	public static void main(String[] args) {
		SpringApplication.run(PnUserAttributesApplication.class, args);
	}

	public PnUserAttributesApplication(){
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
