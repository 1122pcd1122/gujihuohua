<template>
  <div class="analysis-container fade-in">
    <div class="header-row">
      <h2 class="gufeng-title">古籍人物关系抽取</h2>
      <el-button type="primary" class="gufeng-btn" @click="dialogVisible = true">
        <el-icon style="margin-right:5px"><Connection /></el-icon> 选取案卷构建图谱
      </el-button>
    </div>

    <div class="graph-workspace paper-texture">
      <div class="graph-area" ref="graphRef">
        <div v-if="!hasData" class="empty-graph">
          <p>请选取语料以生成人物关系网</p>
        </div>
      </div>

      <div class="node-detail-panel" v-if="currentNode">
        <div class="detail-header">
          <span class="avatar-placeholder">{{ currentNode.name[0] }}</span>
          <div class="name-box">
            <div class="name">{{ currentNode.name }}</div>
            <div class="role">{{ currentNode.category }}</div>
          </div>
        </div>
        <div class="detail-body">
          <p><strong>关联人物：</strong>{{ currentNode.links }} 人</p>
          <p><strong>中心度：</strong>{{ currentNode.value }}</p>
          <div class="desc">此处可显示该人物在古籍中的简要生平或出场段落摘要。</div>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="选取语料" width="800px" class="ancient-dialog">
      <el-button @click="startSimulation">模拟加载《史记·项羽本纪》</el-button>
    </el-dialog>
  </div>
</template>

<script setup>
/* eslint-disable no-undef */
// 修改点：删除了 onMounted
import { ref } from 'vue'
import { Connection } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const dialogVisible = ref(false)
const hasData = ref(false)
const graphRef = ref(null)
const currentNode = ref(null)
let myChart = null

const startSimulation = () => {
  dialogVisible.value = false
  hasData.value = true
  // 模拟数据加载过程
  setTimeout(initGraph, 500)
}

const initGraph = () => {
  if (myChart) myChart.dispose()
  myChart = echarts.init(graphRef.value)

  // 模拟古籍关系数据
  const data = [
    { name: '项羽', category: '核心', value: 100, symbolSize: 80 },
    { name: '刘邦', category: '对手', value: 90, symbolSize: 70 },
    { name: '虞姬', category: '亲属', value: 50, symbolSize: 40 },
    { name: '范增', category: '谋士', value: 60, symbolSize: 40 },
    { name: '项梁', category: '亲属', value: 40, symbolSize: 30 },
    { name: '韩信', category: '对手', value: 70, symbolSize: 50 },
  ]
  const links = [
    { source: '项羽', target: '刘邦', label: { show: true, formatter: '争霸' } },
    { source: '项羽', target: '虞姬', label: { show: true, formatter: '夫妻' } },
    { source: '项羽', target: '范增', label: { show: true, formatter: '亚父' } },
    { source: '项羽', target: '项梁', label: { show: true, formatter: '叔父' } },
    { source: '刘邦', target: '韩信', label: { show: true, formatter: '君臣' } },
  ]

  const option = {
    color: ['#8e3e3e', '#2b4b64', '#b8860b'],
    tooltip: {},
    series: [{
      type: 'graph',
      layout: 'force',
      data: data.map(i => ({...i, label: {show: true}})),
      links: links,
      roam: true,
      label: { position: 'right', formatter: '{b}' },
      lineStyle: { color: 'source', curveness: 0.3 },
      force: { repulsion: 1000, edgeLength: 120 }
    }]
  }

  myChart.setOption(option)

  // 点击事件监听
  myChart.on('click', (params) => {
    if (params.dataType === 'node') {
      currentNode.value = {
        name: params.name,
        category: params.data.category,
        value: params.data.value,
        links: 3 // 模拟数据
      }
    }
  })

  // 监听窗口大小改变，自适应图表
  window.addEventListener('resize', () => {
    myChart && myChart.resize()
  })
}
</script>

<style scoped>
/* 样式略，核心是 graph-workspace 占据全屏，右侧 detail-panel 绝对定位浮在上面 */
.analysis-container { height: 100%; display: flex; flex-direction: column; }
.header-row { display: flex; justify-content: space-between; padding-bottom: 10px; border-bottom: 2px solid #e6dcc8; margin-bottom: 10px;}
.gufeng-title { color: #5e482d; border-left: 4px solid #8e3e3e; padding-left: 15px; margin: 0; }
.gufeng-btn { background: #8e3e3e; border: none; font-family: "KaiTi"; }
.graph-workspace { flex: 1; position: relative; background: #fffdf6; border: 1px solid #dcc8a6; overflow: hidden; }
.graph-area { width: 100%; height: 100%; }
.node-detail-panel {
  position: absolute; top: 20px; right: 20px; width: 240px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #e6dcc8;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  padding: 20px;
  border-radius: 4px;
  backdrop-filter: blur(5px);
}
.empty-graph { height: 100%; display: flex; align-items: center; justify-content: center; color: #999; }
.detail-header { display: flex; align-items: center; gap: 10px; margin-bottom: 15px; border-bottom: 1px dashed #ccc; padding-bottom: 10px;}
.avatar-placeholder { width: 40px; height: 40px; background: #8e3e3e; color: #fff; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 20px; font-family: "KaiTi"; }
.name { font-size: 18px; font-weight: bold; color: #333; }
.role { font-size: 12px; color: #666; }
.desc { font-size: 13px; color: #555; line-height: 1.6; margin-top: 10px; }
</style>