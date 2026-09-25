package sv.edu.udb.ucacfcconnect.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "UCA-CFC Connect API",
                version = "1.0.0",
                description = "API REST para la gestión académica y administrativa del Centro de Formación Continua de la UCA.",
                contact = @Contact(name = "Equipo UCA-CFC Connect"),
                license = @License(name = "Uso académico")
        )
)
public class OpenApiConfig {
}
