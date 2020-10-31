package client

import (
	"fmt"
	"net/url"
)

// Context defines metadata for the client:
// This is sent to the FeatureHub server, and powers the rollout strategy decisions.
type Context struct {
	Userkey  string // Unique key which will be hashed to calculate percentage rollouts
	Session  string // Session ID key
	Device   string // [browser, mobile, desktop]
	Platform string // [linux, windows,	macos, android, ios]
	Country  string // Country / geographic region: https://www.britannica.com/topic/list-of-countries-1993160
	Version  string // Version of the client
}

// String concatenates the context and URL encodes it:
func (c *Context) String() string {
	return url.QueryEscape(fmt.Sprintf("userkey=%s,session=%s,device=%s,platform=%s,country=%s,version=%s", c.Userkey, c.Session, c.Device, c.Platform, c.Country, c.Version))
}
