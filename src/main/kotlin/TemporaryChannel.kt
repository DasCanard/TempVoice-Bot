import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel

data class TemporaryChannel(
    val channelId: String,
    val hubId: String,
    val creatorId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val channel: VoiceChannel,  // Ändere dies zu einer Property
    val hubChannel: VoiceChannel
)
