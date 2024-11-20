data class TemporaryChannel(
    val channelId: String,
    val hubId: String,
    val creatorId: String,
    val createdAt: Long = System.currentTimeMillis()
)
