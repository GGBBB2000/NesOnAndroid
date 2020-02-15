package com.example.nesonandroid.model

import com.example.nesonandroid.model.device.*
import java.nio.file.Files
import java.nio.file.Paths

public class Nes(file: ByteArray) {
    private var charRom: Rom
    private var progRom: Rom
    private var cpu: Cpu
    private var ppu: Ppu
    private var screen: Array<Int>? = null

    init {
        val data = load(file);
        progRom = Rom(data.first)
        charRom = Rom(data.second)
        val ram = Ram(0x800);
        ppu = Ppu(charRom)
        val bus = CpuBus(ram, progRom, ppu)
        cpu = Cpu(bus);
        cpu.reset()
    }

    private fun load(file: ByteArray) : Pair<ByteArray, ByteArray> {
        val iNES_HEADER_SIZE = 16
        //val result = Files.exists(Paths.get(filePath))
        //val file = Files.readAllBytes(Paths.get(filePath));
        if (file.size < 16
            || (file[0] != 'N'.toByte() && file[1] != 'E'.toByte() && file[2] != 'S'.toByte())
        ) {
            error("iNESフォーマットを検出できません")
        }
        val progRomSize = file[4] * 16384; //PRG ROM のサイズ 16384 * x bytes 16KB
        val charRomSize = file[5] * 8192;  //CHAR ROM のサイズ 8192 * x bytes 8KB
        val flag6 = file[6] // Mapper情報とかのフラグ
        val trainerSize = if ((flag6.toInt() and 0b0100) == 0b00000100) {
            0x200
        } else {
            0
        };

        val characterRomStart = iNES_HEADER_SIZE + trainerSize + progRomSize;
        val characterRomEnd = characterRomStart + charRomSize - 1
        val prog = file.slice(iNES_HEADER_SIZE..characterRomStart - 1).toByteArray()
        val char = file.slice(characterRomStart..characterRomEnd).toByteArray()
        return prog to char
    }

    private fun load(filePath: String) : Pair<List<Byte>, List<Byte>> {
        val iNES_HEADER_SIZE = 16
        val result = Files.exists(Paths.get(filePath))
        val file = Files.readAllBytes(Paths.get(filePath));
        if (file.size < 16
            || (file[0] != 'N'.toByte() && file[1] != 'E'.toByte() && file[2] != 'S'.toByte())
        ) {
            error("iNESフォーマットを検出できません")
        }
        val progRomSize = file[4] * 16384; //PRG ROM のサイズ 16384 * x bytes 16KB
        val charRomSize = file[5] * 8192;  //CHAR ROM のサイズ 8192 * x bytes 8KB
        val flag6 = file[6] // Mapper情報とかのフラグ
        val trainerSize = if ((flag6.toInt() and 0b0100) == 0b00000100) {
            0x200
        } else {
            0
        };

        val characterRomStart = iNES_HEADER_SIZE + trainerSize + progRomSize;
        val characterRomEnd = characterRomStart + charRomSize - 1
        val prog = file.slice(iNES_HEADER_SIZE..characterRomStart - 1)
        val char = file.slice(characterRomStart..characterRomEnd)
        return prog to char
    }
    public fun run() {
        val cycle = cpu.run()
        ppu.run(cycle)
    }

    public fun getScreen() : Array<Int>? {return ppu.getScreen()}
    public fun isScreenEnable() : Boolean {return ppu.isScreenEnable()}
}