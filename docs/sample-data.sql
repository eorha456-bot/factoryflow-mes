-- =============================================================================
-- FactoryFlow Mini MES 시연·포트폴리오용 샘플 데이터 (골판지 상자 제조 현장 시나리오)
-- =============================================================================
-- 구성 요약 (모든 핵심 기능·상태·필터를 한눈에 시연할 수 있도록 설계):
--   1. 거래처 6건      : 매입처 2, 납품처 2, 겸용 1, 비활성 1
--   2. 품목 10건       : 원재료 5, 완제품 4, 비활성(단종) 1
--   3. BOM 9건         : 완제품 4종 × 다중 원재료(원단·접착제·잉크) 매핑
--   4. 현재고 10건     : 안전재고 미만 경고 3건 포함 (원재료 2 + 완제품 1)
--   5. 작업지시 8건    : 5대 상태 전체 포함 (완료 2, 작업중 2, 일시정지 2, 대기 1, 취소 1)
--   6. 생산실적 10건   : 5일 전·2일 전·어제·오늘 4개 날짜 분산 (날짜별 분류·기간 필터 시연)
--   7. 수불 이력 12건  : 5대 유형 전체 포함 (입고·출고·생산입고·생산투입·실사조정)
--
-- 안전 원칙:
--   - 비파괴·멱등(Idempotent) 스크립트로 DROP / TRUNCATE / DELETE 를 포함하지 않습니다.
--   - CURDATE() / NOW() 기준 상대 날짜를 사용하므로 언제 실행해도 대시보드와
--     날짜별 생산실적 이력이 최신 날짜 기준으로 표시됩니다.
--
-- 실행 예시 (Windows PowerShell):
--   Get-Content docs/sample-data.sql -Raw -Encoding UTF8 | mysql -u factoryflow_user -p --default-character-set=utf8mb4 factoryflow
-- 실행 예시 (macOS / Linux / Git Bash):
--   mysql -u factoryflow_user -p --default-character-set=utf8mb4 factoryflow < docs/sample-data.sql
-- =============================================================================

USE factoryflow;

-- -----------------------------------------------------------------------------
-- 1. 거래처 (매입처 2, 납품처 2, 겸용 1, 비활성 1)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO partners (
    partner_code, partner_name, partner_type, business_number, phone, address, active, created_at, updated_at
) VALUES
('PT-SUP-001', '(주)한빛골판지제지', 'SUPPLIER', '123-81-45678', '031-411-2200', '경기도 안산시 단원구 공단로 120', 1, NOW(), NOW()),
('PT-SUP-002', '대성화성케미칼',   'SUPPLIER', '214-86-33901', '032-522-8810', '인천광역시 서구 가좌로 85',      1, NOW(), NOW()),
('PT-CUS-001', '프레시마켓물류',   'CUSTOMER', '312-81-77412', '02-6200-4100', '경기도 김포시 고촌읍 물류단지 42', 1, NOW(), NOW()),
('PT-CUS-002', '청송농협산지유통', 'CUSTOMER', '508-82-10455', '054-873-5500', '경상북도 청송군 청송읍 중앙로 19', 1, NOW(), NOW()),
('PT-BTH-001', '코리아패키징유통', 'BOTH',     '119-86-99210', '031-795-3300', '경기도 하남시 미사대로 510',     1, NOW(), NOW()),
('PT-OLD-001', '(구)동양제지상사', 'SUPPLIER', '105-81-20911', '031-312-9000', '경기도 시흥시 정왕대로 77',      0, NOW(), NOW());

-- -----------------------------------------------------------------------------
-- 2. 품목 (원재료 5, 완제품 4, 비활성 단종 1)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO items (
    item_code, item_name, item_type, spec, unit, safety_stock, active, created_at, updated_at
) VALUES
('RM-SHEET-B',  'B골 골판지 원단',       'RAW_MATERIAL',  '1200x900mm / 3mm',  'SHEET', 300, 1, NOW(), NOW()),
('RM-SHEET-AB', 'AB골 이중 원단',        'RAW_MATERIAL',  '1400x1000mm / 8mm', 'SHEET', 200, 1, NOW(), NOW()),
('RM-SHEET-A',  'A골 완충 원단',         'RAW_MATERIAL',  '1300x950mm / 5mm',  'SHEET', 250, 1, NOW(), NOW()),
('RM-GLUE-01',  '전분 접착제',           'RAW_MATERIAL',  '20kg 캔',           'KG',     50, 1, NOW(), NOW()),
('RM-INK-BLK',  '수성 인쇄 잉크(흑)',     'RAW_MATERIAL',  '18kg 통',           'KG',     30, 1, NOW(), NOW()),
('FG-BOX-S01',  '택배박스 A형(소)',       'FINISHED_GOOD', '300x200x150mm',     'EA',    500, 1, NOW(), NOW()),
('FG-BOX-AGR',  '농산물 사과박스(10kg)',  'FINISHED_GOOD', '480x330x290mm',     'EA',    300, 1, NOW(), NOW()),
('FG-BOX-ELE',  '전자부품 이중박스',      'FINISHED_GOOD', '520x400x320mm',     'EA',    200, 1, NOW(), NOW()),
('FG-BOX-CLD',  '냉동식품 보냉박스',      'FINISHED_GOOD', '420x310x260mm',     'EA',    150, 1, NOW(), NOW()),
('FG-BOX-OLD',  '구형 의류박스(단종)',    'FINISHED_GOOD', '450x350x200mm',     'EA',      0, 0, NOW(), NOW());

-- -----------------------------------------------------------------------------
-- 3. BOM (완제품 4종 × 다중 원재료 소요량 매핑 9건)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO boms (finished_item_id, raw_material_id, required_quantity, created_at, updated_at)
SELECT f.id, r.id, 1, NOW(), NOW()
FROM items f, items r WHERE f.item_code = 'FG-BOX-S01' AND r.item_code = 'RM-SHEET-B';

INSERT IGNORE INTO boms (finished_item_id, raw_material_id, required_quantity, created_at, updated_at)
SELECT f.id, r.id, 1, NOW(), NOW()
FROM items f, items r WHERE f.item_code = 'FG-BOX-S01' AND r.item_code = 'RM-GLUE-01';

INSERT IGNORE INTO boms (finished_item_id, raw_material_id, required_quantity, created_at, updated_at)
SELECT f.id, r.id, 1, NOW(), NOW()
FROM items f, items r WHERE f.item_code = 'FG-BOX-AGR' AND r.item_code = 'RM-SHEET-AB';

INSERT IGNORE INTO boms (finished_item_id, raw_material_id, required_quantity, created_at, updated_at)
SELECT f.id, r.id, 1, NOW(), NOW()
FROM items f, items r WHERE f.item_code = 'FG-BOX-AGR' AND r.item_code = 'RM-INK-BLK';

INSERT IGNORE INTO boms (finished_item_id, raw_material_id, required_quantity, created_at, updated_at)
SELECT f.id, r.id, 2, NOW(), NOW()
FROM items f, items r WHERE f.item_code = 'FG-BOX-ELE' AND r.item_code = 'RM-SHEET-AB';

INSERT IGNORE INTO boms (finished_item_id, raw_material_id, required_quantity, created_at, updated_at)
SELECT f.id, r.id, 1, NOW(), NOW()
FROM items f, items r WHERE f.item_code = 'FG-BOX-ELE' AND r.item_code = 'RM-GLUE-01';

INSERT IGNORE INTO boms (finished_item_id, raw_material_id, required_quantity, created_at, updated_at)
SELECT f.id, r.id, 1, NOW(), NOW()
FROM items f, items r WHERE f.item_code = 'FG-BOX-CLD' AND r.item_code = 'RM-SHEET-A';

INSERT IGNORE INTO boms (finished_item_id, raw_material_id, required_quantity, created_at, updated_at)
SELECT f.id, r.id, 1, NOW(), NOW()
FROM items f, items r WHERE f.item_code = 'FG-BOX-CLD' AND r.item_code = 'RM-GLUE-01';

INSERT IGNORE INTO boms (finished_item_id, raw_material_id, required_quantity, created_at, updated_at)
SELECT f.id, r.id, 1, NOW(), NOW()
FROM items f, items r WHERE f.item_code = 'FG-BOX-CLD' AND r.item_code = 'RM-INK-BLK';

-- -----------------------------------------------------------------------------
-- 4. 현재고 (안전재고 미만 경고 3건 포함: RM-SHEET-AB, RM-INK-BLK, FG-BOX-CLD)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO inventories (item_id, quantity, version, created_at, updated_at)
SELECT id, 1450, 0, NOW(), NOW() FROM items WHERE item_code = 'RM-SHEET-B';

INSERT IGNORE INTO inventories (item_id, quantity, version, created_at, updated_at)
SELECT id, 85, 0, NOW(), NOW() FROM items WHERE item_code = 'RM-SHEET-AB';

INSERT IGNORE INTO inventories (item_id, quantity, version, created_at, updated_at)
SELECT id, 620, 0, NOW(), NOW() FROM items WHERE item_code = 'RM-SHEET-A';

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

INSERT IGNORE INTO inventories (item_id, quantity, version, created_at, updated_at)
SELECT id, 90, 0, NOW(), NOW() FROM items WHERE item_code = 'FG-BOX-CLD';

INSERT IGNORE INTO inventories (item_id, quantity, version, created_at, updated_at)
SELECT id, 0, 0, NOW(), NOW() FROM items WHERE item_code = 'FG-BOX-OLD';

-- -----------------------------------------------------------------------------
-- 5. 작업지시 8건 (COMPLETED 2 / IN_PROGRESS 2 / PAUSED 2 / WAITING 1 / CANCELED 1)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO work_orders (
    order_number, item_id, target_quantity, produced_good_quantity, produced_defect_quantity,
    status, planned_date, remark, pause_reason, paused_at, version, created_at, updated_at
)
SELECT
    'WO-DEMO-001', id, 500, 500, 12,
    'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 2 DAY), '프레시마켓 정기 납품분 완료',
    NULL, NULL, 0, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)
FROM items WHERE item_code = 'FG-BOX-S01';

INSERT IGNORE INTO work_orders (
    order_number, item_id, target_quantity, produced_good_quantity, produced_defect_quantity,
    status, planned_date, remark, pause_reason, paused_at, version, created_at, updated_at
)
SELECT
    'WO-DEMO-002', id, 200, 200, 4,
    'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 1 DAY), '코리아패키징 전자부품 납품 완료',
    NULL, NULL, 0, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)
FROM items WHERE item_code = 'FG-BOX-ELE';

INSERT IGNORE INTO work_orders (
    order_number, item_id, target_quantity, produced_good_quantity, produced_defect_quantity,
    status, planned_date, remark, pause_reason, paused_at, version, created_at, updated_at
)
SELECT
    'WO-DEMO-003', id, 400, 260, 6,
    'IN_PROGRESS', CURDATE(), '1라인 택배박스 A형 금일 추가 생산',
    NULL, NULL, 0, DATE_SUB(NOW(), INTERVAL 5 HOUR), NOW()
FROM items WHERE item_code = 'FG-BOX-S01';

INSERT IGNORE INTO work_orders (
    order_number, item_id, target_quantity, produced_good_quantity, produced_defect_quantity,
    status, planned_date, remark, pause_reason, paused_at, version, created_at, updated_at
)
SELECT
    'WO-DEMO-004', id, 300, 120, 3,
    'IN_PROGRESS', CURDATE(), '3라인 프레시마켓 보냉박스 긴급 보충 생산',
    NULL, NULL, 0, DATE_SUB(NOW(), INTERVAL 4 HOUR), NOW()
FROM items WHERE item_code = 'FG-BOX-CLD';

INSERT IGNORE INTO work_orders (
    order_number, item_id, target_quantity, produced_good_quantity, produced_defect_quantity,
    status, planned_date, remark, pause_reason, paused_at, version, created_at, updated_at
)
SELECT
    'WO-DEMO-005', id, 300, 120, 5,
    'PAUSED', CURDATE(), '2라인 청송농협 긴급 출하용 사과박스',
    '원자재 부족 (AB골 이중 원단 입고 대기)', DATE_SUB(NOW(), INTERVAL 35 MINUTE),
    0, DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 35 MINUTE)
FROM items WHERE item_code = 'FG-BOX-AGR';

INSERT IGNORE INTO work_orders (
    order_number, item_id, target_quantity, produced_good_quantity, produced_defect_quantity,
    status, planned_date, remark, pause_reason, paused_at, version, created_at, updated_at
)
SELECT
    'WO-DEMO-006', id, 250, 60, 2,
    'PAUSED', CURDATE(), '2라인 전자부품 이중박스 오후 물량',
    '설비 점검/고장 (2호 접합기 온도 센서 점검)', DATE_SUB(NOW(), INTERVAL 15 MINUTE),
    0, DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 15 MINUTE)
FROM items WHERE item_code = 'FG-BOX-ELE';

INSERT IGNORE INTO work_orders (
    order_number, item_id, target_quantity, produced_good_quantity, produced_defect_quantity,
    status, planned_date, remark, pause_reason, paused_at, version, created_at, updated_at
)
SELECT
    'WO-DEMO-007', id, 600, 0, 0,
    'WAITING', DATE_ADD(CURDATE(), INTERVAL 1 DAY), '명일 오전 1라인 택배박스 정기 생산 예정',
    NULL, NULL, 0, NOW(), NOW()
FROM items WHERE item_code = 'FG-BOX-S01';

INSERT IGNORE INTO work_orders (
    order_number, item_id, target_quantity, produced_good_quantity, produced_defect_quantity,
    status, planned_date, remark, pause_reason, paused_at, version, created_at, updated_at
)
SELECT
    'WO-DEMO-008', id, 150, 0, 0,
    'CANCELED', DATE_SUB(CURDATE(), INTERVAL 1 DAY), '고객사 발주 일정 연기로 작업 취소',
    NULL, NULL, 0, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)
FROM items WHERE item_code = 'FG-BOX-CLD';

-- -----------------------------------------------------------------------------
-- 6. 생산실적 이력 10건 (5일 전 / 2일 전 / 어제 / 오늘 4개 날짜 분산)
-- -----------------------------------------------------------------------------
INSERT INTO production_results (work_order_id, good_quantity, defect_quantity, worker_name, remark, created_at, updated_at)
SELECT wo.id, 180, 4, '김철수', '[DEMO] 1차 절단·인쇄 완료', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)
FROM work_orders wo
WHERE wo.order_number = 'WO-DEMO-001'
  AND NOT EXISTS (SELECT 1 FROM production_results pr WHERE pr.work_order_id = wo.id AND pr.remark = '[DEMO] 1차 절단·인쇄 완료');

INSERT INTO production_results (work_order_id, good_quantity, defect_quantity, worker_name, remark, created_at, updated_at)
SELECT wo.id, 170, 5, '이영희', '[DEMO] 2차 접합 및 결속 완료', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)
FROM work_orders wo
WHERE wo.order_number = 'WO-DEMO-001'
  AND NOT EXISTS (SELECT 1 FROM production_results pr WHERE pr.work_order_id = wo.id AND pr.remark = '[DEMO] 2차 접합 및 결속 완료');

INSERT INTO production_results (work_order_id, good_quantity, defect_quantity, worker_name, remark, created_at, updated_at)
SELECT wo.id, 90, 2, '박민수', '[DEMO] 전자부품 이중박스 1차 가공', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)
FROM work_orders wo
WHERE wo.order_number = 'WO-DEMO-002'
  AND NOT EXISTS (SELECT 1 FROM production_results pr WHERE pr.work_order_id = wo.id AND pr.remark = '[DEMO] 전자부품 이중박스 1차 가공');

INSERT INTO production_results (work_order_id, good_quantity, defect_quantity, worker_name, remark, created_at, updated_at)
SELECT wo.id, 150, 3, '김철수', '[DEMO] 택배박스 3차 마감 및 목표 달성', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)
FROM work_orders wo
WHERE wo.order_number = 'WO-DEMO-001'
  AND NOT EXISTS (SELECT 1 FROM production_results pr WHERE pr.work_order_id = wo.id AND pr.remark = '[DEMO] 택배박스 3차 마감 및 목표 달성');

INSERT INTO production_results (work_order_id, good_quantity, defect_quantity, worker_name, remark, created_at, updated_at)
SELECT wo.id, 110, 2, '정수진', '[DEMO] 전자부품 이중박스 최종 검수 완료', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)
FROM work_orders wo
WHERE wo.order_number = 'WO-DEMO-002'
  AND NOT EXISTS (SELECT 1 FROM production_results pr WHERE pr.work_order_id = wo.id AND pr.remark = '[DEMO] 전자부품 이중박스 최종 검수 완료');

INSERT INTO production_results (work_order_id, good_quantity, defect_quantity, worker_name, remark, created_at, updated_at)
SELECT wo.id, 150, 3, '김철수', '[DEMO] 금일 오전 1라인 1차 실적', DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 4 HOUR)
FROM work_orders wo
WHERE wo.order_number = 'WO-DEMO-003'
  AND NOT EXISTS (SELECT 1 FROM production_results pr WHERE pr.work_order_id = wo.id AND pr.remark = '[DEMO] 금일 오전 1라인 1차 실적');

INSERT INTO production_results (work_order_id, good_quantity, defect_quantity, worker_name, remark, created_at, updated_at)
SELECT wo.id, 110, 3, '박민수', '[DEMO] 금일 오후 1라인 2차 실적', DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR)
FROM work_orders wo
WHERE wo.order_number = 'WO-DEMO-003'
  AND NOT EXISTS (SELECT 1 FROM production_results pr WHERE pr.work_order_id = wo.id AND pr.remark = '[DEMO] 금일 오후 1라인 2차 실적');

INSERT INTO production_results (work_order_id, good_quantity, defect_quantity, worker_name, remark, created_at, updated_at)
SELECT wo.id, 120, 3, '정수진', '[DEMO] 금일 3라인 보냉박스 1차 완료', DATE_SUB(NOW(), INTERVAL 90 MINUTE), DATE_SUB(NOW(), INTERVAL 90 MINUTE)
FROM work_orders wo
WHERE wo.order_number = 'WO-DEMO-004'
  AND NOT EXISTS (SELECT 1 FROM production_results pr WHERE pr.work_order_id = wo.id AND pr.remark = '[DEMO] 금일 3라인 보냉박스 1차 완료');

INSERT INTO production_results (work_order_id, good_quantity, defect_quantity, worker_name, remark, created_at, updated_at)
SELECT wo.id, 120, 5, '이영희', '[DEMO] 2라인 사과박스 1차 생산 후 원단 부족 발생', DATE_SUB(NOW(), INTERVAL 50 MINUTE), DATE_SUB(NOW(), INTERVAL 50 MINUTE)
FROM work_orders wo
WHERE wo.order_number = 'WO-DEMO-005'
  AND NOT EXISTS (SELECT 1 FROM production_results pr WHERE pr.work_order_id = wo.id AND pr.remark = '[DEMO] 2라인 사과박스 1차 생산 후 원단 부족 발생');

INSERT INTO production_results (work_order_id, good_quantity, defect_quantity, worker_name, remark, created_at, updated_at)
SELECT wo.id, 60, 2, '박민수', '[DEMO] 2라인 이중박스 가공 중 센서 이상 감지', DATE_SUB(NOW(), INTERVAL 25 MINUTE), DATE_SUB(NOW(), INTERVAL 25 MINUTE)
FROM work_orders wo
WHERE wo.order_number = 'WO-DEMO-006'
  AND NOT EXISTS (SELECT 1 FROM production_results pr WHERE pr.work_order_id = wo.id AND pr.remark = '[DEMO] 2라인 이중박스 가공 중 센서 이상 감지');

-- -----------------------------------------------------------------------------
-- 7. 재고 입출고 수불 이력 12건 (INBOUND / OUTBOUND / PRODUCTION_IN / PRODUCTION_OUT / ADJUST)
-- -----------------------------------------------------------------------------
INSERT INTO inventory_histories (
    item_id, partner_id, transaction_type, before_quantity, change_quantity, after_quantity, remark, created_at, updated_at
)
SELECT i.id, p.id, 'INBOUND', 700, 1000, 1700, '[DEMO] B골 골판지 원단 정기 입고', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)
FROM items i, partners p
WHERE i.item_code = 'RM-SHEET-B' AND p.partner_code = 'PT-SUP-001'
  AND NOT EXISTS (SELECT 1 FROM inventory_histories WHERE remark = '[DEMO] B골 골판지 원단 정기 입고');

INSERT INTO inventory_histories (
    item_id, partner_id, transaction_type, before_quantity, change_quantity, after_quantity, remark, created_at, updated_at
)
SELECT i.id, p.id, 'INBOUND', 220, 500, 720, '[DEMO] A골 완충 원단 입고', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)
FROM items i, partners p
WHERE i.item_code = 'RM-SHEET-A' AND p.partner_code = 'PT-SUP-001'
  AND NOT EXISTS (SELECT 1 FROM inventory_histories WHERE remark = '[DEMO] A골 완충 원단 입고');

INSERT INTO inventory_histories (
    item_id, partner_id, transaction_type, before_quantity, change_quantity, after_quantity, remark, created_at, updated_at
)
SELECT i.id, p.id, 'INBOUND', 40, 100, 140, '[DEMO] 전분 접착제 정기 매입 입고', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)
FROM items i, partners p
WHERE i.item_code = 'RM-GLUE-01' AND p.partner_code = 'PT-SUP-002'
  AND NOT EXISTS (SELECT 1 FROM inventory_histories WHERE remark = '[DEMO] 전분 접착제 정기 매입 입고');

INSERT INTO inventory_histories (
    item_id, partner_id, transaction_type, before_quantity, change_quantity, after_quantity, remark, created_at, updated_at
)
SELECT i.id, NULL, 'ADJUST', 730, -10, 720, '[DEMO] 월말 창고 실사 파손 원단 차감 조정', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)
FROM items i
WHERE i.item_code = 'RM-SHEET-A'
  AND NOT EXISTS (SELECT 1 FROM inventory_histories WHERE remark = '[DEMO] 월말 창고 실사 파손 원단 차감 조정');

INSERT INTO inventory_histories (
    item_id, partner_id, transaction_type, before_quantity, change_quantity, after_quantity, remark, created_at, updated_at
)
SELECT i.id, NULL, 'PRODUCTION_OUT', 1700, -250, 1450, '[DEMO] 작업지시 WO-DEMO-003 B골 원단 생산투입', DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 3 HOUR)
FROM items i
WHERE i.item_code = 'RM-SHEET-B'
  AND NOT EXISTS (SELECT 1 FROM inventory_histories WHERE remark = '[DEMO] 작업지시 WO-DEMO-003 B골 원단 생산투입');

INSERT INTO inventory_histories (
    item_id, partner_id, transaction_type, before_quantity, change_quantity, after_quantity, remark, created_at, updated_at
)
SELECT i.id, NULL, 'PRODUCTION_IN', 560, 260, 820, '[DEMO] 작업지시 WO-DEMO-003 택배박스 생산입고', DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR)
FROM items i
WHERE i.item_code = 'FG-BOX-S01'
  AND NOT EXISTS (SELECT 1 FROM inventory_histories WHERE remark = '[DEMO] 작업지시 WO-DEMO-003 택배박스 생산입고');

INSERT INTO inventory_histories (
    item_id, partner_id, transaction_type, before_quantity, change_quantity, after_quantity, remark, created_at, updated_at
)
SELECT i.id, NULL, 'PRODUCTION_OUT', 720, -100, 620, '[DEMO] 작업지시 WO-DEMO-004 A골 원단 생산투입', DATE_SUB(NOW(), INTERVAL 95 MINUTE), DATE_SUB(NOW(), INTERVAL 95 MINUTE)
FROM items i
WHERE i.item_code = 'RM-SHEET-A'
  AND NOT EXISTS (SELECT 1 FROM inventory_histories WHERE remark = '[DEMO] 작업지시 WO-DEMO-004 A골 원단 생산투입');

INSERT INTO inventory_histories (
    item_id, partner_id, transaction_type, before_quantity, change_quantity, after_quantity, remark, created_at, updated_at
)
SELECT i.id, NULL, 'PRODUCTION_IN', 70, 120, 190, '[DEMO] 작업지시 WO-DEMO-004 보냉박스 생산입고', DATE_SUB(NOW(), INTERVAL 90 MINUTE), DATE_SUB(NOW(), INTERVAL 90 MINUTE)
FROM items i
WHERE i.item_code = 'FG-BOX-CLD'
  AND NOT EXISTS (SELECT 1 FROM inventory_histories WHERE remark = '[DEMO] 작업지시 WO-DEMO-004 보냉박스 생산입고');

INSERT INTO inventory_histories (
    item_id, partner_id, transaction_type, before_quantity, change_quantity, after_quantity, remark, created_at, updated_at
)
SELECT i.id, NULL, 'PRODUCTION_OUT', 210, -125, 85, '[DEMO] 작업지시 WO-DEMO-005 AB골 원단 투입 (안전재고 미만 도달)', DATE_SUB(NOW(), INTERVAL 50 MINUTE), DATE_SUB(NOW(), INTERVAL 50 MINUTE)
FROM items i
WHERE i.item_code = 'RM-SHEET-AB'
  AND NOT EXISTS (SELECT 1 FROM inventory_histories WHERE remark = '[DEMO] 작업지시 WO-DEMO-005 AB골 원단 투입 (안전재고 미만 도달)');

INSERT INTO inventory_histories (
    item_id, partner_id, transaction_type, before_quantity, change_quantity, after_quantity, remark, created_at, updated_at
)
SELECT i.id, NULL, 'PRODUCTION_IN', 290, 120, 410, '[DEMO] 작업지시 WO-DEMO-005 사과박스 1차 생산입고', DATE_SUB(NOW(), INTERVAL 45 MINUTE), DATE_SUB(NOW(), INTERVAL 45 MINUTE)
FROM items i
WHERE i.item_code = 'FG-BOX-AGR'
  AND NOT EXISTS (SELECT 1 FROM inventory_histories WHERE remark = '[DEMO] 작업지시 WO-DEMO-005 사과박스 1차 생산입고');

INSERT INTO inventory_histories (
    item_id, partner_id, transaction_type, before_quantity, change_quantity, after_quantity, remark, created_at, updated_at
)
SELECT i.id, p.id, 'OUTBOUND', 190, -100, 90, '[DEMO] 프레시마켓 보냉박스 긴급 납품 출고 (안전재고 미만 도달)', DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 30 MINUTE)
FROM items i, partners p
WHERE i.item_code = 'FG-BOX-CLD' AND p.partner_code = 'PT-CUS-001'
  AND NOT EXISTS (SELECT 1 FROM inventory_histories WHERE remark = '[DEMO] 프레시마켓 보냉박스 긴급 납품 출고 (안전재고 미만 도달)');

INSERT INTO inventory_histories (
    item_id, partner_id, transaction_type, before_quantity, change_quantity, after_quantity, remark, created_at, updated_at
)
SELECT i.id, p.id, 'OUTBOUND', 610, -200, 410, '[DEMO] 청송농협 사과박스 1차 출하', DATE_SUB(NOW(), INTERVAL 20 MINUTE), DATE_SUB(NOW(), INTERVAL 20 MINUTE)
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
