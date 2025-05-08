package com.example.foodnexus.Fragments

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodnexus.Adapters.WaiterMenuAdapter
import com.example.foodnexus.R
import com.example.foodnexus.Structures.WaiterMenuStructure
import com.example.foodnexus.Utils
import com.example.foodnexus.databinding.FragmentWaiterMenuBinding
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
class WaiterMenuFragment : Fragment() {

    private var _binding: FragmentWaiterMenuBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var preferences: SharedPreferences
    private lateinit var loadingDialog: Dialog
    private lateinit var adapter: WaiterMenuAdapter
    private val menuItems = ArrayList<WaiterMenuStructure>()

    private var userId: String = ""
    private var restaurantName: String = ""
    private var ownerId: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWaiterMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeComponents()
        if (validateUserData()) {
            setupUI()
            loadMenuData()
        }
        setupClickListeners()
    }

    private fun initializeComponents() {
        firestore = FirebaseFirestore.getInstance()
        preferences = requireContext().getSharedPreferences("Details", Context.MODE_PRIVATE)
        loadingDialog = createLoadingDialog()
    }

    private fun createLoadingDialog(): Dialog {
        return Dialog(requireContext()).apply {
            setContentView(R.layout.progress_bar)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setCancelable(false)
        }
    }

    private fun validateUserData(): Boolean {
        userId = preferences.getString("userId", null).orEmpty()
        ownerId = preferences.getString("ownerId", null).orEmpty()

        if (userId.isEmpty() || ownerId.isEmpty()) {
            handleInvalidUser()
            return false
        }

        restaurantName = preferences.getString("name", "Restaurant").toString()
        binding.RestaurantMenuTvName.text = restaurantName
        return true
    }

    private fun handleInvalidUser() {
        Utils.showToast(requireContext(), "Authentication required. Please login again.")
        findNavController().navigate(R.id.action_waiterMenuFragment_to_loginFragment)
    }

    private fun setupUI() {
        adapter = WaiterMenuAdapter(menuItems, requireContext(), userId, ownerId).apply {
            setHasStableIds(true)
        }

        binding.RestaurantMenuRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@WaiterMenuFragment.adapter
            itemAnimator = null
        }
    }

    private fun loadMenuData() {
        if (!isAdded) return

        Utils.showProgress(loadingDialog)

        firestore.collection("Restaurants")
            .document(ownerId)
            .collection("Menu")
            .get()
            .addOnCompleteListener { task ->
                if (!isAdded) return@addOnCompleteListener

                Utils.hideProgress(loadingDialog)

                if (task.isSuccessful) {
                    processMenuData(task.result?.documents)
                } else {
                    handleDataError(task.exception)
                }
            }
    }

    private fun processMenuData(documents: List<DocumentSnapshot>?) {
        menuItems.clear()
        documents?.forEach { doc ->
            menuItems.add(WaiterMenuStructure(
                doc.id,
                doc.getString("Item Name") ?: "Unnamed Item",
                doc.getString("Item Recipe") ?: "No description",
                doc.getString("Item Price") ?: "0"
            ))
        }
        updateUIState()
    }

    private fun updateUIState() {
        val isEmpty = menuItems.isEmpty()
        binding.RestaurantMenuRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.RestaurantMenuTvAddClasses.visibility = if (isEmpty) View.VISIBLE else View.GONE

        if (isAdded) {
            adapter.notifyDataSetChanged()
        }
    }

    private fun handleDataError(exception: Exception?) {
        val errorMessage = exception?.localizedMessage ?: "Unknown error occurred"
        Utils.showToast(requireContext(), "Menu loading failed: $errorMessage")
        updateUIState()
    }

    private fun setupClickListeners() {
        binding.RestaurantMenuImgBtnMenu.setOnClickListener {
            findNavController().navigate(R.id.action_waiterMenuFragment_to_waiterCartFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}