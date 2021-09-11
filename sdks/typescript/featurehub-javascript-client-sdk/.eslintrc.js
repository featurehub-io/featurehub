module.exports = {
  root: true,
  parser: '@typescript-eslint/parser',
  plugins: [
    '@typescript-eslint',
    "filenames-simple"
  ],
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
  ],
  rules: {
    "semi": "off",
    "@typescript-eslint/semi": ["error"],
    "no-mixed-spaces-and-tabs": "error",
    "no-tabs": "error",
    "quotes": ["error", "single", {
      avoidEscape: true
    }],
    "camelcase": "warn",
    "no-trailing-spaces": "warn",
    "filenames-simple/extname": ["error", "lowercase"],
    "filenames-simple/naming-convention": ["error", {
      "rule": "snake_case"
    }],
    "@typescript-eslint/no-var-requires": "off",
    "@typescript-eslint/naming-convention": [
      "error",
      {
        "selector": "default",
        "format": ["camelCase"]
      },

      {
        "selector": "variable",
        "format": ["camelCase", "UPPER_CASE"]
      },
      {
        "selector": "parameter",
        "format": ["camelCase"],
        "leadingUnderscore": "allow"
      },

      {
        "selector": "memberLike",
        "modifiers": ["private"],
        "format": ["camelCase"],
        "leadingUnderscore": "allow"
      },

      {
        "selector": "typeLike",
        "format": ["PascalCase"]
      }
    ],
    "space-before-function-paren": ["warn", {
      "anonymous": "always",
      "named": "never",
      "asyncArrow": "always"
    }],
    "spaced-comment": ["warn", "always", {
      "markers": ["/"]
    }],
    "object-curly-spacing": ["warn", "always"],
    "space-infix-ops": "warn",
    "space-unary-ops": ["warn", {
      "words": true,
      "nonwords": false,
    }],
    "space-in-parens": ["warn", "never"],
    "indent": ["warn", 2, {
      "SwitchCase": 1
    }],
    "space-before-blocks": ["warn", "always"],
    "keyword-spacing": "warn",
    "key-spacing": "warn",
    "no-multiple-empty-lines": "warn",
    "no-use-before-define": "error",
    "require-await": "warn",
    "@typescript-eslint/member-delimiter-style": "warn",
  },
  "overrides": [{
    "files": ["app/**/**.ts", "test/**/**.ts"],
    "rules": {
      "@typescript-eslint/no-explicit-any": "off",
      "@typescript-eslint/naming-convention": "off",
      "@typescript-eslint/no-empty-function": "off",
      "@typescript-eslint/explicit-module-boundary-types": "off"
    }
  }]
};
