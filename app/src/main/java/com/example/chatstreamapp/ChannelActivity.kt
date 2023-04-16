package com.example.chatstreamapp

import android.Manifest.permission.RECORD_AUDIO
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.example.chatstreamapp.databinding.ActivityChannelBinding
import com.example.chatstreamapp.utils.*
import com.getstream.sdk.chat.viewmodel.MessageInputViewModel
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel.Mode.Normal
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel.Mode.Thread
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel.State.NavigateUp
import com.google.gson.Gson
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.models.Channel
import io.getstream.chat.android.client.models.Message
import io.getstream.chat.android.client.models.Reaction
import io.getstream.chat.android.client.models.User
import io.getstream.chat.android.client.utils.onError
import io.getstream.chat.android.client.utils.onSuccess
import io.getstream.chat.android.ui.message.list.header.viewmodel.MessageListHeaderViewModel
import io.getstream.chat.android.ui.message.list.header.viewmodel.bindView
import io.getstream.chat.android.ui.message.list.viewmodel.bindView
import io.getstream.chat.android.ui.message.list.viewmodel.factory.MessageListViewModelFactory
import java.io.File


class ChannelActivity : AppCompatActivity(), AudioDialogFragment.AudioDialogListener,
    MessageOnClick {

    private lateinit var binding: ActivityChannelBinding

    private var audio = true
    private var canRecord  = false
    private lateinit var messageInputViewModel: MessageInputViewModel

    private val REQUEST_RECORD_AUDIO_PERMISSION = 200

    private lateinit var user:User
    private lateinit var client: ChatClient

    private val getAudio =
        registerForActivityResult(ActivityResultContracts.GetContent()) { u: Uri? ->
            showAudioDialog(u)
        }

    private fun requestMicrophonePermission() {
        val permission = RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Check if user has previously denied the permission
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                // Show an explanation to the user
                AlertDialog.Builder(this)
                    .setTitle("Microphone Permission Needed")
                    .setMessage("This app needs the microphone permission to record audio.")
                    .setPositiveButton("Ok") { _, _ ->
                        // Request the permission again
                        ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_RECORD_AUDIO_PERMISSION)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                // Request the permission
                ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_RECORD_AUDIO_PERMISSION)
            }
        } else {
            canRecord = true
            showAudioDialog()
        }
    }

    // Handle the permission request result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            canRecord = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (canRecord) showAudioDialog()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Step 0 - inflate binding
        binding = ActivityChannelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPreferences = getSharedPreferences("Auth", Context.MODE_PRIVATE)

        user = Gson().fromJson(sharedPreferences.getString("user", null), User::class.java)

        client = ClientChat.getClient(applicationContext)

        val cid = checkNotNull(intent.getStringExtra(CID_KEY)) {
            "Specifying a channel id is required when starting ChannelActivity"
        }

        // Step 1 - Create three separate ViewModels for the views so it's easy
        //          to customize them individually
        val factory = MessageListViewModelFactory(cid)
        val messageListHeaderViewModel: MessageListHeaderViewModel by viewModels { factory }
        val messageListViewModel: MessageListViewModel by viewModels { factory }
        val messageInputViewModel: MessageInputViewModel by viewModels { factory }
        this.messageInputViewModel = messageInputViewModel

        // Step 2 - Bind the view and ViewModels, they are loosely coupled so it's easy to customize
        messageListHeaderViewModel.bindView(binding.messageListHeaderView, this)
        messageListViewModel.bindView(binding.messageListView, this)
//        messageInputViewModel.bindView(binding.messageInputView, this)

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

        binding.messageListView.setMessageViewHolderFactory(CustomMessageViewHolderFactory(user.id,this))

        // Step 6 - Handle back button behaviour correctly when you're in a thread
        val backHandler = {
            messageListViewModel.onEvent(MessageListViewModel.Event.BackButtonPressed)
        }
        binding.messageListHeaderView.setBackButtonClickListener(backHandler)

        onBackPressedDispatcher.addCallback(this) {
            backHandler()
        }

        binding.msgEditView.doOnTextChanged { text, _, _, _ ->
            if (text!=null && text.isNotEmpty()){
                audio = false
                binding.sendMsg.setImageResource(R.drawable.ic_send_24)
            } else{
                audio = true
                binding.sendMsg.setImageResource(R.drawable.ic_mic_24)
            }
        }

        binding.sendMsg.setOnClickListener {
            if (audio) {
                if (canRecord){
                    showAudioDialog()
                }  else {
                    requestMicrophonePermission()
                }
            }else{
                messageInputViewModel.sendMessage(binding.msgEditView.text.toString())
                binding.msgEditView.setText("")
            }
        }

        binding.addAudio.setOnClickListener {
            getAudio.launch("audio/*")
        }
    }

    override fun onClickAudio(message: Message, view: View) {
        Toast.makeText(this,"Loading1...",Toast.LENGTH_SHORT).show()

        if (message.attachments[0].name?.startsWith("audio") == true){
            Toast.makeText(this,message.attachments[0].assetUrl,Toast.LENGTH_SHORT).show()

            showAudioDialog(Uri.parse(message.attachments[0].assetUrl),true)
        }
    }


    private fun showAudioDialog(u: Uri? = null, b: Boolean = false) {
        Toast.makeText(this,"Loading...",Toast.LENGTH_SHORT).show()
        val fragment:AudioDialogFragment = if (u == null){
            AudioDialogFragment.newInstance()
        }else{
            AudioDialogFragment.newInstance(u.toString(),b)
        }
        fragment.show(supportFragmentManager, "AudioDialogFragment")
    }

    companion object {
        private const val CID_KEY = "key:cid"

        fun newIntent(context: Context, channel: Channel): Intent =
            Intent(context, ChannelActivity::class.java).putExtra(CID_KEY, channel.cid)
    }

    override fun onAudioRecorded(file: File?) {
        if (file!=null){
            messageInputViewModel.sendMessageWithAttachments(binding.msgEditView.text.toString(),
                listOf(Pair(file, file.name))
            )
        }
    }

    override fun onSentItemLongPress(message: Message, view: View) {
        // Handle long click on message
        // Create a new PopupMenu instance
        val list = mutableListOf<String>()
        var isStar = false
        if (message.extraData[STAR]!=null){
            list.addAll(message.extraData[STAR] as List<String>)
            if (list.contains(user.id)){
                isStar = true
            }
        }
        val popupMenu = PopupMenu(this,view)

        // Inflate the menu resource file
        popupMenu.inflate(R.menu.long_press_menu)

        // Get the menu item by its ID
        val item = popupMenu.menu.findItem(R.id.star)

        if (isStar){
            item.title = "Un-Star"
        }
        // Set a click listener for the menu items
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.delete -> {
                    // Handle the first menu item
                    client.deleteMessage(message.id).enqueue { result ->
                        result.onSuccess {
                            Toast.makeText(this,"Deleted Message",Toast.LENGTH_SHORT).show()
                        }
                        result.onError {
                            Toast.makeText(this,"Error",Toast.LENGTH_SHORT).show()
                        }
                    }
                    true
                }
                R.id.star -> {
                    if (isStar){
                        list.remove(user.id)
                        message.extraData[STAR] = list
                    } else{
                        list.add(user.id)
                        message.extraData[STAR] = list
                    }
                    messageInputViewModel.editMessage(message)
                    // Handle the second menu item
                    true
                }
                else -> false
            }
        }

        // Show the popup menu
        popupMenu.show()
    }

    override fun onReceiveItemLongPress(message: Message, view: View) {
        // Handle long click on message
        // Create a new PopupMenu instance

        var isStar = false
        var reaction = Reaction(
            messageId = message.id,
            type = "like",
            score = 1
        )
        message.ownReactions.forEach{
            val id = it.user?.id
            if (id!=null && id == user.id) {
                isStar = true
                reaction = it
            }
        }

        val popupMenu = PopupMenu(this,view)

        // Inflate the menu resource file
        popupMenu.inflate(R.menu.long_press_menu)

        // Get the menu item by its ID
        val item = popupMenu.menu.findItem(R.id.star)
        popupMenu.menu.findItem(R.id.delete).isVisible = false

        if (isStar){
            item.title = "Un-Star"
        }
        // Set a click listener for the menu items
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.star -> {
                    if (isStar){
                        client.deleteReaction(
                            messageId = message.id,
                            reactionType = "like",
                        ).enqueue { result ->
                            result.onSuccess {
//                                Toast.makeText(this,"Added to Starred",Toast.LENGTH_SHORT).show()
                            }
                            result.onError {
                                Toast.makeText(this,result.error().message.toString(),Toast.LENGTH_SHORT).show()
                            }
                        }

                    } else{
                        client.sendReaction(reaction, enforceUnique = true).enqueue { result ->
                            result.onSuccess {
//                                Toast.makeText(this,"Removed From Starred",Toast.LENGTH_SHORT).show()
                            }
                            result.onError {
                                Toast.makeText(this,result.error().message.toString(),Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    // Handle the second menu item
                    true
                }
                else -> false
            }
        }

        // Show the popup menu
        popupMenu.show()
    }
}