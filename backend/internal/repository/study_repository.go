package repository

import (
	"time"

	"github.com/google/uuid"
	"github.com/takuto277/levelup-study/backend/internal/model"
	"gorm.io/gorm"
)

// ============================================================
// StudyRepository — 勉強セッション & 報酬の CRUD 操作
// ============================================================

type StudyRepository struct {
	db *gorm.DB
}

// NewStudyRepository — コンストラクタ
func NewStudyRepository(db *gorm.DB) *StudyRepository {
	return &StudyRepository{db: db}
}

// --- セッション ---

// CreateSession — 勉強セッションを保存する
func (r *StudyRepository) CreateSession(tx *gorm.DB, session *model.StudySession) error {
	return tx.Create(session).Error
}

// GetSessionByID — セッションを1件取得（報酬明細も含む）
func (r *StudyRepository) GetSessionByID(id uuid.UUID) (*model.StudySession, error) {
	var session model.StudySession
	err := r.db.Preload("Rewards").Where("id = ?", id).First(&session).Error
	if err != nil {
		return nil, err
	}
	return &session, nil
}

// ListSessionsByUser — ユーザーのセッション一覧を取得する（新しい順）
func (r *StudyRepository) ListSessionsByUser(userID uuid.UUID, limit, offset int) ([]model.StudySession, error) {
	var sessions []model.StudySession
	err := r.db.
		Where("user_id = ?", userID).
		Order("started_at DESC").
		Limit(limit).
		Offset(offset).
		Find(&sessions).Error
	return sessions, err
}

// GetDailyStudySeconds — 指定日のユーザーの合計勉強秒数を取得する（日次ボーナス判定用）
func (r *StudyRepository) GetDailyStudySeconds(userID uuid.UUID, date time.Time) (int64, error) {
	// date の 00:00:00 〜 23:59:59 (UTC) の範囲で合計する
	startOfDay := time.Date(date.Year(), date.Month(), date.Day(), 0, 0, 0, 0, time.UTC)
	endOfDay := startOfDay.Add(24 * time.Hour)

	var total int64
	err := r.db.Model(&model.StudySession{}).
		Where("user_id = ? AND started_at >= ? AND started_at < ?", userID, startOfDay, endOfDay).
		Select("COALESCE(SUM(duration_seconds), 0)").
		Scan(&total).Error
	return total, err
}

// DeleteSession — セッションを削除する（報酬も CASCADE で消える）
func (r *StudyRepository) DeleteSession(id uuid.UUID) error {
	return r.db.Delete(&model.StudySession{}, "id = ?", id).Error
}

// --- 報酬 ---

// CreateRewards — 報酬明細を一括保存する
func (r *StudyRepository) CreateRewards(tx *gorm.DB, rewards []model.StudyReward) error {
	if len(rewards) == 0 {
		return nil
	}
	return tx.Create(&rewards).Error
}

// ListRewardsBySession — セッションIDに紐づく報酬一覧を取得する
func (r *StudyRepository) ListRewardsBySession(sessionID uuid.UUID) ([]model.StudyReward, error) {
	var rewards []model.StudyReward
	err := r.db.Where("session_id = ?", sessionID).Find(&rewards).Error
	return rewards, err
}
