package com.example.nesonandroid.model.device

final class Rom(data: ByteArray) : Memory {
    internal var data: ByteArray

    init {
        this.data = data
    }

    override fun read(adder: Int): Byte {
        return data[adder]
    }

    override fun write(adder: Int, data: Byte) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}