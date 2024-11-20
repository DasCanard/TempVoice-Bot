import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

class ChannelManager(private val jda: JDA) {
    private val logger = Logger.getLogger(ChannelManager::class.java.name)
    private val tempChannels = ConcurrentHashMap<String, TemporaryChannel>()

    fun createTemporaryChannel(event: GuildVoiceUpdateEvent, config: BotConfig) {
        val guild = event.guild
        val hubChannel = event.channelJoined!!
        val member = event.member

        val channelName = config.channelNameTemplate
            .replace("%hub%", hubChannel.name)
            .replace("%user%", member.effectiveName)

        val newChannel = guild.createVoiceChannel(channelName)
            .setParent(hubChannel.parentCategory)
            .setBitrate(hubChannel.bitrate)
            .setUserlimit(hubChannel.userLimit)
            .setPosition(hubChannel.positionInCategory - 1)
            .complete()

        tempChannels[newChannel.id] = TemporaryChannel(
            channelId = newChannel.id,
            hubId = hubChannel.id,
            creatorId = member.id
        )

        guild.moveVoiceMember(member, newChannel).queue(
            { logger.info("Created temporary channel: $channelName") },
            { logger.warning("Failed to move member to new channel: ${it.message}") }
        )
    }

    fun deleteTemporaryChannel(guild: Guild, channel: VoiceChannel) {
        channel.delete().queue(
            {
                tempChannels.remove(channel.id)
                logger.info("Deleted temporary channel: ${channel.name}")
            },
            { logger.warning("Failed to delete channel: ${it.message}") }
        )
    }

    fun isTemporaryChannel(channelId: String): Boolean {
        return tempChannels.containsKey(channelId)
    }

    fun cleanupEmptyChannels() {
        tempChannels.forEach { (channelId, _) ->
            val guild = getGuildForChannel(channelId) ?: return@forEach
            val channel = guild.getVoiceChannelById(channelId) ?: return@forEach

            if (channel.members.isEmpty()) {
                deleteTemporaryChannel(guild, channel)
            }
        }
    }

    private fun getGuildForChannel(channelId: String): Guild? {
        return try {
            tempChannels[channelId]?.let { _ ->
                jda.getVoiceChannelById(channelId)?.guild
            }
        } catch (e: Exception) {
            logger.warning("Error getting guild for channel $channelId: ${e.message}")
            null
        }
    }
}
