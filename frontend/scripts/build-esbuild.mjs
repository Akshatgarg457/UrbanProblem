/**
 * Fallback build script using esbuild (pure JS, no native binaries).
 * Use this when the Vite build fails due to a missing rolldown native binary
 * (e.g. node_modules built on a different OS).
 *
 * Requires esbuild >= 0.20 available in node_modules or globally.
 * Usage: node scripts/build-esbuild.mjs
 */
import { createRequire } from 'node:module'
import * as path from 'node:path'
import * as fs from 'node:fs'
import * as crypto from 'node:crypto'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const ROOT = path.resolve(__dirname, '..')
const OUT = path.join(ROOT, 'dist')
const ASSETS = path.join(OUT, 'assets')

// Try to load esbuild from node_modules, then fall back to the tsx bundled copy
const require = createRequire(import.meta.url)
let esbuild
try {
  esbuild = require(path.join(ROOT, 'node_modules/esbuild/lib/main.js'))
} catch {
  // tsx ships esbuild as a dependency — use it as a fallback
  const txsEsbuild = path.join(
    path.dirname(require.resolve('tsx/package.json')),
    'node_modules/esbuild/lib/main.js',
  )
  esbuild = require(txsEsbuild)
}

fs.mkdirSync(ASSETS, { recursive: true })

const jsHash = crypto.randomBytes(4).toString('hex')

// Build JS
await esbuild.build({
  entryPoints: [path.join(ROOT, 'src/main.tsx')],
  bundle: true,
  format: 'esm',
  outfile: path.join(ASSETS, `index-${jsHash}.js`),
  jsx: 'automatic',
  target: ['es2020'],
  loader: { '.tsx': 'tsx', '.ts': 'ts', '.css': 'empty', '.svg': 'dataurl', '.png': 'dataurl' },
  define: { 'process.env.NODE_ENV': '"production"' },
  nodePaths: [path.join(ROOT, 'node_modules')],
  minify: false,
  logLevel: 'info',
})

// Combine Tailwind base + project CSS
const twBase = fs.readFileSync(
  path.join(ROOT, 'node_modules/tailwindcss/index.css'), 'utf8'
)
const ourCss = fs.readFileSync(
  path.join(ROOT, 'src/styles/index.css'), 'utf8'
).replace(/@import\s+["']tailwindcss["'];?\s*/, '')
fs.writeFileSync(path.join(ASSETS, 'index-custom.css'), twBase + '\n' + ourCss)

// Copy public/
const pub = path.join(ROOT, 'public')
if (fs.existsSync(pub))
  fs.readdirSync(pub).forEach(f => fs.copyFileSync(path.join(pub, f), path.join(OUT, f)))

// Write index.html
const tmpl = fs.readFileSync(path.join(ROOT, 'index.html'), 'utf8')
const html = tmpl.replace(
  /<script type="module" src="\/src\/main\.tsx"><\/script>/,
  `<script type="module" crossorigin src="/assets/index-${jsHash}.js"></script>\n    <link rel="stylesheet" crossorigin href="/assets/index-custom.css">`,
)
fs.writeFileSync(path.join(OUT, 'index.html'), html)

console.log('✅  esbuild fallback build complete → dist/')
