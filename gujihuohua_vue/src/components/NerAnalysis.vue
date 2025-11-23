<template>
  <div class="task-container fade-in">
    <div class="header-row">
      <div class="title-box">
        <h2 class="gufeng-title">{{ currentTitle }}</h2>
        <span class="sub-tip" v-if="selectedCorpus">
          <el-icon><CollectionTag /></el-icon> {{ selectedCorpus.category }} · {{ selectedCorpus.sentenceCount }}句
        </span>
      </div>

      <div class="header-actions">
        <div class="current-corpus" v-if="selectedCorpus">
          当前案卷：<span class="highlight">《{{ selectedCorpus.title }}》</span>
        </div>
        <el-button type="primary" class="gufeng-btn" @click="dialogVisible = true">
          <el-icon style="margin-right: 5px"><Document /></el-icon>
          {{ selectedCorpus ? '更换案卷' : '调取案卷' }}
        </el-button>
        <el-button
            v-if="selectedCorpus"
            type="warning"
            class="gufeng-btn action-btn"
            @click="startAnalysis"
            :loading="analyzing"
        >
          <el-icon style="margin-right: 5px"><VideoPlay /></el-icon>
          启动识别
        </el-button>
      </div>
    </div>

    <div class="workspace-box">

      <div v-if="!selectedCorpus" class="empty-state">
        <div class="empty-icon-bg">
          <el-icon :size="50" color="#dcc8a6"><Search /></el-icon>
        </div>
        <p class="empty-text">请先调取一部古籍案卷</p>
        <p class="empty-sub">系统将自动加载文本并进行深度实体抽取</p>
      </div>

      <div v-else class="ner-layout" v-loading="analyzing" element-loading-text="正在通过深度学习模型抽取实体...">

        <div class="text-section paper-texture">
          <div class="section-header">
            <span class="label">原文标注</span>
            <div class="legend">
              <span class="dot person"></span>人名
              <span class="dot location"></span>地名
              <span class="dot office"></span>职官
            </div>
          </div>

          <div class="text-content custom-scrollbar" v-if="analysisResult">
            <div v-for="(sent, idx) in analysisResult.sentences" :key="idx" class="sent-line">
              <span class="line-num">{{ idx + 1 }}</span>
              <span class="line-html" v-html="sent.html"></span>
            </div>
          </div>
          <div class="text-placeholder" v-else>
            <p>点击右上角“启动识别”开始分析...</p>
          </div>
        </div>

        <div class="viz-section">

          <div class="stats-row">
            <div class="stat-card">
              <div class="stat-val">{{ analysisResult ? analysisResult.stats.total : 0 }}</div>
              <div class="stat-label">实体总数</div>
            </div>
            <div class="stat-card">
              <div class="stat-val">{{ analysisResult ? analysisResult.stats.density : '0%' }}</div>
              <div class="stat-label">信息密度</div>
            </div>
            <div class="stat-card">
              <div class="stat-val" style="font-size: 14px">
                {{ analysisResult ? analysisResult.stats.top : '无' }}
              </div>
              <div class="stat-label">最高频</div>
            </div>
          </div>

          <div class="chart-box">
            <div class="section-header"><span class="label">实体分布图谱</span></div>
            <div ref="chartRef" class="chart-container"></div>
          </div>

          <div class="entity-list-box paper-texture">
            <div class="section-header"><span class="label">识别清单</span></div>
            <div class="list-content custom-scrollbar">
              <el-table
                  v-if="analysisResult"
                  :data="analysisResult.entities"
                  style="width: 100%; background: transparent;"
                  :show-header="false"
                  size="small"
              >
                <el-table-column prop="word" label="实体" />
                <el-table-column prop="type" label="类型" width="80">
                  <template #default="scope">
                    <el-tag :class="'tag-' + scope.row.type" effect="plain" size="small">
                      {{ typeNameMap[scope.row.type] }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="count" label="频次" width="60" align="right" />
              </el-table>
              <div v-else class="list-placeholder">暂无数据</div>
            </div>
          </div>

        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="从语料库中选取" width="800px" class="ancient-dialog">
      <el-table :data="libraryList" v-loading="loading" style="width: 100%" height="400px" stripe @row-click="handleSelect" highlight-current-row>
        <el-table-column prop="title" label="篇名" width="250" />
        <el-table-column prop="category" label="类型" width="100" />
        <el-table-column prop="sentenceCount" label="句数" width="100" />
        <el-table-column prop="createTime" label="入库时间">
          <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="right">
          <template #default><el-button type="primary" link>选取</el-button></template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
/* eslint-disable no-undef */
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { CollectionTag, Document, VideoPlay, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import * as echarts from 'echarts'

const props = defineProps(['taskType'])

// 状态定义
const dialogVisible = ref(false)
const loading = ref(false)
const analyzing = ref(false)
const libraryList = ref([])
const selectedCorpus = ref(null)
const analysisResult = ref(null)
const chartRef = ref(null)
let myChart = null

// 标题映射
const titleMap = { ner: '古籍命名实体识别', relation: '古籍人物关系抽取', event: '古籍事件脉络抽取', portrait: '古籍人物画像生成' }
const currentTitle = computed(() => titleMap[props.taskType] || '智能分析')
const typeNameMap = { PER: '人名', LOC: '地名', ORG: '职官' }

// 格式化日期
const formatDate = (str) => str ? str.substring(0, 10) : ''

// 加载语料库
const fetchLibrary = async () => {
  loading.value = true
  try {
    const res = await axios.get('http://localhost:8080/api/library/list')
    libraryList.value = res.data
  } catch (e) { ElMessage.error('语料库加载失败') }
  finally { loading.value = false }
}

const handleSelect = (row) => {
  selectedCorpus.value = row
  dialogVisible.value = false
  analysisResult.value = null // 切换语料清空结果
  ElMessage.success(`已调取《${row.title}》`)
}

// --- 核心：模拟 NER 分析过程 ---
const startAnalysis = async () => {
  analyzing.value = true

  // 模拟网络延迟
  setTimeout(() => {
    // 1. 模拟后端返回的数据 (实际项目中这里替换为 axios.post)
    const mockResponse = {
      sentences: [
        { html: '<span class="ner-per">高祖</span>，<span class="ner-loc">沛</span><span class="ner-loc">丰邑</span><span class="ner-loc">中阳里</span>人，姓<span class="ner-per">刘</span>氏，字<span class="ner-per">季</span>。' },
        { html: '父曰<span class="ner-per">太公</span>，母曰<span class="ner-per">刘媪</span>。其先<span class="ner-per">刘累</span>，主要在<span class="ner-loc">夏</span>为<span class="ner-org">御龙氏</span>。' },
        { html: '<span class="ner-per">高祖</span>为人，隆准而龙颜，美须髯，左股有七十二黑子。' },
        { html: '及壮，试为<span class="ner-org">吏</span>，为<span class="ner-loc">泗水</span><span class="ner-org">亭长</span>，廷中<span class="ner-org">吏</span>无所不狎侮。' }
      ],
      stats: { total: 12, density: '15.4%', top: '高祖' },
      entities: [
        { word: '高祖', type: 'PER', count: 2 },
        { word: '沛', type: 'LOC', count: 1 },
        { word: '丰邑', type: 'LOC', count: 1 },
        { word: '刘累', type: 'PER', count: 1 },
        { word: '御龙氏', type: 'ORG', count: 1 },
        { word: '泗水', type: 'LOC', count: 1 },
        { word: '亭长', type: 'ORG', count: 1 }
      ]
    }

    analysisResult.value = mockResponse
    analyzing.value = false

    // 渲染图表
    nextTick(() => {
      initChart(mockResponse.entities)
    })

    ElMessage.success('实体识别完成')
  }, 1500)
}

// 初始化图表
const initChart = (entities) => {
  if (!chartRef.value) return
  if (myChart) myChart.dispose()

  myChart = echarts.init(chartRef.value)

  // 统计各类型数量
  const typeCounts = { '人名': 0, '地名': 0, '职官': 0 }
  entities.forEach(e => {
    const name = typeNameMap[e.type]
    if(name) typeCounts[name] += e.count
  })

  const option = {
    tooltip: { trigger: 'item' },
    color: ['#8e3e3e', '#2b4b64', '#b8860b'], // 朱砂, 青黛, 赭石
    series: [
      {
        name: '实体分布',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 5,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: { show: false, position: 'center' },
        emphasis: {
          label: { show: true, fontSize: 20, fontWeight: 'bold', fontFamily: 'KaiTi' }
        },
        labelLine: { show: false },
        data: [
          { value: typeCounts['人名'], name: '人名' },
          { value: typeCounts['地名'], name: '地名' },
          { value: typeCounts['职官'], name: '职官' }
        ]
      }
    ]
  }
  myChart.setOption(option)
}

// 监听窗口变化重绘图表
watch(() => props.taskType, () => { analysisResult.value = null })

onMounted(() => {
  fetchLibrary()
  window.addEventListener('resize', () => myChart && myChart.resize())
})
</script>

<style scoped>
/* 布局基础 */
.task-container { height: 100%; display: flex; flex-direction: column; font-family: "KaiTi", serif; }
.header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; border-bottom: 2px solid #e6dcc8; padding-bottom: 10px; }
.title-box { display: flex; flex-direction: column; }
.gufeng-title { margin: 0; color: #5e482d; font-size: 24px; border-left: 4px solid #8e3e3e; padding-left: 15px; letter-spacing: 2px; }
.sub-tip { font-size: 12px; color: #909399; margin-left: 20px; margin-top: 5px; display: flex; align-items: center; gap: 5px; }
.header-actions { display: flex; align-items: center; gap: 15px; }
.current-corpus { font-size: 14px; color: #666; }
.highlight { color: #8e3e3e; font-weight: bold; }
.gufeng-btn { background-color: #8e3e3e; border-color: #8e3e3e; font-family: "KaiTi"; letter-spacing: 1px; transition: all 0.3s; }
.gufeng-btn:hover { background-color: #a64b4b; }
.action-btn { background-color: #cdab84; border-color: #cdab84; color: #fff; }
.action-btn:hover { background-color: #dcc8a6; border-color: #dcc8a6; }

/* 工作区布局 */
.workspace-box { flex: 1; display: flex; flex-direction: column; overflow: hidden; background: #fff; border-radius: 4px; border: 1px solid #e6dcc8; padding: 20px; }
.empty-state { height: 100%; display: flex; flex-direction: column; justify-content: center; align-items: center; color: #909399; }
.empty-icon-bg { width: 100px; height: 100px; background: #fcf9f2; border-radius: 50%; display: flex; justify-content: center; align-items: center; margin-bottom: 20px; }
.empty-text { font-size: 18px; color: #5e482d; margin-bottom: 5px; }

/* NER 专属布局 */
.ner-layout { display: flex; height: 100%; gap: 20px; }

/* 左侧文本区 */
.text-section { flex: 2; display: flex; flex-direction: column; border: 1px solid #dcc8a6; border-radius: 4px; background: #fffdf6; }
.paper-texture { background-image: linear-gradient(90deg, transparent 95%, rgba(220, 200, 166, 0.2) 95%); background-size: 40px 100%; }
.section-header { height: 40px; border-bottom: 1px dashed #dcc8a6; display: flex; justify-content: space-between; align-items: center; padding: 0 15px; background: rgba(205, 171, 132, 0.1); }
.section-header .label { font-weight: bold; color: #5e482d; }
.legend { font-size: 12px; display: flex; gap: 10px; align-items: center; }
.dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; margin-right: 3px; }
.dot.person { background: #8e3e3e; }
.dot.location { background: #2b4b64; }
.dot.office { background: #b8860b; }

.text-content { flex: 1; padding: 20px; overflow-y: auto; font-size: 18px; line-height: 2; color: #333; }
.sent-line { margin-bottom: 10px; display: flex; }
.line-num { color: #cdab84; font-size: 12px; width: 25px; margin-top: 4px; user-select: none; }
.text-placeholder { flex: 1; display: flex; justify-content: center; align-items: center; color: #ccc; }

/* 右侧分析区 */
.viz-section { flex: 1; display: flex; flex-direction: column; gap: 15px; min-width: 300px; }
.stats-row { display: flex; gap: 10px; }
.stat-card { flex: 1; background: #fcf9f2; border: 1px solid #e6dcc8; padding: 15px 0; text-align: center; border-radius: 4px; }
.stat-val { font-size: 20px; font-weight: bold; color: #8e3e3e; font-family: Arial, sans-serif; }
.stat-label { font-size: 12px; color: #8c8272; margin-top: 5px; }

.chart-box { height: 200px; border: 1px solid #e6dcc8; background: #fff; border-radius: 4px; display: flex; flex-direction: column; }
.chart-container { flex: 1; }

.entity-list-box { flex: 1; border: 1px solid #e6dcc8; display: flex; flex-direction: column; background: #fff; }
.list-content { flex: 1; overflow-y: auto; padding: 5px; }
.list-placeholder { text-align: center; color: #ccc; padding-top: 40px; }

/* 动态高亮样式 (Deep Selectors) */
:deep(.ner-per) { color: #8e3e3e; border-bottom: 2px solid #8e3e3e; padding-bottom: 1px; cursor: pointer; }
:deep(.ner-loc) { color: #2b4b64; border-bottom: 2px solid #2b4b64; padding-bottom: 1px; cursor: pointer; }
:deep(.ner-org) { color: #b8860b; border-bottom: 2px solid #b8860b; padding-bottom: 1px; cursor: pointer; }
:deep(.ner-per:hover), :deep(.ner-loc:hover), :deep(.ner-org:hover) { background-color: rgba(0,0,0,0.05); border-radius: 2px; }

/* Tag 样式 */
.tag-PER { color: #8e3e3e !important; border-color: #8e3e3e !important; background-color: #fdf6f6 !important; }
.tag-LOC { color: #2b4b64 !important; border-color: #2b4b64 !important; background-color: #f0f4f8 !important; }
.tag-ORG { color: #b8860b !important; border-color: #b8860b !important; background-color: #fdfbf0 !important; }

.fade-in { animation: fadeIn 0.6s ease; }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
.custom-scrollbar::-webkit-scrollbar { width: 6px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #dcc8a6; border-radius: 3px; }
</style>