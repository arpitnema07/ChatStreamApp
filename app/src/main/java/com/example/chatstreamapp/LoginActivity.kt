package com.example.chatstreamapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.chatstreamapp.databinding.ActivityLoginBinding
import com.google.gson.Gson
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.client.models.User
import io.getstream.chat.android.client.utils.onError
import io.getstream.chat.android.client.utils.onSuccess
import io.getstream.chat.android.core.internal.InternalStreamChatApi
import io.getstream.chat.android.offline.model.message.attachments.UploadAttachmentsNetworkType
import io.getstream.chat.android.offline.plugin.configuration.Config
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory

class LoginActivity : AppCompatActivity() {

    @OptIn(InternalStreamChatApi::class)
    override fun onResume() {
        super.onResume()
        val user = Gson().fromJson(sharedPreferences.getString("user", null), User::class.java)

        if (client.containsStoredCredentials() && user != null){
            binding.progressBar.visibility = View.VISIBLE
            binding.login.isEnabled = false
            client.connectUser(user,client.devToken(user.id),null).enqueue { result ->
                result.onSuccess {
                    Toast.makeText(this, it.user.id, Toast.LENGTH_SHORT).show()
                    sharedPreferences.edit().putString("user", Gson().toJson(it.user)).apply()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                result.onError {
                    Toast.makeText(this, result.error().message.toString(), Toast.LENGTH_SHORT)
                        .show()
                    binding.login.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                }
            }
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


        // Step 1 - Set up the OfflinePlugin for offline storage
        val offlinePluginFactory = StreamOfflinePluginFactory(
            config = Config(
                backgroundSyncEnabled = true,
                userPresence = true,
                persistenceEnabled = true,
                uploadAttachmentsNetworkType = UploadAttachmentsNetworkType.NOT_ROAMING,
            ),
            appContext = applicationContext,
        )

        // Step 2 - Set up the client for API calls with the plugin for offline storage
        client = ChatClient.Builder("4fb96hudgtjx", applicationContext)
            .withPlugin(offlinePluginFactory)
            .logLevel(ChatLogLevel.ALL) // Set to NOTHING in prod
            .build()

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