package com.example.nesonandroid.model.device

interface Memory {
    public fun read(adder: Int): Byte
    public fun write(adder: Int, data: Byte)
}