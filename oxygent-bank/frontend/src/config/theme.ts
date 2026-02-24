import type { ThemeConfig } from 'ant-design-vue/es/config-provider/context'

/**
 * Ant Design 主题配置
 * 与 Tailwind CSS 颜色体系对齐
 */
export const themeConfig: ThemeConfig = {
  token: {
    // 主色
    colorPrimary: '#4F46E5', // Indigo 600
    colorSuccess: '#10B981', // Emerald 500
    colorWarning: '#F59E0B', // Amber 500
    colorError: '#EF4444', // Red 500
    colorInfo: '#4F46E5', // Indigo 600

    // 字体
    fontFamily:
      '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", sans-serif',
    fontSize: 14,
    fontSizeHeading1: 38,
    fontSizeHeading2: 30,
    fontSizeHeading3: 24,
    fontSizeHeading4: 20,
    fontSizeHeading5: 16,

    // 圆角
    borderRadius: 6,
    borderRadiusLG: 8,
    borderRadiusSM: 4,

    // 间距
    padding: 16,
    paddingLG: 24,
    paddingSM: 12,
    paddingXS: 8,
    paddingXXS: 4,

    // 阴影
    boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)',
    boxShadowSecondary: '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)',

    // Layout Background (Moved from components.Layout.bodyBg)
    colorBgLayout: '#f3f4f6',
  },
  components: {
    Layout: {
      headerBg: '#ffffff',
      siderBg: '#111827',
    } as any,
    Menu: {
      // itemBg: 'transparent', // Removed as it causes type error
      // subMenuItemBg: 'transparent', // Removed
      itemColor: 'rgba(255, 255, 255, 0.65)',
      itemHoverColor: '#ffffff',
      itemSelectedColor: '#ffffff',
      itemSelectedBg: '#4F46E5', // Indigo 600
    } as any,
  },
}

/**
 * 响应式断点配置（与 Tailwind 对齐）
 */
export const breakpoints = {
  'xs': 480,
  'sm': 576,
  'md': 768,
  'lg': 992,
  'xl': 1200,
  '2xl': 1600,
} as const
