const { markNotificationRead } = require('../../utils/data-service')

Component({
  properties: {
    visible: { type: Boolean, value: false },
    notification: { type: Object, value: {} }
  },
  data: {
    greetingData: null
  },
  observers: {
    'notification': function(n) {
      if (n && n.type === 'greeting' && n.content) {
        try {
          this.setData({ greetingData: JSON.parse(n.content) })
        } catch (e) {
          this.setData({ greetingData: { greetingText: n.content } })
        }
      } else {
        this.setData({ greetingData: null })
      }
    }
  },
  methods: {
    preventBubble() {},
    onClose() {
      this.triggerEvent('close')
    },
    onConfirm() {
      // 标记已读（仅个人通知需要标记，公共公告无需标记）
      if (this.data.notification && this.data.notification.id && this.data.notification._source !== 'public') {
        markNotificationRead(this.data.notification.id).catch(() => {})
      }
      this.triggerEvent('confirm', { notification: this.data.notification })
    },
    onGoCenter() {
      this.onConfirm()
      wx.navigateTo({ url: '/pages/visitor/notification-center/notification-center' })
    }
  }
})
