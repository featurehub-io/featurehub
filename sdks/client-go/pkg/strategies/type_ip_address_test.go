package strategies

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestIPAddressTypeAssertion(t *testing.T) {
	_, err := IPAddress(ConditionalEquals, []interface{}{123, false}, "10.1.1.6/32")
	assert.Error(t, err)

	_, err = IPAddress(ConditionalEquals, []interface{}{"10.0.0.0/16"}, 9)
	assert.Error(t, err)

	_, err = IPAddress(ConditionalEquals, []interface{}{"10.0.0.0/16"}, "10.1.1.6/32")
	assert.NoError(t, err)
}

func TestIPAddressEquals(t *testing.T) {
	assert.True(t, evaluateIPAddress(ConditionalEquals, []string{"1.2.3.4"}, "1.2.3.4"))
	assert.False(t, evaluateIPAddress(ConditionalEquals, []string{"1.2.3.4"}, "5.6.7.8"))
}

func TestIPAddressNotEquals(t *testing.T) {
	assert.False(t, evaluateIPAddress(ConditionalNotEquals, []string{"1.2.3.4", "5.6.7.8"}, "1.2.3.4"))
	assert.True(t, evaluateIPAddress(ConditionalNotEquals, []string{"1.2.3.4"}, "5.6.7.8"))
}

func TestIPAddressExcludes(t *testing.T) {
	assert.True(t, evaluateIPAddress(ConditionalExcludes, []string{"10.2.0.0/24"}, "1.3.3.4/32"))
	assert.True(t, evaluateIPAddress(ConditionalExcludes, []string{"10.0.0.0/16"}, "10.1.1.6/32"))
	assert.False(t, evaluateIPAddress(ConditionalExcludes, []string{"10.1.0.0/16"}, "10.1.1.6/32"))
}

func TestIPAddressIncludes(t *testing.T) {
	assert.False(t, evaluateIPAddress(ConditionalIncludes, []string{"10.2.0.0/24"}, "1.3.3.4/32"))
	assert.False(t, evaluateIPAddress(ConditionalIncludes, []string{"10.0.0.0/16"}, "10.1.1.6/32"))
	assert.True(t, evaluateIPAddress(ConditionalIncludes, []string{"10.1.0.0/16"}, "10.1.1.6/32"))
	assert.True(t, evaluateIPAddress(ConditionalIncludes, []string{"10.1.0.0/16"}, "10.1.2.0/24"))
}
