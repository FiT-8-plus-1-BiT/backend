package eightplusone.bit.fit.global.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerConfig {

	@Value("${server.host}")
	private String serverHost;

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI().addSecurityItem(new SecurityRequirement()
				.addList("Access Token"))
			.components(new Components()
				.addSecuritySchemes("Access Token", createApiKeyScheme()))
			.info(new Info()
				.title("FIT")
				.description("**`FIT API`**"))
			.servers(servers());
	}

	private List<Server> servers() {
		List<Server> servers = new ArrayList<>();
		servers.add(new Server().url("http://localhost:8080").description("Local URL"));
		servers.add(new Server().url(serverHost).description("Dev Server URL"));
		return servers;
	}

	private SecurityScheme createApiKeyScheme() {
		return new SecurityScheme()
			.type(SecurityScheme.Type.HTTP)
			.in(SecurityScheme.In.HEADER)
			.bearerFormat("JWT")
			.scheme("bearer");
	}
}
