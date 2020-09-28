package com.github.thamid_gamer.locatereborn

import com.github.thamid_gamer.locatereborn.generator.generate
import com.github.thamid_gamer.locatereborn.translator.translate

fun main(args: Array<String>) {
    for (arg in args) {
        when (arg) {
            "generate" -> generate()
            "translate" -> translate()
        }
    }
}