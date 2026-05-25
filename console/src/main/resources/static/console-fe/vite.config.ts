import { defineConfig, type Plugin } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import path from 'path'
import { writeFileSync } from 'fs'
import { resolve } from 'path'

function versionPlugin(): Plugin {
  return {
    name: 'version-plugin',
    buildStart() {
      if (process.env.VERSION) {
        writeFileSync(
          resolve(process.cwd(), 'public/version.json'),
          JSON.stringify({ version: process.env.VERSION }),
        )
      }
    },
  }
}

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    versionPlugin(),
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver({ importStyle: 'css' })],
      imports: ['vue', 'vue-router', 'pinia', 'vue-i18n'],
      dts: 'src/auto-imports.d.ts',
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/components.d.ts',
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(process.cwd(), 'src'),
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@use "@/styles/variables.scss" as *;\n@use "@/styles/mixins.scss" as *;\n`,
      },
    },
  },
})
