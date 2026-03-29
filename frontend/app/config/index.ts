import { defineConfig } from '@tarojs/cli';

export default defineConfig({
  projectName: 'qinghe-night-school-app',
  date: '2026-03-15',
  designWidth: 375,
  deviceRatio: {
    375: 2,
    750: 1
  },
  sourceRoot: 'src',
  outputRoot: 'dist',
  framework: 'react',
  compiler: 'webpack5',
  mini: {
    postcss: {
      pxtransform: {
        enable: true,
        config: {}
      },
      url: {
        enable: true,
        config: {
          limit: 1024
        }
      },
      cssModules: {
        enable: false,
        config: {
          namingPattern: 'module',
          generateScopedName: '[name]__[local]___[hash:base64:5]'
        }
      }
    }
  }
});
