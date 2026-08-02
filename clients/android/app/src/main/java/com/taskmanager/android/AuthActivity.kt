package com.taskmanager.android

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.taskmanager.android.databinding.ActivityAuthBinding
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private lateinit var session: SessionStore
    private lateinit var api: ApiClient
    private var registerMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionStore(this)
        if (session.token != null) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        api = ApiClient(baseUrl = session.baseUrl)

        binding.authToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            registerMode = checkedId == binding.tabRegister.id
            binding.nameInput.visibility = if (registerMode) View.VISIBLE else View.GONE
            binding.submitBtn.text = if (registerMode) getString(R.string.register) else getString(R.string.login)
            binding.authError.text = ""
        }

        binding.submitBtn.setOnClickListener { submit() }

        binding.brandLabel.setOnLongClickListener {
            askText(
                title = getString(R.string.api_url_title),
                hint = getString(R.string.api_url_hint),
                message = session.baseUrl
            ) { url ->
                session.baseUrl = url
                api.baseUrl = url
                toast("API: $url")
            }
            true
        }
    }

    private fun submit() {
        api.baseUrl = session.baseUrl

        val email = binding.emailInput.text?.toString()?.trim().orEmpty()
        val password = binding.passwordInput.text?.toString().orEmpty()
        val name = binding.nameInput.text?.toString()?.trim().orEmpty()

        lifecycleScope.launch {
            try {
                binding.authError.text = ""
                val auth = if (registerMode) {
                    api.register(name, email, password)
                } else {
                    api.login(email, password)
                }
                session.token = auth.token
                startActivity(Intent(this@AuthActivity, HomeActivity::class.java))
                finish()
            } catch (e: Exception) {
                binding.authError.text = e.message
            }
        }
    }
}
