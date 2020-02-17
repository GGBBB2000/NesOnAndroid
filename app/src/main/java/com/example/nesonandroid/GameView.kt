package com.example.nesonandroid

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorSpace
import android.graphics.Paint
import android.os.AsyncTask
import android.os.Handler
import android.util.Log
import android.view.View
import com.example.nesonandroid.model.Nes
import com.example.nesonandroid.model.device.Cpu
import java.sql.Time
import java.util.*
import kotlin.concurrent.timerTask


internal class GameView(context: Context?) : View(context) {
    private val windowWidth = 256
    private val windowHeight = 240
    private var screen: Bitmap =
        Bitmap.createBitmap(windowWidth, windowHeight, Bitmap.Config.ARGB_8888)
    private var timer: Timer = Timer()
    private lateinit var model: Nes

    init {
        val asset = context!!.resources.assets
        val arr = asset.open("Roms/nes-test-roms/other/nestest.nes").readBytes()
        model = Nes(arr)
        val task = NesTask()
        task.execute(model)
        timer.scheduleAtFixedRate(timerTask {
                postInvalidate()
        }, 0, 16)
    }

    override fun onDraw(canvas: Canvas) {
        drawView(canvas)
    }

    private fun drawView(canvas: Canvas) {
        val screenArray = model.getScreen()?: return
        val wm = this.resources.displayMetrics
        screen.setPixels(screenArray.toIntArray(), 0, windowWidth, 0, 0, windowWidth, windowHeight)
        val bmp = Bitmap.createScaledBitmap(screen, windowWidth * 4, windowHeight * 4, false)
        val leftPos = (wm.widthPixels - windowWidth * 4) / 2f
        canvas.drawBitmap(bmp, leftPos, 0f, Paint())
    }
}

final class NesTask : AsyncTask<Nes, Void, Unit>() {
    override fun doInBackground(vararg params: Nes): Unit {
        val model = params[0]
        while (true) {
            model.run()
            //Thread.sleep(0, 1) // 1/1.79M ~= 5586ns
        }
    }
}