package repository

import (
	"time"

	"github.com/google/uuid"
	"github.com/takuto277/levelup-study/backend/internal/model"
	"gorm.io/gorm"
)

// ============================================================
// GachaRepository — ガチャ履歴の CRUD
// 天井カウントはサーバー側で厳密に管理する。
// ============================================================

type GachaRepository struct {
	db *gorm.DB
}

func NewGachaRepository(db *gorm.DB) *GachaRepository {
	return &GachaRepository{db: db}
}

// CreateHistory — ガチャ結果を記録する
func (r *GachaRepository) CreateHistory(tx *gorm.DB, h *model.GachaHistory) error {
	return tx.Create(h).Error
}

// GetPityCount — バナーに対する現在の天井カウント（累計回数）を取得する
func (r *GachaRepository) GetPityCount(userID, bannerID uuid.UUID) (int, error) {
	var maxPity int
	err := r.db.Model(&model.GachaHistory{}).
		Where("user_id = ? AND banner_id = ?", userID, bannerID).
		Select("COALESCE(MAX(pity_count), 0)").
		Scan(&maxPity).Error
	return maxPity, err
}

// ListByUser — ユーザーのガチャ履歴を取得する（新しい順）
func (r *GachaRepository) ListByUser(userID uuid.UUID, limit, offset int) ([]model.GachaHistory, error) {
	var list []model.GachaHistory
	err := r.db.
		Where("user_id = ?", userID).
		Order("created_at DESC").
		Limit(limit).
		Offset(offset).
		Find(&list).Error
	return list, err
}

// ListByBanner — バナー別のガチャ履歴を取得する
func (r *GachaRepository) ListByBanner(userID, bannerID uuid.UUID) ([]model.GachaHistory, error) {
	var list []model.GachaHistory
	err := r.db.
		Where("user_id = ? AND banner_id = ?", userID, bannerID).
		Order("pity_count ASC").
		Find(&list).Error
	return list, err
}

// ============================================================
// MasterRepository — マスタデータの取得
// マスタは管理画面等から投入する想定。ここでは読み取り中心。
// ============================================================

type MasterRepository struct {
	db *gorm.DB
}

func NewMasterRepository(db *gorm.DB) *MasterRepository {
	return &MasterRepository{db: db}
}

// --- キャラクターマスタ ---

// ListCharacters — 有効なキャラクター一覧を取得する
func (r *MasterRepository) ListCharacters() ([]model.MasterCharacter, error) {
	var list []model.MasterCharacter
	err := r.db.Where("is_active = true").Find(&list).Error
	return list, err
}

// GetCharacter — キャラクターを1件取得する
func (r *MasterRepository) GetCharacter(id uuid.UUID) (*model.MasterCharacter, error) {
	var c model.MasterCharacter
	err := r.db.Where("id = ?", id).First(&c).Error
	if err != nil {
		return nil, err
	}
	return &c, nil
}

// CreateCharacter — キャラクターマスタを追加する
func (r *MasterRepository) CreateCharacter(c *model.MasterCharacter) error {
	return r.db.Create(c).Error
}

// UpdateCharacter — キャラクターマスタを更新する
func (r *MasterRepository) UpdateCharacter(c *model.MasterCharacter) error {
	return r.db.Save(c).Error
}

// DeactivateCharacter — キャラクターを論理削除する
func (r *MasterRepository) DeactivateCharacter(id uuid.UUID) error {
	return r.db.Model(&model.MasterCharacter{}).
		Where("id = ?", id).
		Update("is_active", false).Error
}

// --- 武器マスタ ---

// ListWeapons — 有効な武器一覧を取得する
func (r *MasterRepository) ListWeapons() ([]model.MasterWeapon, error) {
	var list []model.MasterWeapon
	err := r.db.Where("is_active = true").Find(&list).Error
	return list, err
}

// GetWeapon — 武器を1件取得する
func (r *MasterRepository) GetWeapon(id uuid.UUID) (*model.MasterWeapon, error) {
	var w model.MasterWeapon
	err := r.db.Where("id = ?", id).First(&w).Error
	if err != nil {
		return nil, err
	}
	return &w, nil
}

// CreateWeapon — 武器マスタを追加する
func (r *MasterRepository) CreateWeapon(w *model.MasterWeapon) error {
	return r.db.Create(w).Error
}

// UpdateWeapon — 武器マスタを更新する
func (r *MasterRepository) UpdateWeapon(w *model.MasterWeapon) error {
	return r.db.Save(w).Error
}

// DeactivateWeapon — 武器を論理削除する
func (r *MasterRepository) DeactivateWeapon(id uuid.UUID) error {
	return r.db.Model(&model.MasterWeapon{}).
		Where("id = ?", id).
		Update("is_active", false).Error
}

// --- ダンジョンマスタ ---

// ListDungeons — 有効なダンジョン一覧を取得する（ステージ情報も含む）
func (r *MasterRepository) ListDungeons() ([]model.MasterDungeon, error) {
	var list []model.MasterDungeon
	err := r.db.
		Where("is_active = true").
		Preload("Stages", func(db *gorm.DB) *gorm.DB {
			return db.Order("stage_number ASC")
		}).
		Preload("Stages.Enemies", func(db *gorm.DB) *gorm.DB {
			return db.Order("sort_order ASC")
		}).
		Preload("Stages.Enemies.Monster").
		Order("sort_order ASC").
		Find(&list).Error
	return list, err
}

// GetDungeon — ダンジョンを1件取得する
func (r *MasterRepository) GetDungeon(id uuid.UUID) (*model.MasterDungeon, error) {
	var d model.MasterDungeon
	err := r.db.
		Preload("Stages", func(db *gorm.DB) *gorm.DB {
			return db.Order("stage_number ASC")
		}).
		Preload("Stages.Enemies", func(db *gorm.DB) *gorm.DB {
			return db.Order("sort_order ASC")
		}).
		Preload("Stages.Enemies.Monster").
		Where("id = ?", id).
		First(&d).Error
	if err != nil {
		return nil, err
	}
	return &d, nil
}

// CreateDungeon — ダンジョンマスタを追加する
func (r *MasterRepository) CreateDungeon(d *model.MasterDungeon) error {
	return r.db.Create(d).Error
}

// UpdateDungeon — ダンジョンマスタを更新する
func (r *MasterRepository) UpdateDungeon(d *model.MasterDungeon) error {
	return r.db.Save(d).Error
}

// DeactivateDungeon — ダンジョンを論理削除する
func (r *MasterRepository) DeactivateDungeon(id uuid.UUID) error {
	return r.db.Model(&model.MasterDungeon{}).
		Where("id = ?", id).
		Update("is_active", false).Error
}

// --- ステージマスタ ---

// ListStagesByDungeon — ダンジョン内のステージ一覧を取得する
func (r *MasterRepository) ListStagesByDungeon(dungeonID uuid.UUID) ([]model.MasterDungeonStage, error) {
	var list []model.MasterDungeonStage
	err := r.db.
		Where("dungeon_id = ?", dungeonID).
		Preload("Enemies", func(db *gorm.DB) *gorm.DB {
			return db.Order("sort_order ASC")
		}).
		Preload("Enemies.Monster").
		Order("stage_number ASC").
		Find(&list).Error
	return list, err
}

// CreateStage — ステージマスタを追加する
func (r *MasterRepository) CreateStage(s *model.MasterDungeonStage) error {
	return r.db.Create(s).Error
}

// UpdateStage — ステージマスタを更新する
func (r *MasterRepository) UpdateStage(s *model.MasterDungeonStage) error {
	return r.db.Save(s).Error
}

// DeleteStage — ステージマスタを物理削除する
func (r *MasterRepository) DeleteStage(id uuid.UUID) error {
	return r.db.Delete(&model.MasterDungeonStage{}, "id = ?", id).Error
}

// --- ガチャバナーマスタ ---

// ListActiveBanners — 現在有効なガチャバナー一覧を取得する
func (r *MasterRepository) ListActiveBanners() ([]model.MasterGachaBanner, error) {
	now := time.Now().UTC()
	var list []model.MasterGachaBanner
	err := r.db.
		Where("is_active = true AND start_at <= ? AND end_at > ?", now, now).
		Find(&list).Error
	return list, err
}

// GetBanner — ガチャバナーを1件取得する
func (r *MasterRepository) GetBanner(id uuid.UUID) (*model.MasterGachaBanner, error) {
	var b model.MasterGachaBanner
	err := r.db.Where("id = ?", id).First(&b).Error
	if err != nil {
		return nil, err
	}
	return &b, nil
}

// CreateBanner — ガチャバナーを追加する
func (r *MasterRepository) CreateBanner(b *model.MasterGachaBanner) error {
	return r.db.Create(b).Error
}

// UpdateBanner — ガチャバナーを更新する
func (r *MasterRepository) UpdateBanner(b *model.MasterGachaBanner) error {
	return r.db.Save(b).Error
}

// DeactivateBanner — バナーを無効化する
func (r *MasterRepository) DeactivateBanner(id uuid.UUID) error {
	return r.db.Model(&model.MasterGachaBanner{}).
		Where("id = ?", id).
		Update("is_active", false).Error
}

// ListFeaturedByBannerID — 1バナーのピックアップ行を取得する
func (r *MasterRepository) ListFeaturedByBannerID(bannerID uuid.UUID) ([]model.MasterGachaBannerFeatured, error) {
	var rows []model.MasterGachaBannerFeatured
	err := r.db.Where("banner_id = ?", bannerID).Order("id ASC").Find(&rows).Error
	return rows, err
}

// ListFeaturedByBannerIDs — 複数バナーのピックアップを一括取得する（API 一覧用）
func (r *MasterRepository) ListFeaturedByBannerIDs(bannerIDs []uuid.UUID) ([]model.MasterGachaBannerFeatured, error) {
	if len(bannerIDs) == 0 {
		return nil, nil
	}
	var rows []model.MasterGachaBannerFeatured
	err := r.db.Where("banner_id IN ?", bannerIDs).Order("banner_id, id ASC").Find(&rows).Error
	return rows, err
}

// --- 勉強ジャンルマスタ ---

// ListStudyGenres — 有効なジャンル一覧を取得する（sort_order 昇順）
func (r *MasterRepository) ListStudyGenres() ([]model.MasterStudyGenre, error) {
	var list []model.MasterStudyGenre
	err := r.db.Where("is_active = true").Order("sort_order ASC").Find(&list).Error
	return list, err
}

// CountActiveStudyGenres — 有効（is_active=true）なジャンル件数
func (r *MasterRepository) CountActiveStudyGenres() (int64, error) {
	var count int64
	err := r.db.Model(&model.MasterStudyGenre{}).Where("is_active = ?", true).Count(&count).Error
	return count, err
}

// CreateStudyGenre — ジャンルを作成する
func (r *MasterRepository) CreateStudyGenre(g *model.MasterStudyGenre) error {
	return r.db.Create(g).Error
}

// GetStudyGenre — ID でジャンルを取得する
func (r *MasterRepository) GetStudyGenre(id uuid.UUID) (*model.MasterStudyGenre, error) {
	var g model.MasterStudyGenre
	if err := r.db.First(&g, "id = ?", id).Error; err != nil {
		return nil, err
	}
	return &g, nil
}

// DeactivateStudyGenre — ジャンルを論理削除する（is_active = false）
func (r *MasterRepository) DeactivateStudyGenre(id uuid.UUID) error {
	return r.db.Model(&model.MasterStudyGenre{}).Where("id = ?", id).Update("is_active", false).Error
}
