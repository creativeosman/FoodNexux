package com.example.foodnexus.Structures
data class ChefOrderStructure(
    val id: String,
    val total: String,
    val items: List<Item>
) {
    data class Item(val name: String, val quantity: Int)
}