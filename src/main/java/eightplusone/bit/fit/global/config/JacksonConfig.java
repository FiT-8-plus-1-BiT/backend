package eightplusone.bit.fit.global.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
public class JacksonConfig {

	private static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATETIME_FORMAT);

	@Bean
	public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
		return new Jackson2ObjectMapperBuilder()
			.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(formatter))
			.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
	}
}
