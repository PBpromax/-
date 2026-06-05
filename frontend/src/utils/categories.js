import expressIcon from '../assets/category-icons/快递跑腿.png'
import tutoringIcon from '../assets/category-icons/学习求助.png'
import secondHandIcon from '../assets/category-icons/二手交易.png'
import materialShareIcon from '../assets/category-icons/资料共享.png'
import teamUpIcon from '../assets/category-icons/组队招募.png'
import carpoolIcon from '../assets/category-icons/拼车出行.png'
import qAndAIcon from '../assets/category-icons/问答求助.png'
import otherIcon from '../assets/category-icons/其他.png'

export const requirementCategories = [
  { type: 'EXPRESS', label: '快递跑腿', icon: '➤', iconUrl: expressIcon, color: '#22c55e' },
  { type: 'TUTORING', aliases: ['STUDY_HELP', 'STUDY'], label: '学习求助', icon: '▤', iconUrl: tutoringIcon, color: '#3b82f6' },
  { type: 'SECOND_HAND', label: '二手交易', icon: '▣', iconUrl: secondHandIcon, color: '#f59e0b' },
  { type: 'MATERIAL_SHARE', aliases: ['MATERIAL'], label: '资料共享', icon: '▰', iconUrl: materialShareIcon, color: '#8b5cf6' },
  { type: 'TEAM_UP', label: '组队招募', icon: '●●', iconUrl: teamUpIcon, color: '#ef4444' },
  { type: 'CARPOOL', label: '拼车出行', icon: '◒', iconUrl: carpoolIcon, color: '#10b981' },
  { type: 'Q_AND_A', aliases: ['QA'], label: '问答求助', icon: '…', iconUrl: qAndAIcon, color: '#fb923c' },
  { type: 'OTHER', label: '其他', icon: '•••', iconUrl: otherIcon, color: '#94a3b8' }
]

export function categoryMeta(type) {
  return requirementCategories.find((item) => item.type === type || item.aliases?.includes(type)) || requirementCategories[requirementCategories.length - 1]
}
