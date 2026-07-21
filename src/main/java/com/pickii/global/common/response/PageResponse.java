package com.pickii.global.common.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * 목록 조회 공통 응답 (API_SPEC 0.8)
 *
 * <pre>
 * {
 *     "content": [...],
 *     "pageInfo": { ... }
 * }
 * </pre>
 */
public record PageResponse<T>(
        List<T> content,
        PageInfo pageInfo
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), PageInfo.from(page));
    }

    /** 엔티티 Page를 DTO로 변환하면서 감쌀 때 사용 */
    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.map(mapper).getContent(), PageInfo.from(page));
    }
}
