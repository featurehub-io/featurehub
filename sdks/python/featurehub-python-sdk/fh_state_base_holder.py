from typing import Optional


class FeatureStateBaseHolder:
    """Holder for features. Wraps raw response with features dictionary"""

    internal_feature_state = {}

    def __eq__(self, other):
        if not isinstance(other, FeatureStateBaseHolder):
            return NotImplemented
        elif self is FeatureStateBaseHolder:
            return True
        else:
            return self.id == other.id and \
                   self.key == other.key and \
                   self.l == other.l and \
                   self.version == other.version \
                   and self.type == other.type and \
                   self.value == other.value

    def __init__(self, feature_state):
        self.__set_feature_state(feature_state)

    def __set_feature_state(self, feature_state):
        self.value = feature_state['value']
        self.type = feature_state['type']
        self.version = feature_state['version']
        self.l = feature_state['l']
        self.key = feature_state['key']
        self.id = feature_state['id']

    def set_feature_state(self, feature_state):
        self.__set_feature_state(feature_state)

    def get_value(self):
        return self.value

    def get_version(self) -> str:
        return self.version

    def get_key(self) -> str:
        return self.key

    def get_string(self) -> Optional[str]:
        if self.type == 'STRING':
            return self.value
        return None

    def get_number(self) -> Optional[int]:
        if self.type == 'NUMBER':
            return self.value
        return None

    def get_raw_json(self) -> Optional[str]:
        if self.type == 'JSON':
            return self.value
        return None

    def get_boolean(self) -> Optional[bool]:
        if self.type == 'BOOLEAN':
            return self.value
        return None

    def get_flag(self) -> bool:
        return self.get_boolean()

    def is_enabled(self) -> bool:
        return self.get_boolean() is True
