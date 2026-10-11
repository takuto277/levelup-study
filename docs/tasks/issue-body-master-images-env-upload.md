## 背景

現状の `make master-images-upload` は単一の `SUPABASE_URL` / `SUPABASE_SERVICE_ROLE_KEY` / `DATABASE_URL` しか見ないため、dev/stg/prod を切り替えるには `backend/.env` を手で書き換える必要があり、誤った環境へ誤爆するリスクがある。

## やること

- `make master-images-upload-{dev,stg,prod}` で環境をコマンド分離
- dev はローカル DB のみ（Storage には上げない）
- stg / prod は各環境の Supabase Storage + DB へ入稿
- prod は `CONFIRM_PROD_UPLOAD=yes` を必須に

## 完了条件

- [ ] 環境別コマンドで入稿できる
- [ ] `make master-images-validate` が通る
- [ ] prod が確認なしで中止する
