package com.example.chatstreamapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.chatstreamapp.databinding.ActivityMainBinding
import com.example.chatstreamapp.utils.ClientChat
import com.google.gson.Gson
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.models.User
import io.getstream.chat.android.client.utils.onError
import io.getstream.chat.android.client.utils.onSuccess
import io.getstream.chat.android.ui.channel.list.viewmodel.ChannelListViewModel
import io.getstream.chat.android.ui.channel.list.viewmodel.bindView
import io.getstream.chat.android.ui.channel.list.viewmodel.factory.ChannelListViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences:SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Step 0 - inflate binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPreferences = getSharedPreferences("Auth", Context.MODE_PRIVATE)

        val user = Gson().fromJson(sharedPreferences.getString("user", null), User::class.java)

        val client = ClientChat.getClient(applicationContext)

        if (user != null){
            binding.progressBar.visibility = View.VISIBLE
            client.connectUser(user,client.devToken(user.id),null).enqueue { result ->
                result.onSuccess {
                    Toast.makeText(this, it.user.id, Toast.LENGTH_SHORT).show()
                    sharedPreferences.edit().putString("user", Gson().toJson(it.user)).apply()
                    binding.progressBar.visibility = View.GONE
                }
                result.onError {
                    Toast.makeText(this, result.error().message.toString(), Toast.LENGTH_SHORT)
                        .show()
                    binding.progressBar.visibility = View.GONE
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
        }else{
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        val viewModelFactory = ChannelListViewModelFactory(null, ChannelListViewModel.DEFAULT_SORT)
        val viewModel: ChannelListViewModel by viewModels { viewModelFactory }

        // Step 5 - Connect the ChannelListViewModel to the ChannelListView, loose
        //          coupling makes it easy to customize
        viewModel.bindView(binding.channelListView, this)
        binding.channelListView.setChannelItemClickListener { channel ->
            startActivity(ChannelActivity.newIntent(this, channel))
        }

    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.logout -> {
                // Handle menu item 1 click
                ChatClient.instance().disconnect(flushPersistence = false).enqueue { result ->
                    result.onSuccess {
                        sharedPreferences.edit().clear().apply()
                        startActivity(Intent(this,LoginActivity::class.java))
                        finish()
                    }
                    result.onError {
                        Toast.makeText(this,result.error().message,Toast.LENGTH_SHORT).show()
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
