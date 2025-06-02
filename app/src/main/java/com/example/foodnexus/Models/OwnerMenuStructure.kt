package com.example.foodnexus.Models

data class OwnerMenuStructure(
    val itemId:String,
    val itemName:String,
    val itemRecipe:String,
    val itemPrice:Double,
    val calories: Float = 0f
)
