package w4cash;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import com.openbravo.pos.forms.AppConfig;

@SpringBootApplication
@EnableAsync
public class W4cashApplication {

	private static final String[] args = new String[] {};
	public static final AppConfig APP_CONFIG = new AppConfig(args);

	public static void main(String... args) {
		APP_CONFIG.load();
		SpringApplication.run(W4cashApplication.class, args);
	}
}
