package com.example.foodnexus.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.foodnexus.R
import com.example.foodnexus.Utils
import com.example.foodnexus.databinding.FragmentStaffSignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class StaffSignUpFragment : Fragment() {
    private var _binding: FragmentStaffSignUpBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val progressDialog by lazy { createProgressDialog() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            SignupFragmentTvLoginLink.setOnClickListener {
                findNavController().navigate(R.id.action_staffSignUpFragment_to_loginFragment)
            }

            // Setup role dropdown
            val roles = listOf("Waiter", "Chef")
            SignupFragmentSpRole.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                roles
            )

            SignupFragmentBtnSignUp.setOnClickListener {
                if (validateInputs()) performSignUp()
            }
        }
    }

    private fun createProgressDialog(): Dialog = Dialog(requireContext()).apply {
        setContentView(R.layout.progress_bar)
        setCancelable(false)
    }

    private fun performSignUp() {
        val email = binding.SignupFragmentEtEmail.text.toString().trim()
        val password = binding.SignupFragmentEtPassword.text.toString().trim()
        val ownerId = binding.SignupFragmentEtStaffId.text.toString().trim()

        Utils.showProgress(progressDialog)
        lifecycleScope.launch {
            try {
                // Create auth user
                auth.createUserWithEmailAndPassword(email, password).await()
                auth.currentUser?.sendEmailVerification()?.await()

                // Verify owner existence and save data
                saveStaffToFirestore(ownerId, email)

                Utils.showToast(requireContext(), "Staff account created! Verification email sent.")
                findNavController().navigate(R.id.action_staffSignUpFragment_to_loginFragment)
            } catch (e: Exception) {
                Utils.showToast(requireContext(), "Sign up failed: ${e.localizedMessage}")
            } finally {
                Utils.hideProgress(progressDialog)
            }
        }
    }

    private suspend fun saveStaffToFirestore(ownerId: String, email: String) {
        // Check owner document
        val ownerRef = db.collection("Restaurants").document(ownerId)
        val ownerSnap = ownerRef.get().await()
        if (!ownerSnap.exists()) {
            Utils.showToast(requireContext(), "Owner ID does not exist.")
            return
        }

        // Prepare data
        val uid = auth.currentUser?.uid ?: return
        val staffData = mapOf(
            "name" to binding.SignupFragmentEtStaffName.text.toString().trim(),
            "providedId" to ownerId,
            "email" to email,
            "phoneNumber" to binding.SignupFragmentEtPhoneNumber.text.toString().trim(),
            "role" to "Staff",
            "category" to binding.SignupFragmentSpRole.selectedItem.toString()
        )
        val roleData = mapOf(
            "role" to binding.SignupFragmentSpRole.selectedItem.toString(),
            "providedId" to ownerId
        )

        // Batch write: add staff under owner and record role
        db.runBatch { batch ->
            val staffRef = ownerRef.collection("Staff").document(uid)
            val rolesRef = db.collection("Roles").document(email)
            batch.set(staffRef, staffData)
            batch.set(rolesRef, roleData)
        }.await()
    }

    private fun validateInputs(): Boolean {
        binding.apply {
            fun showError(field: View, msg: String) {
                (field as? androidx.appcompat.widget.AppCompatEditText)?.error = msg
            }

            return when {
                SignupFragmentEtStaffName.text.isBlank() -> { showError(SignupFragmentEtStaffName, "Enter full name"); false }
                SignupFragmentEtStaffId.text.isBlank() -> { showError(SignupFragmentEtStaffId, "Enter owner ID"); false }
                SignupFragmentEtEmail.text.isBlank() -> { showError(SignupFragmentEtEmail, "Enter email"); false }
                SignupFragmentEtPassword.text.isBlank() -> { showError(SignupFragmentEtPassword, "Enter password"); false }
                SignupFragmentEtConfirmPassword.text.isBlank() -> { showError(SignupFragmentEtConfirmPassword, "Confirm password"); false }
                binding.SignupFragmentEtPassword.text.toString() != binding.SignupFragmentEtConfirmPassword.text.toString() -> {
                    showError(SignupFragmentEtConfirmPassword, "Passwords do not match"); false
                }
                SignupFragmentEtPhoneNumber.text.isBlank() -> { showError(SignupFragmentEtPhoneNumber, "Enter phone number"); false }
                else -> true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}