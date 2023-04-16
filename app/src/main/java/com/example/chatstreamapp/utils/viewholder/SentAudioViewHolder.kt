package com.example.chatstreamapp.utils.viewholder

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import com.example.chatstreamapp.databinding.SentAudioListItemBinding
import com.example.chatstreamapp.utils.MessageOnClick
import com.example.chatstreamapp.utils.STAR
import com.example.chatstreamapp.utils.formatDate
import com.example.chatstreamapp.utils.formatFileSize
import com.getstream.sdk.chat.adapter.MessageListItem
import io.getstream.chat.android.client.models.Attachment
import io.getstream.chat.android.ui.message.list.adapter.BaseMessageItemViewHolder
import io.getstream.chat.android.ui.message.list.adapter.MessageListItemPayloadDiff


class SentAudioViewHolder(
    parentView: ViewGroup,
    private val messageOnClick: MessageOnClick,
    private val binding: SentAudioListItemBinding = SentAudioListItemBinding.inflate(
        LayoutInflater.from(
            parentView.context
        ),
        parentView,
        false
    )
) : BaseMessageItemViewHolder<MessageListItem.MessageItem>(binding.root) {

    override fun bindData(data: MessageListItem.MessageItem, diff: MessageListItemPayloadDiff?) {
        if (data.isMine) {
            val list = mutableListOf<String>()
            var isStar = false
            if (data.message.extraData[STAR]!=null){
                list.addAll(data.message.extraData[STAR] as List<String>)
                if (list.contains(data.message.user.id)){
                    isStar = true
                }
                list.forEach { Log.d("TAG", "bindData: $it") }
            }
            if (isStar){
                binding.star.visibility = View.VISIBLE
            } else{
                binding.star.visibility = View.GONE
            }

            val attachment: Attachment = data.message.attachments[0]

            binding.name.text = attachment.name
            binding.time.text = data.message.createdAt?.let { formatDate(it) }
            binding.size.text = formatFileSize(attachment.fileSize)

            if (data.isMessageRead) {
                binding.seenStatus.setImageDrawable(
                    AppCompatResources.getDrawable(
                        context,
                        io.getstream.chat.android.ui.R.drawable.stream_ui_ic_check_double
                    )
                )
            } else {
                binding.seenStatus.setImageDrawable(
                    AppCompatResources.getDrawable(
                        context,
                        io.getstream.chat.android.ui.R.drawable.stream_ui_ic_check_single
                    )
                )
            }
            binding.msgItem.setOnLongClickListener {
                messageOnClick.onSentItemLongPress(data.message,it)
                true
            }
            binding.msgItem.setOnClickListener {
                messageOnClick.onClickAudio(data.message,it)
            }
        }
    }
}