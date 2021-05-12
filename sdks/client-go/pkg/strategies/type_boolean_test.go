package strategies

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestBooleanVersionTypeAssertion(t *testing.T) {
	_, err := Boolean(ConditionalEquals, []interface{}{123, false}, false)
	assert.Error(t, err)

	_, err = Boolean(ConditionalEquals, []interface{}{true}, 9)
	assert.Error(t, err)

	_, err = Boolean(ConditionalEquals, []interface{}{true}, true)
	assert.NoError(t, err)
}

func TestBooleanEquals(t *testing.T) {
	assert.True(t, evaluateBoolean(ConditionalEquals, []bool{true}, true))
	assert.False(t, evaluateBoolean(ConditionalEquals, []bool{true}, false))
}

func TestBooleanNotEquals(t *testing.T) {
	assert.True(t, evaluateBoolean(ConditionalNotEquals, []bool{true}, false))
	assert.False(t, evaluateBoolean(ConditionalNotEquals, []bool{true}, true))
}
