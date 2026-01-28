export default {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'type-enum': [
      2,
      'always',
      [
        'feat', // 新功能
        'fix', // 修复
        'docs', // 文档
        'style', // 格式
        'refactor', // 重构
        'perf', // 性能
        'test', // 测试
        'chore', // 构建/工具
        'revert', // 回退
      ],
    ],
  },
}
