package com.example.foodnexus.ViewModels

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodnexus.Adapters.OwnerSalesAdapter
import com.example.foodnexus.Models.OwnerSalesStructure
import com.example.foodnexus.R
import com.example.foodnexus.Utils
import com.example.foodnexus.databinding.FragmentResturantsSalesBinding
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class OwnerSalesFragment : Fragment() {
    private var _binding: FragmentResturantsSalesBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: FirebaseDatabase
    private lateinit var userId: String
    private lateinit var orderList: ArrayList<OwnerSalesStructure>
    private lateinit var adapter: OwnerSalesAdapter
    private lateinit var loadingDialog: Dialog
    private lateinit var preferences: SharedPreferences
    private lateinit var currentDate: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResturantsSalesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        loadOrders()
    }

    private fun init() {
        database = FirebaseDatabase.getInstance()
        preferences = requireContext().getSharedPreferences("Details", Context.MODE_PRIVATE)
        currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        userId = preferences.getString("userId", null).toString()

        orderList = ArrayList()
        adapter = OwnerSalesAdapter(orderList, this, userId)

        binding.RestaurantSalesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.RestaurantSalesRecyclerView.adapter = adapter

        loadingDialog = Dialog(requireContext()).apply {
            setContentView(R.layout.progress_bar)
            setCancelable(false)
        }
    }

    private fun loadOrders() {
        Utils.showProgress(loadingDialog)

        val ordersRef = database.reference
            .child("Restaurants")
            .child(userId)
            .child("Orders")
            .child("Completed Orders")
            .child(currentDate)

        ordersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                orderList.clear()
                for (orderSnap in snapshot.children) {
                    val orderId = orderSnap.key ?: continue
                    val items = orderSnap.child("OrderedItems").getValue(String::class.java) ?: "Unknown"
                    val totalAmount = orderSnap.child("TotalAmount").getValue(Double::class.java) ?: 0.0

                    orderList.add(OwnerSalesStructure(orderId, items, totalAmount))
                }
                adapter.notifyDataSetChanged()
                Utils.hideProgress(loadingDialog)
            }

            override fun onCancelled(error: DatabaseError) {
                Utils.showToast(requireContext(), "Failed to load orders: ${error.message}")
                Utils.hideProgress(loadingDialog)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
