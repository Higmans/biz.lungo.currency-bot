# Add Telegram Business Mode Support

## Overview
Enable the bot to receive and respond to commands in business-connected chats (private and group). When a Telegram Business account adds the bot, messages arrive as `business_message` updates with a `business_connection_id`. All Telegram API calls that send/edit messages must pass this ID back, otherwise the API rejects them. Currently `MessageResponse` only has a `message` field, so business_message updates are ignored.

## Context
- Files involved:
  - `src/main/kotlin/biz/lungo/currencybot/data/MessageResponse.kt` - extend with business fields
  - `src/main/kotlin/biz/lungo/currencybot/data/MessageRequest.kt` - add businessConnectionId
  - `src/main/kotlin/biz/lungo/currencybot/data/ChatAction.kt` - add businessConnectionId
  - `src/main/kotlin/biz/lungo/currencybot/data/EditMessageRequest.kt` - add businessConnectionId
  - `src/main/kotlin/biz/lungo/currencybot/data/PhotoRequest.kt` - add businessConnectionId
  - `src/main/kotlin/biz/lungo/currencybot/BotTypingJob.kt` - pass businessConnectionId to ChatAction
  - `src/main/kotlin/biz/lungo/currencybot/plugins/Bot.kt` - core logic update
  - New: `src/main/kotlin/biz/lungo/currencybot/data/BusinessConnection.kt`
  - New: `src/main/kotlin/biz/lungo/currencybot/data/BusinessMessagesDeleted.kt`
- Related patterns: Gson @SerializedName for all Telegram API field mappings; all data classes use nullable defaults for optional fields
- Dependencies: none new; Telegram Bot API already supports this when `can_reply` is true on the connection

## Development Approach
- **Testing approach**: Regular (no test infrastructure exists in the project)
- Complete each task fully before moving to the next
- No tests required (no test infrastructure)

## Implementation Steps

### Task 1: Create new business mode data classes and extend MessageResponse

**Files:**
- Create: `src/main/kotlin/biz/lungo/currencybot/data/BusinessConnection.kt`
- Create: `src/main/kotlin/biz/lungo/currencybot/data/BusinessMessagesDeleted.kt`
- Modify: `src/main/kotlin/biz/lungo/currencybot/data/MessageResponse.kt`

- [x] Create `BusinessConnection.kt` with fields: `id: String`, `user: User`, `userChatId: Long`, `date: Long`, `canReply: Boolean`, `isEnabled: Boolean`
- [x] Create `BusinessMessagesDeleted.kt` with fields: `businessConnectionId: String`, `chat: Chat`, `date: Long`, `messageIds: List<Long>`
- [x] In `MessageResponse`, add optional fields: `businessConnection: BusinessConnection? = null`, `businessMessage: Message? = null`, `editedBusinessMessage: Message? = null`, `deletedBusinessMessages: BusinessMessagesDeleted? = null`
- [x] In `Message`, add optional field: `businessConnectionId: String? = null` mapped to `"business_connection_id"`
- [x] Build the project to confirm compilation

### Task 2: Extend request/action data classes with businessConnectionId

**Files:**
- Modify: `src/main/kotlin/biz/lungo/currencybot/data/MessageRequest.kt`
- Modify: `src/main/kotlin/biz/lungo/currencybot/data/ChatAction.kt`
- Modify: `src/main/kotlin/biz/lungo/currencybot/data/EditMessageRequest.kt`
- Modify: `src/main/kotlin/biz/lungo/currencybot/data/PhotoRequest.kt`

- [ ] In `MessageRequest`, add `businessConnectionId: String? = null` mapped to `"business_connection_id"`
- [ ] In `ChatAction`, add `businessConnectionId: String? = null` mapped to `"business_connection_id"`
- [ ] In `EditMessageRequest`, add `businessConnectionId: String? = null` mapped to `"business_connection_id"`
- [ ] In `PhotoRequest`, add `businessConnectionId: String? = null` mapped to `"business_connection_id"`
- [ ] Build the project to confirm compilation

### Task 3: Update BotTypingJob to forward businessConnectionId

**Files:**
- Modify: `src/main/kotlin/biz/lungo/currencybot/BotTypingJob.kt`

- [ ] Add `businessConnectionId: String? = null` as second constructor parameter
- [ ] Pass `businessConnectionId` when constructing `ChatAction` in `sendChatTypingAction()`
- [ ] Build the project to confirm compilation

### Task 4: Update Bot.kt command handler for business mode

**Files:**
- Modify: `src/main/kotlin/biz/lungo/currencybot/plugins/Bot.kt`

- [ ] After `call.receive<MessageResponse>()`, add early return if `update.businessConnection != null` (log the connection event and return)
- [ ] Resolve message as `update.message ?: update.businessMessage` (keep existing `val message = ...` pattern)
- [ ] Extract `val businessConnectionId = message?.businessConnectionId`
- [ ] For `Command.Start`: when `businessConnectionId != null`, send a simple welcome message (no pin flow, since pinning is not supported in business chats)
- [ ] Pass `businessConnectionId` to all `sendTelegramMessage(chatId, ..., businessConnectionId = businessConnectionId)` calls throughout all commands
- [ ] Pass `businessConnectionId` to `sendTelegramPhoto` call (Meme command)
- [ ] Pass `businessConnectionId` when constructing `BotTypingJob(chatId, businessConnectionId)` for all typing jobs
- [ ] Add `businessConnectionId: String? = null` parameter to `sendTelegramMessage` function and include it in `MessageRequest`
- [ ] Add `businessConnectionId: String? = null` parameter to `sendTelegramPhoto` function and include it in `PhotoRequest`
- [ ] Build the project to confirm compilation

### Task 5: Verify acceptance criteria

- [ ] Manual test: send `/start` in a regular group — bot should send and pin the exchange rate message
- [ ] Manual test: send `/start` in a business-connected private chat — bot should send simple welcome, no pin
- [ ] Manual test: send `/nburate USD` in a business-connected chat — bot should reply with the rate
- [ ] Manual test: send `/crypto` in a business-connected chat — bot should reply with crypto rates
- [ ] Manual test: send `/joke` in a business-connected chat — bot should reply with a joke
- [ ] Manual test: regular (non-business) chats still work as before
- [ ] Build the project: `./gradlew build`

### Task 6: Update documentation

- [ ] Move this plan to `docs/plans/completed/`
