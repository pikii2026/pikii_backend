package com.pickii.domain.resume.service;

import com.pickii.domain.member.entity.Member;
import com.pickii.domain.member.entity.MemberUniv;
import com.pickii.domain.member.entity.Univ;
import com.pickii.domain.member.repository.MemberRepository;
import com.pickii.domain.member.repository.MemberUnivRepository;
import com.pickii.domain.member.repository.UnivRepository;
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
import com.pickii.global.exception.BusinessException;
import com.pickii.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /** 4-1 내 프로필 조회 */
    public UserProfileResponse getMyProfile(Long memberId) {
        MemberResume resume = memberResumeRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));
        Member member = resume.getMember();
        MemberUniv memberUniv = memberUnivRepository.findById(memberId).orElseThrow();

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
        MemberUniv memberUniv = memberUnivRepository.findById(targetMemberId).orElseThrow();

        return new MemberProfileResponse(
                member.getNickname(),
                memberUniv.getUniv().getId(),
                memberUniv.getUniv().getName(),
                memberUniv.getMajor(),
                memberUniv.getStatus(),
                resume.getContactEmail(),
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
                .contactEmail(member.getEmail())
                .hope(request.hope())
                .strength(request.strength())
                .aboutMe(generateAboutMe(request))
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
        resume.update(request.hope(), request.strength(), request.aboutMe(), request.contactEmail());

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
            License license = licenseRepository.findByName(item.licenseName())
                    .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED,
                            "존재하지 않는 자격증입니다: " + item.licenseName()));
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

    /** TODO: 실제 AI 서버 연동 전까지는 목업 - 입력값을 조합해 자기소개 초안을 생성한다. */
    private String generateAboutMe(ResumeRequest request) {
        StringBuilder sb = new StringBuilder();
        if (request.hope() != null && !request.hope().isBlank()) {
            sb.append(request.hope()).append("를 목표로 하는 인재입니다. ");
        }
        if (request.strength() != null && !request.strength().isBlank()) {
            sb.append("강점: ").append(request.strength()).append(".");
        }
        return sb.isEmpty() ? "성실하게 프로젝트에 임하는 팀원입니다." : sb.toString().trim();
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
