package models

// CallbackFuncFeature defines signature used for notifier callback functions:
type CallbackFuncFeature func(*FeatureState)

// CallbackFuncBoolean defines signature used for notifier callback functions:
type CallbackFuncBoolean func(bool)

// CallbackFuncJSON defines signature used for notifier callback functions:
type CallbackFuncJSON func(string)

// CallbackFuncNumber defines signature used for notifier callback functions:
type CallbackFuncNumber func(float64)

// CallbackFuncString defines signature used for notifier callback functions:
type CallbackFuncString func(string)
