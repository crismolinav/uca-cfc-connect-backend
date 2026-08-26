package sv.edu.udb.ucacfcconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

// Excluimos la seguridad temporalmente para la Fase 2
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class UcaCfcConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(UcaCfcConnectApplication.class, args);
    }

}