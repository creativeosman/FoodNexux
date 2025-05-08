package com.example.foodnexus.Models

data class WaiterCartStructure(
    val itemId:String,
    val itemName: String,
    val itemPrice: Double,
    var quantity:Int,
    val itemCustomizeRecipe:String
)