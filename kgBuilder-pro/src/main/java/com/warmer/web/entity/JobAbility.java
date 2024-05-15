package com.warmer.web.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class JobAbility implements Serializable {
    private Integer abilityid;

    private String abilityno;

    private String abilitynm;

    private Integer level;

    private Integer upabilityid;

    private LocalDateTime createtime;

    private LocalDateTime updatetime;

    private static final long serialVersionUID = 1L;
}