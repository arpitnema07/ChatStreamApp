package com.example.chatstreamapp.utils

import android.content.Context
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.offline.model.message.attachments.UploadAttachmentsNetworkType
import io.getstream.chat.android.offline.plugin.configuration.Config
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory

object ClientChat {

    private lateinit var client: ChatClient

    fun getClient(applicationContext: Context): ChatClient {
        if (!::client.isInitialized) {
            val offlinePluginFactory = StreamOfflinePluginFactory(
                config = Config(
                    backgroundSyncEnabled = true,
                    userPresence = true,
                    persistenceEnabled = true,
                    uploadAttachmentsNetworkType = UploadAttachmentsNetworkType.NOT_ROAMING,
                ),
                appContext = applicationContext,
            )

            client = ChatClient.Builder("4fb96hudgtjx", applicationContext)
                .withPlugin(offlinePluginFactory)
                .logLevel(ChatLogLevel.ALL) // Set to NOTHING in prod
                .build()
        }

        return client
    }
}
