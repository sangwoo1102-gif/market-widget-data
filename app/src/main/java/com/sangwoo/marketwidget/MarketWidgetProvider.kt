package com.sangwoo.marketwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

/** 홈 화면 위젯. 시스템 주기 갱신(약 1시간)과 ↻ 버튼 탭 때 데이터를 다시 읽는다. */
class MarketWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            refresh(context)
        }
    }

    private fun refresh(context: Context) {
        val pending = goAsync()
        val app = context.applicationContext
        Thread {
            try {
                WidgetUpdater.update(app)
            } finally {
                pending.finish()
            }
        }.start()
    }

    companion object {
        const val ACTION_REFRESH = "com.sangwoo.marketwidget.REFRESH"
    }
}
