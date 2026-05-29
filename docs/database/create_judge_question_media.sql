CREATE TABLE IF NOT EXISTS judge_question_media (
    question_id varchar(32) NOT NULL,
    image_path varchar(255) NOT NULL,
    image_mode decimal(3,2) DEFAULT NULL,
    PRIMARY KEY (question_id),
    CONSTRAINT fk_judge_question_media_question
        FOREIGN KEY (question_id) REFERENCES judge_question_bank(id)
        ON DELETE CASCADE
);
