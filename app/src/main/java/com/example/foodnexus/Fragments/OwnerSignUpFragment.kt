package com.example.foodnexus.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.foodnexus.R
import com.example.foodnexus.Utils
import com.example.foodnexus.databinding.FragmentOwnerSignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class OwnerSignUpFragment : Fragment() {
    private var _binding: FragmentOwnerSignUpBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val progressDialog by lazy { createProgressDialog() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOwnerSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            SignupFragmentTvLoginLink.setOnClickListener {
                findNavController().navigate(R.id.action_ownerSignUpFragment_to_loginFragment)
            }

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

        Utils.showProgress(progressDialog)

        lifecycleScope.launchWhenStarted {
            try {
                // Create account
                auth.createUserWithEmailAndPassword(email, password).await()
                auth.currentUser?.sendEmailVerification()?.await()

                // Save to Firestore
                saveUserToFirestore()

                Utils.showToast(requireContext(), "Account created! Verification email sent.")
                findNavController().navigate(R.id.action_ownerSignUpFragment_to_loginFragment)

            } catch (e: Exception) {
                Utils.showToast(requireContext(), "Sign up failed: ${e.localizedMessage}")
            } finally {
                Utils.hideProgress(progressDialog)
            }
        }
    }

    private suspend fun saveUserToFirestore() {
        val uid = auth.currentUser?.uid ?: return
        val email = binding.SignupFragmentEtEmail.text.toString().trim()

        val userData = mapOf(
            "ownerName" to binding.SignupFragmentEtOwnerName.text.toString().trim(),
            "email" to email,
            "phoneNumber" to binding.SignupFragmentEtPhoneNumber.text.toString().trim(),
            "restaurantName" to binding.SignupFragmentEtRestaurantName.text.toString().trim(),
            "address" to binding.SignupFragmentEtAddress.text.toString().trim(),
            "role" to "Owner"
        )

        val roleData = mapOf("role" to "Owner")

        // Batch write for consistency
        val batch = db.batch()
        val restaurantRef = db.collection("Restaurants").document(uid)
        val rolesRef = db.collection("Roles").document(email)

        batch.set(restaurantRef, userData)
        batch.set(rolesRef, roleData)

        batch.commit().await()
    }

    private fun validateInputs(): Boolean {
        binding.apply {
            fun View.showError(msg: String) {
                when (this) {
                    is androidx.appcompat.widget.AppCompatEditText -> error = msg
                    else -> {}
                }
            }

            return when {
                SignupFragmentEtOwnerName.text.isBlank() -> { SignupFragmentEtOwnerName.showError("Enter your name"); false }
                SignupFragmentEtEmail.text.isBlank() -> { SignupFragmentEtEmail.showError("Enter your email"); false }
                SignupFragmentEtPassword.text.isBlank() -> { SignupFragmentEtPassword.showError("Enter a password"); false }
                SignupFragmentEtConfirmPassword.text.isBlank() -> { SignupFragmentEtConfirmPassword.showError("Confirm your password"); false }
                binding.SignupFragmentEtPassword.text.toString() != binding.SignupFragmentEtConfirmPassword.text.toString() -> {
                    SignupFragmentEtConfirmPassword.showError("Passwords do not match"); false }
                SignupFragmentEtPhoneNumber.text.isBlank() -> { SignupFragmentEtPhoneNumber.showError("Enter phone number"); false }
                SignupFragmentEtRestaurantName.text.isBlank() -> { SignupFragmentEtRestaurantName.showError("Enter restaurant name"); false }
                SignupFragmentEtAddress.text.isBlank() -> { SignupFragmentEtAddress.showError("Enter address"); false }
                else -> true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
