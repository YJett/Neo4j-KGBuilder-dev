package com.warmer.web.service;

import com.warmer.web.entity.JobAbility;

import java.util.List;
/*
sync job_ablity

 */
public interface AbilitySyncService {


    void createAbility(JobAbility ability);
    void updateAbility(JobAbility ability);
    void deleteAbility(JobAbility ability);
}
