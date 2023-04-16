package com.example.chatstreamapp.utils.viewholder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.chatstreamapp.R
import com.example.chatstreamapp.databinding.ReceiveMessageListItemBinding
import com.example.chatstreamapp.utils.MessageOnClick
import com.example.chatstreamapp.utils.formatDate
import com.getstream.sdk.chat.adapter.MessageListItem
import io.getstream.chat.android.ui.message.list.adapter.BaseMessageItemViewHolder
import io.getstream.chat.android.ui.message.list.adapter.MessageListItemPayloadDiff


class ReceivedViewHolder(private val userId :String,
                         parentView: ViewGroup,
                         private val messageOnClick: MessageOnClick,
                         private val binding: ReceiveMessageListItemBinding = ReceiveMessageListItemBinding.inflate(
                             LayoutInflater.from(
                                 parentView.context),
                             parentView,
                             false)
) : BaseMessageItemViewHolder<MessageListItem.MessageItem>(binding.root) {

    override fun bindData(data: MessageListItem.MessageItem, diff: MessageListItemPayloadDiff?) {
        if (data.isTheirs){
            var isStar = false

            data.message.ownReactions.forEach{
                val id = it.user?.id
                if (id!=null && id == userId) {
                    isStar = true
                }
            }

            if (isStar){
                binding.star.visibility = View.VISIBLE
            } else{
                binding.star.visibility = View.GONE
            }
            binding.textLabel.text = data.message.text
            binding.time.text = data.message.createdAt?.let { formatDate(it) }
            Glide.with(binding.root.context).load(data.message.user.image).placeholder(R.mipmap.ic_launcher_round).into(binding.profile)
            binding.msgItem.setOnLongClickListener {
                messageOnClick.onReceiveItemLongPress(data.message,it)
                true
            }
        }

    }
}