package service

import (
	"testing"

	"github.com/google/uuid"
	"github.com/takuto277/levelup-study/backend/internal/model"
)

func TestMergeFeaturedIntoRateTable(t *testing.T) {
	a := uuid.MustParse("a0000000-0000-0000-0000-000000000001")
	base := []RateTableEntry{
		{ItemID: a, ResultType: "character", Rarity: 5, Rate: 0.5},
		{ItemID: uuid.MustParse("a0000000-0000-0000-0000-000000000002"), ResultType: "character", Rarity: 4, Rate: 0.5},
	}
	feat := []model.MasterGachaBannerFeatured{
		{ItemID: a, ItemType: "character", RateUp: 0.5},
	}
	out, err := mergeFeaturedIntoRateTable(base, feat)
	if err != nil {
		t.Fatal(err)
	}
	if len(out) != 2 {
		t.Fatalf("len=%d", len(out))
	}
	// 0.5 * 1.5 = 0.75, other 0.5 → normalized 0.75/1.25=0.6, 0.5/1.25=0.4
	if out[0].Rate < 0.59 || out[0].Rate > 0.61 {
		t.Errorf("first rate: got=%v want~0.6", out[0].Rate)
	}
	sum := 0.0
	for _, e := range out {
		sum += e.Rate
	}
	if sum < 0.999 || sum > 1.001 {
		t.Errorf("sum of rates: got=%v want 1", sum)
	}
}

func TestNormalizeRateTableRates(t *testing.T) {
	base := []RateTableEntry{
		{Rarity: 3, Rate: 0.3},
		{Rarity: 3, Rate: 0.3},
	}
	out, err := normalizeRateTableRates(base)
	if err != nil {
		t.Fatal(err)
	}
	if out[0].Rate != 0.5 || out[1].Rate != 0.5 {
		t.Errorf("got %+v", out)
	}
}
