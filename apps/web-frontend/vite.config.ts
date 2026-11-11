import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const gatewayTarget = env.GATEWAY_TARGET || 'http://localhost:8080'

  return {
    plugins: [react()],
    server: {
      proxy: {
        '/api': { target: gatewayTarget, changeOrigin: true },
        '/actuator': { target: gatewayTarget, changeOrigin: true },
      },
    },
  }
})
