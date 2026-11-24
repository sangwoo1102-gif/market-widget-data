package com.sangwoo.marketwidget

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** 앱 아이콘을 눌렀을 때 보이는 안내 화면. 위젯 추가 방법과 대시보드 바로가기. */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (24 * resources.displayMetrics.density).toInt()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(pad, pad * 2, pad, pad)
        }
        layout.addView(TextView(this).apply {
            text = "시황 위젯"
            textSize = 24f
        })
        layout.addView(TextView(this).apply {
            text = "\n홈 화면 빈 곳을 길게 누르고 → 위젯 → '시황 위젯'을 찾아 끌어다 놓으세요.\n\n" +
                "위젯은 약 1시간마다, 그리고 ↻를 누를 때 새 데이터를 읽습니다. " +
                "위젯 본문을 누르면 시황 대시보드가 열립니다.\n"
            textSize = 16f
        })
        layout.addView(Button(this).apply {
            text = "시황 대시보드 열기"
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(WidgetUpdater.DASHBOARD_URL)))
            }
        })
        setContentView(layout)
    }
}
