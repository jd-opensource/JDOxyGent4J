import antfu from '@antfu/eslint-config'

export default antfu({
  formatters: true,
  vue: true,
  typescript: true,
  stylistic: {
    indent: 2,
    quotes: 'single',
  },
  rules: {
    'vue/multi-word-component-names': 'off',
    'vue/component-name-in-template-casing': ['error', 'kebab-case', {
      registeredComponentsOnly: false,
      ignores: [],
    }],
  },
}, {
  // 全局忽略 - 单独的配置对象
  ignores: [
    'dist/**',
    'node_modules/**',
    '**/*.d.ts',
    '.nuxt/**',
    '.output/**',
    'src/api/apiDefinitions.ts',
    'src/api/createApis.ts',
    'src/api/globals.d.ts',
    'api-proxy/**',
    'CLAUDE.md',
  ],
}, {
  files: ['**/.eslintrc-auto-import.json'],
  rules: {
    'jsonc/no-template-literals': 'off',
  },
})
