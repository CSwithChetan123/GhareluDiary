package com.ghareludiary.app.model

enum class CategoryType (val displayName: String, val displayEmoji: String) {
    MILK("Milk", "🥛"),
    WATER("Water", "💧"),
    MAID("Maid", "🧹"),
    COOK("Cook", "🍲"),
    DRIVER("Driver", "🚘"),
    GARDENER("Gardener", "🌸");

    fun hasQuantity(): Boolean = this == MILK || this == WATER
    fun getQuantity(): String = when (this) {
        MILK -> "Liters"
        WATER -> "Cans"
        else -> ""
    }

    fun getCountLable(): String = when(this){
        MAID -> "Days"
        COOK -> "Days"
        DRIVER -> "Days"
        GARDENER -> "Visits"
        else -> ""
    }
}

