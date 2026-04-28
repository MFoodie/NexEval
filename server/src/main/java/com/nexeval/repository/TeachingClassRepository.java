package com.nexeval.repository;

import com.nexeval.model.TeachingClass;
import com.nexeval.model.TeachingClassId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeachingClassRepository extends JpaRepository<TeachingClass, TeachingClassId> {

	@Query("""
		select tc.id.cno as cno, c.cname as cname, tc.id.eid as eid
		from TeachingClass tc
		join Course c on c.cno = tc.id.cno
		where tc.id.eid = :eid
		order by tc.id.cno
		""")
	List<TeacherClassRow> findTeacherClasses(@Param("eid") String eid);

	interface TeacherClassRow {
		String getCno();

		String getCname();

		String getEid();
	}
}