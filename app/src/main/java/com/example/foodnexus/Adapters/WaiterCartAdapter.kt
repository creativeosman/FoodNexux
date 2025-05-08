package com.example.foodnexus.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnexus.R
import com.example.foodnexus.Structures.WaiterCartStructure
class WaiterCartAdapter(
    private val items: MutableList<WaiterCartStructure>,
    private val onIncreaseClicked: (Int) -> Unit,
    private val onDecreaseClicked: (Int) -> Unit
) : RecyclerView.Adapter<WaiterCartAdapter.CartViewHolder>() {

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemName: TextView = itemView.findViewById(R.id.cartItemName)
        private val itemPrice: TextView = itemView.findViewById(R.id.cartItemPrice)
        private val itemQuantity: TextView = itemView.findViewById(R.id.cartItemQuantity)
        private val increaseButton: ImageButton = itemView.findViewById(R.id.btnIncrease)
        private val decreaseButton: ImageButton = itemView.findViewById(R.id.btnDecrease)
        private val itemRecipe: TextView = itemView.findViewById(R.id.CartItemRecipe)
        fun bind(item: WaiterCartStructure) {
            itemName.text = item.itemName

            // Update price based on quantity
            val basePrice = item.itemPrice.toDoubleOrNull() ?: 0.0
            val totalPrice = basePrice * item.quantity
            itemPrice.text = totalPrice.toString()
            itemQuantity.text = item.quantity.toString()
            itemRecipe.text = item.itemCustomizeRecipe
            increaseButton.setOnClickListener { onIncreaseClicked(adapterPosition) }
            decreaseButton.setOnClickListener { onDecreaseClicked(adapterPosition) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.waiter_cart_recycler_view, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
