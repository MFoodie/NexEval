SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Add IRT parameters to question tables if missing
SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'question_bank'
    AND column_name = 'difficulty_b'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE question_bank ADD COLUMN difficulty_b decimal(4,2) NOT NULL DEFAULT 0.00 AFTER difficulty',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'question_bank'
    AND column_name = 'discrimination_a'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE question_bank ADD COLUMN discrimination_a decimal(4,2) NOT NULL DEFAULT 1.00 AFTER difficulty_b',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'judge_question_bank'
    AND column_name = 'difficulty_b'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE judge_question_bank ADD COLUMN difficulty_b decimal(4,2) NOT NULL DEFAULT 0.00 AFTER difficulty',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'judge_question_bank'
    AND column_name = 'discrimination_a'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE judge_question_bank ADD COLUMN discrimination_a decimal(4,2) NOT NULL DEFAULT 1.00 AFTER difficulty_b',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'blank_question_bank'
    AND column_name = 'difficulty_b'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE blank_question_bank ADD COLUMN difficulty_b decimal(4,2) NOT NULL DEFAULT 0.00 AFTER difficulty',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'blank_question_bank'
    AND column_name = 'discrimination_a'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE blank_question_bank ADD COLUMN discrimination_a decimal(4,2) NOT NULL DEFAULT 1.00 AFTER difficulty_b',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'essay_question_bank'
    AND column_name = 'difficulty_b'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE essay_question_bank ADD COLUMN difficulty_b decimal(4,2) NOT NULL DEFAULT 0.00 AFTER difficulty',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'essay_question_bank'
    AND column_name = 'discrimination_a'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE essay_question_bank ADD COLUMN discrimination_a decimal(4,2) NOT NULL DEFAULT 1.00 AFTER difficulty_b',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Initialize b/a from legacy difficulty
UPDATE question_bank
SET difficulty_b = CASE
  WHEN difficulty <= 2.0 THEN -1.0
  WHEN difficulty <= 3.5 THEN 0.0
  ELSE 1.0
END,
  discrimination_a = 1.0;

UPDATE judge_question_bank
SET difficulty_b = CASE
  WHEN difficulty <= 2.0 THEN -1.0
  WHEN difficulty <= 3.5 THEN 0.0
  ELSE 1.0
END,
  discrimination_a = 1.0;

UPDATE blank_question_bank
SET difficulty_b = CASE
  WHEN difficulty <= 2.0 THEN -1.0
  WHEN difficulty <= 3.5 THEN 0.0
  ELSE 1.0
END,
  discrimination_a = 1.0;

UPDATE essay_question_bank
SET difficulty_b = CASE
  WHEN difficulty <= 2.0 THEN -1.0
  WHEN difficulty <= 3.5 THEN 0.0
  ELSE 1.0
END,
  discrimination_a = 1.0;
