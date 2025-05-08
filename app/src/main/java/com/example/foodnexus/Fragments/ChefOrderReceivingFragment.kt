package com.example.foodnexus.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodnexus.Adapters.ChefOrderAdapter
import com.example.foodnexus.Structures.ChefOrderStructure
import com.example.foodnexus.databinding.FragmentChefOrderReceivingBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class ChefOrderReceivingFragment : Fragment() {
    private var _binding: FragmentChefOrderReceivingBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var adapter: ChefOrderAdapter
    private val orders = mutableListOf<ChefOrderStructure>()
    private lateinit var ownerId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChefOrderReceivingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ownerId = requireContext().getSharedPreferences("Details", 0)
            .getString("ownerId", "").orEmpty()

        adapter = ChefOrderAdapter(orders,
            onAccept = { orderId -> changeStatus(orderId, "accepted") },
            onReject = { orderId -> changeStatus(orderId, "declined") }
        )

        binding.rvChefOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ChefOrderReceivingFragment.adapter
        }

        fetchPendingOrders()
    }

    private fun fetchPendingOrders() {
        firestore.collection("Restaurants")
            .document(ownerId)
            .collection("Pending Orders")
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snapshot: QuerySnapshot ->
                orders.clear()
                for (doc in snapshot.documents) {
                    val id = doc.id
                    val total = doc.getString("totalPrice").orEmpty()
                    val items = (doc.get("items") as? List<Map<String, Any>>)?.map { m ->
                        ChefOrderStructure.Item(
                            m["itemName"].toString(),
                            (m["quantity"] as? Long)?.toInt() ?: 0
                        )
                    }.orEmpty()

                    orders.add(ChefOrderStructure(id, total, items))
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load orders", Toast.LENGTH_SHORT).show()
            }
    }

    private fun changeStatus(orderId: String, newStatus: String) {
        firestore.collection("Restaurants")
            .document(ownerId)
            .collection("Pending Orders")
            .document(orderId)
            .update("status", newStatus)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Order $newStatus", Toast.LENGTH_SHORT).show()
                fetchPendingOrders()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update order", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}