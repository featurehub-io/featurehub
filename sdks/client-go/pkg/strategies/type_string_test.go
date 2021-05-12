package strategies

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestStringTypeAssertion(t *testing.T) {
	_, err := String(ConditionalEquals, []interface{}{123, false}, "string")
	assert.Error(t, err)

	_, err = String(ConditionalEquals, []interface{}{"string"}, 9)
	assert.Error(t, err)

	_, err = String(ConditionalEquals, []interface{}{"string"}, "string")
	assert.NoError(t, err)
}

func TestStringEquals(t *testing.T) {
	assert.True(t, evaluateString(ConditionalEquals, []string{"blue", "green"}, "blue"))
	assert.False(t, evaluateString(ConditionalEquals, []string{"blue", "green"}, "yellow"))
}

func TestStringGreater(t *testing.T) {
	assert.True(t, evaluateString(ConditionalGreater, []string{"blue", "green"}, "orange"))
	assert.False(t, evaluateString(ConditionalGreater, []string{"blue", "green"}, "brown"))
	assert.False(t, evaluateString(ConditionalGreater, []string{"blue", "green"}, "blue"))
}

func TestStringGreaterEquals(t *testing.T) {
	assert.True(t, evaluateString(ConditionalGreaterEquals, []string{"blue", "green"}, "orange"))
	assert.False(t, evaluateString(ConditionalGreaterEquals, []string{"blue", "green"}, "brown"))
	assert.True(t, evaluateString(ConditionalGreaterEquals, []string{"blue", "green"}, "green"))
}

func TestStringLess(t *testing.T) {
	assert.False(t, evaluateString(ConditionalLess, []string{"blue", "green"}, "orange"))
	assert.False(t, evaluateString(ConditionalLess, []string{"blue", "green"}, "brown"))
	assert.True(t, evaluateString(ConditionalLess, []string{"blue", "green"}, "aquamarine"))
}

func TestStringLessEquals(t *testing.T) {
	assert.False(t, evaluateString(ConditionalLessEquals, []string{"blue", "green"}, "orange"))
	assert.True(t, evaluateString(ConditionalLessEquals, []string{"blue", "green"}, "blue"))
	assert.True(t, evaluateString(ConditionalLessEquals, []string{"blue", "green"}, "aquamarine"))
}

func TestStringNotEquals(t *testing.T) {
	assert.False(t, evaluateString(ConditionalNotEquals, []string{"blue", "green"}, "blue"))
	assert.True(t, evaluateString(ConditionalNotEquals, []string{"blue", "green"}, "yellow"))
}
