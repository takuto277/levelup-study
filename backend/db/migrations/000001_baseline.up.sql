-- LevelUp Study — golang-migrate baseline
--
-- 目的: schema_migrations テーブルを作成し、バージョン管理の起点とする。
-- 現状のテーブル作成は GORM AutoMigrate（cmd/api 起動時）が担当する。
--
-- 今後カラム・テーブルを追加するときは、このディレクトリに
--   000002_xxx.up.sql / 000002_xxx.down.sql
-- のように連番で SQL を置き、`make migrate-up` で適用する。
SELECT 1;
