package com.example.foodnexus.Structures

data class WaiterCartStructure(
    val itemId:String,
    val itemName : String,
    val itemPrice:String,
    var quantity:Int,
    val itemCustomizeRecipe:String
)
