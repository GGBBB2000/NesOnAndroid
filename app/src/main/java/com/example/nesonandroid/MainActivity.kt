package com.example.nesonandroid

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.nesonandroid.model.Nes

class MainActivity : AppCompatActivity() {
    lateinit var model : Nes
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //var assets = this.resources.assets
        //val arr = assets.open("Roms/sample1.nes").readBytes()
        //model = Nes(arr)
        //model.start()
        val gameView = GameView(this)
        setContentView(gameView)
    }
}
