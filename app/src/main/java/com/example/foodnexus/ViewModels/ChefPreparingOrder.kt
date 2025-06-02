package com.example.foodnexus.ViewModels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodnexus.Adapters.ChefOrderAdapter
import com.example.foodnexus.Models.ChefOrderStructure
import com.example.foodnexus.R
import com.example.foodnexus.databinding.FragmentChefOrderReceivingBinding
import com.example.foodnexus.databinding.FragmentChefPreparingOrderBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.collections.get

class ChefPreparingOrder : Fragment() {
    private var _binding: FragmentChefPreparingOrderBinding? = null
    private val binding get() = _binding!!
    private lateinit var bottomNav: BottomNavigationView
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var adapter: ChefOrderAdapter
    private val orders = mutableListOf<ChefOrderStructure>()
    private lateinit var ownerId: String
    private var activeFragment: Fragment? = null
    private val fragmentMap = mutableMapOf<String, Fragment>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChefPreparingOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        ownerId = requireContext().getSharedPreferences("Details", 0)
            .getString("ownerId", "").orEmpty()

        adapter = ChefOrderAdapter(
            orders,
            onAccept = { orderId -> changeStatus(orderId, "accepted") },
            onReject = { orderId -> changeStatus(orderId, "declined") }
        )

        binding.rvChefOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ChefPreparingOrder.adapter
        }
        fetchPendingOrders()
    }


    private fun fetchPendingOrders() {
        firestore.collection("Restaurants")
            .document(ownerId)
            .collection("Pending Orders")
            .whereEqualTo("Status", "preparing")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(requireContext(), "Error loading orders", Toast.LENGTH_SHORT)
                        .show()
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    orders.clear()
                    for (doc in snapshot.documents) {
                        val id = doc.id
                        val total = doc.getString("totalPrice").orEmpty()

                        val items = (doc.get("items") as? List<*>)
                            ?.mapNotNull { item ->
                                val map = item as? Map<*, *> ?: return@mapNotNull null
                                val name = map["itemName"]?.toString() ?: return@mapNotNull null
                                val quantity = when (val q = map["quantity"]) {
                                    is Long -> q.toInt()
                                    is Int -> q
                                    is Double -> q.toInt()
                                    else -> 0
                                }
                                ChefOrderStructure.Item(name, quantity)
                            }.orEmpty()

                        orders.add(ChefOrderStructure(id, total, items))
                    }

                    adapter.notifyDataSetChanged()
                } else {
                    orders.clear()
                    adapter.notifyDataSetChanged()
                }
            }
    }

    private fun changeStatus(orderId: String, newStatus: String) {
        firestore.collection("Restaurants")
            .document(ownerId)
            .collection("Pending Orders")
            .document(orderId)
            .update("Status", newStatus)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Order $newStatus", Toast.LENGTH_SHORT).show()
                fetchPendingOrders()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update order", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}