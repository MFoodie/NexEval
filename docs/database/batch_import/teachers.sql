SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 由 teachers.xlsx 生成：users + teacher
INSERT IGNORE INTO users (id, name, sex, type, password, phone, email) VALUES
('101010101', '甲', TRUE, 'teacher', '123456', '00000000000', NULL),
('101010102', '乙', FALSE, 'teacher', '123456', '00000000000', NULL),
('101010103', '丙', TRUE, 'teacher', '123456', '00000000000', NULL),
('101010104', '丁', FALSE, 'teacher', '123456', '00000000000', NULL),
('101010105', '戊', TRUE, 'teacher', '123456', '00000000000', NULL),
('101010106', '己', TRUE, 'teacher', '123456', '00000000000', NULL),
('101010107', '庚', FALSE, 'teacher', '123456', '00000000000', NULL),
('101010108', '辛', TRUE, 'teacher', '123456', '00000000000', NULL),
('101010109', '壬', FALSE, 'teacher', '123456', '00000000000', NULL),
('101010110', '癸', TRUE, 'teacher', '123456', '00000000000', NULL);

INSERT IGNORE INTO teacher (id, eid, enteryear, title, department) VALUES
('101010101', '09T09011', 2009, 'associate_professor', '计算机学院'),
('101010102', '09T00012', 2000, 'professor', '计算机学院'),
('101010103', '09T12023', 2012, 'associate_professor', '计算机学院'),
('101010104', '57T20011', 2020, 'lecture', '网安学院'),
('101010105', '04T21001', 2021, 'lecture', '信息学院'),
('101010106', '07T95002', 1995, 'professor', '数学学院'),
('101010107', '07T07001', 2007, 'associate_professor', '数学学院'),
('101010108', '10T10001', 2010, 'associate_professor', '物理学院'),
('101010109', '09T98001', 1998, 'professor', '计算机学院'),
('101010110', '17T99001', 1999, 'professor', '外国语学院');

