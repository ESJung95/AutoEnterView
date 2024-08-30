package com.ctrls.auto_enter_view.repository;

import com.ctrls.auto_enter_view.entity.JobPostingEntity;
import com.ctrls.auto_enter_view.enums.Education;
import com.ctrls.auto_enter_view.enums.JobCategory;
import com.ctrls.auto_enter_view.enums.TechStack;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobPostingRepositoryDSL {

  Page<JobPostingEntity> searchJobPosting(
      Pageable pageable,
      JobCategory jobCategory,
      List<TechStack> techStacks,
      String employmentType,
      Integer minCareer,
      Integer maxCareer,
      Education education
  );
}