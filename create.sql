CREATE TABLE users (
    id CHAR(9) NOT NULL,
    name VARCHAR(20) NOT NULL,
    sex BOOLEAN NOT NULL,
    type ENUM('student', 'teacher', 'admin') NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone CHAR(11) NOT NULL,
    email VARCHAR(254),
    PRIMARY KEY (id)
);

INSERT INTO users (id, name, sex, type, password, phone,email) 
VALUES ('213230001', '张三', TRUE, 'student', '123456', '12345678910', NULL),
('213230002', '李四', False, 'student', '123456', '12345678910', NULL),
('101010001', '李刚', TRUE, 'teacher', '123456', '12345678910', NULL),
('101010002', '王红', False, 'teacher', '123456', '12345678910', NULL),
('000000001', '甲', TRUE, 'admin', '123456', '12345678910', NULL),
('000000002', '乙', FALSE, 'admin', '123456', '12345678910', NULL);