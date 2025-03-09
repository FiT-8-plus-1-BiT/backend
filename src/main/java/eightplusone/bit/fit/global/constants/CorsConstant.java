package eightplusone.bit.fit.global.constants;

import java.util.List;

public class CorsConstant {

	public static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
	public static final List<String> ALLOWED_HEADERS = List.of("X-Requested-With", "Content-Type", "Accept");
	public static final boolean ALLOWED_CREDENTIALS = true;
	public static final long MAX_AGE = 3600L;
}
