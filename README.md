# Currency Bot for Telegram

Requirements: JDK 21 or higher

Usage:
1. Fill missing properties in `sample.env` and rename it to `.env`
2. Build JAR file with `./gradlew shadowJar copyRelease` command or take it from [releases]
3. Use provided `docker-compose.yml` file to build docker image and start the bot

Features:
- Works in regular chats: `/start` sends and pins an exchange rate message
- Works in Telegram Business-connected chats: all commands supported; `/start` sends a greeting (pinning is not available in business chats)