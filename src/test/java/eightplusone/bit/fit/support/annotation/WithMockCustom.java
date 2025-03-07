package eightplusone.bit.fit.support.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.test.context.support.WithSecurityContext;

import eightplusone.bit.fit.support.security.MockCustomUserSecurityContextFactory;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@WithSecurityContext(factory = MockCustomUserSecurityContextFactory.class)
public @interface WithMockCustom {
	long id() default 1L;

	String role();
}
