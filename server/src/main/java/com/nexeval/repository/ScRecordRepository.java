package com.nexeval.repository;

import com.nexeval.model.ScRecord;
import com.nexeval.model.ScRecordId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScRecordRepository extends JpaRepository<ScRecord, ScRecordId> {

	@Query("""
		select u.id as userId, s.sno as sno, u.name as name, u.sex as sex, sc.grade as grade
		from ScRecord sc
		join StudentProfile s on s.sno = sc.id.sno
		join UserAccount u on u.id = s.id
		where sc.id.cno = :cno and sc.id.eid = :eid
		order by s.sno
		""")
	List<ClassStudentRow> findClassStudents(@Param("cno") String cno, @Param("eid") String eid);

	@Query("""
		select sc.id.cno as cno, c.cname as cname, sc.id.eid as eid, u.name as teacherName
		from ScRecord sc
		join Course c on c.cno = sc.id.cno
		join TeacherProfile t on t.eid = sc.id.eid
		join UserAccount u on u.id = t.id
		where sc.id.sno = :sno
		order by sc.id.cno
		""")
	List<StudentClassRow> findStudentClasses(@Param("sno") String sno);

	interface ClassStudentRow {
		String getUserId();

		String getSno();

		String getName();

		boolean isSex();

		Integer getGrade();
	}

	interface StudentClassRow {
		String getCno();

		String getCname();

		String getEid();

		String getTeacherName();
	}
}