package com.pickii.domain.member.entity;

import com.pickii.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String nickname;

    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt 암호화된 비밀번호 */
    @Column(nullable = false)
    private String password;

    /** 활동 경험치. 프로젝트 상호평가(Feedback) 종합 시 리뷰어들의 평균 점수만큼 적립된다. */
    @Column(nullable = false)
    private int exp;

    private LocalDateTime lastLoginAt;

    @Builder
    public Member(String nickname, String email, String password) {
        this.nickname = nickname;
        this.email = email;
        this.password = password;
        this.exp = 0;
    }

    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /** 상호평가 결과 등으로 획득한 경험치를 누적한다. */
    public void gainExp(int amount) {
        this.exp += amount;
    }

    public MemberLevel getLevel() {
        return MemberLevel.from(this.exp);
    }
}
