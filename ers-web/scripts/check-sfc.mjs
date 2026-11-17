// 前端静态校验：不依赖 esbuild（沙箱内 spawn 被禁用），用 compiler-sfc + babel parser 检查语法
import fs from 'node:fs'
import path from 'node:path'
import { parse as parseSFC } from '@vue/compiler-sfc'
import { parse as parseJS } from '@babel/parser'

const root = path.resolve(process.cwd(), 'src')
const problems = []
let vueCount = 0
let jsCount = 0

function walk(dir) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) walk(full)
    else if (entry.name.endsWith('.vue')) checkVue(full)
    else if (entry.name.endsWith('.js')) checkJS(full, fs.readFileSync(full, 'utf8'), false)
  }
}

function checkJS(file, code, isModule = true) {
  jsCount++
  try {
    parseJS(code, {
      sourceType: 'module',
      plugins: ['importAttributes', 'dynamicImport', 'topLevelAwait']
    })
  } catch (e) {
    problems.push(`${path.relative(root, file)}: ${e.message}`)
  }
  if (!isModule) return
}

function checkVue(file) {
  vueCount++
  const source = fs.readFileSync(file, 'utf8')
  const rel = path.relative(root, file)
  const { descriptor, errors } = parseSFC(source, { filename: file })
  for (const e of errors) problems.push(`${rel}: SFC ${e.message}`)

  if (descriptor.scriptSetup || descriptor.script) {
    const script = (descriptor.scriptSetup?.content || '') + '\n' + (descriptor.script?.content || '')
    try {
      parseJS(script, { sourceType: 'module', plugins: ['importAttributes', 'topLevelAwait'] })
    } catch (e) {
      problems.push(`${rel}: script ${e.message}`)
    }
  }
  const template = descriptor.template?.content || ''
  if (template) {
    const open = (template.match(/<([A-Za-z][\w-]*)/g) || []).length
    const close = (template.match(/<\/([A-Za-z][\w-]*)>/g) || []).length
    const selfClosing = (template.match(/\/>/g) || []).length
    if (open - selfClosing !== close) {
      problems.push(`${rel}: 模板标签数量不匹配（open=${open} close=${close} self=${selfClosing}）`)
    }
  }
}

walk(root)
console.log(`checked vue=${vueCount} js=${jsCount}`)
if (problems.length) {
  console.log('PROBLEMS:')
  problems.forEach((p) => console.log(' - ' + p))
  process.exit(1)
}
console.log('ALL OK')
