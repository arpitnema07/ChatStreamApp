package com.example.chatstreamapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.widget.doOnTextChanged
import com.example.chatstreamapp.databinding.ActivityChannelBinding
import com.getstream.sdk.chat.viewmodel.MessageInputViewModel
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel.Mode.Normal
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel.Mode.Thread
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel.State.NavigateUp
import io.getstream.chat.android.client.models.Channel
import io.getstream.chat.android.ui.message.input.viewmodel.bindView
import io.getstream.chat.android.ui.message.list.header.viewmodel.MessageListHeaderViewModel
import io.getstream.chat.android.ui.message.list.header.viewmodel.bindView
import io.getstream.chat.android.ui.message.list.viewmodel.bindView
import io.getstream.chat.android.ui.message.list.viewmodel.factory.MessageListViewModelFactory


class ChannelActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChannelBinding

    private var audio = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Step 0 - inflate binding
        binding = ActivityChannelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val cid = checkNotNull(intent.getStringExtra(CID_KEY)) {
            "Specifying a channel id is required when starting ChannelActivity"
        }

        // Step 1 - Create three separate ViewModels for the views so it's easy
        //          to customize them individually
        val factory = MessageListViewModelFactory(cid)
        val messageListHeaderViewModel: MessageListHeaderViewModel by viewModels { factory }
        val messageListViewModel: MessageListViewModel by viewModels { factory }
        val messageInputViewModel: MessageInputViewModel by viewModels { factory }


        // Step 2 - Bind the view and ViewModels, they are loosely coupled so it's easy to customize
        messageListHeaderViewModel.bindView(binding.messageListHeaderView, this)
        messageListViewModel.bindView(binding.messageListView, this)
        messageInputViewModel.bindView(binding.messageInputView, this)

        // Step 3 - Let both MessageListHeaderView and MessageInputView know when we open a thread
        messageListViewModel.mode.observe(this) { mode ->
            when (mode) {
                is Thread -> {
                    messageListHeaderViewModel.setActiveThread(mode.parentMessage)
                    messageInputViewModel.setActiveThread(mode.parentMessage)
                }
                Normal -> {
                    messageListHeaderViewModel.resetThread()
                    messageInputViewModel.resetThread()
                }
            }
        }

        // Step 4 - Let the message input know when we are editing a message
        binding.messageListView.setMessageEditHandler(messageInputViewModel::postMessageToEdit)

        // Step 5 - Handle navigate up state
        messageListViewModel.state.observe(this) { state ->
            if (state is NavigateUp) {
                finish()
            }
        }

        // Step 6 - Handle back button behaviour correctly when you're in a thread
        val backHandler = {
            messageListViewModel.onEvent(MessageListViewModel.Event.BackButtonPressed)
        }
        binding.messageListHeaderView.setBackButtonClickListener(backHandler)
        onBackPressedDispatcher.addCallback(this) {
            backHandler()
        }
        binding.messageInputView.setSendMessageButtonEnabledDrawable(AppCompatResources.getDrawable(this,R.drawable.ic_mic_24)!!)
        binding.messageInputView.setSendMessageButtonDisabledDrawable(AppCompatResources.getDrawable(this,R.drawable.ic_mic_24)!!)

        val editText: AppCompatEditText = findViewById(io.getstream.chat.android.ui.R.id.messageEditText)
        editText.doOnTextChanged { text, _, _, count ->
            if (count<1 || text.toString() == ""){
                audio = true
                binding.messageInputView.setSendMessageButtonEnabledDrawable(AppCompatResources.getDrawable(this,R.drawable.ic_mic_24)!!)
                binding.messageInputView.setSendMessageButtonDisabledDrawable(AppCompatResources.getDrawable(this,R.drawable.ic_mic_24)!!)
            } else{
                audio = false
                binding.messageInputView.setSendMessageButtonEnabledDrawable(AppCompatResources.getDrawable(this,R.drawable.ic_send_24)!!)
                binding.messageInputView.setSendMessageButtonDisabledDrawable(AppCompatResources.getDrawable(this,R.drawable.ic_send_24)!!)
            }
        }

        binding.messageInputView.setOnSendButtonClickListener{
            Toast.makeText(this,"here",Toast.LENGTH_SHORT).show()
            Log.d("TAG", "onCreate: Here")
        }
    }


    companion object {
        private const val CID_KEY = "key:cid"

        fun newIntent(context: Context, channel: Channel): Intent =
            Intent(context, ChannelActivity::class.java).putExtra(CID_KEY, channel.cid)
    }
}