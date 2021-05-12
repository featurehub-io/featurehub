package strategies

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestSemanticVersionTypeAssertion(t *testing.T) {
	_, err := SemanticVersion(ConditionalEquals, []interface{}{123, false}, "0.1.2")
	assert.Error(t, err)

	_, err = SemanticVersion(ConditionalEquals, []interface{}{"0.1.2"}, 9)
	assert.Error(t, err)

	_, err = SemanticVersion(ConditionalEquals, []interface{}{"0.1.2"}, "0.1.2")
	assert.NoError(t, err)
}

func TestSemanticVersionEquals(t *testing.T) {
	assert.True(t, evaluateSemanticVersion(ConditionalEquals, []string{"1.0.0", "2.0.0"}, "1.0.0"))
	assert.False(t, evaluateSemanticVersion(ConditionalEquals, []string{"1.0.0", "2.0.0"}, "3.0.0"))
}

func TestSemanticVersionGreater(t *testing.T) {
	assert.True(t, evaluateSemanticVersion(ConditionalGreater, []string{"3.0.0", "2.0.0"}, "10.0.0"))
	assert.False(t, evaluateSemanticVersion(ConditionalGreater, []string{"5.0.0", "4.0.0"}, "3.2.0"))
	assert.False(t, evaluateSemanticVersion(ConditionalGreater, []string{"5.0.0", "4.0.0"}, "5.0.0"))
	assert.False(t, evaluateSemanticVersion(ConditionalGreater, []string{"5.5.5", "6.16.6"}, "6.6.6"))
}

func TestSemanticVersionGreaterEquals(t *testing.T) {
	assert.True(t, evaluateSemanticVersion(ConditionalGreaterEquals, []string{"3.0.0", "2.0.0"}, "10.0.0"))
	assert.False(t, evaluateSemanticVersion(ConditionalGreaterEquals, []string{"5.0.0", "4.0.0"}, "3.2.0"))
	assert.True(t, evaluateSemanticVersion(ConditionalGreaterEquals, []string{"5.0.0", "4.0.0"}, "5.0.0"))
	assert.False(t, evaluateSemanticVersion(ConditionalGreaterEquals, []string{"5.5.5", "6.16.6"}, "6.6.6"))
}

func TestSemanticVersionLess(t *testing.T) {
	assert.False(t, evaluateSemanticVersion(ConditionalLess, []string{"3.0.0", "2.0.0"}, "10.0.0"))
	assert.True(t, evaluateSemanticVersion(ConditionalLess, []string{"5.0.0", "4.0.0"}, "3.2.0"))
	assert.False(t, evaluateSemanticVersion(ConditionalLess, []string{"5.0.0", "4.0.0"}, "5.0.0"))
	assert.False(t, evaluateSemanticVersion(ConditionalLess, []string{"5.5.5", "6.16.6"}, "6.6.6"))
}

func TestSemanticVersionLessEquals(t *testing.T) {
	assert.False(t, evaluateSemanticVersion(ConditionalLessEquals, []string{"3.0.0", "2.0.0"}, "10.0.0"))
	assert.True(t, evaluateSemanticVersion(ConditionalLessEquals, []string{"5.0.0", "4.0.0"}, "3.2.0"))
	assert.True(t, evaluateSemanticVersion(ConditionalLessEquals, []string{"5.0.0", "4.0.0"}, "4.0.0"))
	assert.False(t, evaluateSemanticVersion(ConditionalLessEquals, []string{"5.5.5", "6.16.6"}, "6.6.6"))
}

func TestSemanticVersionNotEquals(t *testing.T) {
	assert.False(t, evaluateSemanticVersion(ConditionalNotEquals, []string{"1.0.0", "2.0.0"}, "1.0.0"))
	assert.True(t, evaluateSemanticVersion(ConditionalNotEquals, []string{"1.0.0", "2.0.0"}, "3.0.0"))
}
