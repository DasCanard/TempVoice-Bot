import java.io.File

data class BotConfig(
    val hubChannelIds: Set<String>,
    val deleteDelay: Long,
    val channelNameTemplate: String
)

object ConfigLoader {
    fun loadConfig(): BotConfig {
        return BotConfig(
            hubChannelIds = getHubChannelIds(),
            deleteDelay = getEnvOrDefault("DELETE_DELAY", "5").toLong(),
            channelNameTemplate = getEnvOrDefault("CHANNEL_NAME_TEMPLATE", "%hub% - %user%")
        )
    }

    fun loadEnvFile() {
        File(".env").takeIf { it.exists() }?.readLines()?.forEach { line ->
            if (line.isNotBlank() && !line.startsWith("#")) {
                val (key, value) = line.split("=", limit = 2)
                if (System.getenv(key.trim()) == null) {
                    System.setProperty(key.trim(), value.trim())
                }
            }
        }
    }

    private fun getEnvOrDefault(key: String, default: String): String {
        return System.getenv(key) ?: default
    }

    private fun getHubChannelIds(): Set<String> {
        return System.getenv("HUB_CHANNEL_IDS")
            ?.split(",")
            ?.map { it.trim() }
            ?.toSet()
            ?: emptySet()
    }
}
