package com.basebox.ridelite.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration.
 * 
 * WHAT IS SWAGGER?
 * Auto-generates interactive API documentation.
 * Access at: http://localhost:8080/swagger-ui.html
 * 
 * BENEFITS:
 * - See all endpoints
 * - Test APIs directly in browser
 * - View request/response examples
 * - Generate client code
 */
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI rideliteOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("RideLite API")
                .description("Ride-sharing application backend API")
                .version("v1.0")
                .contact(new Contact()
                    .name("Your Name")
                    .email("your.email@example.com")
                    .url("https://github.com/yourusername"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Development server"),
                new Server()
                    .url("https://api.ridelite.com")
                    .description("Production server")));
    }
}
