package com.example.foodnexus.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnexus.R
import com.example.foodnexus.Structures.ChefOrderStructure

class ChefOrderAdapter(
    private val orders: List<ChefOrderStructure>,
    private val onAccept: (String) -> Unit,
    private val onReject: (String) -> Unit
) : RecyclerView.Adapter<ChefOrderAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val orderId: TextView = view.findViewById(R.id.textOrderId)
        private val orderTotal: TextView = view.findViewById(R.id.textOrderTotal)
        private val orderItems: TextView = view.findViewById(R.id.textOrderItems)
        private val btnAccept: Button = view.findViewById(R.id.btnAccept)
        private val btnReject: Button = view.findViewById(R.id.btnReject)

        fun bind(order: ChefOrderStructure) {
            orderId.text = "Order #${order.id}"
            orderTotal.text = "Total: PKR ${order.total}"
            orderItems.text = "${order.items.size} items"

            btnAccept.setOnClickListener { onAccept(order.id) }
            btnReject.setOnClickListener { onReject(order.id) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chef_cart_recycler_view, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount(): Int = orders.size
}