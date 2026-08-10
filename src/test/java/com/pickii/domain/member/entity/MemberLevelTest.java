package com.pickii.domain.member.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberLevelTest {

    @Test
    void 경험치_0은_레벨1이다() {
        assertThat(MemberLevel.from(0).getLevel()).isEqualTo(1);
    }

    @Test
    void 레벨2_임계값_바로_아래는_아직_레벨1이다() {
        assertThat(MemberLevel.from(19).getLevel()).isEqualTo(1);
    }

    @Test
    void 경험치_20이면_레벨2다() {
        assertThat(MemberLevel.from(20).getLevel()).isEqualTo(2);
    }

    @Test
    void 레벨3_임계값_바로_아래는_아직_레벨2이다() {
        assertThat(MemberLevel.from(59).getLevel()).isEqualTo(2);
    }

    @Test
    void 경험치_60이면_레벨3이다() {
        assertThat(MemberLevel.from(60).getLevel()).isEqualTo(3);
    }

    @Test
    void 레벨4_임계값_바로_아래는_아직_레벨3이다() {
        assertThat(MemberLevel.from(139).getLevel()).isEqualTo(3);
    }

    @Test
    void 경험치_140이면_최종_레벨4다() {
        assertThat(MemberLevel.from(140).getLevel()).isEqualTo(4);
    }

    @Test
    void 경험치가_140을_초과해도_최종_레벨4에_머문다() {
        assertThat(MemberLevel.from(9999).getLevel()).isEqualTo(4);
    }
}
