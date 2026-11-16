import { createApp } from 'vue'
import { createPinia } from 'pinia'
import {
  ElButton, ElDialog, ElForm, ElFormItem, ElIcon, ElInput,
  ElOption, ElSelect, ElSwitch, ElTabPane, ElTabs, ElTooltip,
} from 'element-plus'
import 'element-plus/es/components/button/style/css'
import 'element-plus/es/components/dialog/style/css'
import 'element-plus/es/components/form/style/css'
import 'element-plus/es/components/icon/style/css'
import 'element-plus/es/components/input/style/css'
import 'element-plus/es/components/select/style/css'
import 'element-plus/es/components/switch/style/css'
import 'element-plus/es/components/tabs/style/css'
import 'element-plus/es/components/tooltip/style/css'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(createPinia())
app.use(router)
// Register only the components used by this application.
for (const component of [
  ElButton, ElDialog, ElForm, ElFormItem, ElIcon, ElInput,
  ElOption, ElSelect, ElSwitch, ElTabPane, ElTabs, ElTooltip,
]) {
  app.component(component.name!, component)
}
app.mount('#app')
