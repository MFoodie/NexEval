SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS score_appeal (
    id bigint NOT NULL AUTO_INCREMENT,
    session_id varchar(32) NOT NULL,
    user_id varchar(32) NOT NULL,
    course_no char(8) NOT NULL,
    reason varchar(1024),
    status varchar(16) NOT NULL,
    created_at datetime NOT NULL,
    handled_at datetime,
    handled_by varchar(32),
    handled_note varchar(255),
    PRIMARY KEY (id),
    UNIQUE KEY uq_score_appeal_session (session_id)
);

CREATE INDEX idx_score_appeal_status ON score_appeal(status);
CREATE INDEX idx_score_appeal_user ON score_appeal(user_id);
CREATE INDEX idx_score_appeal_course ON score_appeal(course_no);
