package com.example.nesonandroid.model.device

final class Ram(initSize:Int) : Memory {
    private var ram: Array<Byte>

    init {
        val size = initSize;
        this.ram = Array<Byte>(size, {i -> 0.toByte()})
    }
    override fun read(adder: Int): Byte {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        return ram[adder]
    }

    override fun write(adder: Int, data: Byte) {
        ram[adder] = data
    }

    internal fun getSlice(begin: Int, end: Int) : ByteArray {
        return ram.slice(begin..end).toByteArray()
    }
}