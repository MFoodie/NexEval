SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'question_bank'
    AND column_name = 'image_path'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE question_bank ADD COLUMN image_path varchar(255) DEFAULT NULL AFTER stem',
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
    AND column_name = 'image_mode'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE question_bank ADD COLUMN image_mode decimal(3,2) DEFAULT NULL AFTER image_path',
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
    AND column_name = 'image_path'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE blank_question_bank ADD COLUMN image_path varchar(255) DEFAULT NULL AFTER stem',
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
    AND column_name = 'image_mode'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE blank_question_bank ADD COLUMN image_mode decimal(3,2) DEFAULT NULL AFTER image_path',
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
    AND column_name = 'image_path'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE essay_question_bank ADD COLUMN image_path varchar(255) DEFAULT NULL AFTER stem',
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
    AND column_name = 'image_mode'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE essay_question_bank ADD COLUMN image_mode decimal(3,2) DEFAULT NULL AFTER image_path',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO blank_question_bank (id, stem, image_path, image_mode, answer_key, points, difficulty, cno, active)
VALUES (
  'F_IMG_001',
  '【知识点：最小生成树】该图的最小生成树的权值是 ____。',
  '/fig/0.png',
  0.30,
  '99',
  3,
  2.0,
  'BJSL0002',
  true
)
ON DUPLICATE KEY UPDATE
  stem = VALUES(stem),
  image_path = VALUES(image_path),
  image_mode = VALUES(image_mode),
  answer_key = VALUES(answer_key),
  points = VALUES(points),
  difficulty = VALUES(difficulty),
  cno = VALUES(cno),
  active = VALUES(active);

INSERT INTO essay_question_bank (id, stem, image_path, image_mode, points, difficulty, cno, active)
VALUES (
  'E_IMG_001',
  '【知识点：Cache】回答下面的问题',
  '/fig/4.png',
  1.00,
  10,
  4.0,
  'BJSL0001',
  true
)
ON DUPLICATE KEY UPDATE
  stem = VALUES(stem),
  image_path = VALUES(image_path),
  image_mode = VALUES(image_mode),
  points = VALUES(points),
  difficulty = VALUES(difficulty),
  cno = VALUES(cno),
  active = VALUES(active);

INSERT INTO essay_question_bank (id, stem, image_path, image_mode, points, difficulty, cno, active)
VALUES (
  'E_IMG_002',
  '【知识点：最小生成树】分别使用 Kruskal 算法和 Prim 算法计算它的最小生成树，并给出求解过程',
  '/fig/1.png',
  0.35,
  8,
  4.0,
  'BJSL0002',
  true
)
ON DUPLICATE KEY UPDATE
  stem = VALUES(stem),
  image_path = VALUES(image_path),
  image_mode = VALUES(image_mode),
  points = VALUES(points),
  difficulty = VALUES(difficulty),
  cno = VALUES(cno),
  active = VALUES(active);

INSERT INTO essay_question_bank (id, stem, image_path, image_mode, points, difficulty, cno, active)
VALUES (
  'E_IMG_003',
  '【知识点：最短路径】分别使用 Dijkstra 算法和 Floyd 算法计算图中的节点A到其余节点的最短路径 ，并给出求解过程',
  '/fig/1.png',
  0.35,
  12,
  4.0,
  'BJSL0002',
  true
)
ON DUPLICATE KEY UPDATE
  stem = VALUES(stem),
  image_path = VALUES(image_path),
  image_mode = VALUES(image_mode),
  points = VALUES(points),
  difficulty = VALUES(difficulty),
  cno = VALUES(cno),
  active = VALUES(active);

INSERT INTO essay_question_bank (id, stem, image_path, image_mode, points, difficulty, cno, active)
VALUES (
  'E_IMG_004',
  '【知识点：Cache】回答此题并给出求解过程',
  '/fig/5.png',
  1.00,
  15,
  4.0,
  'BJSL0001',
  true
)
ON DUPLICATE KEY UPDATE
  stem = VALUES(stem),
  image_path = VALUES(image_path),
  image_mode = VALUES(image_mode),
  points = VALUES(points),
  difficulty = VALUES(difficulty),
  cno = VALUES(cno),
  active = VALUES(active);

INSERT INTO essay_question_bank (id, stem, image_path, image_mode, points, difficulty, cno, active)
VALUES (
  'E_IMG_005',
  '【知识点：曲面积分】回答此题并给出求解过程',
  '/fig/6.png',
  1.00,
  10,
  4.0,
  'B07M0001',
  true
)
ON DUPLICATE KEY UPDATE
  stem = VALUES(stem),
  image_path = VALUES(image_path),
  image_mode = VALUES(image_mode),
  points = VALUES(points),
  difficulty = VALUES(difficulty),
  cno = VALUES(cno),
  active = VALUES(active);

INSERT INTO essay_question_bank (id, stem, image_path, image_mode, points, difficulty, cno, active)
VALUES (
  'E_IMG_006',
  '【知识点：曲线积分】回答此题并给出求解过程',
  '/fig/7.png',
  1.00,
  10,
  4.0,
  'B07M0001',
  true
)
ON DUPLICATE KEY UPDATE
  stem = VALUES(stem),
  image_path = VALUES(image_path),
  image_mode = VALUES(image_mode),
  points = VALUES(points),
  difficulty = VALUES(difficulty),
  cno = VALUES(cno),
  active = VALUES(active);

INSERT INTO blank_question_bank (id, stem, image_path, image_mode, answer_key, points, difficulty, cno, active)
VALUES (
  'F_IMG_002',
  '【知识点：CPI】计算它的有效 CPI 为 ____ （保留3位有效数字）。',
  '/fig/3.png',
  0.45,
  '1.55',
  3,
  2.0,
  'BJSL0001',
  true
)
ON DUPLICATE KEY UPDATE
  stem = VALUES(stem),
  image_path = VALUES(image_path),
  image_mode = VALUES(image_mode),
  answer_key = VALUES(answer_key),
  points = VALUES(points),
  difficulty = VALUES(difficulty),
  cno = VALUES(cno),
  active = VALUES(active);