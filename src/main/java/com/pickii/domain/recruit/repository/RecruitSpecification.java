package com.pickii.domain.recruit.repository;

import com.pickii.domain.member.entity.MemberUniv;
import com.pickii.domain.recruit.entity.Recruit;
import com.pickii.domain.recruit.entity.RecruitCategory;
import com.pickii.domain.recruit.entity.RecruitTopic;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * 2-1 공고 검색/목록 조회 동적 쿼리 (API_SPEC 2-1)
 *
 * <p>필터 결합 규칙: 서로 다른 종류의 필터는 AND, 같은 종류의 필터(예: categoryIds에 여러 값)는 OR로 묶는다.</p>
 */
public final class RecruitSpecification {

    private RecruitSpecification() {
    }

    public static Specification<Recruit> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    /** 목록 화면에 필요한 작성자 닉네임을 N+1 없이 가져오기 위한 fetch join (count 쿼리에는 적용하지 않는다) */
    public static Specification<Recruit> fetchMember() {
        return (root, query, cb) -> {
            if (Long.class != query.getResultType()) {
                root.fetch("member", JoinType.LEFT);
            }
            return cb.conjunction();
        };
    }

    public static Specification<Recruit> titleContains(String keyword) {
        return (root, query, cb) -> cb.like(root.get("title"), "%" + keyword + "%");
    }

    /** categoryIds 안에서는 OR (RecruitCategory에 매핑된 categoryId 중 하나라도 일치) */
    public static Specification<Recruit> categoryIn(List<Long> categoryIds) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            var rc = subquery.from(RecruitCategory.class);
            subquery.select(rc.get("recruitId")).where(rc.get("categoryId").in(categoryIds));
            return root.get("id").in(subquery);
        };
    }

    /** topicIds 안에서는 OR (RecruitTopic에 매핑된 topicId 중 하나라도 일치) */
    public static Specification<Recruit> topicIn(List<Long> topicIds) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            var rt = subquery.from(RecruitTopic.class);
            subquery.select(rt.get("recruitId")).where(rt.get("topicId").in(topicIds));
            return root.get("id").in(subquery);
        };
    }

    /** onCampus=false 요청: 교외 공고만 (모든 사용자에게 노출) */
    public static Specification<Recruit> offCampusOnly() {
        return (root, query, cb) -> cb.isFalse(root.get("onCampus"));
    }

    /** onCampus=true 요청: 조회자와 같은 대학교 소속 작성자의 교내 공고만 (viewerUnivId는 호출 전에 null 아님이 보장되어야 한다) */
    public static Specification<Recruit> onCampusForUniv(Long viewerUnivId) {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("onCampus")),
                root.get("member").get("id").in(memberIdsWithUniv(query, cb, viewerUnivId))
        );
    }

    /**
     * onCampus 필터를 지정하지 않은 경우: 교외 공고는 전부, 교내 공고는 같은 학교 소속일 때만 노출.
     * viewerUnivId가 null(비회원 또는 학교 정보 없음)이면 교외 공고만 노출된다.
     */
    public static Specification<Recruit> visibleGivenUniv(Long viewerUnivId) {
        return (root, query, cb) -> {
            Predicate offCampus = cb.isFalse(root.get("onCampus"));
            if (viewerUnivId == null) {
                return offCampus;
            }
            Predicate onCampusSameUniv = cb.and(
                    cb.isTrue(root.get("onCampus")),
                    root.get("member").get("id").in(memberIdsWithUniv(query, cb, viewerUnivId))
            );
            return cb.or(offCampus, onCampusSameUniv);
        };
    }

    private static Subquery<Long> memberIdsWithUniv(CriteriaQuery<?> query,
                                                      CriteriaBuilder cb,
                                                      Long univId) {
        Subquery<Long> subquery = query.subquery(Long.class);
        var mu = subquery.from(MemberUniv.class);
        subquery.select(mu.get("memberId")).where(cb.equal(mu.get("univ").get("id"), univId));
        return subquery;
    }
}
