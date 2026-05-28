CREATE TABLE IF NOT EXISTS paper_question_item (
    id bigint NOT NULL AUTO_INCREMENT,
    paper_id varchar(32) NOT NULL,
    question_id varchar(32) NOT NULL,
    question_type varchar(16) NOT NULL,
    display_order int NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_paper_id (paper_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
