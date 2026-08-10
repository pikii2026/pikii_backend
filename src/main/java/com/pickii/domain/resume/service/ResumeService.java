package com.pickii.domain.resume.service;

import com.pickii.domain.member.entity.AcademicStatus;
import com.pickii.domain.member.entity.Member;
import com.pickii.domain.member.entity.MemberUniv;
import com.pickii.domain.member.entity.Univ;
import com.pickii.domain.member.repository.MemberRepository;
import com.pickii.domain.member.repository.MemberUnivRepository;
import com.pickii.domain.member.repository.UnivRepository;
import com.pickii.domain.recruit.entity.Topic;
import com.pickii.domain.recruit.repository.TopicRepository;
import com.pickii.domain.resume.dto.AdditionalLinkItem;
import com.pickii.domain.resume.dto.AdditionalLinkRequest;
import com.pickii.domain.resume.dto.ExperienceItem;
import com.pickii.domain.resume.dto.ExperienceRequest;
import com.pickii.domain.resume.dto.LicenseItem;
import com.pickii.domain.resume.dto.LicenseRequest;
import com.pickii.domain.resume.dto.MemberProfileResponse;
import com.pickii.domain.resume.dto.ResumeCreateResponse;
import com.pickii.domain.resume.dto.ResumeRequest;
import com.pickii.domain.resume.dto.SkillToolItem;
import com.pickii.domain.resume.dto.SkillToolRequest;
import com.pickii.domain.resume.dto.UserProfileResponse;
import com.pickii.domain.resume.entity.AdditionalLink;
import com.pickii.domain.resume.entity.DetailExperience;
import com.pickii.domain.resume.entity.DetailTopic;
import com.pickii.domain.resume.entity.License;
import com.pickii.domain.resume.entity.LinkCategory;
import com.pickii.domain.resume.entity.MemberLicense;
import com.pickii.domain.resume.entity.MemberResume;
import com.pickii.domain.resume.entity.MemberTechStack;
import com.pickii.domain.resume.entity.TechStack;
import com.pickii.domain.resume.repository.AdditionalLinkRepository;
import com.pickii.domain.resume.repository.DetailExperienceRepository;
import com.pickii.domain.resume.repository.DetailTopicRepository;
import com.pickii.domain.resume.repository.LicenseRepository;
import com.pickii.domain.resume.repository.LinkCategoryRepository;
import com.pickii.domain.resume.repository.MemberLicenseRepository;
import com.pickii.domain.resume.repository.MemberResumeRepository;
import com.pickii.domain.resume.repository.MemberTechStackRepository;
import com.pickii.domain.resume.repository.TechStackRepository;
import com.pickii.global.ai.GeminiClient;
import com.pickii.global.exception.BusinessException;
import com.pickii.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 내 프로필 조회 (API_SPEC 4-1), 프로필 생성 (API_SPEC 4-2), 프로필(이력서) 수정 (API_SPEC 4-3),
 * 다른 회원 프로필 조회 (API_SPEC 10-1)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeService {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final MemberRepository memberRepository;
    private final MemberResumeRepository memberResumeRepository;
    private final MemberUnivRepository memberUnivRepository;
    private final UnivRepository univRepository;
    private final TopicRepository topicRepository;
    private final DetailTopicRepository detailTopicRepository;
    private final MemberTechStackRepository memberTechStackRepository;
    private final TechStackRepository techStackRepository;
    private final MemberLicenseRepository memberLicenseRepository;
    private final LicenseRepository licenseRepository;
    private final DetailExperienceRepository detailExperienceRepository;
    private final AdditionalLinkRepository additionalLinkRepository;
    private final LinkCategoryRepository linkCategoryRepository;
    private final GeminiClient geminiClient;

    /** 4-1 내 프로필 조회 */
    public UserProfileResponse getMyProfile(Long memberId) {
        MemberResume resume = memberResumeRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));
        Member member = resume.getMember();
        MemberUniv memberUniv = memberUnivRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        List<Long> topicIds = detailTopicRepository.findTopicIdsByMemberId(memberId);
        List<SkillToolItem> skillTool = getSkillTools(memberId);
        List<LicenseItem> license = getLicenses(memberId);
        List<ExperienceItem> experience = getExperiences(memberId);
        List<AdditionalLinkItem> additionalLink = getAdditionalLinks(memberId);

        return new UserProfileResponse(
                member.getNickname(),
                memberUniv.getUniv().getId(),
                memberUniv.getUniv().getName(),
                memberUniv.getMajor(),
                memberUniv.getStatus(),
                resume.getHope(),
                resume.getStrength(),
                resume.getAboutMe(),
                member.getExp(),
                topicIds,
                skillTool,
                license,
                experience,
                additionalLink
        );
    }

    /** 10-1 회원 프로필 조회 (탈퇴 회원/프로필 미작성 회원은 RESUME_NOT_FOUND) */
    public MemberProfileResponse getProfile(Long targetMemberId) {
        MemberResume resume = memberResumeRepository.findById(targetMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));
        Member member = resume.getMember();
        MemberUniv memberUniv = memberUnivRepository.findById(targetMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        return new MemberProfileResponse(
                member.getNickname(),
                memberUniv.getUniv().getId(),
                memberUniv.getUniv().getName(),
                memberUniv.getMajor(),
                memberUniv.getStatus(),
                resume.getHope(),
                resume.getStrength(),
                resume.getAboutMe(),
                member.getExp(),
                detailTopicRepository.findTopicIdsByMemberId(targetMemberId),
                getSkillTools(targetMemberId),
                getLicenses(targetMemberId),
                getExperiences(targetMemberId),
                getAdditionalLinks(targetMemberId)
        );
    }

    /** 4-2 프로필 생성 */
    @Transactional
    public ResumeCreateResponse createResume(Long memberId, ResumeRequest request) {
        if (memberResumeRepository.existsById(memberId)) {
            throw new BusinessException(ErrorCode.RESUME_ALREADY_EXISTS);
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Univ univ = univRepository.findById(request.univId())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNIV_NOT_FOUND));
        List<Long> topicIds = request.topic() == null ? List.of() : request.topic();
        validateTopicIds(topicIds);

        MemberUniv memberUniv = MemberUniv.builder()
                .member(member)
                .univ(univ)
                .major(request.major())
                .status(request.academicStatus())
                .build();
        memberUnivRepository.save(memberUniv);

        MemberResume resume = MemberResume.builder()
                .member(member)
                .hope(request.hope())
                .strength(request.strength())
                .aboutMe(generateAboutMe(univ, topicIds, request))
                .build();
        memberResumeRepository.save(resume);

        saveTopics(memberId, topicIds);
        saveSkillTools(memberId, request.skillTool());
        saveLicenses(memberId, request.license());
        saveExperiences(member, request.experience());
        saveAdditionalLinks(member, request.additionalLink());

        return new ResumeCreateResponse("Resume Created");
    }

    /** 4-3 프로필(이력서) 수정 */
    @Transactional
    public void updateResume(Long memberId, ResumeRequest request) {
        MemberResume resume = memberResumeRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));
        MemberUniv memberUniv = memberUnivRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));
        Univ univ = univRepository.findById(request.univId())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNIV_NOT_FOUND));
        List<Long> topicIds = request.topic() == null ? List.of() : request.topic();
        validateTopicIds(topicIds);

        memberUniv.update(univ, request.major(), request.academicStatus());
        resume.update(request.hope(), request.strength(), request.aboutMe());

        Member member = resume.getMember();
        detailTopicRepository.deleteAllByMemberId(memberId);
        memberTechStackRepository.deleteAllByMemberId(memberId);
        memberLicenseRepository.deleteAllByMemberId(memberId);
        detailExperienceRepository.deleteAllByMemberId(memberId);
        additionalLinkRepository.deleteAllByMemberId(memberId);

        saveTopics(memberId, topicIds);
        saveSkillTools(memberId, request.skillTool());
        saveLicenses(memberId, request.license());
        saveExperiences(member, request.experience());
        saveAdditionalLinks(member, request.additionalLink());
    }

    private void validateTopicIds(List<Long> topicIds) {
        if (!topicIds.isEmpty() && topicRepository.countByIdIn(topicIds) != topicIds.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "존재하지 않는 주제가 포함되어 있습니다.");
        }
    }

    private void saveTopics(Long memberId, List<Long> topicIds) {
        topicIds.forEach(topicId -> detailTopicRepository.save(new DetailTopic(memberId, topicId)));
    }

    private void saveSkillTools(Long memberId, List<SkillToolRequest> skillTools) {
        if (skillTools == null) {
            return;
        }
        skillTools.forEach(item -> {
            TechStack techStack = techStackRepository.findByName(item.techStackName())
                    .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED,
                            "존재하지 않는 기술 스택입니다: " + item.techStackName()));
            memberTechStackRepository.save(new MemberTechStack(memberId, techStack.getId(), item.level()));
        });
    }

    private void saveLicenses(Long memberId, List<LicenseRequest> licenses) {
        if (licenses == null) {
            return;
        }
        licenses.forEach(item -> {
            // 자격증은 종류가 너무 많아 마스터로 전부 관리할 수 없으므로,
            // 목록에 없는 이름은 사용자 입력 그대로 마스터에 자동 등록한다
            License license = licenseRepository.findByName(item.licenseName())
                    .orElseGet(() -> licenseRepository.save(new License(item.licenseName())));
            memberLicenseRepository.save(new MemberLicense(memberId, license.getId(), parseYearMonth(item.date())));
        });
    }

    private void saveExperiences(Member member, List<ExperienceRequest> experiences) {
        if (experiences == null) {
            return;
        }
        experiences.forEach(item -> detailExperienceRepository.save(DetailExperience.builder()
                .member(member)
                .title(item.title())
                .organization(item.organization())
                .description(item.description())
                .startDate(parseYearMonth(item.startDate()))
                .endDate(item.endDate() == null || item.endDate().isBlank() ? null : parseYearMonth(item.endDate()))
                .build()));
    }

    private void saveAdditionalLinks(Member member, List<AdditionalLinkRequest> links) {
        if (links == null) {
            return;
        }
        links.forEach(item -> {
            LinkCategory linkCategory = linkCategoryRepository.findByName(item.linkName())
                    .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED,
                            "존재하지 않는 링크 카테고리입니다: " + item.linkName()));
            additionalLinkRepository.save(AdditionalLink.builder()
                    .member(member)
                    .linkCategory(linkCategory)
                    .url(item.url())
                    .build());
        });
    }

    private LocalDate parseYearMonth(String yearMonth) {
        try {
            return YearMonth.parse(yearMonth, YEAR_MONTH_FORMATTER).atDay(1);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "날짜는 YYYY-MM 형식으로 입력해주세요.");
        }
    }

    /** 4-2 자기소개(aboutMe) 최초 1회 AI 생성 (Gemini) */
    private String generateAboutMe(Univ univ, List<Long> topicIds, ResumeRequest request) {
        String topicNames = topicIds.isEmpty()
                ? "(없음)"
                : topicRepository.findAllById(topicIds).stream().map(Topic::getName)
                        .collect(Collectors.joining(", "));

        String skillToolText = request.skillTool() == null || request.skillTool().isEmpty()
                ? "(없음)"
                : request.skillTool().stream()
                        .map(s -> s.techStackName() + "(숙련도 " + s.level() + ")")
                        .collect(Collectors.joining(", "));

        String licenseText = request.license() == null || request.license().isEmpty()
                ? "(없음)"
                : request.license().stream().map(LicenseRequest::licenseName)
                        .collect(Collectors.joining(", "));

        String experienceText = request.experience() == null || request.experience().isEmpty()
                ? "(없음)"
                : request.experience().stream()
                        .map(e -> e.title() + (e.organization() == null || e.organization().isBlank() ? "" : " (" + e.organization() + ")"))
                        .collect(Collectors.joining(", "));

        String prompt = """
                너는 대학생 이력서의 자기소개(aboutMe)를 대신 작성해주는 도우미다.
                아래 입력 정보만을 바탕으로 자연스럽고 신뢰가 가는 한국어 자기소개를 300자 이내로 작성하라.
                입력되지 않은("(없음)") 항목은 언급하지 말고, 없는 사실을 지어내지 마라.
                결과는 자기소개 본문만 출력하라 (설명, 따옴표 없이).

                대학교: %s
                전공: %s
                학적 상태: %s
                희망 진로: %s
                장점: %s
                관심 주제: %s
                기술 스택: %s
                자격증: %s
                수상/경험: %s
                """.formatted(
                        univ.getName(),
                        request.major(),
                        academicStatusKorean(request.academicStatus()),
                        StringUtils.hasText(request.hope()) ? request.hope() : "(없음)",
                        StringUtils.hasText(request.strength()) ? request.strength() : "(없음)",
                        topicNames,
                        skillToolText,
                        licenseText,
                        experienceText
                );

        return geminiClient.generateText(prompt).trim();
    }

    private String academicStatusKorean(AcademicStatus status) {
        return switch (status) {
            case ENROLLED -> "재학";
            case LEAVE_OF_ABSENCE -> "휴학";
            case GRADUATION_DEFERRED -> "졸업유예";
            case GRADUATED -> "졸업";
        };
    }

    private List<SkillToolItem> getSkillTools(Long memberId) {
        List<MemberTechStack> memberTechStacks = memberTechStackRepository.findAllByMemberId(memberId);
        Map<Long, TechStack> techStackById = techStackRepository
                .findAllById(memberTechStacks.stream().map(MemberTechStack::getTechStackId).toList())
                .stream().collect(Collectors.toMap(TechStack::getId, Function.identity()));
        return memberTechStacks.stream()
                .map(mts -> new SkillToolItem(techStackById.get(mts.getTechStackId()).getName(), mts.getLevel()))
                .toList();
    }

    private List<LicenseItem> getLicenses(Long memberId) {
        List<MemberLicense> memberLicenses = memberLicenseRepository.findAllByMemberId(memberId);
        Map<Long, License> licenseById = licenseRepository
                .findAllById(memberLicenses.stream().map(MemberLicense::getLicenseId).toList())
                .stream().collect(Collectors.toMap(License::getId, Function.identity()));
        return memberLicenses.stream()
                .map(ml -> new LicenseItem(licenseById.get(ml.getLicenseId()).getName(), formatYearMonth(ml.getAcquired())))
                .toList();
    }

    private List<ExperienceItem> getExperiences(Long memberId) {
        return detailExperienceRepository.findAllByMemberId(memberId).stream()
                .map(e -> new ExperienceItem(
                        formatYearMonth(e.getStartDate()),
                        e.getEndDate() == null ? null : formatYearMonth(e.getEndDate()),
                        e.getTitle(),
                        e.getOrganization(),
                        e.getDescription()
                ))
                .toList();
    }

    private List<AdditionalLinkItem> getAdditionalLinks(Long memberId) {
        return additionalLinkRepository.findAllWithCategoryByMemberId(memberId).stream()
                .map(al -> new AdditionalLinkItem(al.getLinkCategory().getName(), al.getUrl()))
                .toList();
    }

    private String formatYearMonth(LocalDate date) {
        return date.format(YEAR_MONTH_FORMATTER);
    }
}
