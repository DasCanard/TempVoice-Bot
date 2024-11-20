import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.events.StatusChangeEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

class Main(private val token: String) : ListenerAdapter() {
    private val logger = Logger.getLogger(Main::class.java.name)
    private val scheduler = Executors.newScheduledThreadPool(1)
    private lateinit var jda: JDA
    private lateinit var channelManager: ChannelManager
    private val config = ConfigLoader.loadConfig()

    fun start() {
        jda = JDABuilder.createDefault(token)
            .enableIntents(GatewayIntent.GUILD_VOICE_STATES)
            .addEventListeners(this)
            .build()

        jda.awaitReady()
        channelManager = ChannelManager(jda)
        logger.info("Bot started successfully!")

        startCleanupTask()
    }

    override fun onReady(event: ReadyEvent) {
        channelManager.validateChannels()
    }

    override fun onStatusChange(event: StatusChangeEvent) {
        if (event.newStatus == JDA.Status.CONNECTED) {
            channelManager.validateChannels()
        }
    }

    private fun startCleanupTask() {
        scheduler.scheduleAtFixedRate({
            try {
                channelManager.cleanupEmptyChannels()
            } catch (e: Exception) {
                logger.warning("Error during cleanup: ${e.message}")
            }
        }, 5, 5, TimeUnit.MINUTES)
    }

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        try {
            val channelJoined = event.channelJoined
            val channelLeft = event.channelLeft

            if (channelJoined != null && config.hubChannelIds.contains(channelJoined.id)) {
                channelManager.createTemporaryChannel(event, config)
            }

            if (channelLeft != null && channelManager.isTemporaryChannel(channelLeft.id)) {
                (channelLeft as? VoiceChannel)?.let { voiceChannel ->
                    handleTempChannelLeave(event.guild, voiceChannel)
                }
            }
        } catch (e: Exception) {
            logger.warning("Error handling voice update: ${e.message}")
        }
    }

    private fun handleTempChannelLeave(guild: Guild, channel: VoiceChannel) {
        if (channel.members.isEmpty()) {
            scheduler.schedule({
                if (channel.members.isEmpty()) {
                    channelManager.deleteTemporaryChannel(guild, channel)
                }
            }, config.deleteDelay, TimeUnit.SECONDS)
        }
    }

    fun shutdown() {
        scheduler.shutdown()
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow()
            }
        } catch (e: InterruptedException) {
            scheduler.shutdownNow()
        }
    }
}

fun main() {
    ConfigLoader.loadEnvFile()

    val token = System.getenv("DISCORD_TOKEN") ?: run {
        println("Error: DISCORD_TOKEN environment variable is not set")
        return
    }

    val bot = Main(token)
    bot.start()

    Runtime.getRuntime().addShutdownHook(Thread {
        bot.shutdown()
    })
}
