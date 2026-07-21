package com.pickii.domain.schedule.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 팀 일정에 적용할 개인 카테고리 매핑.
 * 팀원별로 해당 프로젝트의 팀 일정을 자기 캘린더에서 어떤 색으로 볼지 지정한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(ProjectScheduleCategory.Pk.class)
public class ProjectScheduleCategory {

    @Id
    private Long scId;

    @Id
    private Long projectId;

    public ProjectScheduleCategory(Long scId, Long projectId) {
        this.scId = scId;
        this.projectId = projectId;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private Long scId;
        private Long projectId;
    }
}
