package com.ctrls.auto_enter_view.repository;

import static com.ctrls.auto_enter_view.entity.QJobPostingEntity.jobPostingEntity;
import static com.ctrls.auto_enter_view.entity.QJobPostingTechStackEntity.jobPostingTechStackEntity;

import com.ctrls.auto_enter_view.entity.JobPostingEntity;
import com.ctrls.auto_enter_view.enums.Education;
import com.ctrls.auto_enter_view.enums.JobCategory;
import com.ctrls.auto_enter_view.enums.TechStack;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

@Repository
public class JobPostingRepositoryDSLImpl extends QuerydslRepositorySupport implements
    JobPostingRepositoryDSL {

  private final JPAQueryFactory jpaQueryFactory;

  public JobPostingRepositoryDSLImpl(JPAQueryFactory jpaQueryFactory) {

    super(JobPostingEntity.class);
    this.jpaQueryFactory = jpaQueryFactory;
  }

  public Page<JobPostingEntity> searchJobPosting(
      Pageable pageable,
      JobCategory jobCategory,
      List<TechStack> techStacks,
      String employmentType,
      Integer minCareer,
      Integer maxCareer,
      Education education
  ) {

    JPQLQuery<JobPostingEntity> query = jpaQueryFactory.selectFrom(jobPostingEntity)
        .leftJoin(jobPostingTechStackEntity)
        .on(jobPostingEntity.jobPostingKey.eq(jobPostingTechStackEntity.jobPostingKey))
        .where(
            eqJobCategory(jobCategory),
            eqTechStack(techStacks),
            eqEmploymentType(employmentType),
            eqCareer(minCareer, maxCareer),
            eqEducation(education)
        )
        .distinct();

    List<JobPostingEntity> entities = this.getQuerydsl().applyPagination(pageable, query).fetch();
    return new PageImpl<>(entities, pageable, query.fetch().size());
  }

  private BooleanExpression eqJobCategory(JobCategory jobCategory) {

    if (jobCategory == null) {
      return null;
    }
    return jobPostingEntity.jobCategory.eq(jobCategory);
  }

  private BooleanBuilder eqTechStack(List<TechStack> techStacks) {

    if (techStacks == null || techStacks.isEmpty()) {
      return null;
    }

    BooleanBuilder booleanBuilder = new BooleanBuilder();
    for (TechStack techStack : techStacks) {
      booleanBuilder.and(jobPostingTechStackEntity.techName.eq(techStack));
    }
    return booleanBuilder;
  }

  private BooleanExpression eqEmploymentType(String employmentType) {

    if (employmentType == null) {
      return null;
    }
    return jobPostingEntity.employmentType.eq(employmentType);
  }

  private BooleanExpression eqCareer(Integer minCareer, Integer maxCareer) {

    if (minCareer == null && maxCareer == null) {
      return null;
    } else if (minCareer != null && maxCareer == null) {
      return jobPostingEntity.career.goe(minCareer);
    } else if (minCareer == null && maxCareer != null) {
      return jobPostingEntity.career.loe(maxCareer);
    } else {
      return jobPostingEntity.career.between(minCareer, maxCareer);
    }
  }

  private BooleanExpression eqEducation(Education education) {

    if (education == null) {
      return null;
    }
    return jobPostingEntity.education.eq(education);
  }
}