package models

import "github.com/sirupsen/logrus"

// logger is a package-global logger which can be used by model methods (this prevents us having to pass a logger all over the place):
var logger *logrus.Logger = logrus.New()

// SetLogger allows other packages to update the logger:
func SetLogger(newLogger *logrus.Logger) {
	logger = newLogger
}
