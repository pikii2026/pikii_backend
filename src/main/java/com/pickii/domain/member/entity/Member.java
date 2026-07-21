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

    /** 활동 경험치 (적립 규칙 추후 확정) */
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
}
