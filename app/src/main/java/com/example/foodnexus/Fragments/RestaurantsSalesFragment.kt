package com.example.foodnexus.Fragments

import android.annotation.SuppressLint
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
import com.example.foodnexus.Utils
import com.example.foodnexus.R
import com.example.foodnexus.Structures.OwnerSalesStructure
import com.example.foodnexus.databinding.FragmentResturantsSalesBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class RestaurantsSalesFragment : Fragment() {
    private var _binding: FragmentResturantsSalesBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var userId: String
    private lateinit var orderList: ArrayList<OwnerSalesStructure>
    private lateinit var adapter: OwnerSalesAdapter
    private lateinit var loadingDialog: Dialog
    private lateinit var preferences: SharedPreferences
    private lateinit var currentDate:String

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
        firestore = FirebaseFirestore.getInstance()
        preferences = requireContext().getSharedPreferences("Details", Context.MODE_PRIVATE)
        currentDate= SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).toString()
        userId = preferences.getString("userId", null).toString()

        orderList = ArrayList()
        adapter = OwnerSalesAdapter(orderList,this,userId)

        binding.RestaurantSalesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.RestaurantSalesRecyclerView.adapter = adapter

        loadingDialog = Dialog(requireContext()).apply {
            setContentView(R.layout.progress_bar)
            setCancelable(false)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadOrders() {
        Utils.showProgress(loadingDialog)

        firestore.collection("Restaurants")
            .document(userId)
            .collection("Orders")
            .document("Completed Orders")
            .collection(currentDate)
            .get()
            .addOnSuccessListener { docs ->
                orderList.clear()
                for (doc in docs) {
                    val orderId = doc.id
                    val items = doc.getString("OrderedItems") ?: "Unknown"
                    val totalAmount = doc.getString("TotalAmount") ?: "0"
                    orderList.add(OwnerSalesStructure(orderId, items, totalAmount))
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Utils.showToast(requireContext(), "Failed to load orders: ${e.localizedMessage}")
            }
            .addOnCompleteListener {
                Utils.hideProgress(loadingDialog)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
