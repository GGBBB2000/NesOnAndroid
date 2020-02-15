package com.example.nesonandroid.model.device

import android.util.Log
import kotlin.experimental.and

final class Ppu(rom: Rom) {
    private var ppuCtrl: Byte = 0
    private var ppuCtrl2: Byte = 0
    private var spMemAddr: Byte = 0 //ここに2回書き込んで(アドレスを組み立てる),
    private var spMemData: Byte = 0 // データを番地に書き込む
    private var ppuAddr: Int = 0
    private var ppuData: Byte = 0
    private var ppuAddrCount = 0
    private var vScroll = 0
    private var hScroll = 0
    private var vIsSet = false
    private var hIsSet = false

    private var cpuCycle = 0
    private var line = 0

    private var chrRom = rom
    private var accessAddr = 0x0
    private var vram = Ram(0x1F30)
    private var screen = Array(256 * 240) {0xFF0000FF.toInt()}

        private val palette: Array<Int> = arrayOf(
        0xFF808080.toInt(), 0xFF003DA6.toInt(), 0xFF0012B0.toInt(), 0xFF440096.toInt(),
        0xFFA1005E.toInt(), 0xFFC70028.toInt(), 0xFFBA0600.toInt(), 0xFF8C1700.toInt(),
        0xFF5C2F00.toInt(), 0xFF104500.toInt(), 0xFF054A00.toInt(), 0xFF00472E.toInt(),
        0xFF004166.toInt(), 0xFF000000.toInt(), 0xFF050505.toInt(), 0xFF050505.toInt(),
        0xFFC7C7C7.toInt(), 0xFF0077FF.toInt(), 0xFF2155FF.toInt(), 0xFF8237FA.toInt(),
        0xFFEB2FB5.toInt(), 0xFFFF2950.toInt(), 0xFFFF2200.toInt(), 0xFFD63200.toInt(),
        0xFFC46200.toInt(), 0xFF358000.toInt(), 0xFF058F00.toInt(), 0xFF008A55.toInt(),
        0xFF0099CC.toInt(), 0xFF212121.toInt(), 0xFF090909.toInt(), 0xFF090909.toInt(),
        0xFFFFFFFF.toInt(), 0xFF0FD7FF.toInt(), 0xFF69A2FF.toInt(), 0xFFD480FF.toInt(),
        0xFFFF45F3.toInt(), 0xFFFF618B.toInt(), 0xFFFF8833.toInt(), 0xFFFF9C12.toInt(),
        0xFFFABC20.toInt(), 0xFF9FE30E.toInt(), 0xFF2BF035.toInt(), 0xFF0CF0A4.toInt(),
        0xFF05FBFF.toInt(), 0xFF5E5E5E.toInt(), 0xFF0D0D0D.toInt(), 0xFF0D0D0D.toInt(),
        0xFFFFFFFF.toInt(), 0xFFA6FCFF.toInt(), 0xFFB3ECFF.toInt(), 0xFFDAABEB.toInt(),
        0xFFFFA8F9.toInt(), 0xFFFFABB3.toInt(), 0xFFFFD2B0.toInt(), 0xFFFFEFA6.toInt(),
        0xFFFFF79C.toInt(), 0xFFD7E895.toInt(), 0xFFA6EDAF.toInt(), 0xFFA2F2DA.toInt(),
        0xFF99FFFC.toInt(), 0xFFDDDDDD.toInt(), 0xFF111111.toInt(), 0xFF111111.toInt()
    )

    private fun bitTest(data: Byte, index: Int): Boolean {
        return (data and (1 shl index).toByte()) != 0.toByte()
    }

    internal fun writeToRegister(adder: Int, data: Byte) {
        when (adder) {
            0x2000 -> {
                this.ppuCtrl = data
            }
            0x2001 -> {
                this.ppuCtrl2 = data
            }
            0x2003 -> {
                spMemAddr = data
            }
            0x2004 -> {
            }
            0x2005 -> {
                // 画面フォーカスの座標をx, y の順に書き込む
            }
            0x2006 -> {
                //PPUADDR
                //PPUCTRLのbit2の値によって加算量を変える
                assert(ppuAddrCount <= 1)
                if (ppuAddrCount == 0) {
                    ppuAddr = (data.toInt() and 0xFF) shl 8
                } else if (ppuAddrCount == 1) {
                    ppuAddr = ppuAddr or
                            (data.toInt() and 0xFF)
                }
                ppuAddrCount = (ppuAddrCount + 1) % 2

            }
            0x2007 -> {
                vram.write(ppuAddr - 0x2000, data)
                ppuAddr += if (bitTest(ppuCtrl, 2)) {
                    32
                } else {
                    1
                }
            }
        }
    }

    private fun readFromVRam(adder: Int): Byte {
        return when (adder) {
            in 0x0000..0x1FFF -> {
                chrRom.read(adder)
            }
            //in 0x1000..0x1FFF -> {}
            in 0x2000..0x3FFF -> {
                vram.read(adder - 0x2000)
            }
            else -> {
                error("VRAM: 不正なアドレス $adder")
            }
        }
    }

    private fun writeLine() {
        var y = (hScroll + line - 1) % 240 // ネームテーブル別の条件分岐
        var bgTableIndex = 0x0000 // 本来はCTRLレジスタの値を見て決める
        var spTableIndex = 0x0000 // 同上
        var nameTable = 0x0       // 同上
        var nameTableAdder = when (nameTable) {
            0x0 -> 0x2000
            0x1 -> 0x2400
            0x2 -> 0x2800
            0x3 -> 0x2C00
            else -> error("無効なネームテーブル ${nameTable}")
        }

        var attributeTableAdder = when (nameTable) {
            0x0 -> 0x23C0
            0x1 -> 0x27C0
            0x2 -> 0x2BC0
            0x3 -> 0x2FC0
            else -> error("無効なネームテーブル ${nameTable}")
        }
        for (index in 0 until 256) {
            val x = (vScroll +  index) % 255
            // ネームテーブル(今テーブル0)から座標(x, y)にあるスプライト番号を取ってくる
            // ネームテーブルの1タイルは8*8ピクセルであることに注意
            val spriteNum = readFromVRam(nameTableAdder + x / 8 + ((line - 1) / 8) * 32)
            //  属性テーブルから座標(x, y)が属するテーブルを取ってくる
            //print("(${x / 16 + ((line - 1) / 16) * 16})")
            val attrTableIndex = x / 16 + ((line - 1) / 16) * 16
            // テーブルからパレットを抽出
            var attribute = readFromVRam(attributeTableAdder + attrTableIndex)
            //  スプライトから色情報を抽出(パレットから色を選択する)
            // attributeが４色の色情報を持っているので, スプライトのインデックスを見て色を選択
            // TODO paletteIndexのx, yがおかしい
            val paletteIndex = getSpriteColorInfo(spriteNum.toInt(), x % 8, y % 8)

            val color = palette[readFromVRam(0x3F00 + paletteIndex).toInt()]
            //print("(${(line - 1) * 255 + index})")
            //  ピクセルに着色
            screen[(line - 1) * 256 + index] = color
        }
        println()
    }

    private fun getSpriteColorInfo(spriteNum: Int, x: Int, y: Int) : Int {
        val rightLine = readFromVRam(spriteNum * 16 + y)
        val leftLine = readFromVRam(spriteNum * 16 + 8 + y)
        var value = if (bitTest(leftLine, 7 - x)) {0b10} else {0b00}
        value += if (bitTest(rightLine, 7 - x)) {0b01} else {0b00}
        return value
    }

    var screenEnable = false
    public fun run(cycle: Int) {
        cpuCycle += cycle

        if (cpuCycle >= 341) {
            cpuCycle %= 341
            line++

            if (line <= 240) {
                // 背景をつくる
                writeLine()
            }

            if (line == 262) {
                line = 0
                screenEnable = true
            }
        }
        println(line)
    }

    public fun getScreen() : Array<Int> {
        screenEnable = false
        return screen
    }

    public fun isScreenEnable() : Boolean {return screenEnable}
}