package com.pickii.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 프로젝트 전체 Error Code 정의 (ERROR_CODE.md)
 *
 * HTTP Status와 Error Code는 항상 일치해야 하며, 값은 변경하지 않는다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ===== Common =====
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "입력값 검증에 실패했습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh Token이 만료되었거나 유효하지 않습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "중복된 리소스입니다."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "파일 크기가 초과되었습니다. (최대 10MB)"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "요청 횟수를 초과했습니다. 잠시 후 다시 시도해주세요."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    AI_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 생성에 실패했습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),

    // ===== Auth =====
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "가입되지 않은 사용자입니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "인증번호가 일치하지 않습니다."),
    VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "인증번호가 만료되었거나 존재하지 않습니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    REQUIRED_TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "필수 약관에 동의해주세요."),
    INVALID_VERIFICATION_TOKEN(HttpStatus.UNAUTHORIZED, "인증 토큰이 만료되었거나 유효하지 않습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."),
    INVALID_SOCIAL_TOKEN(HttpStatus.UNAUTHORIZED, "소셜 토큰 검증에 실패했습니다."),
    NOT_LINKED_ACCOUNT(HttpStatus.NOT_FOUND, "연동된 계정이 없습니다. 로그인 후 프로필에서 먼저 연동해주세요."),
    ALREADY_LINKED(HttpStatus.CONFLICT, "이미 연동된 계정입니다."),

    // ===== Recruit / Comment / Apply / Scrap =====
    RECRUIT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 공고입니다."),
    ALREADY_CLOSED(HttpStatus.CONFLICT, "이미 마감된 공고입니다."),
    ALREADY_ADDITIONAL(HttpStatus.CONFLICT, "이미 추가 모집 중인 공고입니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다."),
    RECRUIT_CLOSED(HttpStatus.BAD_REQUEST, "모집이 종료된 공고입니다."),
    ALREADY_APPLIED(HttpStatus.CONFLICT, "이미 지원한 공고입니다."),
    APPLY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 지원서입니다."),
    APPLY_NOT_WAITING(HttpStatus.CONFLICT, "대기 상태가 아니어서 취소할 수 없습니다."),
    ALREADY_SCRAPPED(HttpStatus.CONFLICT, "이미 스크랩한 공고입니다."),
    SCRAP_NOT_FOUND(HttpStatus.NOT_FOUND, "스크랩하지 않은 공고입니다."),

    // ===== Project =====
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 프로젝트입니다."),
    PROJECT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 프로젝트가 생성된 공고입니다."),
    NO_ACCEPTED_APPLICANT(HttpStatus.CONFLICT, "수락된 지원자가 없습니다."),
    ALREADY_ENDED(HttpStatus.CONFLICT, "이미 종료된 프로젝트입니다."),
    LEADER_CANNOT_LEAVE(HttpStatus.CONFLICT, "프로젝트장은 위임 후에만 나갈 수 있습니다."),
    CANNOT_KICK_SELF(HttpStatus.BAD_REQUEST, "자기 자신은 퇴출할 수 없습니다."),
    PROJECT_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트 팀원이 아닙니다."),

    // ===== Schedule / Meeting Poll =====
    INVALID_RRULE(HttpStatus.BAD_REQUEST, "반복 규칙(RRULE) 형식이 올바르지 않습니다."),
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 일정입니다."),
    SCHEDULE_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 일정 카테고리입니다."),
    PARTY_SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 팀 일정입니다."),
    POLL_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 일정 조율입니다."),
    SLOT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 후보 슬롯입니다."),
    POLL_ALREADY_ACTIVE(HttpStatus.CONFLICT, "진행 중인 일정 조율이 이미 있습니다."),
    POLL_NOT_COLLECTING(HttpStatus.CONFLICT, "응답 수집 단계가 아닙니다."),
    UNANSWERED_EXISTS(HttpStatus.CONFLICT, "미응답자가 있습니다. 확정하려면 강제 확정이 필요합니다."),

    // ===== Chat =====
    CHATROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 채팅방입니다."),
    CANNOT_CHAT_SELF(HttpStatus.BAD_REQUEST, "자기 자신과는 채팅할 수 없습니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 형식입니다."),

    // ===== Notification =====
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 알림입니다."),

    // ===== User / Resume =====
    UNIV_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 대학교입니다."),
    RESUME_NOT_FOUND(HttpStatus.NOT_FOUND, "작성된 프로필이 없습니다."),

    // ===== Feedback =====
    PROJECT_NOT_ENDED(HttpStatus.CONFLICT, "아직 종료되지 않은 프로젝트입니다."),
    EVALUATION_PERIOD_EXPIRED(HttpStatus.CONFLICT, "평가 기간(3일)이 종료되었습니다."),
    ALREADY_EVALUATED(HttpStatus.BAD_REQUEST, "이미 평가를 완료했습니다."),
    EVALUATION_NOT_FOUND(HttpStatus.NOT_FOUND, "AI 피드백 데이터가 없습니다."),
    EVALUATION_NOT_COMPLETE(HttpStatus.CONFLICT, "평가 기간이 아직 끝나지 않았습니다."),
    INSUFFICIENT_EVALUATION(HttpStatus.CONFLICT, "최소 평가 인원이 부족합니다.");

    private final HttpStatus status;
    private final String message;
}
