package eightplusone.bit.fit.domain.session.repository;

import static eightplusone.bit.fit.domain.mysession.entity.QMySession.*;
import static eightplusone.bit.fit.domain.session.entity.QSession.*;
import static eightplusone.bit.fit.domain.speaker.entity.QSpeaker.*;
import static eightplusone.bit.fit.domain.tag.entity.QTag.*;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import eightplusone.bit.fit.domain.mysession.enums.MySessionType;
import eightplusone.bit.fit.domain.tag.dto.TagDto;
import io.micrometer.common.lang.Nullable;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SessionRepositoryImpl implements SessionRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public Page<Object[]> tagFilterAndSearch(Pageable pageable, TagDto dto, @Nullable Long userId) {
		BooleanExpression joinCondition = Expressions.FALSE;

		if (userId != null) {
			joinCondition = mySession.session.eq(session)
				.and(mySession.user.id.eq(userId))
				.and(mySession.type.eq(MySessionType.REGISTER));
		}

		List<Tuple> results = queryFactory
			.select(session, tag, speaker, mySession.id)
			.from(session)
			.leftJoin(tag).on(tag.session.eq(session))
			.leftJoin(speaker).on(speaker.session.eq(session))
			.leftJoin(mySession).on(joinCondition)
			.where(
				containField(dto.getField()),
				containTopic(dto.getTopic()),
				containType(dto.getType()),
				containLevel(dto.getLevel())
			)
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		List<Object[]> mappedResults = results.stream()
			.map(Tuple::toArray)
			.toList();

		JPAQuery<Long> countQuery = queryFactory
			.select(session.count())
			.from(session)
			.leftJoin(tag).on(tag.session.eq(session))
			.where(
				containField(dto.getField()),
				containTopic(dto.getTopic()),
				containType(dto.getType()),
				containLevel(dto.getLevel())
			);

		return new PageImpl<>(mappedResults, pageable, countQuery.fetchOne());
	}

	public static BooleanExpression containField(String field) {
		return StringUtils.hasText(field) ? tag.field.eq(field) : null;
	}

	public static BooleanExpression containTopic(String topic) {
		return StringUtils.hasText(topic) ? tag.topic.eq(topic) : null;
	}

	public static BooleanExpression containType(String type) {
		return StringUtils.hasText(type) ? tag.type.eq(type) : null;
	}

	public static BooleanExpression containLevel(String level) {
		return StringUtils.hasText(level) ? tag.level.eq(level) : null;
	}

	@Override
	public List<Object[]> findLiveSessionsWithSpeakerAndTag(@Nullable Long userId) {
		BooleanExpression joinCondition = Expressions.FALSE;

		if (userId != null) {
			joinCondition = mySession.session.eq(session)
				.and(mySession.user.id.eq(userId))
				.and(mySession.type.eq(MySessionType.REGISTER));
		}

		LocalDateTime now = LocalDateTime.now();

		List<Tuple> results = queryFactory
			.select(session, tag, speaker, mySession.id)
			.from(session)
			.leftJoin(tag).on(tag.session.eq(session))
			.leftJoin(speaker).on(speaker.session.eq(session))
			.leftJoin(mySession).on(joinCondition)
			.where(session.startTime.loe(now), session.endTime.goe(now))
			.fetch();

		return results.stream()
			.map(
				tuple -> new Object[] {tuple.get(session), tuple.get(speaker), tuple.get(tag), tuple.get(mySession.id)})
			.toList();
	}

	@Override
	public Object[] findSessionDetailWithSpeakerAndTag(Long sessionId, @Nullable Long userId) {
		BooleanExpression joinCondition = Expressions.FALSE;

		if (userId != null) {
			joinCondition = mySession.session.eq(session)
				.and(mySession.user.id.eq(userId))
				.and(mySession.type.eq(MySessionType.LIKE));
		}

		// 좋아요 개수 카운트
		var totalLikes = JPAExpressions
			.select(mySession.count())
			.from(mySession)
			.where(
				mySession.session.eq(session)
					.and(mySession.type.eq(MySessionType.LIKE)));

		var likeCount = ExpressionUtils.as(totalLikes, "likeCount");

		Tuple result = queryFactory
			.select(session, tag, speaker, mySession.id, likeCount)
			.from(session)
			.leftJoin(tag).on(tag.session.eq(session))
			.leftJoin(speaker).on(speaker.session.eq(session))
			.leftJoin(mySession).on(joinCondition)
			.where(session.sessionId.eq(sessionId))
			.fetchOne();

		return new Object[] {result.get(session), result.get(speaker), result.get(tag), result.get(mySession.id),
			result.get(likeCount)};
	}

	@Override
	public List<Object[]> findAllWithSpeakerAndTag() {
		List<Tuple> results = queryFactory
			.select(session, tag, speaker)
			.from(session)
			.leftJoin(tag).on(tag.session.eq(session))
			.leftJoin(speaker).on(speaker.session.eq(session))
			.fetch();

		return results.stream().map(Tuple::toArray).toList();
	}
}
