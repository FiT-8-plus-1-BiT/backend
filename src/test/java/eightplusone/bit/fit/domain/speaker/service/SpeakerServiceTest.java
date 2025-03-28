package eightplusone.bit.fit.domain.speaker.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import eightplusone.bit.fit.domain.speaker.dto.SpeakerListResponseDto;
import eightplusone.bit.fit.domain.speaker.entity.Speaker;
import eightplusone.bit.fit.domain.speaker.repository.SpeakerRepository;
import eightplusone.bit.fit.domain.tag.entity.Tag;
import eightplusone.bit.fit.domain.tag.repository.TagRepository;
import eightplusone.bit.fit.support.fixture.SpeakerFixture;
import eightplusone.bit.fit.support.fixture.TagFixture;

class SpeakerServiceTest {

	@Mock
	private SpeakerRepository speakerRepository;

	@Mock
	private TagRepository tagRepository;

	@InjectMocks
	private SpeakerService speakerService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	@DisplayName("연사 정보 전체조회를 한다.")
	void getAllSpeakers_withMatchingSessions() {
		// given
		Speaker speaker1 = SpeakerFixture.SPEAKER_FIXTURE_1.createSpeaker();
		Tag tag1 = TagFixture.TAG_FIXTURE_1.createTag();

		Speaker speaker2 = SpeakerFixture.SPEAKER_FIXTURE_2.createSpeaker();
		Tag tag2 = TagFixture.TAG_FIXTURE_2.createTag();

		setField(speaker1.getSession(), "sessionId", 101L);
		setField(tag1.getSession(), "sessionId", 101L);

		setField(speaker2.getSession(), "sessionId", 202L);
		setField(tag2.getSession(), "sessionId", 202L);

		when(speakerRepository.findAllWithSession()).thenReturn(List.of(speaker1, speaker2));
		when(tagRepository.findAll()).thenReturn(List.of(tag1, tag2));

		// when
		List<SpeakerListResponseDto> result = speakerService.getAll();

		// then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getSpeaker().getName()).isEqualTo(speaker1.getName());
		assertThat(result.get(0).getTags().getField()).isEqualTo(tag1.getField());
		assertThat(result.get(1).getSpeaker().getName()).isEqualTo(speaker2.getName());
		assertThat(result.get(1).getTags().getField()).isEqualTo(tag2.getField());
	}
}
