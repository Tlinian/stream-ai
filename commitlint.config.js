export default {
  extends: ["@commitlint/config-conventional"],
  parserPreset: {
    parserOpts: {
      headerPattern: /^(\w+)(?:<#\d+>)?: (.*)$/,
      headerCorrespondence: ["type", "subject"],
    },
  },
  rules: {
    "type-enum": [
      2,
      "always",
      [
        "feat",
        "fix",
        "docs",
        "style",
        "refactor",
        "perf",
        "test",
        "build",
        "ci",
        "chore",
        "revert"
      ]
    ],
    "type-empty": [2, "never"],
    "subject-empty": [2, "never"],
    "subject-max-length": [2, "always", 72],
    "header-max-length": [2, "always", 120]
  }
};
