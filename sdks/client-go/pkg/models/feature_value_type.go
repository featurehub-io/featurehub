package models

const (
	// TypeBoolean is a basic boolean:
	TypeBoolean FeatureValueType = "BOOLEAN"
	// TypeFeature is a generic feature:
	TypeFeature FeatureValueType = "FEATURE"
	// TypeString is a basic string:
	TypeString FeatureValueType = "STRING"
	// TypeNumber is a basic number (float64):
	TypeNumber FeatureValueType = "NUMBER"
	// TypeJSON is a serialised JSON string:
	TypeJSON FeatureValueType = "JSON"
)

// FeatureValueType defines model for FeatureValueType.
type FeatureValueType string
