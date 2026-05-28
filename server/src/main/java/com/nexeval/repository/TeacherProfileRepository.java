package com.nexeval.repository;

import com.nexeval.dto.TeacherExamPermView;
import com.nexeval.dto.TeacherVipView;
import com.nexeval.model.TeacherProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, String> {

  Optional<TeacherProfile> findFirstById(String id);

  Optional<TeacherProfile> findFirstByEid(String eid);

  @Query("""
    select new com.nexeval.dto.TeacherVipView(
      t.eid,
      u.id,
      u.name,
      t.title,
      t.department,
      t.vip
    )
    from TeacherProfile t, UserAccount u
    where u.id = t.id
      and (
        :keyword is null
        or :keyword = ''
        or lower(u.id) like lower(concat('%', :keyword, '%'))
        or lower(u.name) like lower(concat('%', :keyword, '%'))
        or lower(t.eid) like lower(concat('%', :keyword, '%'))
      )
      and (
        :vipStatus = 'all'
        or (:vipStatus = 'vip' and t.vip = true)
        or (:vipStatus = 'nonvip' and t.vip = false)
      )
    order by t.eid
  """)
  List<TeacherVipView> findTeacherVipViews(
    @Param("keyword") String keyword,
    @Param("vipStatus") String vipStatus
  );

  @Query("""
    select new com.nexeval.dto.TeacherExamPermView(
      t.eid,
      u.id,
      u.name,
      t.title,
      t.department,
      t.canCreateExam
    )
    from TeacherProfile t, UserAccount u
    where u.id = t.id
      and (
        :keyword is null
        or :keyword = ''
        or lower(u.id) like lower(concat('%', :keyword, '%'))
        or lower(u.name) like lower(concat('%', :keyword, '%'))
        or lower(t.eid) like lower(concat('%', :keyword, '%'))
      )
      and (
        :permStatus = 'all'
        or (:permStatus = 'permitted' and t.canCreateExam = true)
        or (:permStatus = 'not_permitted' and t.canCreateExam = false)
      )
    order by t.eid
  """)
  List<TeacherExamPermView> findTeacherExamPermViews(
    @Param("keyword") String keyword,
    @Param("permStatus") String permStatus
  );
}
