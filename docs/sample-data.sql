-- =============================================================================
-- FactoryFlow Mini MES 시연·포트폴리오용 샘플 데이터 (골판지 상자 제조 현장 시나리오)
-- =============================================================================
-- 사전 조건:
--   1. Spring Boot 백엔드를 최소 1회 실행해 테이블이 자동 생성된 상태여야 합니다.
--   2. 비파괴·멱등(Idempotent) 스크립트로 작성되어 여러 번 실행해도 기존 데이터를
--      삭제하거나 중복 생성하지 않습니다. (DROP / TRUNCATE / DELETE 미포함)
--   3. CURDATE() / NOW() 기준 상대 날짜를 사용하므로 언제 실행해도 대시보드와
--      날짜별 생산실적 이력이 최신 날짜 기준으로 표시됩니다.
--
-- 실행 예시 (Windows PowerShell):
--   Get-Content docs/sample-data.sql -Raw -Encoding UTF8 | mysql -u factoryflow_user -p --default-character-set=utf8mb4 factoryflow
-- 실행 예시 (macOS / Linux / Git Bash):
--   mysql -u factoryflow_user -p --default-character-set=utf8mb4 factoryflow < docs/sample-data.sql
-- =============================================================================

USE factoryflow;

-- -----------------------------------------------------------------------------
-- 1. 거래처 (매입처 2, 납품처 2, 겸용 1)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO partners (
    partner_code, partner_name, partner_type, business_number, phone, address, active, created_at, updated_at
) VALUES
('PT-SUP-001', '(주)한빛골판지제지', 'SUPPLIER', '123-81-45678', '031-411-2200', '경기도 안산시 단원구 공단로 120', 1, NOW(), NOW()),
('PT-SUP-002', '대성화성케미칼',   'SUPPLIER', '214-86-33901', '032-522-8810', '인천광역시 서구 가좌로 85',      1, NOW(), NOW()),
('PT-CUS-001', '프레시마켓물류',   'CUSTOMER', '312-81-77412', '02-6200-4100', '경기도 김포시 고촌읍 물류단지 42', 1, NOW(), NOW()),
('PT-CUS-002', '청송농협산지유통', 'CUSTOMER', '508-82-10455', '054-873-5500', '경상북도 청송군 청송읍 중앙로 19', 1, NOW(), NOW()),
('PT-BTH-001', '코리아패키징유통', 'BOTH',     '119-86-99210', '031-795-3300', '경기도 하남시 미사대로 510',     1, NOW(), NOW());

-- -----------------------------------------------------------------------------
-- 2. 품목 (원재료 4종 + 완제품 3종)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO items (
    item_code, item_name, item_type, spec, unit, safety_stock, active, created_at, updated_at
) VALUES
('RM-SHEET-B',  'B골 골판지 원단',   'RAW_MATERIAL',  '1200x900mm / 3mm',  'SHEET', 300, 1, NOW(), NOW()),
('RM-SHEET-AB', 'AB골 이중 원단',    'RAW_MATERIAL',  '1400x1000mm / 8mm', 'SHEET', 200, 1, NOW(), NOW()),
('RM-GLUE-01',  '전분 접착제',       'RAW_MATERIAL',  '20kg 캔',           'KG',     50, 1, NOW(), NOW()),
('RM-INK-BLK',  '수성 인쇄 잉크(흑)', 'RAW_MATERIAL',  '18kg 통',           'KG',     30, 1, NOW(), NOW()),
('FG-BOX-S01',  '택배박스 A형(소)',   'FINISHED_GOOD', '300x200x150mm',     'EA',    500, 1, NOW(), NOW()),
('FG-BOX-AGR',  '농산물 사과박스(10kg)', 'FINISHED_GOOD', '480x330x290mm',  'EA',    300, 1, NOW(), NOW()),
('FG-BOX-ELE',  '전자부품 이중박스',   'FINISHED_GOOD', '520x400x320mm',     'EA',    200, 1, NOW(), NOW());

-- -----------------------------------------------------------------------------
-- 3. BOM (완제품 1개 생산 시 필요한 원재료 소요량)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO boms (finished_item_id, raw_material_id, required_quantity, created_at, updated_at)
SELECT f.id, r.id, 1, NOW(), NOW()
FROM items f, items r
WHERE f.item_code = 'FG-BOX-S01' AND r.item_code = 'RM-SHEET-B';

INSERT IGNORE INTO boms (finished_item_id, raw_material_id, required_quantity, created_at, updated_at)
SELECT f.id, r.id, 1, NOW(), NOW()
FROM items f, items r
WHERE f.item_code = 'FG-BOX-AGR' AND r.item_code = 'RM-SHEET-AB';

INSERT IGNORE INTO boms (finished_item_id, raw_material_id, required_quantity, created_at, updated_at)
SELECT f.id, r.id, 1, NOW(), NOW()
FROM items f, items r
WHERE f.item_code = 'FG-BOX-AGR' AND r.item_code = 'RM-INK-BLK';

INSERT IGNORE INTO boms (finished_item_id, raw_material_id, required_quantity, created_at, updated_at)
SELECT f.id, r.id, 2, NOW(), NOW()
FROM items f, items r
WHERE f.item_code = 'FG-BOX-ELE' AND r.item_code = 'RM-SHEET-AB';

INSERT IGNORE INTO boms (finished_item_id, raw_material_id, required_quantity, created_at, updated_at)
SELECT f.id, r.id, 1, NOW(), NOW()
FROM items f, items r
WHERE f.item_code = 'FG-BOX-ELE' AND r.item_code = 'RM-GLUE-01';

-- -----------------------------------------------------------------------------
-- 4. 현재고 (일부 품목은 안전재고 미만으로 설정하여 대시보드 경고 시연)
--   - RM-SHEET-AB: 현재고 85 < 안전재고 200 (안전재고 미만 경고!)
--   - RM-INK-BLK : 현재고 18 < 안전재고 30  (안전재고 미만 경고!)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO inventories (item_id, quantity, version, created_at, updated_at)
SELECT id, 1250, 0, NOW(), NOW() FROM items WHERE item_code = 'RM-SHEET-B';

INSERT IGNORE INTO inventories (item_id, quantity, version, created_at, updated_at)
SELECT id, 85, 0, NOW(), NOW() FROM items WHERE item_code = 'RM-SHEET-AB';

INSERT IGNORE INTO inventories (item_id, quantity, version, created_at, updated_at)
SELECT id, 140, 0, NOW(), NOW() FROM items WHERE item_code = 'RM-GLUE-01';

INSERT IGNORE INTO inventories (item_id, quantity, version, created_at, updated_at)
SELECT id, 18, 0, NOW(), NOW() FROM items WHERE item_code = 'RM-INK-BLK';

INSERT IGNORE INTO inventories (item_id, quantity, version, created_at, updated_at)
SELECT id, 820, 0, NOW(), NOW() FROM items WHERE item_code = 'FG-BOX-S01';

INSERT IGNORE INTO inventories (item_id, quantity, version, created_at, updated_at)
SELECT id, 410, 0, NOW(), NOW() FROM items WHERE item_code = 'FG-BOX-AGR';

INSERT IGNORE INTO inventories (item_id, quantity, version, created_at, updated_at)
SELECT id, 260, 0, NOW(), NOW() FROM items WHERE item_code = 'FG-BOX-ELE';

-- -----------------------------------------------------------------------------
-- 5. 작업지시 (COMPLETED 1 / IN_PROGRESS 1 / PAUSED 1 / WAITING 1)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO work_orders (
    order_number, item_id, target_quantity, produced_good_quantity, produced_defect_quantity,
    status, planned_date, remark, pause_reason, paused_at, version, created_at, updated_at
)
SELECT
    'WO-DEMO-001', id, 500, 500, 12,
    'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 1 DAY), '프레시마켓 정기 납품분 완료',
    NULL, NULL, 0, DATE_SUB(NOW(), INTERVAL 26 HOUR), DATE_SUB(NOW(), INTERVAL 18 HOUR)
FROM items WHERE item_code = 'FG-BOX-S01';

INSERT IGNORE INTO work_orders (
    order_number, item_id, target_quantity, produced_good_quantity, produced_defect_quantity,
    status, planned_date, remark, pause_reason, paused_at, version, created_at, updated_at
)
SELECT
    'WO-DEMO-002', id, 400, 260, 6,
    'IN_PROGRESS', CURDATE(), '오후 1라인 택배박스 A형 추가 생산',
    NULL, NULL, 0, DATE_SUB(NOW(), INTERVAL 4 HOUR), NOW()
FROM items WHERE item_code = 'FG-BOX-S01';

INSERT IGNORE INTO work_orders (
    order_number, item_id, target_quantity, produced_good_quantity, produced_defect_quantity,
    status, planned_date, remark, pause_reason, paused_at, version, created_at, updated_at
)
SELECT
    'WO-DEMO-003', id, 300, 120, 5,
    'PAUSED', CURDATE(), '청송농협 긴급 출하용 사과박스 2라인',
    '원자재 부족 (AB골 이중 원단 입고 대기)', DATE_SUB(NOW(), INTERVAL 35 MINUTE),
    0, DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_SUB(NOW(), INTERVAL 35 MINUTE)
FROM items WHERE item_code = 'FG-BOX-AGR';

INSERT IGNORE INTO work_orders (
    order_number, item_id, target_quantity, produced_good_quantity, produced_defect_quantity,
    status, planned_date, remark, pause_reason, paused_at, version, created_at, updated_at
)
SELECT
    'WO-DEMO-004', id, 250, 0, 0,
    'WAITING', DATE_ADD(CURDATE(), INTERVAL 1 DAY), '명일 오전 전자부품 이중박스 생산 예정',
    NULL, NULL, 0, NOW(), NOW()
FROM items WHERE item_code = 'FG-BOX-ELE';

-- -----------------------------------------------------------------------------
-- 6. 생산실적 이력 (그제 / 어제 / 오늘 분산 -> 날짜별 그룹화 & 대시보드 오늘 집계 시연)
-- -----------------------------------------------------------------------------
INSERT INTO production_results (work_order_id, good_quantity, defect_quantity, worker_name, remark, created_at, updated_at)
SELECT wo.id, 220, 5, '김철수', '[DEMO] 1차 절단·접합 완료', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)
FROM work_orders wo
WHERE wo.order_number = 'WO-DEMO-001'
  AND NOT EXISTS (
      SELECT 1 FROM production_results pr WHERE pr.work_order_id = wo.id AND pr.remark = '[DEMO] 1차 절단·접합 완료'
  );

INSERT INTO production_results (work_order_id, good_quantity, defect_quantity, worker_name, remark, created_at, updated_at)
SELECT wo.id, 280, 7, '이영희', '[DEMO] 2차 최종 포장 및 목표 달성', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)
FROM work_orders wo
WHERE wo.order_number = 'WO-DEMO-001'
  AND NOT EXISTS (
      SELECT 1 FROM production_results pr WHERE pr.work_order_id = wo.id AND pr.remark = '[DEMO] 2차 최종 포장 및 목표 달성'
  );

INSERT INTO production_results (work_order_id, good_quantity, defect_quantity, worker_name, remark, created_at, updated_at)
SELECT wo.id, 150, 3, '김철수', '[DEMO] 금일 오전 1라인 1차 실적', DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 3 HOUR)
FROM work_orders wo
WHERE wo.order_number = 'WO-DEMO-002'
  AND NOT EXISTS (
      SELECT 1 FROM production_results pr WHERE pr.work_order_id = wo.id AND pr.remark = '[DEMO] 금일 오전 1라인 1차 실적'
  );

INSERT INTO production_results (work_order_id, good_quantity, defect_quantity, worker_name, remark, created_at, updated_at)
SELECT wo.id, 110, 3, '박민수', '[DEMO] 금일 오후 1라인 2차 실적', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR)
FROM work_orders wo
WHERE wo.order_number = 'WO-DEMO-002'
  AND NOT EXISTS (
      SELECT 1 FROM production_results pr WHERE pr.work_order_id = wo.id AND pr.remark = '[DEMO] 금일 오후 1라인 2차 실적'
  );

INSERT INTO production_results (work_order_id, good_quantity, defect_quantity, worker_name, remark, created_at, updated_at)
SELECT wo.id, 120, 5, '이영희', '[DEMO] 2라인 사과박스 1차 생산 후 원단 부족 발생', DATE_SUB(NOW(), INTERVAL 50 MINUTE), DATE_SUB(NOW(), INTERVAL 50 MINUTE)
FROM work_orders wo
WHERE wo.order_number = 'WO-DEMO-003'
  AND NOT EXISTS (
      SELECT 1 FROM production_results pr WHERE pr.work_order_id = wo.id AND pr.remark = '[DEMO] 2라인 사과박스 1차 생산 후 원단 부족 발생'
  );

-- -----------------------------------------------------------------------------
-- 7. 재고 입출고 수불 이력 (최근 이력 대시보드 및 재고 이력 화면 시연)
-- -----------------------------------------------------------------------------
INSERT INTO inventory_histories (
    item_id, partner_id, transaction_type, before_quantity, change_quantity, after_quantity, remark, created_at, updated_at
)
SELECT i.id, p.id, 'INBOUND', 500, 1000, 1500, '[DEMO] B골 원단 정기 입고', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)
FROM items i, partners p
WHERE i.item_code = 'RM-SHEET-B' AND p.partner_code = 'PT-SUP-001'
  AND NOT EXISTS (SELECT 1 FROM inventory_histories WHERE remark = '[DEMO] B골 원단 정기 입고');

INSERT INTO inventory_histories (
    item_id, partner_id, transaction_type, before_quantity, change_quantity, after_quantity, remark, created_at, updated_at
)
SELECT i.id, NULL, 'PRODUCTION_OUT', 1500, -250, 1250, '[DEMO] 작업지시 WO-DEMO-002 생산투입 차감', DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR)
FROM items i
WHERE i.item_code = 'RM-SHEET-B'
  AND NOT EXISTS (SELECT 1 FROM inventory_histories WHERE remark = '[DEMO] 작업지시 WO-DEMO-002 생산투입 차감');

INSERT INTO inventory_histories (
    item_id, partner_id, transaction_type, before_quantity, change_quantity, after_quantity, remark, created_at, updated_at
)
SELECT i.id, NULL, 'PRODUCTION_IN', 560, 260, 820, '[DEMO] 작업지시 WO-DEMO-002 완제품 생산입고', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR)
FROM items i
WHERE i.item_code = 'FG-BOX-S01'
  AND NOT EXISTS (SELECT 1 FROM inventory_histories WHERE remark = '[DEMO] 작업지시 WO-DEMO-002 완제품 생산입고');

INSERT INTO inventory_histories (
    item_id, partner_id, transaction_type, before_quantity, change_quantity, after_quantity, remark, created_at, updated_at
)
SELECT i.id, NULL, 'PRODUCTION_OUT', 210, -125, 85, '[DEMO] 작업지시 WO-DEMO-003 AB골 원단 투입 (안전재고 미만 도달)', DATE_SUB(NOW(), INTERVAL 50 MINUTE), DATE_SUB(NOW(), INTERVAL 50 MINUTE)
FROM items i
WHERE i.item_code = 'RM-SHEET-AB'
  AND NOT EXISTS (SELECT 1 FROM inventory_histories WHERE remark = '[DEMO] 작업지시 WO-DEMO-003 AB골 원단 투입 (안전재고 미만 도달)');

INSERT INTO inventory_histories (
    item_id, partner_id, transaction_type, before_quantity, change_quantity, after_quantity, remark, created_at, updated_at
)
SELECT i.id, p.id, 'OUTBOUND', 610, -200, 410, '[DEMO] 청송농협 사과박스 1차 출하', DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 30 MINUTE)
FROM items i, partners p
WHERE i.item_code = 'FG-BOX-AGR' AND p.partner_code = 'PT-CUS-002'
  AND NOT EXISTS (SELECT 1 FROM inventory_histories WHERE remark = '[DEMO] 청송농협 사과박스 1차 출하');

-- -----------------------------------------------------------------------------
-- 8. 적재 결과 확인 (읽기 전용 조회)
-- -----------------------------------------------------------------------------
SELECT 'partners' AS table_name, COUNT(*) AS row_count FROM partners
UNION ALL SELECT 'items', COUNT(*) FROM items
UNION ALL SELECT 'boms', COUNT(*) FROM boms
UNION ALL SELECT 'inventories', COUNT(*) FROM inventories
UNION ALL SELECT 'work_orders', COUNT(*) FROM work_orders
UNION ALL SELECT 'production_results', COUNT(*) FROM production_results
UNION ALL SELECT 'inventory_histories', COUNT(*) FROM inventory_histories;
