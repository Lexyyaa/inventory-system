-- 업체 seed
-- 기동마다 실행된다. ON CONFLICT DO NOTHING이라 재기동해도 중복되지 않는다

INSERT INTO tenant (code, name)
VALUES ('tenant-001', '업체 A'),
       ('tenant-002', '업체 B')
ON CONFLICT (code) DO NOTHING;
