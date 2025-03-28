package eightplusone.bit.fit.domain.interest.mapper;

import java.util.List;
import java.util.Map;

public class InterestMapper {
	public static final Map<String, List<String>> INTEREST_TAG_MAP = Map.ofEntries(
		Map.entry("개인 금융 관리", List.of("금융 데이터 분석", "금융 소프트웨어 및 IT 서비스")),
		Map.entry("디지털 뱅킹", List.of("송금 / 결제", "금융 소프트웨어 및 IT 서비스")),
		Map.entry("결제 및 송금", List.of("송금 / 결제", "블록체인 및 암호화폐")),
		Map.entry("금융 포용", List.of("송금 / 결제", "규제 기술")),
		Map.entry("투자 및 자산 관리", List.of("자산관리", "중개 플랫폼")),
		Map.entry("대출", List.of("자산관리", "중개 플랫폼")),
		Map.entry("마이데이터", List.of("금융 데이터 분석")),
		Map.entry("핀테크 인프라", List.of("금융 소프트웨어 및 IT 서비스")),
		Map.entry("보험 테크", List.of("인슈어테크")),
		Map.entry("규제 기술", List.of("인슈어테크", "금융 포용")),
		Map.entry("크라우드펀딩", List.of("중개 플랫폼")),
		Map.entry("블록체인 및 암호화폐", List.of("블록체인 및 암호화폐"))
	);

	public static List<String> mapToTagFields(List<String> interests) {
		return interests.stream()
			.flatMap(interest -> INTEREST_TAG_MAP.getOrDefault(interest, List.of()).stream())
			.distinct()
			.toList();
	}
}

