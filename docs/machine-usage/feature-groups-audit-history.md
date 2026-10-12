Action log does not update for feature groups

There is an outstanding bug associated with feature groups (DbFeatureGroup functionality hosted in 
When associating a feature group with a feature value FeatureGoupSqlApi). Feature Groups are associated with
an environment and adding a feature to a feature group, updating the strategy defined in the feature group or changing the value associate with a feature for that environment should cause an update in the audit history 
represented by DbFeatureValueVersion. It is also not generating
a webhook (FeatureMessagingUpdate in messaging-api.yaml is missing strategies associated with the feature value
and when changes occur). Recommend fixes. 
