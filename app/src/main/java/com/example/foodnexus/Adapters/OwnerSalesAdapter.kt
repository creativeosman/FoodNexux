package com.example.foodnexus.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnexus.Fragments.RestaurantsSalesFragment
import com.example.foodnexus.R
import com.example.foodnexus.Structures.OwnerSalesStructure
import com.google.firebase.firestore.FirebaseFirestore

class OwnerSalesAdapter(
    private var arrayList: ArrayList<OwnerSalesStructure>,
    private var fragment: RestaurantsSalesFragment,
    private var userId:String
): RecyclerView.Adapter<OwnerSalesAdapter.ViewHolder>() {
    private var firestore= FirebaseFirestore.getInstance()
    class ViewHolder(itemView:View):RecyclerView.ViewHolder(itemView){

        val orderId: TextView =itemView.findViewById(R.id.tvOrderId)
        val orderItems: TextView =itemView.findViewById(R.id.tvOrderedItems)
        val orderPrice: TextView =itemView.findViewById(R.id.tvTotalAmount)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view=LayoutInflater.from(parent.context).inflate(R.layout.owner_sales_recycler_view,parent,false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val itemData=arrayList[position]

        holder.orderId.text=itemData.orderId
        holder.orderItems.text=itemData.orderItems
        holder.orderPrice.text=itemData.orderPrice
    }
}