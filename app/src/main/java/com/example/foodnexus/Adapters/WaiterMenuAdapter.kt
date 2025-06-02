package com.example.foodnexus.Adapters

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnexus.Models.WaiterMenuStructure
import com.example.foodnexus.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.*

class WaiterMenuAdapter(
    private val menuItems: List<WaiterMenuStructure>,
    private val context: Context,
    private val userId: String,
    private val ownerId: String
) : RecyclerView.Adapter<WaiterMenuAdapter.MenuItemViewHolder>() {

    private val rtdb: DatabaseReference = FirebaseDatabase.getInstance().reference
    private lateinit var customizeRecipe: String

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
        holder.itemPrice.text = menuItem.itemPrice.toString()

        holder.addButton.setOnClickListener {
            Dialog(context).apply {
                setContentView(R.layout.cutomize_order_layout)
                window?.setBackgroundDrawableResource(android.R.color.transparent)
                setCancelable(true)

                val etCustomizedRecipe = findViewById<TextView>(R.id.CustomizeOrderEtCustomizedRecipie)
                val btnAddToCart = findViewById<MaterialButton>(R.id.CustomizeOrderBtnAddToCart)
                show()

                etCustomizedRecipe.text = menuItem.itemRecipe

                btnAddToCart.setOnClickListener {
                    customizeRecipe = etCustomizedRecipe.text.toString().trim()
                    handleAddToCart(menuItem)
                    dismiss()
                }
            }
        }
    }

    private fun handleAddToCart(menuItem: WaiterMenuStructure) {
        val cartRef = rtdb.child("Restaurants")
            .child(ownerId)
            .child("Staff")
            .child(userId)
            .child("Carts")
            .child(menuItem.itemId)

        cartRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    updateExistingItem(cartRef)
                } else {
                    addNewItem(cartRef, menuItem)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Database error: ${error.message}")
            }
        })
    }

    private fun updateExistingItem(cartRef: DatabaseReference) {
        cartRef.child("Quantity").runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentQuantity = currentData.getValue(Int::class.java) ?: 0
                currentData.value = currentQuantity + 1
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (error == null && committed) {
                    showToast("Quantity updated")
                } else {
                    showToast("Update failed: ${error?.message}")
                }
            }
        })
    }

    private fun addNewItem(cartRef: DatabaseReference, menuItem: WaiterMenuStructure) {
        val cartItem = mapOf(
            "Item Id" to menuItem.itemId,
            "Item Name" to menuItem.itemName,
            "Item Price" to menuItem.itemPrice,
            "Quantity" to 1,
            "Customize Recipe" to customizeRecipe
        )

        cartRef.setValue(cartItem)
            .addOnSuccessListener { showToast("Added to cart") }
            .addOnFailureListener { showToast("Failed to add: ${it.localizedMessage}") }
    }

    private fun showToast(message: String) {
        if (context is Activity && !context.isDestroyed) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = menuItems.size
    override fun getItemId(position: Int) = menuItems[position].itemId.hashCode().toLong()
}
