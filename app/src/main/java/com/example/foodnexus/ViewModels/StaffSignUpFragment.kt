package com.example.foodnexus.ViewModels

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
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class StaffSignUpFragment : Fragment() {
    private var _binding: FragmentStaffSignUpBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val dbRef by lazy { FirebaseDatabase.getInstance().reference }
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

        // Navigate to login if user taps “Already have an account?”
        binding.SignupFragmentTvLoginLink.setOnClickListener {
            findNavController().navigate(R.id.action_staffSignUpFragment_to_loginFragment)
        }

        // Populate Role spinner with “Waiter” / “Chef”
        val roles = listOf("Waiter", "Chef")
        binding.SignupFragmentSpRole.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            roles
        )

        // Sign up button
        binding.SignupFragmentBtnSignUp.setOnClickListener {
            if (validateInputs()) {
                performSignUp()
            }
        }
    }

    private fun createProgressDialog(): Dialog = Dialog(requireContext()).apply {
        setContentView(R.layout.progress_bar)
        setCancelable(false)
    }

    private fun performSignUp() {
        val email    = binding.SignupFragmentEtEmail.text.toString().trim()
        val password = binding.SignupFragmentEtPassword.text.toString().trim()
        val ownerId  = binding.SignupFragmentEtStaffId.text.toString().trim()

        Utils.showProgress(progressDialog)
        lifecycleScope.launch {
            try {
                // 1) Create the Firebase Auth user
                auth.createUserWithEmailAndPassword(email, password).await()
                auth.currentUser?.sendEmailVerification()?.await()

                // 2) Save staff + role in RTDB
                saveStaffToRTDB(ownerId, email)

                Utils.showToast(requireContext(), "Staff account created! Verification email sent.")
                findNavController().navigate(R.id.action_staffSignUpFragment_to_loginFragment)
            } catch (e: Exception) {
                Utils.showToast(requireContext(), "Sign up failed: ${e.localizedMessage}")
            } finally {
                Utils.hideProgress(progressDialog)
            }
        }
    }

    private suspend fun saveStaffToRTDB(ownerId: String, email: String) {
        // 1) Check that the ownerId actually exists under /Restaurants/<ownerId>
        val ownerRef = dbRef.child("Restaurants").child(ownerId)
        val ownerSnap = ownerRef.get().await()
        if (!ownerSnap.exists()) {
            Utils.showToast(requireContext(), "Owner ID does not exist.")
            return
        }

        val uid = auth.currentUser?.uid ?: return

        // 2) Prepare the staff data
        val staffData = mapOf(
            "name"       to binding.SignupFragmentEtStaffName.text.toString().trim(),
            "providedId" to ownerId,
            "email"      to email,
            "phoneNumber" to binding.SignupFragmentEtPhoneNumber.text.toString().trim(),
            "role"       to "Staff",
            "category"   to binding.SignupFragmentSpRole.selectedItem.toString()
        )

        // 3) Prepare the role node; LOWERCASE + replace . → _
        val normalizedEmailKey = email.lowercase().replace(".", "_")
        val roleData = mapOf(
            "role"       to binding.SignupFragmentSpRole.selectedItem.toString(),
            "providedId" to ownerId
        )

        // 4) Write to two places:
        //    a) /Restaurants/<ownerId>/Staff/<uid>  ← staffData
        //    b) /Roles/<lowercase_email_key>         ← roleData
        dbRef.child("Restaurants")
            .child(ownerId)
            .child("Staff")
            .child(uid)
            .setValue(staffData)
            .await()

        dbRef.child("Roles")
            .child(normalizedEmailKey)
            .setValue(roleData)
            .await()
    }

    private fun validateInputs(): Boolean {
        binding.apply {
            fun showError(field: View, msg: String) {
                (field as? androidx.appcompat.widget.AppCompatEditText)?.error = msg
            }

            return when {
                SignupFragmentEtStaffName.text.isBlank() -> {
                    showError(SignupFragmentEtStaffName, "Enter full name"); false
                }
                SignupFragmentEtStaffId.text.isBlank() -> {
                    showError(SignupFragmentEtStaffId, "Enter owner ID"); false
                }
                SignupFragmentEtEmail.text.isBlank() -> {
                    showError(SignupFragmentEtEmail, "Enter email"); false
                }
                SignupFragmentEtPassword.text.isBlank() -> {
                    showError(SignupFragmentEtPassword, "Enter password"); false
                }
                SignupFragmentEtConfirmPassword.text.isBlank() -> {
                    showError(SignupFragmentEtConfirmPassword, "Confirm password"); false
                }
                SignupFragmentEtPassword.text.toString() != SignupFragmentEtConfirmPassword.text.toString() -> {
                    showError(SignupFragmentEtConfirmPassword, "Passwords do not match"); false
                }
                SignupFragmentEtPhoneNumber.text.isBlank() -> {
                    showError(SignupFragmentEtPhoneNumber, "Enter phone number"); false
                }
                else -> true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
