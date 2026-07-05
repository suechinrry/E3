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
      this.triggerEvent('confirm', { notification: this.data.notification })
    }
  }
})
