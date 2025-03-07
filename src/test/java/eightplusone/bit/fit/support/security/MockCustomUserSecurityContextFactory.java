package eightplusone.bit.fit.support.security;

import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import eightplusone.bit.fit.domain.auth.dto.CustomUserDetails;
import eightplusone.bit.fit.domain.user.entity.User;
import eightplusone.bit.fit.support.annotation.WithMockCustom;
import eightplusone.bit.fit.support.fixture.UserFixture;

public class MockCustomUserSecurityContextFactory implements WithSecurityContextFactory<WithMockCustom> {

	@Override
	public SecurityContext createSecurityContext(WithMockCustom customUser) {
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		User mockUser = UserFixture.USER_FIXTURE_1.createUser();

		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
			new CustomUserDetails(mockUser), null, List.of(new SimpleGrantedAuthority(customUser.role())));
		context.setAuthentication(authentication);

		return context;
	}
}
