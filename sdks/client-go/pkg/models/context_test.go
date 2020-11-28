package models

import (
	"net/url"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestContext(t *testing.T) {

	// Make a new context:
	context := &Context{
		Userkey:  "some-random-string",
		Session:  "some-session-ID",
		Device:   "desktop",
		Platform: "macos",
		Country:  "new_zealand",
		Version:  "5.0.0",
	}

	assert.Equal(t, url.QueryEscape("userkey=some-random-string,session=some-session-ID,device=desktop,platform=macos,country=new_zealand,version=5.0.0"), context.String())
}
