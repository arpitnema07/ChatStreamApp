package com.example.chatstreamapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.chatstreamapp.databinding.ActivityLoginBinding
import com.example.chatstreamapp.utils.ClientChat
import com.google.gson.Gson
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.models.User
import io.getstream.chat.android.client.utils.onError
import io.getstream.chat.android.client.utils.onSuccess

class LoginActivity : AppCompatActivity() {

    override fun onResume() {
        super.onResume()
        val user = Gson().fromJson(sharedPreferences.getString("user", null), User::class.java)
        if (user != null){
            binding.progressBar.visibility = View.VISIBLE
            binding.login.isEnabled = false
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
    private lateinit var binding: ActivityLoginBinding
    private lateinit var sharedPreferences:SharedPreferences
    private lateinit var client: ChatClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPreferences = getSharedPreferences("Auth", Context.MODE_PRIVATE)


        val client = ClientChat.getClient(applicationContext)

        binding.login.setOnClickListener {
            val username = binding.usernameEditText.text.toString()
            if (username.length>4){
                // Step 3 - Authenticate and connect the user
                binding.login.isEnabled = false
                val user = User(
                    id = username,
                    name = username,
                    image = "https://bit.ly/321RmWb",
                )
                val token = client.devToken(user.id)
                binding.login.isEnabled = false
                binding.progressBar.visibility = View.VISIBLE
                client.connectUser(user,token,null)
                    .enqueue { result ->
                        result.onSuccess {
                            Toast.makeText(this, it.user.id,Toast.LENGTH_SHORT).show()
                            sharedPreferences.edit().putString("user", Gson().toJson(it.user)).apply()
                            startActivity(Intent(this,MainActivity::class.java))
                            finish()
                        }
                        result.onError {
                            Toast.makeText(this,result.error().message.toString(),Toast.LENGTH_SHORT).show()
                            binding.login.isEnabled = true
                            binding.progressBar.visibility = View.GONE
                        }
                }
            }
        }
    }
}