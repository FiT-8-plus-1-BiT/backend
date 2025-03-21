package eightplusone.bit.fit.domain.session.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import eightplusone.bit.fit.domain.tag.dto.TagDto;

public interface SessionRepositoryCustom {
	Page<Object[]> tagFilterAndSearch(Pageable pageable, TagDto dto);

	List<Object[]> findLiveSessionsWithSpeakerAndTag();
}
