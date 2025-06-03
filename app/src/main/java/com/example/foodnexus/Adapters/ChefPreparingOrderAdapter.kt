// ChefPreparingOrderAdapter.kt
package com.example.foodnexus.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnexus.Models.ChefOrderPreparingStructure
import com.example.foodnexus.R

class ChefPreparingOrderAdapter(
    private val orders: List<ChefOrderPreparingStructure>,
    private val onAccept: (String) -> Unit
) : RecyclerView.Adapter<ChefPreparingOrderAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val orderId: TextView = view.findViewById(R.id.textOrderId)
        private val orderTotal: TextView = view.findViewById(R.id.textOrderTotal)
        private val btnAccept: Button = view.findViewById(R.id.btnPrepared)

        fun bind(order: ChefOrderPreparingStructure) {
            orderId.text = "Order #${order.id}"
            orderTotal.text = "Total: PKR ${order.total}"
            btnAccept.setOnClickListener { onAccept(order.id) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chef_preparing_recycler_view, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount() = orders.size
}
