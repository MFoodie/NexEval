SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS exam_answer (
    id bigint NOT NULL AUTO_INCREMENT,
    session_id varchar(32) NOT NULL,
    user_id varchar(32) NOT NULL,
    course_no char(8),
    question_id varchar(32) NOT NULL,
    question_type varchar(16) NOT NULL,
    answer_text varchar(1024),
    answer_image_path varchar(255),
    correct boolean,
    score int,
    review_note varchar(255),
    reviewed boolean NOT NULL DEFAULT false,
    reviewer_id varchar(32),
    answered_at datetime NOT NULL,
    reviewed_at datetime,
    PRIMARY KEY (id),
    UNIQUE KEY uq_exam_answer_session_question (session_id, question_id)
);

CREATE INDEX idx_exam_answer_session ON exam_answer(session_id);
CREATE INDEX idx_exam_answer_course ON exam_answer(course_no);

SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
        AND table_name = 'exam_answer'
        AND column_name = 'answer_image_path'
);
SET @sql := IF(
    @col_exists = 0,
    'ALTER TABLE exam_answer ADD COLUMN answer_image_path varchar(255) DEFAULT NULL AFTER answer_text',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
