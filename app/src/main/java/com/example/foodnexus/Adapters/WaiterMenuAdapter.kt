package com.example.foodnexus.Adapters

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnexus.R
import com.example.foodnexus.Structures.WaiterMenuStructure
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class WaiterMenuAdapter(
    private val menuItems: List<WaiterMenuStructure>,
    private val context: Context,
    private val userId: String,
    private val ownerId: String
) : RecyclerView.Adapter<WaiterMenuAdapter.MenuItemViewHolder>() {


    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var customizeRecipe :String
    inner class MenuItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemName: TextView = view.findViewById(R.id.WaiterTvItemName)
        val itemRecipe: TextView = view.findViewById(R.id.WaiterTvRecipe)
        val itemPrice: TextView = view.findViewById(R.id.WaiterTvPrice)
        val addButton: MaterialButton = view.findViewById(R.id.BtnCustomization)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.waiter_menu_recycler_view, parent, false)
        return MenuItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
        val menuItem = menuItems[position]

        holder.itemName.text = menuItem.itemName
        holder.itemRecipe.text = menuItem.itemRecipe
        holder.itemPrice.text = menuItem.itemPrice
        holder.addButton.setOnClickListener {

            Dialog(context).apply {
                setContentView(R.layout.cutomize_order_layout)
                window?.setBackgroundDrawableResource(android.R.color.transparent)
                setCancelable(true)
                val etItemName = findViewById<TextView>(R.id.CustomizeOrderEtItemName)
                val etItemRecipe = findViewById<TextView>(R.id.CustomizeOrderEtItemRecipie)
                val etCustomizedRecipe = findViewById<TextView>(R.id.CustomizeOrderEtCustomizedRecipie)
                val btnAddToCart = findViewById<MaterialButton>(R.id.CustomizeOrderBtnAddToCart)
                val price = findViewById<TextView>(R.id.Price)
                etItemName.text = menuItem.itemName
                etItemRecipe.text = menuItem.itemRecipe
                price.text = menuItem.itemPrice
                show()
                btnAddToCart.setOnClickListener {
                    customizeRecipe=etCustomizedRecipe.text.toString().trim()

                    Toast.makeText(context,customizeRecipe,Toast.LENGTH_SHORT).show()

                    handleAddToCart(menuItem)
                    dismiss()
                }
            }
        }
    }
    private fun handleAddToCart(menuItem: WaiterMenuStructure) {
        val cartRef = firestore.collection("Restaurants")
            .document(ownerId)
            .collection("Staff")
            .document(userId)
            .collection("Carts")
            .document(menuItem.itemId)

        cartRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                when {
                    task.result?.exists() == true -> updateExistingItem(cartRef)
                    else -> addNewItem(cartRef, menuItem)
                }
            }
        }
    }

    private fun updateExistingItem(cartRef: DocumentReference) {
        cartRef.update("quantity", FieldValue.increment(1))
            .addOnSuccessListener { showToast("Quantity updated") }
            .addOnFailureListener { showToast("Update failed") }
    }

    private fun addNewItem(cartRef: DocumentReference, menuItem: WaiterMenuStructure) {
        val cartItem = hashMapOf(
            "itemId" to menuItem.itemId,
            "itemName" to menuItem.itemName,
            "itemPrice" to menuItem.itemPrice,
            "quantity" to 1,
            "customizeRecipe" to customizeRecipe
        )

        cartRef.set(cartItem)
            .addOnSuccessListener { showToast("Added to cart") }
            .addOnFailureListener { showToast("Failed to add") }
    }

    private fun showToast(message: String) {
        if (context is Activity && !context.isDestroyed) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = menuItems.size
    override fun getItemId(position: Int) = menuItems[position].itemId.hashCode().toLong()
}