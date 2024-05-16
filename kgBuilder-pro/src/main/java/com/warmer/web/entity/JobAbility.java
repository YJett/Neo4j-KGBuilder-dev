package com.warmer.web.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class JobAbility implements Serializable {

    private Integer abilityId;

    private Integer abilityNo;

    private String abilityNm;

    private Integer level;

    private Integer upabilityId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer jobId;

    private static final long serialVersionUID = 1L;
}