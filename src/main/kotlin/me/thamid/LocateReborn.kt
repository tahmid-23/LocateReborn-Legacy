package me.thamid

import me.thamid.generator.generate
import me.thamid.translator.translate

fun main(args: Array<String>) {
    when (args[0]) {
        "generate" -> generate()
        "translate" -> translate()
    }
}