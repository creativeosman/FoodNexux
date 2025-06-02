package com.example.foodnexus.Models

data class WaiterCartStructure(
    val itemId:String="",
    val itemName: String="",
    val itemPrice: Double=0.0,
    var quantity:Int=1,
    val itemCustomizeRecipe:String=""
)