<template>
  <div class="process-container fade-in">
    <div class="header-box">
      <h2 class="gufeng-title">文本清洗工作台</h2>
      <div class="action-bar">
        <span class="status-tip" v-if="previewLoading">正在研墨分析...</span>
        <span class="status-tip" v-else>已生成 {{ resultCount }} 句</span>

        <el-button type="primary" class="gufeng-btn" @click="openSaveDialog" :disabled="!hasData">
          <el-icon style="margin-right:5px"><FolderAdd /></el-icon> 保存
        </el-button>
      </div>
    </div>

    <el-row :gutter="24" class="workspace">
      <el-col :span="9" class="h-100">
        <div class="panel-box input-panel">
          <el-form :model="form" label-position="top">
            <div class="tabs-header">
              <span :class="{active: inputType==='paste'}" @click="inputType='paste'">手录</span>
              <span class="divider">|</span>
              <span :class="{active: inputType==='file'}" @click="inputType='file'">传书</span>
            </div>
            <div class="input-area">
              <el-input v-if="inputType==='paste'" v-model="form.content" type="textarea" :rows="10" class="gufeng-input" resize="none" placeholder="请在此誊写或粘贴古籍内容..." />
              <el-upload v-else class="upload-box" drag action="#" :auto-upload="false" :on-change="handleFileChange" :limit="1" :show-file-list="false">
                <div class="upload-inner">
                  <el-icon class="upload-icon"><UploadFilled /></el-icon>
                  <div class="upload-text">拖拽 .txt 文件至此</div>
                  <div class="file-name" v-if="fileName">{{ fileName }}</div>
                </div>
              </el-upload>
            </div>
            <div class="settings-area">
              <div class="setting-title"><span>规制设定</span></div>
              <div class="control-row">
                <span class="label">体裁：</span>
                <el-radio-group v-model="form.textCategory" size="small">
                  <el-radio-button label="史书" />
                  <el-radio-button label="志书" />
                  <el-radio-button label="笔记" />
                </el-radio-group>
              </div>
              <div class="control-row">
                <span class="label">繁简：</span>
                <el-switch v-model="form.convertToSimplified" active-text="化繁为简" />
              </div>
              <div class="control-row">
                <span class="label">句读：</span>
                <div class="checkbox-wrap">
                  <el-checkbox v-model="form.splitByPeriod" label="句号切分" />
                  <el-checkbox v-model="form.splitByNewline" label="换行切分" />
                  <el-checkbox v-model="form.standardizeSpaces" label="去空规整" />
                </div>
              </div>
            </div>
          </el-form>
        </div>
      </el-col>
      <el-col :span="15" class="h-100">
        <div class="panel-box paper-panel" v-loading="previewLoading" element-loading-text="墨迹未干...">
          <div class="paper-bg">
            <div v-if="!hasData" class="empty-state">
              <div class="circle-bg">书</div>
              <p>待输入文稿</p>
            </div>
            <div v-else class="text-scroll custom-scrollbar">
              <div v-for="item in previewLines" :key="item.index" class="text-line">
                <span class="index-seal">{{ item.index }}</span>
                <span class="content-char">{{ item.content }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-dialog
        v-model="saveDialogVisible"
        title="文稿入库"
        width="400px"
        class="ancient-dialog"
        align-center
    >
      <div style="padding: 10px">
        <el-form :model="saveForm">
          <el-form-item label="文稿题名">
            <el-input v-model="saveForm.title" placeholder="请输入自定义标题（如：史记·高祖本纪）" />
          </el-form-item>
          <el-form-item label="入库备注">
            <div class="save-tip">
              系统将自动生成：<br/>
              1. {{ saveForm.title }}_{{ timeStr }}.txt (原始文稿)<br/>
              2. {{ saveForm.title }}_{{ timeStr }}.json (结构化数据)
            </div>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="saveDialogVisible = false">取 消</el-button>
          <el-button type="primary" class="gufeng-btn" @click="confirmSave" :loading="saving">
            确 认 保 存
          </el-button>
        </span>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
/* eslint-disable no-undef */
import { ref, reactive, watch, computed } from 'vue'
import { UploadFilled, FolderAdd } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const emit = defineEmits(['save-record'])

// ... 原有变量 ...
const inputType = ref('paste')
const fileName = ref('')
const fileBytes = ref(null)
const previewLines = ref([])
const previewLoading = ref(false)

const form = reactive({
  content: '',
  textCategory: '史书',
  convertToSimplified: true,
  splitByPeriod: true,
  splitByNewline: true,
  standardizeSpaces: true
})

// 新增变量
const saveDialogVisible = ref(false)
const saving = ref(false)
const saveForm = reactive({ title: '' })
const timeStr = computed(() => {
  const now = new Date();
  // 简单生成一个时间后缀用于展示
  return now.getHours() + "" + now.getMinutes();
})

// ... 原有的 watch, computed, debouncePreview, handleFileChange, fetchPreview ...
// (请保持这些逻辑不变，复制之前的代码)
const hasData = computed(() => previewLines.value.length > 0)
const resultCount = computed(() => previewLines.value.length)
let timer = null
const debouncePreview = () => {
  if (timer) clearTimeout(timer)
  previewLoading.value = true
  timer = setTimeout(() => fetchPreview(), 600)
}
watch(form, () => { if (form.content || fileBytes.value) debouncePreview() }, { deep: true })
const handleFileChange = (file) => {
  fileName.value = file.name
  // 如果是文件上传，自动填入文件名作为默认标题（去后缀）
  saveForm.title = file.name.replace(/\.[^/.]+$/, "")
  const reader = new FileReader()
  reader.onload = () => { fileBytes.value = file.raw; debouncePreview() }
  reader.readAsArrayBuffer(file.raw)
}
const fetchPreview = async () => {
  const formData = new FormData()
  const config = { ...form, isPreview: true }
  if (inputType.value === 'file' && fileBytes.value) { formData.append('file', fileBytes.value) }
  formData.append('config', new Blob([JSON.stringify(config)], { type: 'application/json' }))
  try {
    const res = await axios.post('http://localhost:8080/api/text/process', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
    previewLines.value = res.data.lines
  } catch (e) { console.error(e) } finally { previewLoading.value = false }
}


// 修改：打开保存弹窗
const openSaveDialog = () => {
  // 如果还没填标题且是手录，尝试截取前几个字
  if (!saveForm.title && inputType.value === 'paste' && form.content) {
    saveForm.title = form.content.substring(0, 10).replace(/\s+/g, '')
  }
  saveDialogVisible.value = true
}

// 修改：确认保存
const confirmSave = async () => {
  if (!saveForm.title) return ElMessage.warning('请输入题名')

  saving.value = true

  const payload = {
    title: saveForm.title, // 用户输入的自定义标题
    category: form.textCategory,
    count: resultCount.value,
    rawContent: form.content,
    data: previewLines.value
  }

  try {
    await axios.post('http://localhost:8080/api/library/save', payload)
    ElMessage.success('文稿已归档至服务器文件系统')
    saveDialogVisible.value = false
    // 清空表单防止重复提交
    saveForm.title = ''
    emit('save-record')
  } catch (e) {
    console.error(e)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
/* ... 保持之前的样式不变 ... */
.save-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  background: #f4f4f5;
  padding: 8px;
  border-radius: 4px;
}
/* ... 其他样式 ... */
.process-container { height: 100%; display: flex; flex-direction: column; }
.header-box { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; border-bottom: 2px solid #e6dcc8; padding-bottom: 10px; }
.gufeng-title { margin: 0; color: #5e482d; font-weight: normal; font-size: 24px; letter-spacing: 4px; }
.action-bar { display: flex; align-items: center; gap: 15px; }
.status-tip { color: #8c8272; font-size: 14px; }
.gufeng-btn { background-color: #8e3e3e; border-color: #8e3e3e; font-family: "KaiTi"; letter-spacing: 2px; box-shadow: 2px 2px 5px rgba(0,0,0,0.1); transition: all 0.3s; }
.gufeng-btn:hover { background-color: #a64b4b; transform: translateY(-1px); }
.workspace { flex: 1; height: calc(100% - 60px); }
.h-100 { height: 100%; }
.panel-box { height: 100%; background: #fff; border-radius: 4px; box-shadow: 0 2px 12px rgba(0,0,0,0.05); display: flex; flex-direction: column; }
.input-panel { background: #fcfcfc; border: 1px solid #ebeef5; padding: 15px; }
.tabs-header { display: flex; justify-content: center; align-items: center; margin-bottom: 15px; font-size: 16px; color: #909399; cursor: pointer; }
.tabs-header span.active { color: #5e482d; font-weight: bold; border-bottom: 2px solid #8e3e3e; }
.divider { margin: 0 15px; color: #dcdfe6; cursor: default; }
.input-area { flex: 1; display: flex; flex-direction: column; }
.upload-box { width: 100%; flex: 1; display: flex; flex-direction: column; }
.upload-box :deep(.el-upload), .upload-box :deep(.el-upload-dragger) { width: 100%; height: 100%; display: flex; justify-content: center; align-items: center; background: #f5f7fa; border: 2px dashed #dcdfe6; }
.upload-inner { text-align: center; color: #909399; }
.upload-icon { font-size: 40px; margin-bottom: 10px; }
.gufeng-input :deep(.el-textarea__inner) { background-color: #fafafa; border: 1px solid #e4e7ed; font-family: "KaiTi"; font-size: 16px; line-height: 1.8; padding: 15px; }
.settings-area { margin-top: 15px; background: #fdfbf6; border: 1px solid #f0e6d2; padding: 10px; border-radius: 4px; }
.setting-title { font-size: 14px; color: #8e3e3e; margin-bottom: 10px; font-weight: bold; border-bottom: 1px dashed #f0e6d2; padding-bottom: 5px; }
.control-row { display: flex; align-items: center; margin-bottom: 8px; font-size: 14px; }
.label { width: 60px; color: #606266; }
.paper-panel { background: #fffdf6; border: 1px solid #dcc8a6; position: relative; }
.paper-bg { flex: 1; padding: 30px 40px; overflow: hidden; display: flex; flex-direction: column; background-image: linear-gradient(90deg, transparent 95%, rgba(220, 200, 166, 0.3) 95%); background-size: 40px 100%; }
.empty-state { height: 100%; display: flex; flex-direction: column; justify-content: center; align-items: center; color: #c0c4cc; }
.circle-bg { width: 80px; height: 80px; border: 2px solid #e4e7ed; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 32px; color: #dcdfe6; margin-bottom: 10px; }
.text-scroll { overflow-y: auto; height: 100%; padding-right: 10px; }
.text-line { display: flex; align-items: flex-start; margin-bottom: 16px; font-size: 18px; color: #2c3e50; line-height: 1.6; }
.index-seal { display: inline-block; min-width: 24px; height: 24px; line-height: 24px; text-align: center; background-color: #f3e8d3; color: #8f7d62; border-radius: 4px; font-size: 12px; margin-right: 12px; margin-top: 3px; }
.content-char { border-bottom: 1px solid rgba(0,0,0,0.05); width: 100%; }
.custom-scrollbar::-webkit-scrollbar { width: 6px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #dcc8a6; border-radius: 3px; }
</style>