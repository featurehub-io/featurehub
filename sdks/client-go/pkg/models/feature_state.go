package models

// FeatureState defines model for FeatureState.
type FeatureState struct {
	ID       string           `json:"id,omitempty"`       // ID
	Key      string           `json:"key,omitempty"`      // Name of the feature
	Strategy Strategy         `json:"strategy,omitempty"` // Rollout strategy
	Type     FeatureValueType `json:"type,omitempty"`     // Data type
	Value    interface{}      `json:"value,omitempty"`    // the current value
	Version  int64            `json:"version,omitempty"`  // Version
}
