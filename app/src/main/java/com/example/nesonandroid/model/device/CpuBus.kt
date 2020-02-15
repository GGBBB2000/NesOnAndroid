package com.example.nesonandroid.model.device

final class CpuBus(ram : Ram, rom: Rom, ppu: Ppu) {
    private var rom: Rom
    private var ram: Ram
    private var ppu: Ppu

    init {
        this.ram = ram
        this.rom = rom
        this.ppu = ppu
    }

    internal fun readByCpu(addr: Int) : Byte {
        return if (addr < 0x0800) {
            // WRAM
            ram.read(addr)
        } else if (addr < 0x1000) {
            // WRAM Mirror
            ram.read(addr - 0x0800)
        } else if (addr < 0x1800) {
            // WRAM Mirror
            ram.read(addr - 0x1000)
        } else if (addr < 0x2000) {
            // WRAM Mirror
            /*let data = self.wram.read(addr - 0x1800);
            *NMI_INT.borrow_mut() = (((data) & (1 << 7)) >> 7) == 1;
            println!("hogehoge {}", *NMI_INT.borrow());
            data*/
            ram.read(addr - 0x1800)
        } else if (addr < 0x2008) {
            // PPU Register
            /*self.ppu
                .read(ppu::RegType::from_u16(addr - 0x2000).unwrap())
            */
            TODO("PPU I/O")
        } else if (addr < 0x4000) {
            // PPU Mirror
            TODO("PPU I/O Mirror")
        } else if (addr == 0x4016) {
            // Joypad P1
            TODO("Joy pad 1")
        } else if (addr == 0x4017) {
            // Joypad P2
            //0
            TODO("Joy pad 2")
        } else if (addr < 0x6000) {
            // Extended ROM
            0
        } else if (addr < 0x8000) {
            // Extended RAM
            TODO("Ext RAM")
        } else if (addr < 0xC000) {
            // PRG-ROM
            rom.read(addr - 0x8000)
        } else {
            //0xC000 ~ 0xFFFF   // PRG-ROM
            val base_addr = if (rom.data.size == 0x4000) {
                0xC000
            } else {
                0x8000
            };

            rom.read(addr - base_addr)
        }
    }

    internal fun writeByCpu(data : Byte, addr: Int) {
        if (addr < 0x0800) {
            // WRAM
            ram.write(addr,data)
        } else if (addr < 0x1000) {
            // WRAM Mirror
            ram.write(addr - 0x0800, data)
        } else if (addr < 0x1800) {
            // WRAM Mirror
            ram.write(addr - 0x1000, data)
        } else if (addr < 0x2000) {
            // WRAM Mirror
            ram.write(addr - 0x1800, data)
        } else if (addr < 0x2008) {
            // PPU Register
            ppu.writeToRegister(addr, data)
        } else if (addr < 0x4000) {
            // PPU Mirror
            TODO("PPU I/O Mirror")
        } else if (addr == 0x4016) {
            // Joypad P1
            error("Do not write to $addr (Joy pad 1)")
        } else if (addr == 0x4017) {
            // Joypad P2
            //0
            error("Do not write to $addr (Joy pad 2)")
        } else if (addr < 0x6000) {
            // Extended ROM
            error("Do not write to $addr (Ext ROM)")
        } else if (addr < 0x8000) {
            // Extended RAM
            TODO("Ext RAM")
        } else {
            error("Do not write to $addr (PRG-ROM)")
        }
    }
}