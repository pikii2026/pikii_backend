package com.pickii.domain.member.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberTest {

    private Member newMember() {
        return Member.builder()
                .nickname("픽키")
                .email("pikii@test.com")
                .password("encoded")
                .build();
    }

    @Test
    void 신규_회원의_기본_경험치와_레벨은_0과_1이다() {
        Member member = newMember();

        assertThat(member.getExp()).isEqualTo(0);
        assertThat(member.getLevel()).isEqualTo(MemberLevel.LEVEL_1);
    }

    @Test
    void 경험치를_획득하면_누적되고_임계값에_도달하면_레벨도_함께_오른다() {
        Member member = newMember();

        member.gainExp(15);
        assertThat(member.getExp()).isEqualTo(15);
        assertThat(member.getLevel()).isEqualTo(MemberLevel.LEVEL_1);

        member.gainExp(10);
        assertThat(member.getExp()).isEqualTo(25);
        assertThat(member.getLevel()).isEqualTo(MemberLevel.LEVEL_2);
    }
}
