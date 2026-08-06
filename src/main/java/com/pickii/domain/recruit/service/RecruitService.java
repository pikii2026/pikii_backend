package com.pickii.domain.recruit.service;

import com.pickii.domain.member.repository.MemberRepository;
import com.pickii.domain.member.repository.MemberUnivRepository;
import com.pickii.domain.project.entity.Project;
import com.pickii.domain.project.repository.ProjectRepository;
import com.pickii.domain.recruit.dto.AiDraftRequest;
import com.pickii.domain.recruit.dto.AiDraftResponse;
import com.pickii.domain.recruit.dto.CommentCreateRequest;
import com.pickii.domain.recruit.dto.CommentCreateResponse;
import com.pickii.domain.recruit.dto.CommentListResponse;
import com.pickii.domain.recruit.dto.CommentReplyResponse;
import com.pickii.domain.recruit.dto.CommentResponse;
import com.pickii.domain.recruit.dto.MyCommentResponse;
import com.pickii.domain.recruit.dto.RecruitCreateRequest;
import com.pickii.domain.recruit.dto.RecruitCreateResponse;
import com.pickii.domain.recruit.dto.RecruitDetailResponse;
import com.pickii.domain.recruit.dto.RecruitScrapResponse;
import com.pickii.domain.recruit.dto.RecruitScrapSummaryResponse;
import com.pickii.domain.recruit.dto.RecruitSummaryResponse;
import com.pickii.domain.recruit.entity.Comment;
import com.pickii.domain.recruit.entity.Recruit;
import com.pickii.domain.recruit.entity.RecruitCategory;
import com.pickii.domain.recruit.entity.RecruitScrap;
import com.pickii.domain.recruit.entity.RecruitStatus;
import com.pickii.domain.recruit.entity.RecruitTopic;
import com.pickii.domain.recruit.repository.CategoryRepository;
import com.pickii.domain.recruit.repository.CommentRepository;
import com.pickii.domain.recruit.repository.RecruitCategoryRepository;
import com.pickii.domain.recruit.repository.RecruitRepository;
import com.pickii.domain.recruit.repository.RecruitScrapRepository;
import com.pickii.domain.recruit.repository.RecruitSpecification;
import com.pickii.domain.recruit.repository.RecruitTopicRepository;
import com.pickii.domain.recruit.repository.TopicRepository;
import com.pickii.global.common.response.PageResponse;
import com.pickii.global.exception.BusinessException;
import com.pickii.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 메인(Home) 공고 검색/목록 조회 (API_SPEC 2-1), 공고 상세 조회 (API_SPEC 3-1),
 * 공고 작성 (API_SPEC 3-2), AI 공고 초안 생성 (API_SPEC 3-3), 공고 마감 (API_SPEC 3-4),
 * 공고 추가 모집 (API_SPEC 3-5), 공고 삭제 (API_SPEC 3-6), 댓글 및 답글 목록 조회 (API_SPEC 3-7),
 * 댓글 및 답글 작성 (API_SPEC 3-8), 댓글 삭제 (API_SPEC 3-9), 공고 수정 (API_SPEC 3-12),
 * 공고 스크랩 (API_SPEC 3-14), 공고 스크랩 취소 (API_SPEC 3-15), 스크랩한 공고 목록 조회 (API_SPEC 3-16)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitService {

    private static final int KEYWORD_MIN_LENGTH = 2;

    private final RecruitRepository recruitRepository;
    private final MemberRepository memberRepository;
    private final MemberUnivRepository memberUnivRepository;
    private final CategoryRepository categoryRepository;
    private final TopicRepository topicRepository;
    private final RecruitCategoryRepository recruitCategoryRepository;
    private final RecruitTopicRepository recruitTopicRepository;
    private final RecruitScrapRepository recruitScrapRepository;
    private final CommentRepository commentRepository;
    private final ProjectRepository projectRepository;

    public PageResponse<RecruitSummaryResponse> searchRecruits(String keyword, Boolean onCampus,
                                                                List<Long> categoryIds, List<Long> topicIds,
                                                                Long viewerMemberId, Pageable pageable) {
        Specification<Recruit> spec = RecruitSpecification.notDeleted()
                .and(RecruitSpecification.fetchMember());

        if (StringUtils.hasText(keyword)) {
            String trimmed = keyword.trim();
            if (trimmed.length() < KEYWORD_MIN_LENGTH) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "검색어는 %d자 이상 입력해주세요.".formatted(KEYWORD_MIN_LENGTH));
            }
            spec = spec.and(RecruitSpecification.titleContains(trimmed));
        }
        if (categoryIds != null && !categoryIds.isEmpty()) {
            spec = spec.and(RecruitSpecification.categoryIn(categoryIds));
        }
        if (topicIds != null && !topicIds.isEmpty()) {
            spec = spec.and(RecruitSpecification.topicIn(topicIds));
        }
        spec = spec.and(onCampusSpecification(onCampus, viewerMemberId));

        Page<Recruit> page = recruitRepository.findAll(spec, pageable);
        return PageResponse.from(page, this::toSummary);
    }

    private Specification<Recruit> onCampusSpecification(Boolean onCampus, Long viewerMemberId) {
        if (Boolean.FALSE.equals(onCampus)) {
            return RecruitSpecification.offCampusOnly();
        }
        Long viewerUnivId = resolveUnivId(viewerMemberId);
        if (Boolean.TRUE.equals(onCampus)) {
            // 학교 정보가 없는 회원/비회원은 onCampus=true 필터 자체를 사용할 수 없다.
            if (viewerUnivId == null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "학교 정보가 없어 교내 공고를 조회할 수 없습니다.");
            }
            return RecruitSpecification.onCampusForUniv(viewerUnivId);
        }
        return RecruitSpecification.visibleGivenUniv(viewerUnivId);
    }

    private Long resolveUnivId(Long memberId) {
        if (memberId == null) {
            return null;
        }
        return memberUnivRepository.findById(memberId)
                .map(memberUniv -> memberUniv.getUniv().getId())
                .orElse(null);
    }

    /** 3-2 공고 작성 */
    @Transactional
    public RecruitCreateResponse createRecruit(Long memberId, RecruitCreateRequest request) {
        Set<Long> categoryIds = new LinkedHashSet<>(request.categoryIds());
        Set<Long> topicIds = request.topicIds() == null ? Set.of() : new LinkedHashSet<>(request.topicIds());
        validateCategoryAndTopicIds(categoryIds, topicIds);
        validateDateOrder(request.startDate(), request.endDate());

        Recruit recruit = Recruit.builder()
                .member(memberRepository.getReferenceById(memberId))
                .title(request.title())
                .onCampus(request.onCampus())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .simpleDesc(request.simpleDesc())
                .content(request.content())
                .targetCount(request.maxMembers())
                .build();
        recruitRepository.save(recruit);

        categoryIds.forEach(categoryId -> recruitCategoryRepository.save(new RecruitCategory(recruit.getId(), categoryId)));
        topicIds.forEach(topicId -> recruitTopicRepository.save(new RecruitTopic(recruit.getId(), topicId)));

        return new RecruitCreateResponse(recruit.getId());
    }

    /** 3-12 공고 수정 */
    @Transactional
    public void updateRecruit(Long memberId, Long recruitId, RecruitCreateRequest request) {
        Recruit recruit = recruitRepository.findById(recruitId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.RECRUIT_NOT_FOUND));
        if (recruit.getMember() == null || !recruit.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Set<Long> categoryIds = new LinkedHashSet<>(request.categoryIds());
        Set<Long> topicIds = request.topicIds() == null ? Set.of() : new LinkedHashSet<>(request.topicIds());
        validateCategoryAndTopicIds(categoryIds, topicIds);
        validateDateOrder(request.startDate(), request.endDate());

        recruit.update(
                request.title(),
                request.onCampus(),
                request.startDate(),
                request.endDate(),
                request.simpleDesc(),
                request.content(),
                request.maxMembers()
        );

        recruitCategoryRepository.deleteAllByRecruitId(recruitId);
        recruitTopicRepository.deleteAllByRecruitId(recruitId);
        categoryIds.forEach(categoryId -> recruitCategoryRepository.save(new RecruitCategory(recruitId, categoryId)));
        topicIds.forEach(topicId -> recruitTopicRepository.save(new RecruitTopic(recruitId, topicId)));
    }

    /** 3-14 공고 스크랩 */
    @Transactional
    public RecruitScrapResponse scrapRecruit(Long memberId, Long recruitId) {
        recruitRepository.findById(recruitId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.RECRUIT_NOT_FOUND));
        if (recruitScrapRepository.existsByMemberIdAndRecruitId(memberId, recruitId)) {
            throw new BusinessException(ErrorCode.ALREADY_SCRAPPED);
        }
        recruitScrapRepository.save(new RecruitScrap(memberId, recruitId));
        return new RecruitScrapResponse(recruitId, true);
    }

    /** 3-15 공고 스크랩 취소 */
    @Transactional
    public void cancelScrapRecruit(Long memberId, Long recruitId) {
        recruitRepository.findById(recruitId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.RECRUIT_NOT_FOUND));
        if (!recruitScrapRepository.existsByMemberIdAndRecruitId(memberId, recruitId)) {
            throw new BusinessException(ErrorCode.SCRAP_NOT_FOUND);
        }
        recruitScrapRepository.deleteByMemberIdAndRecruitId(memberId, recruitId);
    }

    /** 4-5 작성한 공고 조회 */
    public PageResponse<RecruitSummaryResponse> getMyRecruits(Long memberId, Pageable pageable) {
        Page<Recruit> page = recruitRepository.findByMemberIdAndDeletedAtIsNull(memberId, pageable);
        return PageResponse.from(page, this::toSummary);
    }

    /** 4-6 작성한 댓글 조회 */
    public PageResponse<MyCommentResponse> getMyComments(Long memberId, Pageable pageable) {
        Page<Comment> page = commentRepository.findMyComments(memberId, pageable);
        return PageResponse.from(page, this::toMyCommentResponse);
    }

    private MyCommentResponse toMyCommentResponse(Comment comment) {
        Recruit recruit = comment.getRecruit();
        return new MyCommentResponse(
                comment.getId(),
                recruit.getId(),
                recruit.getTitle(),
                recruit.getStatus(),
                comment.getContent(),
                comment.getCreatedAt().atOffset(ZoneOffset.ofHours(9))
        );
    }

    /** 3-16 스크랩한 공고 목록 조회 */
    public PageResponse<RecruitScrapSummaryResponse> getScrappedRecruits(Long memberId, Pageable pageable) {
        Page<RecruitScrap> page = recruitScrapRepository.findAllByMemberIdWithActiveRecruit(memberId, pageable);
        return PageResponse.from(page, this::toScrapSummary);
    }

    private RecruitScrapSummaryResponse toScrapSummary(RecruitScrap scrap) {
        Recruit recruit = recruitRepository.findById(scrap.getRecruitId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RECRUIT_NOT_FOUND));
        String authorNickname = recruit.getMember() == null ? "알 수 없음" : recruit.getMember().getNickname();
        return new RecruitScrapSummaryResponse(
                recruit.getId(),
                recruit.getTitle(),
                authorNickname,
                recruit.isOnCampus(),
                recruit.getStatus(),
                recruit.getTargetCount(),
                recruit.getAvailableSlots(),
                scrap.getCreatedAt().atOffset(ZoneOffset.ofHours(9))
        );
    }

    private void validateCategoryAndTopicIds(Set<Long> categoryIds, Set<Long> topicIds) {
        if (categoryRepository.countByIdIn(categoryIds) != categoryIds.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "존재하지 않는 카테고리가 포함되어 있습니다.");
        }
        if (!topicIds.isEmpty() && topicRepository.countByIdIn(topicIds) != topicIds.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "존재하지 않는 주제가 포함되어 있습니다.");
        }
    }

    private void validateDateOrder(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "시작일은 종료일보다 이후일 수 없습니다.");
        }
    }

    /** 3-4 공고 마감 */
    @Transactional
    public void closeRecruit(Long memberId, Long recruitId) {
        Recruit recruit = recruitRepository.findById(recruitId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.RECRUIT_NOT_FOUND));
        if (recruit.getMember() == null || !recruit.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (recruit.getStatus() == RecruitStatus.CLOSED) {
            throw new BusinessException(ErrorCode.ALREADY_CLOSED);
        }
        recruit.close();
    }

    /** 3-5 공고 추가 모집 */
    @Transactional
    public void openAdditionalRecruit(Long memberId, Long recruitId) {
        Recruit recruit = recruitRepository.findById(recruitId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.RECRUIT_NOT_FOUND));
        if (recruit.getMember() == null || !recruit.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (recruit.getStatus() == RecruitStatus.ADDITIONAL) {
            throw new BusinessException(ErrorCode.ALREADY_ADDITIONAL);
        }
        Optional<Project> project = projectRepository.findByRecruitId(recruitId);
        if (project.isPresent() && project.get().isEnded()) {
            throw new BusinessException(ErrorCode.PROJECT_ENDED_CANNOT_RECRUIT);
        }
        recruit.openAdditional();
    }

    /** 3-6 공고 삭제 */
    @Transactional
    public void deleteRecruit(Long memberId, Long recruitId) {
        Recruit recruit = recruitRepository.findById(recruitId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.RECRUIT_NOT_FOUND));
        if (recruit.getMember() == null || !recruit.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        recruit.softDelete();
    }

    /** 3-7 댓글 및 답글 목록 조회 */
    public CommentListResponse getComments(Long recruitId) {
        Recruit recruit = recruitRepository.findById(recruitId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.RECRUIT_NOT_FOUND));
        Long recruitAuthorId = recruit.getMember() == null ? null : recruit.getMember().getId();

        List<Comment> allComments = commentRepository.findAllByRecruitIdOrderByCreatedAtAsc(recruitId);

        Map<Long, List<Comment>> repliesByRootId = new LinkedHashMap<>();
        for (Comment comment : allComments) {
            if (comment.getParent() == null) {
                repliesByRootId.putIfAbsent(comment.getId(), new ArrayList<>());
            }
        }
        for (Comment comment : allComments) {
            if (comment.getParent() == null) {
                continue;
            }
            Comment root = comment;
            while (root.getParent() != null) {
                root = root.getParent();
            }
            repliesByRootId.computeIfAbsent(root.getId(), k -> new ArrayList<>()).add(comment);
        }

        List<CommentResponse> comments = new ArrayList<>();
        for (Comment comment : allComments) {
            if (comment.getParent() != null) {
                continue;
            }
            List<CommentReplyResponse> replies = repliesByRootId.getOrDefault(comment.getId(), List.of()).stream()
                    .map(reply -> toReplyResponse(reply, recruitAuthorId))
                    .toList();
            comments.add(toCommentResponse(comment, recruitAuthorId, replies));
        }

        return new CommentListResponse(comments);
    }

    /** 3-8 댓글 및 답글 작성 */
    @Transactional
    public CommentCreateResponse createComment(Long memberId, Long recruitId, CommentCreateRequest request) {
        Recruit recruit = recruitRepository.findById(recruitId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.RECRUIT_NOT_FOUND));

        Comment parent = null;
        if (request.parentCommentId() != null) {
            parent = commentRepository.findById(request.parentCommentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        }

        Comment comment = Comment.builder()
                .recruit(recruit)
                .member(memberRepository.getReferenceById(memberId))
                .parent(parent)
                .content(request.content())
                .build();
        commentRepository.save(comment);

        return new CommentCreateResponse(comment.getId());
    }

    /** 3-9 댓글 삭제 */
    @Transactional
    public void deleteComment(Long memberId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        if (comment.getMember() == null || !comment.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        comment.softDelete();
    }

    private CommentResponse toCommentResponse(Comment comment, Long recruitAuthorId, List<CommentReplyResponse> replies) {
        return new CommentResponse(
                comment.getId(),
                resolveAuthorId(comment),
                resolveAuthorNickname(comment),
                resolveContent(comment),
                comment.getCreatedAt().atOffset(ZoneOffset.ofHours(9)),
                isCommentAuthorRecruitAuthor(comment, recruitAuthorId),
                replies
        );
    }

    private CommentReplyResponse toReplyResponse(Comment comment, Long recruitAuthorId) {
        return new CommentReplyResponse(
                comment.getId(),
                resolveAuthorId(comment),
                resolveAuthorNickname(comment),
                resolveContent(comment),
                comment.getCreatedAt().atOffset(ZoneOffset.ofHours(9)),
                isCommentAuthorRecruitAuthor(comment, recruitAuthorId)
        );
    }

    private Long resolveAuthorId(Comment comment) {
        if (comment.isDeleted() || comment.getMember() == null) {
            return null;
        }
        return comment.getMember().getId();
    }

    private String resolveAuthorNickname(Comment comment) {
        if (comment.isDeleted()) {
            return null;
        }
        return comment.getMember() == null ? "알 수 없음" : comment.getMember().getNickname();
    }

    private String resolveContent(Comment comment) {
        return comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent();
    }

    private boolean isCommentAuthorRecruitAuthor(Comment comment, Long recruitAuthorId) {
        if (comment.isDeleted() || comment.getMember() == null || recruitAuthorId == null) {
            return false;
        }
        return recruitAuthorId.equals(comment.getMember().getId());
    }

    /**
     * 3-3 AI 공고 초안 생성
     *
     * <p>TODO: 실제 AI 서버 연동 전까지는 입력값을 가공해 돌려주는 목업으로 구현한다.
     * (팀 컨벤션: {@code docs/dev-plan/person1/day-02-recruit-create.md} 참고)</p>
     */
    public AiDraftResponse generateAiDraft(AiDraftRequest request) {
        return new AiDraftResponse(
                "AI가 다듬은: " + request.simpleDesc(),
                "AI가 다듬은: " + request.content()
        );
    }

    /** 3-1 공고 상세 조회 */
    public RecruitDetailResponse getRecruitDetail(Long recruitId, Long viewerMemberId) {
        Recruit recruit = recruitRepository.findById(recruitId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.RECRUIT_NOT_FOUND));

        List<Long> categoryIds = recruitCategoryRepository.findCategoryIdsByRecruitId(recruitId);
        List<Long> topicIds = recruitTopicRepository.findTopicIdsByRecruitId(recruitId);
        boolean isScrapped = viewerMemberId != null
                && recruitScrapRepository.existsByMemberIdAndRecruitId(viewerMemberId, recruitId);

        Long authorId = recruit.getMember() == null ? null : recruit.getMember().getId();
        String authorNickname = recruit.getMember() == null ? "알 수 없음" : recruit.getMember().getNickname();
        int authorExp = recruit.getMember() == null ? 0 : recruit.getMember().getExp();

        return new RecruitDetailResponse(
                recruit.getId(),
                recruit.getTitle(),
                authorId,
                authorNickname,
                authorExp,
                recruit.getCreatedAt().atOffset(ZoneOffset.ofHours(9)),
                recruit.getStartDate(),
                recruit.getEndDate(),
                categoryIds,
                topicIds,
                recruit.isOnCampus(),
                recruit.getSimpleDesc(),
                recruit.getContent(),
                recruit.getStatus(),
                recruit.getTargetCount(),
                recruit.getAvailableSlots(),
                isScrapped
        );
    }

    private RecruitSummaryResponse toSummary(Recruit recruit) {
        String authorNickname = recruit.getMember() == null ? "알 수 없음" : recruit.getMember().getNickname();
        return new RecruitSummaryResponse(
                recruit.getId(),
                recruit.getTitle(),
                authorNickname,
                recruit.getTargetCount(),
                recruit.getAvailableSlots(),
                recruit.getStatus(),
                recruit.getCreatedAt().atOffset(ZoneOffset.ofHours(9))
        );
    }
}
