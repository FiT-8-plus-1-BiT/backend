package eightplusone.bit.fit.domain.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import eightplusone.bit.fit.domain.auth.service.OAuth2UnlinkService;
import eightplusone.bit.fit.domain.auth.service.RedisTokenService;
import eightplusone.bit.fit.domain.interest.entity.Interest;
import eightplusone.bit.fit.domain.interest.entity.MyInterest;
import eightplusone.bit.fit.domain.interest.repostiroy.InterestRepository;
import eightplusone.bit.fit.domain.interest.repostiroy.MyInterestRepository;
import eightplusone.bit.fit.domain.user.dto.UserAccountResponseDto;
import eightplusone.bit.fit.domain.user.dto.UserProfileResponseDto;
import eightplusone.bit.fit.domain.user.dto.UserProfileUpdateRequestDto;
import eightplusone.bit.fit.domain.user.entity.User;
import eightplusone.bit.fit.domain.user.repository.UserRepository;
import eightplusone.bit.fit.support.fixture.InterestFixture;
import eightplusone.bit.fit.support.fixture.UserFixture;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	UserRepository userRepository;

	@Mock
	MyInterestRepository myInterestRepository;

	@Mock
	InterestRepository interestRepository;

	@Mock
	RedisTokenService redisTokenService;

	@Mock
	OAuth2UnlinkService oAuth2UnlinkService;

	@InjectMocks
	UserService userService;

	@Test
	@DisplayName("회원 탈퇴를 한다")
	void userDelete() {
		//given
		User user = UserFixture.USER_FIXTURE_1.createUser();
		String provider = user.getProvider();

		Mockito.when(userRepository.findLoginUserByEmail(user.getEmail())).thenReturn(user);
		Mockito.doNothing().when(oAuth2UnlinkService).unlink(provider);
		//when
		userService.delete(user.getEmail());

		//then
		verify(userRepository, Mockito.times(1)).delete(user);
	}

	@Test
	@DisplayName("회원 계정 정보를 조회한다")
	void userAccountGet() {
		//given
		String profileImageName = "imageName";
		String profileImageUrl = "imageUrl";
		User user = UserFixture.USER_FIXTURE_1.createUser();
		user.updateProfileImage(profileImageName, profileImageUrl);

		Mockito.when(userRepository.findLoginUserByEmail(user.getEmail())).thenReturn(user);

		//when
		UserAccountResponseDto accountInfo = userService.getAccountInfo(user.getEmail());

		//then
		assertAll(
			() -> assertThat(accountInfo.getName()).isEqualTo(user.getName()),
			() -> assertThat(accountInfo.getEmail()).isEqualTo(user.getEmail()),
			() -> assertThat(accountInfo.getImageUrl()).isEqualTo(user.getImageUrl())
		);
	}

	@Test
	@DisplayName("회원 개인 정보를 조회한다")
	void userProfileGet() {
		//given
		User user = UserFixture.USER_FIXTURE_1.createUser();

		List<MyInterest> myInterests = List.of(
			MyInterest.of(InterestFixture.INTEREST_FIXTURE_1.createInterest(), user),
			MyInterest.of(InterestFixture.INTEREST_FIXTURE_2.createInterest(), user),
			MyInterest.of(InterestFixture.INTEREST_FIXTURE_3.createInterest(), user)
		);

		Mockito.when(userRepository.findLoginUserByEmail(user.getEmail())).thenReturn(user);
		Mockito.when(myInterestRepository.findByUserIdWithInterests(user.getId())).thenReturn(myInterests);

		//when
		UserProfileResponseDto profileInfo = userService.getProfileInfo(user.getEmail());

		//then
		List<String> expectedInterests = myInterests.stream()
			.map(myInterest -> myInterest.getInterest().getName())
			.collect(Collectors.toList());

		assertAll(
			() -> assertThat(profileInfo.getJob()).isEqualTo(user.getJob()),
			() -> assertThat(profileInfo.getYears()).isEqualTo(user.getYears()),
			() -> assertThat(profileInfo.getInterests()).isEqualTo(expectedInterests)
		);
	}

	@Test
	@DisplayName("회원 개인 정보를 업데이트한다")
	void userProfileUpdate() {
		//given
		User user = UserFixture.USER_FIXTURE_1.createUser();
		Interest interest1 = InterestFixture.INTEREST_FIXTURE_1.createInterest();
		Interest interest2 = InterestFixture.INTEREST_FIXTURE_2.createInterest();
		Interest interest3 = InterestFixture.INTEREST_FIXTURE_3.createInterest();

		UserProfileUpdateRequestDto userProfileUpdateRequestDto = UserProfileUpdateRequestDto.of("디자이너", 10,
			List.of(interest1.getName(), interest2.getName(), interest3.getName()));

		List<MyInterest> myInterests = List.of(
			MyInterest.of(interest1, user),
			MyInterest.of(interest2, user),
			MyInterest.of(interest3, user));

		Mockito.when(userRepository.findLoginUserByEmail(user.getEmail())).thenReturn(user);
		Mockito.when(interestRepository.findByName(interest1.getName())).thenReturn(interest1);
		Mockito.when(interestRepository.findByName(interest2.getName())).thenReturn(interest2);
		Mockito.when(interestRepository.findByName(interest3.getName())).thenReturn(interest3);
		Mockito.when(myInterestRepository.findByUserIdWithInterests(user.getId())).thenReturn(myInterests);

		//when
		userService.updateProfileInfo(user.getEmail(), userProfileUpdateRequestDto);

		//then
		assertAll(
			() -> assertThat(user.getJob()).isEqualTo("디자이너"),
			() -> assertThat(user.getYears()).isEqualTo(10),
			() -> assertThat(myInterests.get(0).getInterest().getName()).isEqualTo(interest1.getName()),
			() -> assertThat(myInterests.get(1).getInterest().getName()).isEqualTo(interest2.getName()),
			() -> assertThat(myInterests.get(2).getInterest().getName()).isEqualTo(interest3.getName())
		);
	}

	@Test
	@DisplayName("최초로 회원 개인 정보를 업데이트한다")
	void userProfileFirstUpdate() {
		//given
		User user = UserFixture.USER_FIXTURE_1.createUser();
		Interest interest1 = InterestFixture.INTEREST_FIXTURE_1.createInterest();
		Interest interest2 = InterestFixture.INTEREST_FIXTURE_2.createInterest();
		Interest interest3 = InterestFixture.INTEREST_FIXTURE_3.createInterest();

		UserProfileUpdateRequestDto userProfileUpdateRequestDto = UserProfileUpdateRequestDto.of("디자이너", 10,
			List.of(interest1.getName(), interest2.getName(), interest3.getName()));

		Mockito.when(userRepository.findLoginUserByEmail(user.getEmail())).thenReturn(user);
		Mockito.when(interestRepository.findByName(interest1.getName())).thenReturn(interest1);
		Mockito.when(interestRepository.findByName(interest2.getName())).thenReturn(interest2);
		Mockito.when(interestRepository.findByName(interest3.getName())).thenReturn(interest3);
		Mockito.when(myInterestRepository.findByUserIdWithInterests(user.getId())).thenReturn(Collections.emptyList());

		List<MyInterest> myInterests = List.of(
			MyInterest.of(interest1, user),
			MyInterest.of(interest2, user),
			MyInterest.of(interest3, user));

		Mockito.when(myInterestRepository.saveAll(any())).thenReturn(myInterests);

		//when
		userService.updateProfileInfo(user.getEmail(), userProfileUpdateRequestDto);

		//then
		assertAll(
			() -> assertThat(user.getJob()).isEqualTo("디자이너"),
			() -> assertThat(user.getYears()).isEqualTo(10),
			() -> assertThat(myInterests.get(0).getInterest().getName()).isEqualTo(interest1.getName()),
			() -> assertThat(myInterests.get(1).getInterest().getName()).isEqualTo(interest2.getName()),
			() -> assertThat(myInterests.get(2).getInterest().getName()).isEqualTo(interest3.getName())
		);
		verify(myInterestRepository, times(1)).saveAll(any());
	}
}