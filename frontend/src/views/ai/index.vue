<script setup lang="ts">
/**
 * AI 工业大脑 —— 数字员工入口页。
 *
 * ## 定位
 *
 * 这是「AI 智能体」的统一入口：一排数字员工，点进去各干各的活。
 * 目前有 3 个真能用的：
 *  - **AI 采购助手** → 跳到 `/ai-brain/purchase`（对话式智能下单 → 自动填采购申请单）
 *  - **AI 销售助手** → 跳到 `/ai-brain/sales`（对话或传图 → 自动填销售订单，支持手写/打印订单图）
 *  - **数据知识库助手** → 直接开右侧助手抽屉（知识库问答 + 智能问数）
 * 其余 3 个先占位 —— **能跑通的闭环，比一堆空壳有价值**。
 *
 * ## 设计取舍
 *
 * - 人物形象：已有的用真实数字人素材，缺的用**图标占位**而不是拿假图凑
 *   （假图会让人以为已经做好了；占位反而说明「这里还差素材」）
 * - 只有 `ready: true` 的卡片可点，其余按钮明确写「敬请期待」，不做假跳转
 * - 动作分两种：`to` 跳路由（采购助手是独立工作台页），`onClick` 就地开抽屉
 *   （数据助手只是换了个入口，没必要单独做个页）
 */
import { MagicStick, Service, ShoppingCart, DataLine, Tools, CircleCheck, UserFilled } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useAiStore } from '@/stores/ai'
import staffPurchase from '@/assets/ai-staff/staff-purchase.png'
import staffSales from '@/assets/ai-staff/staff-sales.png'
import staffData from '@/assets/ai-staff/staff-data.png'

const router = useRouter()
const aiStore = useAiStore()

interface Agent {
  key: string
  name: string
  desc: string
  /** 是否已实现（未实现的卡片不可点） */
  ready: boolean
  /** 人物形象；没有就显示图标占位 */
  img?: string
  icon: any
  /** ready 时的跳转路径 */
  to?: string
  /** ready 时的自定义动作（开抽屉等）；给了它就不用 `to` 了 */
  onClick?: () => void
}

// ⚠️ 用普通数组而不是 `ref` —— 里面存了**组件对象**（icon），
//    放进响应式会触发 Vue 的「组件被代理」告警。
const agents: Agent[] = [
  {
    key: 'purchase',
    name: 'AI 采购助手',
    desc: '对话式智能下单 —— 说清楚要买什么，自动填好采购申请单',
    ready: true,
    img: staffPurchase,
    icon: ShoppingCart,
    to: '/ai-brain/purchase'
  },
  {
    key: 'data',
    // 「数据知识库助手」不跳路由，而是直接开右侧助手抽屉，并点名落在「知识库问答」；
    // 底部的模式胶囊就在输入框下面，想切「智能问数」点一下即可。
    name: '数据知识库助手',
    desc: '知识库问答 + 智能问数 —— 既查系统怎么用，也查系统里现在有多少',
    ready: true,
    img: staffData,
    icon: DataLine,
    onClick: () => aiStore.openAssistant('kb')
  },
  {
    key: 'sales',
    name: 'AI 销售助手',
    desc: '对话或传图开单 —— 说清客户和货品，自动填好销售订单；打印单、手写单都能认',
    ready: true,
    img: staffSales,
    icon: UserFilled,
    to: '/ai-brain/sales'
  },
  {
    key: 'service',
    name: 'AI 客服助手',
    desc: '客户咨询应答、售后工单与常见问题处理',
    ready: false,
    icon: Service
  },
  {
    key: 'production',
    name: 'AI 生产助手',
    desc: '工单排产、报工与生产进度问答',
    ready: false,
    icon: Tools
  },
  {
    key: 'quality',
    name: 'AI 质检助手',
    desc: '质检标准、不良品判定与处理流程问答',
    ready: false,
    icon: CircleCheck
  }
]

function enter(a: Agent) {
  if (!a.ready) return
  if (a.onClick) {
    a.onClick()
    return
  }
  if (a.to) router.push(a.to)
}
</script>

<template>
  <div class="brain">
    <!-- ===== 顶部 ===== -->
    <header class="brain__hero">
      <div class="brain__badge"><el-icon><MagicStick /></el-icon></div>
      <h1>AI 工业大脑</h1>
      <p class="brain__sub">工业 AI 数字员工 · 赋能智造新纪元</p>
      <p class="brain__desc">
        面向制造企业的一体化 AI 智能体平台，覆盖采购、销售、生产、质检与知识服务
      </p>
    </header>

    <!-- ===== 数字员工 ===== -->
    <div class="brain__grid">
      <div
        v-for="a in agents"
        :key="a.key"
        class="agent"
        :class="{ 'agent--ready': a.ready }"
      >
        <div class="agent__stage">
          <img v-if="a.img" :src="a.img" :alt="a.name" class="agent__img" />
          <div v-else class="agent__ph">
            <el-icon :size="36"><component :is="a.icon" /></el-icon>
            <span>形象待补</span>
          </div>
          <!-- 可用标记：左上角一条竖线，比角标更克制 -->
          <i v-if="a.ready" class="agent__mark" />
        </div>

        <div class="agent__body">
          <div class="agent__title">
            <span class="agent__name">{{ a.name }}</span>
            <span class="agent__tag" :class="{ on: a.ready }">
              {{ a.ready ? '可用' : '敬请期待' }}
            </span>
          </div>
          <p class="agent__desc">{{ a.desc }}</p>
          <el-button
            v-if="a.ready"
            type="primary"
            size="small"
            class="agent__go"
            @click="enter(a)"
          >
            进入
          </el-button>
          <el-button v-else size="small" disabled class="agent__go">敬请期待</el-button>
        </div>
      </div>
    </div>

    <p class="brain__foot">
      数字员工共享同一套能力底座：知识库问答（RAG）+ 智能问数（指标库）+
      <b>单据智能填写</b>
    </p>
  </div>
</template>

<style scoped>
.brain {
  padding: 28px 24px 36px;
  min-height: 100%;
  background: #f7f7f5;
}

/* ---------- 顶部 ---------- */
.brain__hero {
  text-align: center;
  margin-bottom: 30px;
}
.brain__badge {
  width: 46px;
  height: 46px;
  margin: 0 auto 12px;
  border-radius: 14px;
  background: #fbeaea;
  color: #d32f2f;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}
.brain__hero h1 {
  margin: 0 0 8px;
  font-size: 25px;
  font-weight: 500;
  letter-spacing: 4px;
  color: #2c2c2a;
}
.brain__sub {
  margin: 0 0 10px;
  font-size: 13.5px;
  color: #d32f2f;
  letter-spacing: 1px;
}
.brain__desc {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.8;
  color: #888780;
}

/* ---------- 卡片 ---------- */
.brain__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 16px;
  max-width: 1180px;
  margin: 0 auto;
}
.agent {
  background: #fff;
  border: 1px solid #e6e4de;
  border-radius: 14px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: border-color 0.18s ease, transform 0.18s ease;
}
.agent--ready {
  border-color: #f0c8c8;
}
.agent--ready:hover {
  border-color: #d32f2f;
  transform: translateY(-2px);
}

/* 形象展示区：固定高度 + contain，人物不会被拉变形 */
.agent__stage {
  position: relative;
  height: 230px;
  background: #fbfbf9;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #f0efe9;
}
.agent__img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  object-position: center top;
}
/* 占位：图标 + 一行小字，比塞假图诚实 */
.agent__ph {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: #c9c7bf;
}
.agent__ph span {
  font-size: 12px;
}
.agent__mark {
  position: absolute;
  left: 0;
  top: 18px;
  width: 3px;
  height: 34px;
  border-radius: 0 3px 3px 0;
  background: #d32f2f;
}

.agent__body {
  padding: 12px 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}
.agent__title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.agent__name {
  font-size: 14px;
  font-weight: 500;
  color: #2c2c2a;
}
.agent__tag {
  flex: none;
  font-size: 11px;
  padding: 1px 7px;
  border-radius: 99px;
  background: #f1efe8;
  color: #a8a69d;
}
.agent__tag.on {
  background: #fbeaea;
  color: #d32f2f;
}
.agent__desc {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.75;
  color: #888780;
  flex: 1;
}
.agent__go {
  width: 100%;
}

.brain__foot {
  max-width: 1180px;
  margin: 26px auto 0;
  text-align: center;
  font-size: 12px;
  color: #a8a69d;
}
.brain__foot b {
  color: #5f5e5a;
  font-weight: 500;
}
</style>
