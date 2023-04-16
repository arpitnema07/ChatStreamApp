package com.example.chatstreamapp.utils.viewholder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.chatstreamapp.R
import com.example.chatstreamapp.databinding.ReceiveAudioListItemBinding
import com.example.chatstreamapp.utils.MessageOnClick
import com.example.chatstreamapp.utils.formatDate
import com.example.chatstreamapp.utils.formatFileSize
import com.getstream.sdk.chat.adapter.MessageListItem
import io.getstream.chat.android.client.models.Attachment
import io.getstream.chat.android.ui.message.list.adapter.BaseMessageItemViewHolder
import io.getstream.chat.android.ui.message.list.adapter.MessageListItemPayloadDiff


class ReceivedAudioViewHolder(private val userId :String,
                              parentView: ViewGroup,
                              private val messageOnClick: MessageOnClick,
                              private val binding: ReceiveAudioListItemBinding = ReceiveAudioListItemBinding.inflate(
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
            val attachment:Attachment = data.message.attachments[0]

            binding.name.text = attachment.name.toString()
            binding.time.text = data.message.createdAt?.let { formatDate(it) }
            binding.size.text = formatFileSize(attachment.fileSize)
            Glide.with(binding.root.context).load(data.message.user.image).placeholder(R.mipmap.ic_launcher_round).into(binding.profile)
            binding.msgItem.setOnClickListener {
                messageOnClick.onClickAudio(data.message,it)
            }
            binding.msgItem.setOnLongClickListener {
                messageOnClick.onReceiveItemLongPress(data.message,it)
                true
            }
        }

    }
}