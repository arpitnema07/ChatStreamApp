package com.example.chatstreamapp.utils

import android.view.ViewGroup
import com.example.chatstreamapp.utils.viewholder.ReceivedAudioViewHolder
import com.example.chatstreamapp.utils.viewholder.ReceivedViewHolder
import com.example.chatstreamapp.utils.viewholder.SentAudioViewHolder
import com.example.chatstreamapp.utils.viewholder.SentViewHolder
import com.getstream.sdk.chat.adapter.MessageListItem
import io.getstream.chat.android.ui.common.extensions.isDeleted
import io.getstream.chat.android.ui.message.list.adapter.BaseMessageItemViewHolder
import io.getstream.chat.android.ui.message.list.adapter.MessageListItemViewHolderFactory


class CustomMessageViewHolderFactory(private val userId:String,private val onClick: MessageOnClick) : MessageListItemViewHolderFactory() {
    override fun getItemViewType(item: MessageListItem): Int {
        var isAudio = false
        if (item is MessageListItem.MessageItem){
            item.message.attachments.forEach {
                if (it.name?.startsWith("audio") == true){
                    isAudio = true
                }
            }
        }
        return if (item is MessageListItem.MessageItem &&
            item.isTheirs &&
            item.message.attachments.isEmpty() && !item.message.isDeleted()
        ) {
            RECEIVED_MSG
        } else if (item is MessageListItem.MessageItem &&
            item.isMine &&
            item.message.attachments.isEmpty() && !item.message.isDeleted()
        ) {
            SENT_MSG
        }else if (item is MessageListItem.MessageItem &&
            item.isTheirs &&
            isAudio && !item.message.isDeleted()
        ) {
            RECEIVED_AUDIO
        } else if (item is MessageListItem.MessageItem &&
            item.isMine &&
            isAudio && !item.message.isDeleted()
        ) {
            SENT_AUDIO
        } else {
            super.getItemViewType(item)
        }
    }

    override fun createViewHolder(
        parentView: ViewGroup,
        viewType: Int,
    ): BaseMessageItemViewHolder<out MessageListItem> {
        return when (viewType) {
            RECEIVED_MSG -> {
                ReceivedViewHolder(userId,parentView,onClick)
            }
            SENT_MSG -> {
                SentViewHolder(parentView,onClick)
            }
            RECEIVED_AUDIO -> {
                ReceivedAudioViewHolder(userId,parentView,onClick)
            }
            SENT_AUDIO -> {
                SentAudioViewHolder(parentView,onClick)
            }
            else -> {
                super.createViewHolder(parentView, viewType)
            }
        }
    }

    companion object {
        private const val RECEIVED_MSG = 111
        private const val SENT_MSG = 101

        private const val RECEIVED_AUDIO = 110
        private const val SENT_AUDIO = 100
    }
}