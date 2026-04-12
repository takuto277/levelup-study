package model

// EffectiveHP — マスタ基準HP + レベル上昇（Lv1 は基準のみ。1レベ上がるごと +100）
func EffectiveHP(mc *MasterCharacter, level int) int {
	if mc == nil {
		return 0
	}
	if level < 1 {
		level = 1
	}
	return mc.BaseHP + (level-1)*100
}

// EffectiveATK — マスタ基準ATK + レベル上昇（1レベごと +10）
func EffectiveATK(mc *MasterCharacter, level int) int {
	if mc == nil {
		return 0
	}
	if level < 1 {
		level = 1
	}
	return mc.BaseATK + (level-1)*10
}

// EffectiveDEF — マスタ基準DEF + レベル上昇（1レベごと +10）
func EffectiveDEF(mc *MasterCharacter, level int) int {
	if mc == nil {
		return 0
	}
	if level < 1 {
		level = 1
	}
	return mc.BaseDEF + (level-1)*10
}
