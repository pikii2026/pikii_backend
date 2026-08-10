package com.pickii.domain.member.entity;

import java.util.Arrays;
import java.util.Comparator;

/**
 * 활동 경험치(exp) 누적에 따른 회원 레벨.
 * LEVEL_1(기본) → 20점 적립 시 LEVEL_2 → 40점 더 적립(누적 60) 시 LEVEL_3
 * → 80점 더 적립(누적 140) 시 최종 단계 LEVEL_4.
 * DB에 저장하지 않고 exp로부터 매번 계산한다.
 */
public enum MemberLevel {
    LEVEL_1(1, 0),
    LEVEL_2(2, 20),
    LEVEL_3(3, 60),
    LEVEL_4(4, 140);

    private final int level;
    private final int requiredExp;

    MemberLevel(int level, int requiredExp) {
        this.level = level;
        this.requiredExp = requiredExp;
    }

    public int getLevel() {
        return level;
    }

    public static MemberLevel from(int exp) {
        return Arrays.stream(values())
                .filter(memberLevel -> exp >= memberLevel.requiredExp)
                .max(Comparator.comparingInt(memberLevel -> memberLevel.requiredExp))
                .orElse(LEVEL_1);
    }
}
