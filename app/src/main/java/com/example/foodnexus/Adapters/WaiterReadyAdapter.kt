// ChefPreparingOrderAdapter.kt
package com.example.foodnexus.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnexus.Models.WaiterReadyStructure
import com.example.foodnexus.R

class WaiterReadyAdapter(
    private val orders: List<WaiterReadyStructure>,
    private val onAccept: (String) -> Unit
) : RecyclerView.Adapter<WaiterReadyAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val orderId: TextView = view.findViewById(R.id.textOrderId)
        private val orderTotal: TextView = view.findViewById(R.id.textOrderTotal)
        private val btnAccept: Button = view.findViewById(R.id.btnPrepared)

        fun bind(order: WaiterReadyStructure) {
            orderId.text = "Order #${order.id}"
            orderTotal.text = "Total: PKR ${order.total}"
            btnAccept.setOnClickListener { onAccept(order.id) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.waiter_ready_recycler_view, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount() = orders.size
}
