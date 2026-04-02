-- ============================================================
-- LevelUp Study — 開発用シードデータ
--
-- 使い方:
--   make seed   (backend/ ディレクトリで実行)
--   または直接: docker exec -i levelup-study-db psql -U postgres -d levelup_study < db/seed.sql
--
-- 前提: テーブルは GORM AutoMigrate で作成済み
-- ============================================================

BEGIN;

-- ────────────────────────────────────────────────
-- 0. 既存データをクリア（開発用のみ）
-- ────────────────────────────────────────────────
TRUNCATE
  gacha_histories,
  user_dungeon_progresses,
  user_party_slots,
  user_weapons,
  user_characters,
  study_rewards,
  study_sessions,
  m_gacha_banners,
  m_dungeon_stages,
  m_dungeons,
  m_weapons,
  m_characters,
  users
CASCADE;

-- ────────────────────────────────────────────────
-- 1. テストユーザー
-- ────────────────────────────────────────────────
INSERT INTO users (id, display_name, total_study_seconds, stones, gold, created_at, updated_at) VALUES
  ('00000000-0000-0000-0000-000000000001', '勇者タクト', 45000, 1000, 5000, NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000002', '見習い魔法使い', 12000, 300, 1500, NOW(), NOW());

-- ────────────────────────────────────────────────
-- 2. キャラクターマスタ（30体）
--    UUID prefix: a0000000-0000-0000-0000-0000000000xx
-- ────────────────────────────────────────────────

-- ★5（5体）
INSERT INTO m_characters (id, name, rarity, base_hp, base_atk, base_def, image_url, is_active, created_at) VALUES
  ('a0000000-0000-0000-0000-000000000001', '光の勇者アリア',       5, 1500, 420, 300, 'https://placehold.co/200x200/FFD700/000?text=Aria',     true, NOW()),
  ('a0000000-0000-0000-0000-000000000002', '闇の魔王ゼファー',     5, 1800, 480, 250, 'https://placehold.co/200x200/4B0082/FFF?text=Zephyr',   true, NOW()),
  ('a0000000-0000-0000-0000-000000000003', '聖女セラフィーナ',     5, 1200, 200, 380, 'https://placehold.co/200x200/FFB6C1/000?text=Seraphina', true, NOW()),
  ('a0000000-0000-0000-0000-000000000004', '竜騎士ドラク',         5, 2000, 400, 350, 'https://placehold.co/200x200/DC143C/FFF?text=Draco',     true, NOW()),
  ('a0000000-0000-0000-0000-000000000005', '叡智の大賢者マーリン', 5, 1100, 500, 200, 'https://placehold.co/200x200/8B5CF6/FFF?text=Merlin',   true, NOW());

-- ★4（10体）
INSERT INTO m_characters (id, name, rarity, base_hp, base_atk, base_def, image_url, is_active, created_at) VALUES
  ('a0000000-0000-0000-0000-000000000006', '炎の魔術師レイ',         4, 850,  350, 180, 'https://placehold.co/200x200/EF4444/FFF?text=Ray',    true, NOW()),
  ('a0000000-0000-0000-0000-000000000007', '氷の弓使いリナ',         4, 750,  380, 160, 'https://placehold.co/200x200/3B82F6/FFF?text=Rina',   true, NOW()),
  ('a0000000-0000-0000-0000-000000000008', '風の剣士カイト',         4, 900,  320, 220, 'https://placehold.co/200x200/10B981/FFF?text=Kite',   true, NOW()),
  ('a0000000-0000-0000-0000-000000000009', '雷撃のソラ',             4, 800,  400, 150, 'https://placehold.co/200x200/F59E0B/000?text=Sora',   true, NOW()),
  ('a0000000-0000-0000-0000-00000000000a', '鉄壁のガルド',           4, 1400, 180, 400, 'https://placehold.co/200x200/6B7280/FFF?text=Guard',  true, NOW()),
  ('a0000000-0000-0000-0000-00000000000b', '忍者カゲ',               4, 650,  420, 130, 'https://placehold.co/200x200/1F2937/FFF?text=Kage',   true, NOW()),
  ('a0000000-0000-0000-0000-00000000000c', '踊り子ルナ',             4, 700,  250, 280, 'https://placehold.co/200x200/EC4899/FFF?text=Luna',   true, NOW()),
  ('a0000000-0000-0000-0000-00000000000d', '錬金術師アルケム',       4, 780,  300, 250, 'https://placehold.co/200x200/A78BFA/FFF?text=Alchem', true, NOW()),
  ('a0000000-0000-0000-0000-00000000000e', '海賊キャプテン・リーフ', 4, 1000, 340, 200, 'https://placehold.co/200x200/0EA5E9/FFF?text=Reef',  true, NOW()),
  ('a0000000-0000-0000-0000-00000000000f', '占い師ステラ',           4, 680,  360, 190, 'https://placehold.co/200x200/D946EF/FFF?text=Stella', true, NOW());

-- ★3（15体）
INSERT INTO m_characters (id, name, rarity, base_hp, base_atk, base_def, image_url, is_active, created_at) VALUES
  ('a0000000-0000-0000-0000-000000000010', '見習い戦士タロウ',     3, 600, 200, 180, 'https://placehold.co/200x200/9CA3AF/000?text=Taro',    true, NOW()),
  ('a0000000-0000-0000-0000-000000000011', '森の精霊コダマ',       3, 500, 180, 220, 'https://placehold.co/200x200/22C55E/FFF?text=Kodama',  true, NOW()),
  ('a0000000-0000-0000-0000-000000000012', '街の商人マルコ',       3, 550, 150, 200, 'https://placehold.co/200x200/F97316/FFF?text=Marco',   true, NOW()),
  ('a0000000-0000-0000-0000-000000000013', '旅の吟遊詩人バード',   3, 480, 220, 160, 'https://placehold.co/200x200/8B5CF6/FFF?text=Bard',    true, NOW()),
  ('a0000000-0000-0000-0000-000000000014', '村の鍛冶師ヴォルク',   3, 700, 190, 250, 'https://placehold.co/200x200/78716C/FFF?text=Volk',    true, NOW()),
  ('a0000000-0000-0000-0000-000000000015', '見習い僧侶ヒカリ',     3, 520, 130, 280, 'https://placehold.co/200x200/FBBF24/000?text=Hikari',  true, NOW()),
  ('a0000000-0000-0000-0000-000000000016', '野良猫シャム',         3, 400, 260, 120, 'https://placehold.co/200x200/FB923C/FFF?text=Sham',    true, NOW()),
  ('a0000000-0000-0000-0000-000000000017', '農夫ダイチ',           3, 650, 170, 230, 'https://placehold.co/200x200/84CC16/FFF?text=Daichi',  true, NOW()),
  ('a0000000-0000-0000-0000-000000000018', '釣り人ウミ',           3, 530, 200, 190, 'https://placehold.co/200x200/06B6D4/FFF?text=Umi',     true, NOW()),
  ('a0000000-0000-0000-0000-000000000019', '見習い魔女ノワール',   3, 450, 250, 140, 'https://placehold.co/200x200/7C3AED/FFF?text=Noir',    true, NOW()),
  ('a0000000-0000-0000-0000-00000000001a', '門番ゲート',           3, 800, 140, 300, 'https://placehold.co/200x200/475569/FFF?text=Gate',    true, NOW()),
  ('a0000000-0000-0000-0000-00000000001b', '料理人シェフ',         3, 500, 180, 200, 'https://placehold.co/200x200/E11D48/FFF?text=Chef',    true, NOW()),
  ('a0000000-0000-0000-0000-00000000001c', '図書館員ページ',       3, 420, 230, 170, 'https://placehold.co/200x200/6366F1/FFF?text=Page',    true, NOW()),
  ('a0000000-0000-0000-0000-00000000001d', '配達人ポスト',         3, 550, 210, 180, 'https://placehold.co/200x200/14B8A6/FFF?text=Post',    true, NOW()),
  ('a0000000-0000-0000-0000-00000000001e', '大工カーペン',         3, 680, 190, 240, 'https://placehold.co/200x200/B45309/FFF?text=Carpen',  true, NOW());

-- ────────────────────────────────────────────────
-- 3. 武器マスタ（15本）
--    UUID prefix: b0000000-0000-0000-0000-0000000000xx
-- ────────────────────────────────────────────────

-- ★5（3本）
INSERT INTO m_weapons (id, name, rarity, base_atk, image_url, is_active, created_at) VALUES
  ('b0000000-0000-0000-0000-000000000001', '聖剣エクスカリバー',  5, 150, 'https://placehold.co/200x200/FFD700/000?text=Excalibur',   true, NOW()),
  ('b0000000-0000-0000-0000-000000000002', '闇の大鎌デスサイズ',  5, 165, 'https://placehold.co/200x200/4B0082/FFF?text=Deathscythe', true, NOW()),
  ('b0000000-0000-0000-0000-000000000003', '癒しの聖杖',          5, 80,  'https://placehold.co/200x200/FFB6C1/000?text=HolyStaff',  true, NOW());

-- ★4（5本）
INSERT INTO m_weapons (id, name, rarity, base_atk, image_url, is_active, created_at) VALUES
  ('b0000000-0000-0000-0000-000000000004', '炎の杖ヘルフレイム',   4, 95,  'https://placehold.co/200x200/EF4444/FFF?text=HellFlame',   true, NOW()),
  ('b0000000-0000-0000-0000-000000000005', '氷の弓フロストアロー', 4, 100, 'https://placehold.co/200x200/3B82F6/FFF?text=FrostArrow',  true, NOW()),
  ('b0000000-0000-0000-0000-000000000006', '雷槍サンダーランス',   4, 110, 'https://placehold.co/200x200/F59E0B/000?text=ThunderLance', true, NOW()),
  ('b0000000-0000-0000-0000-000000000007', '叡智の杖',             4, 85,  'https://placehold.co/200x200/8B5CF6/FFF?text=WisdomStaff', true, NOW()),
  ('b0000000-0000-0000-0000-000000000008', '風切りの双剣',         4, 105, 'https://placehold.co/200x200/10B981/FFF?text=WindBlade',   true, NOW());

-- ★3（7本）
INSERT INTO m_weapons (id, name, rarity, base_atk, image_url, is_active, created_at) VALUES
  ('b0000000-0000-0000-0000-000000000009', '鉄の剣',     3, 55, 'https://placehold.co/200x200/9CA3AF/000?text=IronSword',      true, NOW()),
  ('b0000000-0000-0000-0000-00000000000a', '木の杖',     3, 40, 'https://placehold.co/200x200/84CC16/FFF?text=WoodStaff',      true, NOW()),
  ('b0000000-0000-0000-0000-00000000000b', '革の盾',     3, 30, 'https://placehold.co/200x200/78716C/FFF?text=LeatherShield',  true, NOW()),
  ('b0000000-0000-0000-0000-00000000000c', '石の弓',     3, 50, 'https://placehold.co/200x200/6B7280/FFF?text=StoneBow',       true, NOW()),
  ('b0000000-0000-0000-0000-00000000000d', '銅のナイフ', 3, 45, 'https://placehold.co/200x200/B45309/FFF?text=CopperKnife',    true, NOW()),
  ('b0000000-0000-0000-0000-00000000000e', '見習いの杖', 3, 35, 'https://placehold.co/200x200/7C3AED/FFF?text=AppStaff',       true, NOW()),
  ('b0000000-0000-0000-0000-00000000000f', '旅人の短剣', 3, 48, 'https://placehold.co/200x200/14B8A6/FFF?text=TravelerDagger', true, NOW());

-- ────────────────────────────────────────────────
-- 4. ダンジョンマスタ（6種）
--    UUID prefix: d0000000-0000-0000-0000-0000000000xx
-- ────────────────────────────────────────────────
INSERT INTO m_dungeons (id, name, sort_order, unlock_condition, image_url, is_active, created_at) VALUES
  ('d0000000-0000-0000-0000-000000000001', 'はじまりの森',   1, NULL,                                           'https://placehold.co/400x200/22C55E/FFF?text=Forest',         true, NOW()),
  ('d0000000-0000-0000-0000-000000000002', '水晶の洞窟',     2, NULL,                                           'https://placehold.co/400x200/3B82F6/FFF?text=Crystal+Cave',   true, NOW()),
  ('d0000000-0000-0000-0000-000000000003', '炎の塔',         3, NULL,                                           'https://placehold.co/400x200/EF4444/FFF?text=Flame+Tower',    true, NOW()),
  ('d0000000-0000-0000-0000-000000000004', 'コードの迷宮',   4, NULL,                                           'https://placehold.co/400x200/6366F1/FFF?text=Code+Labyrinth', true, NOW()),
  ('d0000000-0000-0000-0000-000000000005', '天空の聖域',     5, '{"min_cleared_dungeons": 3}',                   'https://placehold.co/400x200/F59E0B/FFF?text=Sky+Sanctuary',  true, NOW()),
  ('d0000000-0000-0000-0000-000000000006', '深淵の図書館',   6, '{"min_cleared_dungeons": 5, "min_level": 20}',  'https://placehold.co/400x200/7C3AED/FFF?text=Abyss+Library',  true, NOW());

-- ────────────────────────────────────────────────
-- 4.1 ダンジョンステージ
--     UUID prefix: e0000000-0000-0000-0000-0000000000xx
-- ────────────────────────────────────────────────

-- はじまりの森（5ステージ、推奨25分 → 5分/ステージ）
INSERT INTO m_dungeon_stages (id, dungeon_id, stage_number, recommended_power, enemy_composition, drop_table) VALUES
  ('e0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 1, 100,
   '[{"name":"スライム","emoji":"🟢","hp":30,"atk":8},{"name":"スライム","emoji":"🟢","hp":30,"atk":8}]',
   '[{"item_type":"gold","amount":20,"rate":1.0},{"item_type":"xp","amount":10,"rate":1.0}]'),
  ('e0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000001', 2, 120,
   '[{"name":"ゴブリン","emoji":"👺","hp":50,"atk":12},{"name":"スライム","emoji":"🟢","hp":30,"atk":8}]',
   '[{"item_type":"gold","amount":30,"rate":1.0},{"item_type":"xp","amount":15,"rate":1.0}]'),
  ('e0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000001', 3, 150,
   '[{"name":"コウモリ","emoji":"🦇","hp":40,"atk":15},{"name":"ゴブリン","emoji":"👺","hp":50,"atk":12}]',
   '[{"item_type":"gold","amount":40,"rate":1.0},{"item_type":"xp","amount":20,"rate":1.0},{"item_type":"stones","amount":3,"rate":0.3}]'),
  ('e0000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000001', 4, 180,
   '[{"name":"オオカミ","emoji":"🐺","hp":70,"atk":18},{"name":"コウモリ","emoji":"🦇","hp":40,"atk":15}]',
   '[{"item_type":"gold","amount":50,"rate":1.0},{"item_type":"xp","amount":25,"rate":1.0},{"item_type":"stones","amount":5,"rate":0.3}]'),
  ('e0000000-0000-0000-0000-000000000005', 'd0000000-0000-0000-0000-000000000001', 5, 220,
   '[{"name":"森のトレント","emoji":"🌳","hp":120,"atk":25}]',
   '[{"item_type":"gold","amount":100,"rate":1.0},{"item_type":"xp","amount":50,"rate":1.0},{"item_type":"stones","amount":10,"rate":0.5}]');

-- 水晶の洞窟（5ステージ、推奨30分）
INSERT INTO m_dungeon_stages (id, dungeon_id, stage_number, recommended_power, enemy_composition, drop_table) VALUES
  ('e0000000-0000-0000-0000-000000000006', 'd0000000-0000-0000-0000-000000000002', 1, 200,
   '[{"name":"クリスタルスライム","emoji":"💎","hp":60,"atk":15},{"name":"洞窟コウモリ","emoji":"🦇","hp":50,"atk":12}]',
   '[{"item_type":"gold","amount":40,"rate":1.0},{"item_type":"xp","amount":20,"rate":1.0}]'),
  ('e0000000-0000-0000-0000-000000000007', 'd0000000-0000-0000-0000-000000000002', 2, 250,
   '[{"name":"ストーンゴーレム","emoji":"🗿","hp":100,"atk":20},{"name":"クリスタルスライム","emoji":"💎","hp":60,"atk":15}]',
   '[{"item_type":"gold","amount":60,"rate":1.0},{"item_type":"xp","amount":30,"rate":1.0}]'),
  ('e0000000-0000-0000-0000-000000000008', 'd0000000-0000-0000-0000-000000000002', 3, 300,
   '[{"name":"ダークマイナー","emoji":"⛏️","hp":80,"atk":25},{"name":"ストーンゴーレム","emoji":"🗿","hp":100,"atk":20}]',
   '[{"item_type":"gold","amount":80,"rate":1.0},{"item_type":"xp","amount":40,"rate":1.0},{"item_type":"stones","amount":5,"rate":0.3}]'),
  ('e0000000-0000-0000-0000-000000000009', 'd0000000-0000-0000-0000-000000000002', 4, 350,
   '[{"name":"クリスタルドラゴン","emoji":"🐉","hp":150,"atk":30}]',
   '[{"item_type":"gold","amount":120,"rate":1.0},{"item_type":"xp","amount":60,"rate":1.0},{"item_type":"stones","amount":8,"rate":0.4}]'),
  ('e0000000-0000-0000-0000-00000000000a', 'd0000000-0000-0000-0000-000000000002', 5, 400,
   '[{"name":"水晶の番人","emoji":"🔮","hp":200,"atk":35}]',
   '[{"item_type":"gold","amount":200,"rate":1.0},{"item_type":"xp","amount":100,"rate":1.0},{"item_type":"stones","amount":15,"rate":0.5}]');

-- 炎の塔（5ステージ、推奨45分）
INSERT INTO m_dungeon_stages (id, dungeon_id, stage_number, recommended_power, enemy_composition, drop_table) VALUES
  ('e0000000-0000-0000-0000-00000000000b', 'd0000000-0000-0000-0000-000000000003', 1, 350,
   '[{"name":"ファイアスライム","emoji":"🔥","hp":80,"atk":22},{"name":"サラマンダー","emoji":"🦎","hp":90,"atk":25}]',
   '[{"item_type":"gold","amount":60,"rate":1.0},{"item_type":"xp","amount":30,"rate":1.0}]'),
  ('e0000000-0000-0000-0000-00000000000c', 'd0000000-0000-0000-0000-000000000003', 2, 400,
   '[{"name":"溶岩ゴーレム","emoji":"🌋","hp":150,"atk":30},{"name":"ファイアスライム","emoji":"🔥","hp":80,"atk":22}]',
   '[{"item_type":"gold","amount":90,"rate":1.0},{"item_type":"xp","amount":45,"rate":1.0}]'),
  ('e0000000-0000-0000-0000-00000000000d', 'd0000000-0000-0000-0000-000000000003', 3, 500,
   '[{"name":"フレイムナイト","emoji":"🔥","hp":180,"atk":35},{"name":"溶岩ゴーレム","emoji":"🌋","hp":150,"atk":30}]',
   '[{"item_type":"gold","amount":120,"rate":1.0},{"item_type":"xp","amount":60,"rate":1.0},{"item_type":"stones","amount":8,"rate":0.3}]'),
  ('e0000000-0000-0000-0000-00000000000e', 'd0000000-0000-0000-0000-000000000003', 4, 600,
   '[{"name":"ヘルハウンド","emoji":"🐕","hp":200,"atk":40}]',
   '[{"item_type":"gold","amount":150,"rate":1.0},{"item_type":"xp","amount":80,"rate":1.0},{"item_type":"stones","amount":10,"rate":0.4}]'),
  ('e0000000-0000-0000-0000-00000000000f', 'd0000000-0000-0000-0000-000000000003', 5, 700,
   '[{"name":"炎獄の守護者イフリート","emoji":"😈","hp":350,"atk":50}]',
   '[{"item_type":"gold","amount":350,"rate":1.0},{"item_type":"xp","amount":150,"rate":1.0},{"item_type":"stones","amount":20,"rate":0.6}]');

-- コードの迷宮（4ステージ、推奨30分）
INSERT INTO m_dungeon_stages (id, dungeon_id, stage_number, recommended_power, enemy_composition, drop_table) VALUES
  ('e0000000-0000-0000-0000-000000000010', 'd0000000-0000-0000-0000-000000000004', 1, 250,
   '[{"name":"バグスライム","emoji":"🐛","hp":50,"atk":18},{"name":"バグスライム","emoji":"🐛","hp":50,"atk":18}]',
   '[{"item_type":"gold","amount":50,"rate":1.0},{"item_type":"xp","amount":25,"rate":1.0}]'),
  ('e0000000-0000-0000-0000-000000000011', 'd0000000-0000-0000-0000-000000000004', 2, 350,
   '[{"name":"エラーゴースト","emoji":"👻","hp":90,"atk":25},{"name":"バグスライム","emoji":"🐛","hp":50,"atk":18}]',
   '[{"item_type":"gold","amount":80,"rate":1.0},{"item_type":"xp","amount":40,"rate":1.0}]'),
  ('e0000000-0000-0000-0000-000000000012', 'd0000000-0000-0000-0000-000000000004', 3, 450,
   '[{"name":"スタックオーバーフロー","emoji":"💥","hp":160,"atk":35}]',
   '[{"item_type":"gold","amount":120,"rate":1.0},{"item_type":"xp","amount":60,"rate":1.0},{"item_type":"stones","amount":10,"rate":0.4}]'),
  ('e0000000-0000-0000-0000-000000000013', 'd0000000-0000-0000-0000-000000000004', 4, 550,
   '[{"name":"無限ループの悪魔","emoji":"♾️","hp":250,"atk":45}]',
   '[{"item_type":"gold","amount":250,"rate":1.0},{"item_type":"xp","amount":120,"rate":1.0},{"item_type":"stones","amount":15,"rate":0.5}]');

-- 天空の聖域（3ステージ、推奨60分）
INSERT INTO m_dungeon_stages (id, dungeon_id, stage_number, recommended_power, enemy_composition, drop_table) VALUES
  ('e0000000-0000-0000-0000-000000000014', 'd0000000-0000-0000-0000-000000000005', 1, 500,
   '[{"name":"天使のガーディアン","emoji":"👼","hp":200,"atk":35},{"name":"雲のエレメンタル","emoji":"☁️","hp":120,"atk":28}]',
   '[{"item_type":"gold","amount":150,"rate":1.0},{"item_type":"xp","amount":80,"rate":1.0}]'),
  ('e0000000-0000-0000-0000-000000000015', 'd0000000-0000-0000-0000-000000000005', 2, 700,
   '[{"name":"嵐の鳥フェニックス","emoji":"🦅","hp":300,"atk":45}]',
   '[{"item_type":"gold","amount":300,"rate":1.0},{"item_type":"xp","amount":150,"rate":1.0},{"item_type":"stones","amount":15,"rate":0.5}]'),
  ('e0000000-0000-0000-0000-000000000016', 'd0000000-0000-0000-0000-000000000005', 3, 900,
   '[{"name":"天空の審判者","emoji":"⚡","hp":500,"atk":60}]',
   '[{"item_type":"gold","amount":500,"rate":1.0},{"item_type":"xp","amount":300,"rate":1.0},{"item_type":"stones","amount":30,"rate":0.7}]');

-- 深淵の図書館（3ステージ、推奨120分）
INSERT INTO m_dungeon_stages (id, dungeon_id, stage_number, recommended_power, enemy_composition, drop_table) VALUES
  ('e0000000-0000-0000-0000-000000000017', 'd0000000-0000-0000-0000-000000000006', 1, 800,
   '[{"name":"忘却の書物","emoji":"📕","hp":250,"atk":40},{"name":"インクモンスター","emoji":"🖋️","hp":180,"atk":35}]',
   '[{"item_type":"gold","amount":200,"rate":1.0},{"item_type":"xp","amount":100,"rate":1.0}]'),
  ('e0000000-0000-0000-0000-000000000018', 'd0000000-0000-0000-0000-000000000006', 2, 1200,
   '[{"name":"古代の司書","emoji":"🧙","hp":400,"atk":55},{"name":"禁書のガーゴイル","emoji":"🗿","hp":300,"atk":45}]',
   '[{"item_type":"gold","amount":500,"rate":1.0},{"item_type":"xp","amount":250,"rate":1.0},{"item_type":"stones","amount":25,"rate":0.5}]'),
  ('e0000000-0000-0000-0000-000000000019', 'd0000000-0000-0000-0000-000000000006', 3, 1500,
   '[{"name":"叡智の魔神アザトース","emoji":"👁️","hp":800,"atk":80}]',
   '[{"item_type":"gold","amount":1000,"rate":1.0},{"item_type":"xp","amount":500,"rate":1.0},{"item_type":"stones","amount":50,"rate":0.8}]');

-- ────────────────────────────────────────────────
-- 5. ガチャバナー（3種）
--    UUID prefix: f0000000-0000-0000-0000-0000000000xx
-- ────────────────────────────────────────────────
INSERT INTO m_gacha_banners (id, name, banner_type, start_at, end_at, pity_threshold, rate_table, is_active) VALUES
  ('f0000000-0000-0000-0000-000000000001', '光の勇者ピックアップ', 'character',
   '2026-01-01 00:00:00+00', '2027-12-31 23:59:59+00', 90,
   '[
     {"item_id":"a0000000-0000-0000-0000-000000000001","result_type":"character","rarity":5,"rate":0.015},
     {"item_id":"a0000000-0000-0000-0000-000000000002","result_type":"character","rarity":5,"rate":0.005},
     {"item_id":"a0000000-0000-0000-0000-000000000003","result_type":"character","rarity":5,"rate":0.005},
     {"item_id":"a0000000-0000-0000-0000-000000000004","result_type":"character","rarity":5,"rate":0.003},
     {"item_id":"a0000000-0000-0000-0000-000000000005","result_type":"character","rarity":5,"rate":0.002},
     {"item_id":"a0000000-0000-0000-0000-000000000006","result_type":"character","rarity":4,"rate":0.015},
     {"item_id":"a0000000-0000-0000-0000-000000000007","result_type":"character","rarity":4,"rate":0.015},
     {"item_id":"a0000000-0000-0000-0000-000000000008","result_type":"character","rarity":4,"rate":0.015},
     {"item_id":"a0000000-0000-0000-0000-000000000009","result_type":"character","rarity":4,"rate":0.015},
     {"item_id":"a0000000-0000-0000-0000-00000000000a","result_type":"character","rarity":4,"rate":0.015},
     {"item_id":"a0000000-0000-0000-0000-00000000000b","result_type":"character","rarity":4,"rate":0.015},
     {"item_id":"a0000000-0000-0000-0000-00000000000c","result_type":"character","rarity":4,"rate":0.015},
     {"item_id":"a0000000-0000-0000-0000-00000000000d","result_type":"character","rarity":4,"rate":0.015},
     {"item_id":"a0000000-0000-0000-0000-00000000000e","result_type":"character","rarity":4,"rate":0.015},
     {"item_id":"a0000000-0000-0000-0000-00000000000f","result_type":"character","rarity":4,"rate":0.015},
     {"item_id":"a0000000-0000-0000-0000-000000000010","result_type":"character","rarity":3,"rate":0.054},
     {"item_id":"a0000000-0000-0000-0000-000000000011","result_type":"character","rarity":3,"rate":0.054},
     {"item_id":"a0000000-0000-0000-0000-000000000012","result_type":"character","rarity":3,"rate":0.054},
     {"item_id":"a0000000-0000-0000-0000-000000000013","result_type":"character","rarity":3,"rate":0.054},
     {"item_id":"a0000000-0000-0000-0000-000000000014","result_type":"character","rarity":3,"rate":0.054},
     {"item_id":"a0000000-0000-0000-0000-000000000015","result_type":"character","rarity":3,"rate":0.054},
     {"item_id":"a0000000-0000-0000-0000-000000000016","result_type":"character","rarity":3,"rate":0.054},
     {"item_id":"a0000000-0000-0000-0000-000000000017","result_type":"character","rarity":3,"rate":0.054},
     {"item_id":"a0000000-0000-0000-0000-000000000018","result_type":"character","rarity":3,"rate":0.054},
     {"item_id":"a0000000-0000-0000-0000-000000000019","result_type":"character","rarity":3,"rate":0.054},
     {"item_id":"a0000000-0000-0000-0000-00000000001a","result_type":"character","rarity":3,"rate":0.054},
     {"item_id":"a0000000-0000-0000-0000-00000000001b","result_type":"character","rarity":3,"rate":0.054},
     {"item_id":"a0000000-0000-0000-0000-00000000001c","result_type":"character","rarity":3,"rate":0.054},
     {"item_id":"a0000000-0000-0000-0000-00000000001d","result_type":"character","rarity":3,"rate":0.054},
     {"item_id":"a0000000-0000-0000-0000-00000000001e","result_type":"character","rarity":3,"rate":0.054}
   ]'::jsonb, true),

  ('f0000000-0000-0000-0000-000000000002', '伝説の聖剣ガチャ', 'weapon',
   '2026-01-01 00:00:00+00', '2027-12-31 23:59:59+00', 80,
   '[
     {"item_id":"b0000000-0000-0000-0000-000000000001","result_type":"weapon","rarity":5,"rate":0.02},
     {"item_id":"b0000000-0000-0000-0000-000000000002","result_type":"weapon","rarity":5,"rate":0.005},
     {"item_id":"b0000000-0000-0000-0000-000000000003","result_type":"weapon","rarity":5,"rate":0.005},
     {"item_id":"b0000000-0000-0000-0000-000000000004","result_type":"weapon","rarity":4,"rate":0.03},
     {"item_id":"b0000000-0000-0000-0000-000000000005","result_type":"weapon","rarity":4,"rate":0.03},
     {"item_id":"b0000000-0000-0000-0000-000000000006","result_type":"weapon","rarity":4,"rate":0.03},
     {"item_id":"b0000000-0000-0000-0000-000000000007","result_type":"weapon","rarity":4,"rate":0.03},
     {"item_id":"b0000000-0000-0000-0000-000000000008","result_type":"weapon","rarity":4,"rate":0.03},
     {"item_id":"b0000000-0000-0000-0000-000000000009","result_type":"weapon","rarity":3,"rate":0.117},
     {"item_id":"b0000000-0000-0000-0000-00000000000a","result_type":"weapon","rarity":3,"rate":0.117},
     {"item_id":"b0000000-0000-0000-0000-00000000000b","result_type":"weapon","rarity":3,"rate":0.117},
     {"item_id":"b0000000-0000-0000-0000-00000000000c","result_type":"weapon","rarity":3,"rate":0.117},
     {"item_id":"b0000000-0000-0000-0000-00000000000d","result_type":"weapon","rarity":3,"rate":0.117},
     {"item_id":"b0000000-0000-0000-0000-00000000000e","result_type":"weapon","rarity":3,"rate":0.117},
     {"item_id":"b0000000-0000-0000-0000-00000000000f","result_type":"weapon","rarity":3,"rate":0.118}
   ]'::jsonb, true),

  ('f0000000-0000-0000-0000-000000000003', '新学期スペシャル召喚', 'mixed',
   '2026-01-01 00:00:00+00', '2027-12-31 23:59:59+00', NULL,
   '[
     {"item_id":"a0000000-0000-0000-0000-000000000001","result_type":"character","rarity":5,"rate":0.01},
     {"item_id":"a0000000-0000-0000-0000-000000000005","result_type":"character","rarity":5,"rate":0.01},
     {"item_id":"b0000000-0000-0000-0000-000000000001","result_type":"weapon","rarity":5,"rate":0.01},
     {"item_id":"a0000000-0000-0000-0000-000000000006","result_type":"character","rarity":4,"rate":0.025},
     {"item_id":"a0000000-0000-0000-0000-000000000007","result_type":"character","rarity":4,"rate":0.025},
     {"item_id":"a0000000-0000-0000-0000-000000000008","result_type":"character","rarity":4,"rate":0.025},
     {"item_id":"b0000000-0000-0000-0000-000000000004","result_type":"weapon","rarity":4,"rate":0.025},
     {"item_id":"b0000000-0000-0000-0000-000000000005","result_type":"weapon","rarity":4,"rate":0.025},
     {"item_id":"b0000000-0000-0000-0000-000000000006","result_type":"weapon","rarity":4,"rate":0.025},
     {"item_id":"a0000000-0000-0000-0000-000000000010","result_type":"character","rarity":3,"rate":0.05},
     {"item_id":"a0000000-0000-0000-0000-000000000011","result_type":"character","rarity":3,"rate":0.05},
     {"item_id":"a0000000-0000-0000-0000-000000000012","result_type":"character","rarity":3,"rate":0.05},
     {"item_id":"a0000000-0000-0000-0000-000000000013","result_type":"character","rarity":3,"rate":0.05},
     {"item_id":"a0000000-0000-0000-0000-000000000014","result_type":"character","rarity":3,"rate":0.05},
     {"item_id":"b0000000-0000-0000-0000-000000000009","result_type":"weapon","rarity":3,"rate":0.05},
     {"item_id":"b0000000-0000-0000-0000-00000000000a","result_type":"weapon","rarity":3,"rate":0.05},
     {"item_id":"b0000000-0000-0000-0000-00000000000b","result_type":"weapon","rarity":3,"rate":0.05},
     {"item_id":"b0000000-0000-0000-0000-00000000000c","result_type":"weapon","rarity":3,"rate":0.05},
     {"item_id":"b0000000-0000-0000-0000-00000000000d","result_type":"weapon","rarity":3,"rate":0.05},
     {"item_id":"b0000000-0000-0000-0000-00000000000e","result_type":"weapon","rarity":3,"rate":0.05},
     {"item_id":"b0000000-0000-0000-0000-00000000000f","result_type":"weapon","rarity":3,"rate":0.05}
   ]'::jsonb, true);

-- ────────────────────────────────────────────────
-- 6. テストユーザー1の所持キャラ（6体）
--    UUID prefix: ca000000-0000-0000-0000-0000000000xx
-- ────────────────────────────────────────────────
INSERT INTO user_characters (id, user_id, character_id, level, current_xp, equipped_weapon_id, obtained_at) VALUES
  ('ca000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000005', 15, 4200, NULL, '2026-01-15T00:00:00Z'),
  ('ca000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000008', 8,  1200, NULL, '2026-02-01T00:00:00Z'),
  ('ca000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000007', 10, 1800, NULL, '2026-02-10T00:00:00Z'),
  ('ca000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000003', 12, 3000, NULL, '2026-01-20T00:00:00Z'),
  ('ca000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-00000000000b', 5,  600,  NULL, '2026-03-01T00:00:00Z'),
  ('ca000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000004', 3,  200,  NULL, '2026-03-20T00:00:00Z');

-- ────────────────────────────────────────────────
-- 7. テストユーザー1の所持武器（3本）
--    UUID prefix: cb000000-0000-0000-0000-0000000000xx
-- ────────────────────────────────────────────────
INSERT INTO user_weapons (id, user_id, weapon_id, level, obtained_at) VALUES
  ('cb000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000007', 5, '2026-01-15T00:00:00Z'),
  ('cb000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000009', 3, '2026-02-01T00:00:00Z'),
  ('cb000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000003', 7, '2026-01-20T00:00:00Z');

-- 武器の装備を反映
UPDATE user_characters SET equipped_weapon_id = 'cb000000-0000-0000-0000-000000000001' WHERE id = 'ca000000-0000-0000-0000-000000000001';
UPDATE user_characters SET equipped_weapon_id = 'cb000000-0000-0000-0000-000000000002' WHERE id = 'ca000000-0000-0000-0000-000000000002';
UPDATE user_characters SET equipped_weapon_id = 'cb000000-0000-0000-0000-000000000003' WHERE id = 'ca000000-0000-0000-0000-000000000004';

-- ────────────────────────────────────────────────
-- 8. テストユーザー1のパーティ編成（3スロット）
--    UUID prefix: cc000000-0000-0000-0000-0000000000xx
-- ────────────────────────────────────────────────
INSERT INTO user_party_slots (id, user_id, slot_position, user_character_id) VALUES
  ('cc000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 1, 'ca000000-0000-0000-0000-000000000001'),
  ('cc000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 2, 'ca000000-0000-0000-0000-000000000002'),
  ('cc000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 3, 'ca000000-0000-0000-0000-000000000004');

-- ────────────────────────────────────────────────
-- 9. テストユーザー1のダンジョン進行状況
--    UUID prefix: cd000000-0000-0000-0000-0000000000xx
-- ────────────────────────────────────────────────
INSERT INTO user_dungeon_progresses (id, user_id, dungeon_id, current_stage, max_cleared_stage, updated_at) VALUES
  ('cd000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 4, 3, NOW()),
  ('cd000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000002', 2, 1, NOW());

COMMIT;
