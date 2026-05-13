package middleware

import (
	"strings"
	"testing"
)

func TestRedactSecrets(t *testing.T) {
	in := `Authorization: Bearer eyJhbGciOiJIUzI1NiJ.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U`
	got := redactSecrets(in)
	if got != `Authorization: Bearer <redacted>` {
		t.Fatalf("Bearer: got %q", got)
	}
	j := `{"access_token":"secret123","refresh_token":"r2","password":"x"}`
	got2 := redactSecrets(j)
	if strings.Contains(got2, "secret123") || strings.Contains(got2, "r2") || strings.Contains(got2, `"password":"x"`) {
		t.Fatalf("JSON tokens not redacted: %q", got2)
	}
}

func TestCompactOneLine(t *testing.T) {
	got := compactOneLine("a\nb\rc   d")
	if got != "a b c d" {
		t.Fatalf("got %q", got)
	}
}
