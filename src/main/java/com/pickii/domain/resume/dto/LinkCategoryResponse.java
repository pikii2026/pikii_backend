package com.pickii.domain.resume.dto;

import com.pickii.domain.resume.entity.LinkCategory;

/**
 * API_SPEC 5-5 링크 카테고리 조회 응답 항목
 */
public record LinkCategoryResponse(
        Long linkCategoryId,
        String name,
        String picUrl
) {
    public static LinkCategoryResponse from(LinkCategory linkCategory) {
        return new LinkCategoryResponse(linkCategory.getId(), linkCategory.getName(), linkCategory.getPicUrl());
    }
}
