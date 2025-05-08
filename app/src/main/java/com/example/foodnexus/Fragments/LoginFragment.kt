package com.example.foodnexus.fragments

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings.Global.putString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.foodnexus.R
import com.example.foodnexus.Utils
import com.example.foodnexus.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private lateinit var prefs: SharedPreferences
    private val progressDialog by lazy { createProgressDialog() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = requireContext().getSharedPreferences("Details", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.LoginFragmentTvSignUpLink.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_roleAssignFragment)
        }
        binding.LoginFragmentTvForgotPassword.setOnClickListener { handlePasswordReset() }
        binding.LoginFragmentBtnLogin.setOnClickListener { handleLogin() }
    }

    private fun createProgressDialog(): Dialog = Dialog(requireContext()).apply {
        setContentView(R.layout.progress_bar)
        setCancelable(false)
    }

    private fun handlePasswordReset() {
        val email = binding.LoginFragmentEtEmail.text.toString().trim()
        if (email.isEmpty()) {
            binding.LoginFragmentEtEmail.error = "Please enter your email"
            return
        }
        Utils.showProgress(progressDialog)
        lifecycleScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                Utils.showToast(requireContext(), "Password reset email sent.")
            } catch (e: Exception) {
                Utils.showToast(requireContext(), "Reset failed: ${e.localizedMessage}")
            } finally {
                Utils.hideProgress(progressDialog)
            }
        }
    }

    private fun handleLogin() {
        val email = binding.LoginFragmentEtEmail.text.toString().trim()
        val password = binding.LoginFragmentEtPassword.text.toString().trim()

        binding.apply {
            if (email.isEmpty()) { LoginFragmentEtEmail.error = "Please enter your email"; return }
            if (password.isEmpty()) { LoginFragmentEtPassword.error = "Please enter your password"; return }
        }

        Utils.showProgress(progressDialog)
        lifecycleScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                val user = auth.currentUser
                if (user == null || !user.isEmailVerified) {
                    Utils.showToast(requireContext(), "Please verify your email before login.")
                    auth.signOut()
                    return@launch
                }

                // Fetch role document
                val roleDoc = db.collection("Roles").document(email).get().await()
                if (!roleDoc.exists()) {
                    Utils.showToast(requireContext(), "User role not found.")
                    return@launch
                }

                val role = roleDoc.getString("role") ?: ""
                val providedId = roleDoc.getString("providedId") ?: ""

                saveSessionAndNavigate(role, user.uid, providedId)

            } catch (e: Exception) {
                Utils.showToast(requireContext(), "Login failed: ${e.localizedMessage}")
            } finally {
                Utils.hideProgress(progressDialog)
            }
        }
    }

    private suspend fun saveSessionAndNavigate(role: String, uid: String, providedId: String) {
        val editor = prefs.edit().apply {
            putBoolean("isLogin", true)
            putString("userId", uid)
            putString("role", role)
        }

        when (role) {
            "Owner" -> {
                val restDoc = db.collection("Restaurants").document(uid).get().await()
                val restName = restDoc.getString("restaurantName") ?: ""
                editor.putString("restaurantName", restName)
                editor.putString("role","Owner")
                editor.apply()
                findNavController().navigate(R.id.action_loginFragment_to_restaurantMenuFragment)
            }
            "Waiter" -> {
                val staffDoc = db.collection("Restaurants")
                    .document(providedId)
                    .collection("Staff")
                    .document(uid)
                    .get()
                    .await()

                if (!staffDoc.exists()) {
                    Utils.showToast(requireContext(), "Staff data missing.")
                    return
                }

                editor.apply {
                    putString("ownerId", providedId)
                    putString("userId", uid)
                    putString("name", staffDoc.getString("name"))
                    putString("role","Waiter")
                }.apply()
                findNavController().navigate(R.id.action_loginFragment_to_waiterMenuFragment)
            }

            "Chef" -> {
                val staffDoc = db.collection("Restaurants")
                    .document(providedId)
                    .collection("Staff")
                    .document(uid)
                    .get()
                    .await()

                if (!staffDoc.exists()) {
                    Utils.showToast(requireContext(), "Staff data missing.")
                    return
                }

                editor.apply {
                    putString("ownerId", providedId)
                    putString("userId", uid)
                    putString("name", staffDoc.getString("name"))
                    putString("role","Chef")
                }.apply()
                findNavController().navigate(R.id.action_loginFragment_to_chefOrderReceivingFragment)
            }



            else -> {
                Utils.showToast(requireContext(), "Unknown role: $role")
                editor.apply()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
