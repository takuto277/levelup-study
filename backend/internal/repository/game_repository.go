package repository

import (
	"github.com/google/uuid"
	"github.com/takuto277/levelup-study/backend/internal/model"
	"gorm.io/gorm"
	"gorm.io/gorm/clause"
)

// ============================================================
// CharacterRepository — ユーザー所持キャラの CRUD
// ============================================================

type CharacterRepository struct {
	db *gorm.DB
}

func NewCharacterRepository(db *gorm.DB) *CharacterRepository {
	return &CharacterRepository{db: db}
}

// Create — キャラクターをユーザーに付与する（ガチャ結果の保存等）
func (r *CharacterRepository) Create(tx *gorm.DB, uc *model.UserCharacter) error {
	return tx.Create(uc).Error
}

// GetByID — 所持キャラを1件取得する（マスタ情報・装備武器も含む）
func (r *CharacterRepository) GetByID(id uuid.UUID) (*model.UserCharacter, error) {
	var uc model.UserCharacter
	err := r.db.
		Preload("Character").
		Preload("Weapon").
		Where("id = ?", id).
		First(&uc).Error
	if err != nil {
		return nil, err
	}
	return &uc, nil
}

// GetByIDForUserTx — トランザクション内で user_id を検証して所持キャラを取得する
func (r *CharacterRepository) GetByIDForUserTx(tx *gorm.DB, id uuid.UUID, userID uuid.UUID) (*model.UserCharacter, error) {
	var uc model.UserCharacter
	err := tx.First(&uc, "id = ? AND user_id = ?", id, userID).Error
	if err != nil {
		return nil, err
	}
	return &uc, nil
}

// ListByUser — ユーザーの所持キャラ一覧を取得する
func (r *CharacterRepository) ListByUser(userID uuid.UUID) ([]model.UserCharacter, error) {
	var list []model.UserCharacter
	err := r.db.
		Preload("Character").
		Preload("Weapon").
		Where("user_id = ?", userID).
		Find(&list).Error
	return list, err
}

// AddXP — 経験値を加算する（トランザクション内で使う）
func (r *CharacterRepository) AddXP(tx *gorm.DB, id uuid.UUID, xp int) error {
	return tx.Model(&model.UserCharacter{}).
		Where("id = ?", id).
		Update("current_xp", gorm.Expr("current_xp + ?", xp)).Error
}

// LevelUp — レベルを更新する
func (r *CharacterRepository) LevelUp(tx *gorm.DB, id uuid.UUID, newLevel int) error {
	return tx.Model(&model.UserCharacter{}).
		Where("id = ?", id).
		Update("level", newLevel).Error
}

// UpdateLevelAndXP — レベルと進捗XPをまとめて更新する（勉強報酬の一括反映用）
func (r *CharacterRepository) UpdateLevelAndXP(tx *gorm.DB, id uuid.UUID, level int, currentXP int) error {
	return tx.Model(&model.UserCharacter{}).
		Where("id = ?", id).
		Updates(map[string]interface{}{
			"level":       level,
			"current_xp":  currentXP,
		}).Error
}

// EquipWeapon — 武器を装備する（null で装備解除）
func (r *CharacterRepository) EquipWeapon(id uuid.UUID, weaponID *uuid.UUID) error {
	return r.db.Model(&model.UserCharacter{}).
		Where("id = ?", id).
		Update("equipped_weapon_id", weaponID).Error
}

// Delete — 所持キャラを削除する
func (r *CharacterRepository) Delete(id uuid.UUID) error {
	return r.db.Delete(&model.UserCharacter{}, "id = ?", id).Error
}

// ============================================================
// WeaponRepository — ユーザー所持武器の CRUD
// ============================================================

type WeaponRepository struct {
	db *gorm.DB
}

func NewWeaponRepository(db *gorm.DB) *WeaponRepository {
	return &WeaponRepository{db: db}
}

// Create — 武器をユーザーに付与する
func (r *WeaponRepository) Create(tx *gorm.DB, uw *model.UserWeapon) error {
	return tx.Create(uw).Error
}

// GetByID — 所持武器を1件取得する（マスタ情報含む）
func (r *WeaponRepository) GetByID(id uuid.UUID) (*model.UserWeapon, error) {
	var uw model.UserWeapon
	err := r.db.Preload("Weapon").Where("id = ?", id).First(&uw).Error
	if err != nil {
		return nil, err
	}
	return &uw, nil
}

// ListByUser — ユーザーの所持武器一覧を取得する
func (r *WeaponRepository) ListByUser(userID uuid.UUID) ([]model.UserWeapon, error) {
	var list []model.UserWeapon
	err := r.db.Preload("Weapon").Where("user_id = ?", userID).Find(&list).Error
	return list, err
}

// LevelUp — 武器レベルを更新する
func (r *WeaponRepository) LevelUp(id uuid.UUID, newLevel int) error {
	return r.db.Model(&model.UserWeapon{}).
		Where("id = ?", id).
		Update("level", newLevel).Error
}

// Delete — 所持武器を削除する
func (r *WeaponRepository) Delete(id uuid.UUID) error {
	return r.db.Delete(&model.UserWeapon{}, "id = ?", id).Error
}

// ============================================================
// PartyRepository — パーティ編成の CRUD
// ============================================================

type PartyRepository struct {
	db *gorm.DB
}

func NewPartyRepository(db *gorm.DB) *PartyRepository {
	return &PartyRepository{db: db}
}

// Upsert — スロットにキャラを配置する（既存なら上書き）
func (r *PartyRepository) Upsert(slot *model.UserPartySlot) error {
	return r.db.Clauses(clause.OnConflict{
		Columns:   []clause.Column{{Name: "user_id"}, {Name: "slot_position"}},
		DoUpdates: clause.AssignmentColumns([]string{"user_character_id"}),
	}).Create(slot).Error
}

// GetByUser — ユーザーのパーティ編成を取得する（スロット順）
func (r *PartyRepository) GetByUser(userID uuid.UUID) ([]model.UserPartySlot, error) {
	return r.GetByUserTx(r.db, userID)
}

// GetByUserTx — トランザクション上でパーティ編成を取得する（スロット昇順）
func (r *PartyRepository) GetByUserTx(tx *gorm.DB, userID uuid.UUID) ([]model.UserPartySlot, error) {
	var slots []model.UserPartySlot
	err := tx.
		Preload("UserCharacter").
		Preload("UserCharacter.Character").
		Where("user_id = ?", userID).
		Order("slot_position ASC").
		Find(&slots).Error
	return slots, err
}

// RemoveSlot — スロットからキャラを外す
func (r *PartyRepository) RemoveSlot(userID uuid.UUID, slotPosition int) error {
	return r.db.
		Where("user_id = ? AND slot_position = ?", userID, slotPosition).
		Delete(&model.UserPartySlot{}).Error
}

// ClearAll — ユーザーのパーティを全解除する
func (r *PartyRepository) ClearAll(userID uuid.UUID) error {
	return r.db.Where("user_id = ?", userID).Delete(&model.UserPartySlot{}).Error
}

// ============================================================
// DungeonProgressRepository — ダンジョン進行状況の CRUD
// ============================================================

type DungeonProgressRepository struct {
	db *gorm.DB
}

func NewDungeonProgressRepository(db *gorm.DB) *DungeonProgressRepository {
	return &DungeonProgressRepository{db: db}
}

// Upsert — ダンジョン進行状況を作成/更新する
func (r *DungeonProgressRepository) Upsert(tx *gorm.DB, progress *model.UserDungeonProgress) error {
	return tx.Clauses(clause.OnConflict{
		Columns:   []clause.Column{{Name: "user_id"}, {Name: "dungeon_id"}},
		DoUpdates: clause.AssignmentColumns([]string{"current_stage", "max_cleared_stage"}),
	}).Create(progress).Error
}

// GetByUserAndDungeon — 特定ダンジョンの進行状況を取得する
func (r *DungeonProgressRepository) GetByUserAndDungeon(userID, dungeonID uuid.UUID) (*model.UserDungeonProgress, error) {
	var progress model.UserDungeonProgress
	err := r.db.
		Where("user_id = ? AND dungeon_id = ?", userID, dungeonID).
		First(&progress).Error
	if err != nil {
		return nil, err
	}
	return &progress, nil
}

// ListByUser — ユーザーの全ダンジョン進行状況を取得する
func (r *DungeonProgressRepository) ListByUser(userID uuid.UUID) ([]model.UserDungeonProgress, error) {
	var list []model.UserDungeonProgress
	err := r.db.
		Preload("Dungeon").
		Where("user_id = ?", userID).
		Find(&list).Error
	return list, err
}

// AdvanceStage — ステージを進める（トランザクション内で使う）
func (r *DungeonProgressRepository) AdvanceStage(tx *gorm.DB, id uuid.UUID, newStage int) error {
	return tx.Model(&model.UserDungeonProgress{}).
		Where("id = ?", id).
		Updates(map[string]interface{}{
			"current_stage":     newStage,
			"max_cleared_stage": gorm.Expr("GREATEST(max_cleared_stage, ?)", newStage-1),
		}).Error
}
