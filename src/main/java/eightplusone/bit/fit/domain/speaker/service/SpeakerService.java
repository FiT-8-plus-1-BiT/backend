package eightplusone.bit.fit.domain.speaker.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import eightplusone.bit.fit.domain.speaker.dto.SpeakerListResponseDto;
import eightplusone.bit.fit.domain.speaker.dto.SpeakerResponseDto;
import eightplusone.bit.fit.domain.speaker.entity.Speaker;
import eightplusone.bit.fit.domain.speaker.repository.SpeakerRepository;
import eightplusone.bit.fit.domain.tag.dto.TagDto;
import eightplusone.bit.fit.domain.tag.entity.Tag;
import eightplusone.bit.fit.domain.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SpeakerService {

	private final SpeakerRepository speakerRepository;
	private final TagRepository tagRepository;

	public List<SpeakerListResponseDto> getAll() {
		List<Speaker> speakers = speakerRepository.findAllWithSession();

		Map<Long, Tag> tagMap = tagRepository.findAll().stream()
			.collect(Collectors.toMap(
				tag -> tag.getSession().getSessionId(),
				tag -> tag
			));

		return speakers.stream()
			.map(speaker -> SpeakerListResponseDto.from(
				speaker.getSession(),
				SpeakerResponseDto.from(speaker),
				TagDto.from(tagMap.get(speaker.getSession().getSessionId()))
			))
			.toList();
	}
}
