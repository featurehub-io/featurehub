package models

// Strategy defines model for Strategy.
type Strategy struct {
	Name  string          `json:"name"`
	Pairs []*StrategyPair `json:"pairs,omitempty"`
	Value interface{}     `json:"value,omitempty"` // this value is used if it is a simple attribute or percentage. If it is more complex then the pairs are passed
}

// StrategyPair defines model for StrategyPair.
type StrategyPair struct {
	Name  string `json:"name"`
	Value string `json:"value,omitempty"`
}
