package kafkastarttutorials.martins.jean.kafkastarttutorials;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@RestController
	@RequestMapping("/kafka")
	class HelloController{

		private HelloProducer service;

		public HelloController(HelloProducer service){
			this.service = service;
		}

		@GetMapping("/hello/{name}")
		public String hello(@PathVariable("name") final String name){
			service.sendMessage(name);
			return "Ok";
		}
	}
}
