package com.example.nesonandroid.model.device

import com.example.nesonandroid.Instruction
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor

final class Cpu(bus: CpuBus) {
    private var bus: CpuBus
    private var a: Byte;
    private var x: Byte;
    private var y: Byte;
    private var pc: Int;// program counter
    private var sp: Byte; // stack pointer
    private var p: Byte; // status register

    init {
        this.a = 0
        this.x = 0
        this.y = 0
        this.pc = 0xFFFC
        this.sp = 0x0.toByte() //上位ビットは0x01
        this.p = 0b00100000
        this.bus = bus
    }

    private fun setNegative(flag: Boolean) {
        // N V 1 B D I Z C
        this.p = if (flag) {
            this.p or 0b10000000.toByte()
        } else {
            this.p and 0b01111111.toByte()
        }
    }

    private fun setOverflow(flag: Boolean) {
        // N V 1 B D I Z C
        this.p = if (flag) {
            this.p or 0b01000000.toByte()
        } else {
            this.p and 0b10111111.toByte()
        }
    }

    private fun setBreak(flag: Boolean) {
        // N V 1 B D I Z C
        this.p = if (flag) {
            this.p or 0b00010000.toByte()
        } else {
            this.p and 0b11101111.toByte()
        }
    }

    private fun setDecimal(flag: Boolean) {
        // N V 1 B D I Z C
        this.p = if (flag) {
            this.p or 0b00001000.toByte()
        } else {
            this.p and 0b11110111.toByte()
        }
    }

    private fun setInterrupt(flag: Boolean) {
        // N V 1 B D I Z C
        this.p = if (flag) {
            this.p or 0b00000100.toByte()
        } else {
            this.p and 0b11111011.toByte()
        }
    }

    private fun setZero(flag: Boolean) {
        // N V 1 B D I Z C
        this.p = if (flag) {
            this.p or 0b00000010.toByte()
        } else {
            this.p and 0b11111101.toByte()
        }
    }

    private fun setCarry(flag: Boolean) {
        // N V 1 B D I Z C
        this.p = if (flag) {
            this.p or 0b00000001.toByte()
        } else {
            this.p and 0b11111110.toByte()
        }
    }

    private fun bitTest(data: Byte, index: Int): Boolean {
        // N V 1 B D I Z C
        return (data and (1 shl index).toByte()) != 0.toByte()
    }

    private enum class Addressing {
        ACC,  // Accumulator
        IMM,   // Immediate
        ABS,   // Absolute
        Zero,  // ZeroPage
        ZeroX, // ZeroPageX
        ZeroY, // ZeroPageY
        ABSX,  // AbsoluteX
        ABSY,  // AbsoluteY
        IMP,   // Implied
        REL,   // Relative
        INDX,  // Indirect Index (Index X)
        INDY,  // Idexed Indirect(Index Y)
        ABSIN, // Absolute Indirect
    }

    internal fun reset() {
        this.a = 0
        this.x = 0
        this.y = 0
        this.pc = 0xFFFC
        this.sp = 0.toByte()
        this.p = 0b00100000

        this.pc = fetchAddr()
    }

    private fun readByte(addr: Int): Byte {
        return bus.readByCpu(addr)
    }

    private fun writeByte(data: Byte, addr: Int) {
        this.bus.writeByCpu(data, addr)
    }

    private fun readAddr(addr: Int): Int {
        val lower = readByte(addr)
        val upper = readByte(addr + 1)
        val res = ((upper.toInt() and 0b11111111) shl 8) + (lower.toInt() and 0b11111111)
        return res
    }

    private fun fetch(): Byte {
        val data = readByte(this.pc)
        pc++
        return data
    }

    private fun fetchAddr(): Int {
        val lower = fetch()
        val upper = fetch()
        val addr = ((upper.toInt() and 0b11111111) shl 8) + (lower.toInt() and 0b11111111)
        return addr
    }

    val CYCLE = arrayOf(
        /*0x00*/ 7, 6, 2, 8, 3, 3, 5, 5, 3, 2, 2, 2, 4, 4, 6, 6,
        /*0x10*/ 2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 6, 7,
        /*0x20*/ 6, 6, 2, 8, 3, 3, 5, 5, 4, 2, 2, 2, 4, 4, 6, 6,
        /*0x30*/ 2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 6, 7,
        /*0x40*/ 6, 6, 2, 8, 3, 3, 5, 5, 3, 2, 2, 2, 3, 4, 6, 6,
        /*0x50*/ 2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 6, 7,
        /*0x60*/ 6, 6, 2, 8, 3, 3, 5, 5, 4, 2, 2, 2, 5, 4, 6, 6,
        /*0x70*/ 2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 6, 7,
        /*0x80*/ 2, 6, 2, 6, 3, 3, 3, 3, 2, 2, 2, 2, 4, 4, 4, 4,
        /*0x90*/ 2, 6, 2, 6, 4, 4, 4, 4, 2, 4, 2, 5, 5, 4, 5, 5,
        /*0xA0*/ 2, 6, 2, 6, 3, 3, 3, 3, 2, 2, 2, 2, 4, 4, 4, 4,
        /*0xB0*/ 2, 5, 2, 5, 4, 4, 4, 4, 2, 4, 2, 4, 4, 4, 4, 4,
        /*0xC0*/ 2, 6, 2, 8, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6,
        /*0xD0*/ 2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
        /*0xE0*/ 2, 6, 3, 8, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6,
        /*0xF0*/ 2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7
    )

    private fun decode(data: Int): Triple<Instruction, Addressing, Int> {
        return when (data) {
            0x69 -> {
                Triple(Instruction.ADC, Addressing.IMM, CYCLE[data])
            }
            0x65 -> {
                Triple(Instruction.ADC, Addressing.Zero, CYCLE[data])
            }
            0x75 -> {
                Triple(Instruction.ADC, Addressing.ZeroX, CYCLE[data])
            }
            0x6D -> {
                Triple(Instruction.ADC, Addressing.ABS, CYCLE[data])
            }
            0x7D -> {
                Triple(Instruction.ADC, Addressing.ABSX, CYCLE[data])
            }
            0x79 -> {
                Triple(Instruction.ADC, Addressing.ABSY, CYCLE[data])
            }
            0x61 -> {
                Triple(Instruction.ADC, Addressing.INDX, CYCLE[data])
            }
            0x71 -> {
                Triple(Instruction.ADC, Addressing.INDY, CYCLE[data])
            }

            0xE9 -> {
                Triple(Instruction.SBC, Addressing.IMM, CYCLE[data])
            }
            0xE5 -> {
                Triple(Instruction.SBC, Addressing.Zero, CYCLE[data])
            }
            0xF5 -> {
                Triple(Instruction.SBC, Addressing.ZeroX, CYCLE[data])
            }
            0xED -> {
                Triple(Instruction.SBC, Addressing.ABS, CYCLE[data])
            }
            0xFD -> {
                Triple(Instruction.SBC, Addressing.ABSX, CYCLE[data])
            }
            0xF9 -> {
                Triple(Instruction.SBC, Addressing.ABSY, CYCLE[data])
            }
            0xE1 -> {
                Triple(Instruction.SBC, Addressing.INDX, CYCLE[data])
            }
            0xF1 -> {
                Triple(Instruction.SBC, Addressing.INDY, CYCLE[data])
            }

            0x29 -> {
                Triple(Instruction.AND, Addressing.IMM, CYCLE[data])
            }
            0x25 -> {
                Triple(Instruction.AND, Addressing.Zero, CYCLE[data])
            }
            0x35 -> {
                Triple(Instruction.AND, Addressing.ZeroX, CYCLE[data])
            }
            0x2D -> {
                Triple(Instruction.AND, Addressing.ABS, CYCLE[data])
            }
            0x3D -> {
                Triple(Instruction.AND, Addressing.ABSX, CYCLE[data])
            }
            0x39 -> {
                Triple(Instruction.AND, Addressing.ABSY, CYCLE[data])
            }
            0x21 -> {
                Triple(Instruction.AND, Addressing.INDX, CYCLE[data])
            }
            0x31 -> {
                Triple(Instruction.AND, Addressing.INDY, CYCLE[data])
            }

            0x09 -> {
                Triple(Instruction.ORA, Addressing.IMM, CYCLE[data])
            }
            0x05 -> {
                Triple(Instruction.ORA, Addressing.Zero, CYCLE[data])
            }
            0x15 -> {
                Triple(Instruction.ORA, Addressing.ZeroX, CYCLE[data])
            }
            0x0D -> {
                Triple(Instruction.ORA, Addressing.ABS, CYCLE[data])
            }
            0x1D -> {
                Triple(Instruction.ORA, Addressing.ABSX, CYCLE[data])
            }
            0x19 -> {
                Triple(Instruction.ORA, Addressing.ABSY, CYCLE[data])
            }
            0x01 -> {
                Triple(Instruction.ORA, Addressing.INDX, CYCLE[data])
            }
            0x11 -> {
                Triple(Instruction.ORA, Addressing.INDY, CYCLE[data])
            }

            0x49 -> {
                Triple(Instruction.EOR, Addressing.IMM, CYCLE[data])
            }
            0x45 -> {
                Triple(Instruction.EOR, Addressing.Zero, CYCLE[data])
            }
            0x55 -> {
                Triple(Instruction.EOR, Addressing.ZeroX, CYCLE[data])
            }
            0x4D -> {
                Triple(Instruction.EOR, Addressing.ABS, CYCLE[data])
            }
            0x5D -> {
                Triple(Instruction.EOR, Addressing.ABSX, CYCLE[data])
            }
            0x59 -> {
                Triple(Instruction.EOR, Addressing.ABSY, CYCLE[data])
            }
            0x41 -> {
                Triple(Instruction.EOR, Addressing.INDX, CYCLE[data])
            }
            0x51 -> {
                Triple(Instruction.EOR, Addressing.INDY, CYCLE[data])
            }
            0x0A -> {
                Triple(Instruction.ASL, Addressing.ACC, CYCLE[data])
            }
            0x06 -> {
                Triple(Instruction.ASL, Addressing.Zero, CYCLE[data])
            }
            0x16 -> {
                Triple(Instruction.ASL, Addressing.ZeroX, CYCLE[data])
            }
            0x0E -> {
                Triple(Instruction.ASL, Addressing.ABS, CYCLE[data])
            }
            0x1E -> {
                Triple(Instruction.ASL, Addressing.ABSX, CYCLE[data])
            }

            0x4A -> {
                Triple(Instruction.LSR, Addressing.ACC, CYCLE[data])
            }
            0x46 -> {
                Triple(Instruction.LSR, Addressing.Zero, CYCLE[data])
            }
            0x56 -> {
                Triple(Instruction.LSR, Addressing.ZeroX, CYCLE[data])
            }
            0x4E -> {
                Triple(Instruction.LSR, Addressing.ABS, CYCLE[data])
            }
            0x5E -> {
                Triple(Instruction.LSR, Addressing.ABSX, CYCLE[data])
            }

            0x2A -> {
                Triple(Instruction.ROL, Addressing.ACC, CYCLE[data])
            }
            0x26 -> {
                Triple(Instruction.ROL, Addressing.Zero, CYCLE[data])
            }
            0x36 -> {
                Triple(Instruction.ROL, Addressing.ZeroX, CYCLE[data])
            }
            0x2E -> {
                Triple(Instruction.ROL, Addressing.ABS, CYCLE[data])
            }
            0x3E -> {
                Triple(Instruction.ROL, Addressing.ABSX, CYCLE[data])
            }

            0x6A -> {
                Triple(Instruction.ROR, Addressing.ACC, CYCLE[data])
            }
            0x66 -> {
                Triple(Instruction.ROR, Addressing.Zero, CYCLE[data])
            }
            0x76 -> {
                Triple(Instruction.ROR, Addressing.ZeroX, CYCLE[data])
            }
            0x6E -> {
                Triple(Instruction.ROR, Addressing.ABS, CYCLE[data])
            }
            0x7E -> {
                Triple(Instruction.ROR, Addressing.ABSX, CYCLE[data])
            }

            0x90 -> {
                Triple(Instruction.BCC, Addressing.REL, CYCLE[data])
            }
            0xB0 -> {
                Triple(Instruction.BCS, Addressing.REL, CYCLE[data])
            }
            0xF0 -> {
                Triple(Instruction.BEQ, Addressing.REL, CYCLE[data])
            }
            0xD0 -> {
                Triple(Instruction.BNE, Addressing.REL, CYCLE[data])
            }
            0x50 -> {
                Triple(Instruction.BVC, Addressing.REL, CYCLE[data])
            }
            0x70 -> {
                Triple(Instruction.BVS, Addressing.REL, CYCLE[data])
            }
            0x10 -> {
                Triple(Instruction.BPL, Addressing.REL, CYCLE[data])
            }
            0x30 -> {
                Triple(Instruction.BMI, Addressing.REL, CYCLE[data])
            }

            0x24 -> {
                Triple(Instruction.BIT, Addressing.Zero, CYCLE[data])
            }
            0x2C -> {
                Triple(Instruction.BIT, Addressing.ABS, CYCLE[data])
            }

            0x4C -> {
                Triple(Instruction.JMP, Addressing.ABS, CYCLE[data])
            }
            0x6C -> {
                Triple(Instruction.JMP, Addressing.ABSIN, CYCLE[data])
            }

            0x20 -> {
                Triple(Instruction.JSR, Addressing.ABS, CYCLE[data])
            }

            0x60 -> {
                Triple(Instruction.RTS, Addressing.IMP, CYCLE[data])
            }

            0x00 -> {
                Triple(Instruction.BRK, Addressing.IMP, CYCLE[data])
            }
            0x40 -> {
                Triple(Instruction.RTI, Addressing.IMP, CYCLE[data])
            }

            0xC9 -> {
                Triple(Instruction.CMP, Addressing.IMM, CYCLE[data])
            }
            0xC5 -> {
                Triple(Instruction.CMP, Addressing.Zero, CYCLE[data])
            }
            0xD5 -> {
                Triple(Instruction.CMP, Addressing.ZeroX, CYCLE[data])
            }
            0xCD -> {
                Triple(Instruction.CMP, Addressing.ABS, CYCLE[data])
            }
            0xDD -> {
                Triple(Instruction.CMP, Addressing.ABSX, CYCLE[data])
            }
            0xD9 -> {
                Triple(Instruction.CMP, Addressing.ABSY, CYCLE[data])
            }
            0xC1 -> {
                Triple(Instruction.CMP, Addressing.INDX, CYCLE[data])
            }
            0xD1 -> {
                Triple(Instruction.CMP, Addressing.INDY, CYCLE[data])
            }

            0xE0 -> {
                Triple(Instruction.CPX, Addressing.IMM, CYCLE[data])
            }
            0xE4 -> {
                Triple(Instruction.CPX, Addressing.Zero, CYCLE[data])
            }
            0xEC -> {
                Triple(Instruction.CPX, Addressing.ABS, CYCLE[data])
            }
            0xC0 -> {
                Triple(Instruction.CPY, Addressing.IMM, CYCLE[data])
            }
            0xC4 -> {
                Triple(Instruction.CPY, Addressing.Zero, CYCLE[data])
            }
            0xCC -> {
                Triple(Instruction.CPY, Addressing.ABS, CYCLE[data])
            }

            0xE6 -> {
                Triple(Instruction.INC, Addressing.Zero, CYCLE[data])
            }
            0xF6 -> {
                Triple(Instruction.INC, Addressing.ZeroX, CYCLE[data])
            }
            0xEE -> {
                Triple(Instruction.INC, Addressing.ABS, CYCLE[data])
            }
            0xFE -> {
                Triple(Instruction.INC, Addressing.ABSX, CYCLE[data])
            }

            0xC6 -> {
                Triple(Instruction.DEC, Addressing.Zero, CYCLE[data])
            }
            0xD6 -> {
                Triple(Instruction.DEC, Addressing.ZeroX, CYCLE[data])
            }
            0xCE -> {
                Triple(Instruction.DEC, Addressing.ABS, CYCLE[data])
            }
            0xDE -> {
                Triple(Instruction.DEC, Addressing.ABSX, CYCLE[data])
            }

            0xE8 -> {
                Triple(Instruction.INX, Addressing.IMP, CYCLE[data])
            }
            0xCA -> {
                Triple(Instruction.DEX, Addressing.IMP, CYCLE[data])
            }
            0xC8 -> {
                Triple(Instruction.INY, Addressing.IMP, CYCLE[data])
            }
            0x88 -> {
                Triple(Instruction.DEY, Addressing.IMP, CYCLE[data])
            }

            0x18 -> {
                Triple(Instruction.CLC, Addressing.IMP, CYCLE[data])
            }
            0x38 -> {
                Triple(Instruction.SEC, Addressing.IMP, CYCLE[data])
            }
            0x58 -> {
                Triple(Instruction.CLI, Addressing.IMP, CYCLE[data])
            }
            0x78 -> {
                Triple(Instruction.SEI, Addressing.IMP, CYCLE[data])
            }
            0xD8 -> {
                Triple(Instruction.CLD, Addressing.IMP, CYCLE[data])
            }
            0xF8 -> {
                Triple(Instruction.SED, Addressing.IMP, CYCLE[data])
            }
            0xB8 -> {
                Triple(Instruction.CLV, Addressing.IMP, CYCLE[data])
            }

            0xA9 -> {
                Triple(Instruction.LDA, Addressing.IMM, CYCLE[data])
            }
            0xA5 -> {
                Triple(Instruction.LDA, Addressing.Zero, CYCLE[data])
            }
            0xB5 -> {
                Triple(Instruction.LDA, Addressing.ZeroX, CYCLE[data])
            }
            0xAD -> {
                Triple(Instruction.LDA, Addressing.ABS, CYCLE[data])
            }
            0xBD -> {
                Triple(Instruction.LDA, Addressing.ABSX, CYCLE[data])
            }
            0xB9 -> {
                Triple(Instruction.LDA, Addressing.ABSY, CYCLE[data])
            }
            0xA1 -> {
                Triple(Instruction.LDA, Addressing.INDX, CYCLE[data])
            }
            0xB1 -> {
                Triple(Instruction.LDA, Addressing.INDY, CYCLE[data])
            }

            0xA2 -> {
                Triple(Instruction.LDX, Addressing.IMM, CYCLE[data])
            }
            0xA6 -> {
                Triple(Instruction.LDX, Addressing.Zero, CYCLE[data])
            }
            0xB6 -> {
                Triple(Instruction.LDX, Addressing.ZeroY, CYCLE[data])
            }
            0xAE -> {
                Triple(Instruction.LDX, Addressing.ABS, CYCLE[data])
            }
            0xBE -> {
                Triple(Instruction.LDX, Addressing.ABSY, CYCLE[data])
            }

            0xA0 -> {
                Triple(Instruction.LDY, Addressing.IMM, CYCLE[data])
            }
            0xA4 -> {
                Triple(Instruction.LDY, Addressing.Zero, CYCLE[data])
            }
            0xB4 -> {
                Triple(Instruction.LDY, Addressing.ZeroX, CYCLE[data])
            }
            0xAC -> {
                Triple(Instruction.LDY, Addressing.ABS, CYCLE[data])
            }
            0xBC -> {
                Triple(Instruction.LDY, Addressing.ABSX, CYCLE[data])
            }

            0x85 -> {
                Triple(Instruction.STA, Addressing.Zero, CYCLE[data])
            }
            0x95 -> {
                Triple(Instruction.STA, Addressing.ZeroX, CYCLE[data])
            }
            0x8D -> {
                Triple(Instruction.STA, Addressing.ABS, CYCLE[data])
            }
            0x9D -> {
                Triple(Instruction.STA, Addressing.ABSX, CYCLE[data])
            }
            0x99 -> {
                Triple(Instruction.STA, Addressing.ABSY, CYCLE[data])
            }
            0x81 -> {
                Triple(Instruction.STA, Addressing.INDX, CYCLE[data])
            }
            0x91 -> {
                Triple(Instruction.STA, Addressing.INDY, CYCLE[data])
            }

            0x86 -> {
                Triple(Instruction.STX, Addressing.Zero, CYCLE[data])
            }
            0x96 -> {
                Triple(Instruction.STX, Addressing.ZeroY, CYCLE[data])
            }
            0x8E -> {
                Triple(Instruction.STX, Addressing.ABS, CYCLE[data])
            }

            0x84 -> {
                Triple(Instruction.STY, Addressing.Zero, CYCLE[data])
            }
            0x94 -> {
                Triple(Instruction.STY, Addressing.ZeroY, CYCLE[data])
            }
            0x8C -> {
                Triple(Instruction.STY, Addressing.ABS, CYCLE[data])
            }

            0xAA -> {
                Triple(Instruction.TAX, Addressing.IMP, CYCLE[data])
            }
            0x8A -> {
                Triple(Instruction.TXA, Addressing.IMP, CYCLE[data])
            }
            0xA8 -> {
                Triple(Instruction.TAY, Addressing.IMP, CYCLE[data])
            }
            0x98 -> {
                Triple(Instruction.TYA, Addressing.IMP, CYCLE[data])
            }
            0x9A -> {
                Triple(Instruction.TXS, Addressing.IMP, CYCLE[data])
            }
            0xBA -> {
                Triple(Instruction.TSX, Addressing.IMP, CYCLE[data])
            }

            0x48 -> {
                Triple(Instruction.PHA, Addressing.IMP, CYCLE[data])
            }
            0x68 -> {
                Triple(Instruction.PLA, Addressing.IMP, CYCLE[data])
            }
            0x08 -> {
                Triple(Instruction.PHP, Addressing.IMP, CYCLE[data])
            }
            0x28 -> {
                Triple(Instruction.PLP, Addressing.IMP, CYCLE[data])
            }

            0xEA -> {
                Triple(Instruction.NOP, Addressing.IMP, CYCLE[data])
            }

            else -> {
                error("Illegal Operation: $data\n")
            }
        }
    }

    private fun fetchOperandAdder(addressing: Addressing): Int {
        return when (addressing) {
            Addressing.ABS -> fetchAddr()
            Addressing.ABSX -> fetchAddr() + this.x
            Addressing.ABSY -> fetchAddr() + this.y
            Addressing.Zero -> fetch().toInt() and 0b11111111
            Addressing.ZeroX -> fetch().toInt() and 0b11111111 + this.x
            Addressing.ZeroY -> fetch().toInt() and 0b11111111 + this.y
            Addressing.REL -> fetch() + this.pc
            Addressing.INDX -> {
                val zeroLow = ((fetch().toInt() and 0b1111111) + this.x) and 0b11111111
                readAddr(zeroLow)
            }
            Addressing.INDY -> {
                val zeroLow = fetch().toInt() and 0b1111111
                val tmpAdder = readAddr(zeroLow)
                tmpAdder + this.y
            }
            Addressing.ABSIN -> {
                val absAdder = fetchAddr()
                readAddr(absAdder)
            }
            else -> {
                error("アドレスを返せません")
            }
        }
    }

    private fun push(byte: Byte) {
        writeByte(byte, (0x100 + this.sp.toInt() and 0xFF))
        this.sp = ((this.sp.toInt().toInt() and 0xFF) - 1).toByte()
    }

    private fun pop(): Byte {
        this.sp = ((this.sp.toInt().toInt() and 0xFF) + 1).toByte()
        return readByte(0x100 + this.sp.toInt() and 0xFF)
    }

    private fun exec(decResult: Triple<Instruction, Addressing, Int>) {
        val addressing = decResult.second
        val instruction = decResult.first
        when (instruction) {
            Instruction.ADC -> {
                val value = when (addressing) {
                    Addressing.IMM -> {
                        fetch()
                    }
                    else -> {
                        readByte(fetchOperandAdder(addressing))
                    }
                }
                // N V 1 B D I Z C
                val diff = (this.a < 0) == (value < 0)
                this.a = (this.a + value).toByte() and 0xFF.toByte()
                +if (bitTest(this.p, 0)) {
                    1.toByte()
                } else {
                    0.toByte()
                }
                setNegative(this.a < 0)
                setZero(this.a == 0.toByte())
                setOverflow(diff and (this.a < 0 != value < 0))
            }
            Instruction.SBC -> {
                val value = when (addressing) {
                    Addressing.IMM -> {
                        fetch()
                    }
                    else -> {
                        readByte(fetchOperandAdder(addressing))
                    }
                }
                // N V 1 B D I Z C
                val diff = (this.a < 0) != (value < 0)
                val isNegative = value < 0
                this.a = (this.a - value).toByte() and 0xFF.toByte()
                -if (bitTest(this.p, 0)) {
                    0.toByte()
                } else {
                    1.toByte()
                }
                setNegative(this.a < 0)
                setZero(this.a == 0.toByte())
                setOverflow(diff and (isNegative == (this.a < 0)))
            }
            Instruction.AND -> {
                val value = when (addressing) {
                    Addressing.IMM -> {
                        fetch()
                    }
                    else -> {
                        readByte(fetchOperandAdder(addressing))
                    }
                }
                this.a = this.a and value
                setNegative(this.a < 0)
                setZero(this.a == 0.toByte())
            }
            Instruction.ORA -> {
                val value = when (addressing) {
                    Addressing.IMM -> {
                        fetch()
                    }
                    else -> {
                        readByte(fetchOperandAdder(addressing))
                    }
                }
                this.a = this.a or value
                setNegative(this.a < 0)
                setZero(this.a == 0.toByte())
            }
            Instruction.EOR -> {
                val value = when (addressing) {
                    Addressing.IMM -> {
                        fetch()
                    }
                    else -> {
                        readByte(fetchOperandAdder(addressing))
                    }
                }
                this.a = this.a xor value
                setNegative(this.a < 0)
                setZero(this.a == 0.toByte())
            }
            Instruction.ASL -> {
                var operandAdder: Int? = null
                val value = when (addressing) {
                    Addressing.ACC -> this.a
                    else -> {
                        operandAdder = fetchOperandAdder(addressing)
                        readByte(operandAdder)
                    }
                }
                setCarry(value < 0)
                val result = (value.toInt() and 0xFF) shl 1
                setZero(result == 0)
                setNegative(result < 0)
                if (operandAdder != null) {
                    writeByte(result.toByte(), operandAdder)
                } else {
                    this.a = result.toByte()
                }
            }
            Instruction.LSR -> {
                var operandAdder: Int? = null
                val value = when (addressing) {
                    Addressing.ACC -> this.a
                    else -> {
                        operandAdder = fetchOperandAdder(addressing)
                        readByte(operandAdder)
                    }
                }
                setCarry(value < 0)
                val result = (value.toInt() and 0xFF) ushr 1
                setZero(result == 0)
                setNegative(result < 0)
                if (operandAdder != null) {
                    writeByte(result.toByte(), operandAdder)
                } else {
                    this.a = result.toByte()
                }
            }
            Instruction.ROL -> {
                TODO("not implemented")
            }
            Instruction.ROR -> {
                TODO("not implemented")
            }
            Instruction.INC -> {
                val adder = fetchOperandAdder(addressing)
                val result = (readByte(adder).toInt() and 0xFF + 1).toByte()
                writeByte(result, adder)
            }
            Instruction.DEC -> {
                val adder = fetchOperandAdder(addressing)
                val result = (readByte(adder).toInt() and 0xFF - 1).toByte()
                writeByte(result, adder)
            }
            Instruction.INX -> {
                this.x++
                setZero(this.x == 0.toByte())
                setNegative(this.x < 0)
            }
            Instruction.DEX -> {
                this.x--
                setZero(this.x == 0.toByte())
                setNegative(this.x < 0)
            }
            Instruction.INY -> {
                this.y++
                setZero(this.y == 0.toByte())
                setNegative(this.y < 0)
            }
            Instruction.DEY -> {
                this.y--
                setZero(this.y == 0.toByte())
                setNegative(this.y < 0)
            }
            Instruction.CLC -> {
                // N V 1 B D I Z C
                setCarry(false)
            }
            Instruction.SEC -> {
                setCarry(true)
            }
            Instruction.CLI -> {
                setInterrupt(false)
            }
            Instruction.SEI -> {
                //Set Interrupt
                setInterrupt(true)
            }
            Instruction.CLD -> {
                setDecimal(false)
            }
            Instruction.SED -> {
                setDecimal(true)
            }
            Instruction.CLV -> {
                setOverflow(false)
            }
            Instruction.LDA -> {
                // Load A from M
                val value = when (addressing) {
                    Addressing.ACC -> {
                        this.a
                    }
                    Addressing.IMM -> {
                        fetch()
                    }
                    else -> {
                        readByte(fetchOperandAdder(addressing))
                    }
                }
                this.a = value;
                setNegative(value < 0)
                setZero(value == 0.toByte())
            }
            Instruction.LDX -> {
                //Load X from M
                val value = when (addressing) {
                    Addressing.ACC -> {
                        this.x
                    }
                    Addressing.IMM -> {
                        fetch()
                    }
                    else -> {
                        readByte(fetchOperandAdder(addressing))
                    }
                }
                this.x = value;
                setNegative(value < 0)
                setZero(value == 0.toByte())
            }
            Instruction.LDY -> {
                //Load Y from M
                val value = when (addressing) {
                    Addressing.ACC -> {
                        this.y
                    }
                    Addressing.IMM -> {
                        fetch()
                    }
                    else -> {
                        readByte(fetchOperandAdder(addressing))
                    }
                }
                this.y = value;
                setNegative(value < 0)
                setZero(value == 0.toByte())
            }
            Instruction.STA -> {
                // Store A to M
                writeByte(this.a, fetchOperandAdder(addressing))
            }
            Instruction.STX -> {
                // Store X to M
                writeByte(this.x, fetchOperandAdder(addressing))
            }
            Instruction.STY -> {
                // Store Y to M
                writeByte(this.y, fetchOperandAdder(addressing))
            }
            Instruction.TXS -> {
                // Transfer X to S
                this.sp = this.x
            }
            Instruction.BCC -> {
                // N V 1 B D I Z C
                if (!bitTest(this.p, 0)) {
                    this.pc = fetchOperandAdder(addressing)
                } else {
                    this.pc++
                }
            }
            Instruction.BCS -> {
                // N V 1 B D I Z C
                if (bitTest(this.p, 0)) {
                    this.pc = fetchOperandAdder(addressing)
                } else {
                    this.pc++
                }
            }
            Instruction.BEQ -> {
                // N V 1 B D I Z C
                if (bitTest(this.p, 1)) {
                    this.pc = fetchOperandAdder(addressing)
                } else {
                    this.pc++
                }
            }
            Instruction.BNE -> {
                // N V 1 B D I Z C
                if (!bitTest(this.p, 1)) {
                    this.pc = fetchOperandAdder(addressing)
                } else {
                    this.pc++
                }
            }
            Instruction.BVC -> {
                // N V 1 B D I Z C
                if (!bitTest(this.p, 6)) {
                    this.pc = fetchOperandAdder(addressing)
                } else {
                    this.pc++
                }
            }
            Instruction.BPL -> {
                // N V 1 B D I Z C
                if (!bitTest(this.p, 7)) {
                    this.pc = fetchOperandAdder(addressing)
                } else {
                    this.pc++
                }
            }
            Instruction.BVS -> {
                // N V 1 B D I Z C
                if (bitTest(this.p, 6)) {
                    this.pc = fetchOperandAdder(addressing)
                } else {
                    this.pc++
                }
            }
            Instruction.BMI -> {
                // N V 1 B D I Z C
                if (bitTest(this.p, 7)) {
                    this.pc = fetchOperandAdder(addressing)
                } else {
                    this.pc++
                }
            }
            Instruction.BIT -> {
                // N V 1 B D I Z C
                val adder = fetchOperandAdder(addressing)
                val value = readByte(adder)
                val result = this.a and value
                setZero(result == 0.toByte())
                setNegative(bitTest(value, 7))
                setOverflow(bitTest(value, 6))
            }
            Instruction.JMP -> {
                this.pc = fetchOperandAdder(addressing)
            }
            Instruction.JSR -> {
                val adder = fetchOperandAdder(addressing)
                val upperByte = (((this.pc - 1) and 0xFF00) shr 8).toByte()
                val lowerByte = ((this.pc - 1) and 0xFF).toByte()
                push(upperByte)
                push(lowerByte)

            }
            Instruction.RTS -> {
                val lowerByte = (pop().toInt()) and 0xFF
                val upperByte = ((pop().toInt()) and 0xFF) shl 8
                this.pc = upperByte + lowerByte + 1
            }
            else -> {
                TODO("unimplemented ${decResult.first}")
            }
        }

    }

    public fun run(): Int {
        val opcode = fetch().toInt() and 0b11111111
        val decResult = decode(opcode)
        exec(decResult)
        // println("${decResult.first}, ${this.pc}")
        return decResult.third
    }
}