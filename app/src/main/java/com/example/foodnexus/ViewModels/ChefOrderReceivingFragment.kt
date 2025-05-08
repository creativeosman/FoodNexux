package com.example.foodnexus.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodnexus.Adapters.ChefOrderAdapter
import com.example.foodnexus.Models.ChefOrderStructure
import com.example.foodnexus.Utils
import com.example.foodnexus.databinding.FragmentChefOrderReceivingBinding
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ChefOrderReceivingFragment : Fragment() {

    private var _binding: FragmentChefOrderReceivingBinding? = null
    private val binding get() = _binding!!
//    private val firestore = FirebaseFirestore.getInstance()
//    private lateinit var adapter: ChefOrderAdapter
//    private val orders = mutableListOf<ChefOrderStructure>()
//    private lateinit var ownerId: String
//    private var pendingListener: ListenerRegistration? = null
//
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChefOrderReceivingBinding.inflate(inflater, container, false)
        return binding.root
    }
//
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Utils.showToast(requireContext(),"hh")
//        setupSharedPreferences()
//        setupRecyclerView()
//        startPendingOrdersListener()
    }
//
//    private fun setupSharedPreferences() {
//        ownerId = requireContext()
//            .getSharedPreferences("Details", 0)
//            .getString("ownerId", "")
//            .orEmpty()
//
//        if (ownerId.isEmpty()) {
//            Toast.makeText(requireContext(), "Restaurant ID not found", Toast.LENGTH_LONG).show()
//        }
//    }
//
//    private fun setupRecyclerView() {
//        adapter = ChefOrderAdapter(
//            orders,
//            onAccept = { orderId -> changeStatus(orderId, "accepted") },
//            onReject = { orderId -> changeStatus(orderId, "declined") }
//        )
//        binding.rvChefOrders.apply {
//            layoutManager = LinearLayoutManager(requireContext())
//            adapter = this@ChefOrderReceivingFragment.adapter
//        }
//    }
//
//    private fun startPendingOrdersListener() {
//        pendingListener?.remove()
//        if (ownerId.isEmpty()) return
//
//        pendingListener = firestore.collection("Restaurants")
//            .document(ownerId)
//            .collection("Pending Orders")
//            .whereEqualTo("Status", "pending")
//            .addSnapshotListener(EventListener { snapshot, error ->
//                when {
//                    error != null -> {
//                        Toast.makeText(
//                            requireContext(),
//                            "Error: ${error.localizedMessage}",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                    snapshot != null -> {
//                        processSnapshot(snapshot)
//                    }
//                }
//            })
//    }
//
//    private fun processSnapshot(snapshot: com.google.firebase.firestore.QuerySnapshot) {
//        orders.clear()
//        snapshot.documents.forEach { doc ->
//            val order = parseOrderDocument(doc)
//            order?.let { orders.add(it) }
//        }
//        adapter.notifyDataSetChanged()
//    }
//
//    private fun parseOrderDocument(doc: com.google.firebase.firestore.DocumentSnapshot): ChefOrderStructure? {
//        return try {
//            val id = doc.id
//            val total = doc.getString("Total Price") ?: "0.00"
//            val items = parseItems(doc.get("Items"))
//            ChefOrderStructure(id, total, items)
//        } catch (e: Exception) {
//            null
//        }
//    }
//
//    private fun parseItems(itemsAny: Any?): List<ChefOrderStructure.Item> {
//        return (itemsAny as? List<*>)?.mapNotNull { item ->
//            try {
//                val map = item as Map<*, *>
//                ChefOrderStructure.Item(
//                    name = map["Item Name"].toString(),
//                    quantity = (map["Quantity"] as? Number)?.toInt() ?: 0
//                )
//            } catch (e: Exception) {
//                null
//            }
//        } ?: emptyList()
//    }
//
//    private fun changeStatus(orderId: String, newStatus: String) {
//        if (ownerId.isEmpty()) return
//
//        firestore.collection("Restaurants")
//            .document(ownerId)
//            .collection("Pending Orders")
//            .document(orderId)
//            .update("Status", newStatus)
//            .addOnFailureListener {
//                Toast.makeText(requireContext(), "Update failed", Toast.LENGTH_SHORT).show()
//            }
//    }
}
//    override fun onDestroyView() {
//        super.onDestroyView()
//        pendingListener?.remove()
//        _binding = null
//    }

