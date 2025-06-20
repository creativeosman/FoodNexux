// ChefPreparingOrderAdapter.kt
package com.example.foodnexus.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnexus.Models.ChefOrderReadyStructure
import com.example.foodnexus.R

class ChefReadyOrderAdapter(
    private val orders: List<ChefOrderReadyStructure>
) : RecyclerView.Adapter<ChefReadyOrderAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val orderId: TextView = view.findViewById(R.id.textOrderId)
        private val orderTotal: TextView = view.findViewById(R.id.textOrderTotal)

        fun bind(order: ChefOrderReadyStructure) {
            orderId.text = "Order #${order.id}"
            orderTotal.text = "Total: PKR ${order.total}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chef_ready_recycler_view, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount() = orders.size
}
