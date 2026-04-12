package service

import (
	"fmt"
	"strings"

	"github.com/takuto277/levelup-study/backend/internal/model"
)

// mergeFeaturedIntoRateTable — rate_table の各エントリに、ピックアップの rate_up を反映する。
// 一致条件: item_id が同一かつ item_type（character / weapon / costume）が result_type と一致（大小無視）。
// 複数の featured 行が同一エントリにマッチした場合、係数 (1+rate_up) を順に乗算する。
// その後、全 rate の合計が 1 になるよう正規化する（抽選の累積確率を安定させる）。
func mergeFeaturedIntoRateTable(base []RateTableEntry, featured []model.MasterGachaBannerFeatured) ([]RateTableEntry, error) {
	if len(base) == 0 {
		return nil, fmt.Errorf("rate_table が空です")
	}
	out := make([]RateTableEntry, len(base))
	copy(out, base)

	for i := range out {
		w := out[i].Rate
		if w < 0 {
			return nil, fmt.Errorf("rate_table に負の rate があります")
		}
		for _, f := range featured {
			if f.ItemID == out[i].ItemID && strings.EqualFold(f.ItemType, out[i].ResultType) {
				w *= (1.0 + f.RateUp)
			}
		}
		out[i].Rate = w
	}
	return normalizeRateTableRates(out)
}

func normalizeRateTableRates(entries []RateTableEntry) ([]RateTableEntry, error) {
	sum := 0.0
	for _, e := range entries {
		if e.Rate < 0 {
			return nil, fmt.Errorf("rate_table に負の rate があります")
		}
		sum += e.Rate
	}
	if sum <= 0 {
		return nil, fmt.Errorf("rate_table の重みの合計が 0 以下です")
	}
	out := make([]RateTableEntry, len(entries))
	for i, e := range entries {
		out[i] = e
		out[i].Rate = e.Rate / sum
	}
	return out, nil
}
