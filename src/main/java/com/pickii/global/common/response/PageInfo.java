package com.pickii.global.common.response;

import org.springframework.data.domain.Page;

/**
 * 공통 Pagination 정보 (API_SPEC 0.8)
 */
public record PageInfo(
        int currentPage,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static PageInfo from(Page<?> page) {
        return new PageInfo(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
