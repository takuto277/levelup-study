package service

import "testing"

func TestXpRequiredForNextLevel(t *testing.T) {
	cases := []struct {
		level int
		want  int
	}{
		{1, 300},
		{2, 400},
		{10, 1200},
		{11, 1300},
	}
	for _, tc := range cases {
		if got := xpRequiredForNextLevel(tc.level); got != tc.want {
			t.Errorf("xpRequiredForNextLevel(%d) = %d, want %d", tc.level, got, tc.want)
		}
	}
}
