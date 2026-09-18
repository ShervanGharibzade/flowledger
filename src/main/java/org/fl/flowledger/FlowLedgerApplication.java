package org.fl.flowledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class FlowLedgerApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlowLedgerApplication.class, args);
	}

}
