package strategies

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestNumberTypeAssertion(t *testing.T) {
	_, err := Number(ConditionalEquals, []interface{}{"string", false}, 123)
	assert.Error(t, err)

	_, err = Number(ConditionalEquals, []interface{}{55.123}, "string")
	assert.Error(t, err)

	_, err = Number(ConditionalEquals, []interface{}{55.123}, 44.5)
	assert.NoError(t, err)
}

func TestNumberEquals(t *testing.T) {
	assert.True(t, evaluateNumber(ConditionalEquals, []float64{1.2, 1.4}, 1.2))
	assert.False(t, evaluateNumber(ConditionalEquals, []float64{1.2, 1.4}, 1.3))
}

func TestNumberNotEquals(t *testing.T) {
	assert.False(t, evaluateNumber(ConditionalNotEquals, []float64{1.2, 1.4}, 1.2))
	assert.True(t, evaluateNumber(ConditionalNotEquals, []float64{1.2, 1.4}, 1.3))
}

func TestNumberEndsWith(t *testing.T) {
	assert.True(t, evaluateNumber(ConditionalEndsWith, []float64{1.2, float64(234)}, 1.234))
	assert.False(t, evaluateNumber(ConditionalEndsWith, []float64{1.2, 1.4}, 1.3))
}

func TestNumberStartsWith(t *testing.T) {
	assert.True(t, evaluateNumber(ConditionalStartsWith, []float64{1.2, 2.234}, 1.234))
	assert.False(t, evaluateNumber(ConditionalStartsWith, []float64{1.2, 1.4}, 1.3))
}

func TestNumberLess(t *testing.T) {
	assert.True(t, evaluateNumber(ConditionalLess, []float64{1.2, 1.4}, 1.1))
	assert.False(t, evaluateNumber(ConditionalLess, []float64{1.2, 1.4}, 1.2))
	assert.False(t, evaluateNumber(ConditionalLess, []float64{1.2, 1.4}, 1.3))
}

func TestNumberLessEquals(t *testing.T) {
	assert.True(t, evaluateNumber(ConditionalLessEquals, []float64{1.2, 1.4}, 1.1))
	assert.True(t, evaluateNumber(ConditionalLessEquals, []float64{1.2, 1.4}, 1.2))
	assert.False(t, evaluateNumber(ConditionalLessEquals, []float64{1.2, 1.4}, 1.3))
}

func TestNumberGreater(t *testing.T) {
	assert.False(t, evaluateNumber(ConditionalGreater, []float64{1.2, 1.4}, 1.1))
	assert.False(t, evaluateNumber(ConditionalGreater, []float64{1.2, 1.4}, 1.4))
	assert.True(t, evaluateNumber(ConditionalGreater, []float64{1.2, 1.4}, 1.5))
}

func TestNumberGreaterEquals(t *testing.T) {
	assert.False(t, evaluateNumber(ConditionalGreaterEquals, []float64{1.2, 1.4}, 1.1))
	assert.True(t, evaluateNumber(ConditionalGreaterEquals, []float64{1.2, 1.4}, 1.4))
	assert.True(t, evaluateNumber(ConditionalGreaterEquals, []float64{1.2, 1.4}, 1.5))
}

func TestNumberExcludes(t *testing.T) {
	assert.True(t, evaluateNumber(ConditionalExcludes, []float64{1.2, float64(234)}, 1.234))
	assert.False(t, evaluateNumber(ConditionalExcludes, []float64{1.2, 1.4}, float64(4)))
}

func TestNumberIncludes(t *testing.T) {
	assert.False(t, evaluateNumber(ConditionalIncludes, []float64{1.2, float64(234)}, 1.234))
	assert.True(t, evaluateNumber(ConditionalIncludes, []float64{1.2, 1.4}, float64(4)))
}
