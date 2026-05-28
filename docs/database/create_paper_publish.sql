CREATE TABLE IF NOT EXISTS paper_publish (
    id bigint NOT NULL AUTO_INCREMENT,
    definition_id varchar(32) NOT NULL,
    cno char(8) NOT NULL,
    eid char(8) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_paper_class (definition_id, cno, eid),
    INDEX idx_cno_eid (cno, eid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
