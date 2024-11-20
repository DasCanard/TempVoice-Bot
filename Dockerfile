FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY build/libs/discord-bot.jar /app/discord-bot.jar

ENV DISCORD_TOKEN=""
ENV HUB_CHANNEL_IDS=""
ENV DELETE_DELAY="5"
ENV CHANNEL_NAME_TEMPLATE="%hub% - %user%"

CMD ["java", "-jar", "discord-bot.jar"]
