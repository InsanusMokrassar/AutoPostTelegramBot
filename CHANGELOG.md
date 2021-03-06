# Auto Post Telegram Bot Changelog

## 1.8.0

* Version updates:
    * Ktor version `1.2.6` -> `1.3.0`
    * Telegram Bot API `0.21.0` -> `0.22.1`
* `TimerTriggerStrategy` now have new extension `getPostsInRange` and `getTriggersInRange`
* `MostRatedChooser` now correctly work with exceptions list

### 1.8.2

* `DatabaseConfig#connect` now is deprecated. It is recommended to use `DatabaseConfig#database` property
* Fixed issue with `PostsSchedulesTable`, when table was not created automatically
* Refactor work with posts and posts messages
    * `PostsTable` and `PostsMessagesTable` now are deprecated global values. It can be replaced inside of plugins by
    calling `baseConfig.postsTable` or `baseConfig.postsMessagesTable` inside of `onInit` block of your plugin
    * `PostTransaction` now have incoming arguments `postsTable` and `postsMessagesTable`, which are by default set up
    to deprecated global variables
    * `RateChooser` now have two protected fields: `postsTable` and `postsMessagesTable`
    * Several classes now have `postsTable` or/and `postsMessagesTable` as arguments. Remember that it is optional for
    now, but will be required soon:
        * `PublishPost`
        * `DisableTimerCommand`
        * `EnableTimerCommand`
        * `GetSchedulesCommand`
        * `TimerScheduleCommand`
* `PostsSchedulesTable` now require database connection as argument
* `PostsTableScope` now is deprecated due to incorrect visibility level (soon it will be `private`)
* `SafeLazy` util class was added. You can use `CoroutineScope#createSafeLazy` for easily creating of it

### 1.8.1

* `TimerTriggerStrategy` now have additional optional parameter `substitutedByScheduler` (boolean, default `false`).
In case of setting up this parameter to `true`, plugin will skip times which was set in schedule plugin
* Version updates:
    * Ktor version `1.3.0` -> `1.3.1`
    * Telegram Bot API `0.22.1` -> `0.23.2`

## 1.7.0

* Version updates:
    * Kotlin coroutines `1.3.2` -> `1.3.3`
    * Telegram Bot API `0.20.3` -> `0.21.0`
* Now it is not recommended to use `/` in commands notations when use `buildCommandFlow` or `Command` extenders

## 1.6.0

* Version updates:
    * Kotlin version `1.3.41` -> `1.3.61`
    * Kotlin coroutines `1.2.2` -> `1.3.2`
    * Kotlin seriaization `0.11.1` -> `0.14.0`
    * Telegram Bot API `0.17.0` -> `0.19.0`
    * ktor version `1.2.3` -> `1.2.6`
* `PluginSerializer` now is internal
* Now some part of dependencies are included by `api` and must not be included in dependent projects directly:
    * Coroutines
    * Serialization
    * TelegramBotAPI
    * Kotlin Exposed
* Deprecations clean up:
    * `com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers.Chooser`
    * `com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.RatingPlugin`. WARNING: WAS REMOVED WHOLE CONTENT
    OF RATING API PLUGIN
    * `com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.PostsUsedTable`
    * `com.github.insanusmokrassar.AutoPostTelegramBot.utils.MessagesResendingKt.cacheMessages`
    * `com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable#removeMessageOfPost`

### 1.6.1 Dependencies update

* `TelegramBotAPI` version `0.19.0` -> `0.20.1`

### 1.6.2

* `TelegramBotAPI` version `0.20.1` -> `0.20.3`

## 1.5.0

* Kotlin version `1.3.31` -> `1.3.41`
* Kotlin coroutines `1.2.1` -> `1.2.2`
* Kotlin seriaization `0.11.0` -> `0.11.1`
* Telegram Bot API `0.15.0` -> `0.17.0`
* ktor version `1.1.4` -> `1.2.3`

* New field in `BotConfig` - `telegramAPIUrlsKeeper`

### 1.5.1

* Fix for `SmartChooser` item age checker

## 1.4.0

* `PostsSchedulesTable` channels now are private and has no longer accessible
* `PostsMessagesTable` now is using flows for output broadcasting and non-conflated broadcast channels inside
* Remove `Launch` deprecations
* Actualize `Config`
* Actualizing `WebhookConfig`
* `PostsLikesMessagesTable` now is not using deprecated `PostsUsedTable`
* `BasePlugin` now is not using deprecated `PostsUsedTable`
* `RatingCommand` was removed
* `Command` now have no its own logger
* `PluginManager#findFirstPlugin` extension was added
* `Launch` listeners now are private
* `LongPollingConfig` was added
* `BotConfig` now have an additional field: `longPollingConfig`
* `FinalConfig` have no `createFilter` method for now and use `longPollingConfig`
* Fixes in `Scheduler` task update

## 1.3.0

* `TelegramBotAPI` version `0.12.7` -> `0.14.0`
* Added dependencies for server (`CIO`) and client (`OkHttp`) ktor engines
* Added `CoroutineScope` helper functions
* Rewritten logic of scheduler work

### 1.3.1

* Update `TelegramBotAPI` version `0.14.0` -> `0.14.1`
* In `Launch` added `flowFilter` and updates collecting rewritten on flows
* `RatingPlugin` abstraction was added
* `Chooser` now use suspend function instead of common
* Old `RatingPlugin` is deprecated for now
* Checked flows for listeners added
* `buildCommandFlow` function added - it will create flow with checked command from `checkedMessagesFlow`
* Was added `collectWithErrors` function

### 1.3.2

* Added `checkedEditedMessagesFlow`
* `messagesListener` will not broadcast edited events
* `PostsUsedTable` now is deprecated
* `FixPost` will remove transaction only in case of availability of it
* `BasePlugin` now work with internal lambdas and coroutine jobs
* `CommonKnownPostsTransactions` was created
* `TelegramBotAPI` version `0.14.1` -> `0.14.2`

### 1.3.3

* Hotfixes and refactoring in `PostPublisher`
* `cacheMessages` is deprecated for now

### 1.3.4

* Fix problem of publishing info about removing of Post

### 1.3.5

* Fixes in scheduler
* `Scheduler` now is using the flows
* `PostsSchedulesTable` now prefer to use flows for output broadcasting

## 1.2.0

* Update version of `TelegramBotAPI` from `0.11.0` to `0.12.0`
* Remove `CONFLATED_MODE_ON` environment variable checking. Now all channels by default conflated
* Added section to bot config `webhookConfig`
* `DisableRating` now have more obvious logging and behaviour

### 1.2.1

* `TelegramBotAPI` version `0.12.0` -> `0.12.1`

### 1.2.2

* Adapt project tto new types of updates

### 1.2.3

* Update `TelegramBotAPI` version `0.12.2` -> `0.12.3`

### 1.2.4 Hotfix

* Hotfix for `PostPublisher` behaviour - now it will remove posts without forwardable messages

### 1.2.5 Better choosers

* `Chooser` abstraction was replaced to the base package
* Add abstraction `PostId` which actually is `Int`
* Now all choosers can accept exceptions for skip choosing of some posts
* Added new command `getAutoPublications`

### 1.2.6

* Update version of `TelegramBotAPI` from `0.12.3` to `0.12.5`
* Add utility methods for messages resending
* Extracted base commands regexps outside of classes
* `PostPublisher` channel `postPublishedChannel` now have list of messages as value for notification

### 1.2.7

* `kotlin` version `1.3.21` -> `1.3.30`
* `kotlin coroutines` version `1.1.1` -> `1.2.0`
* `kotlin serialization` version `0.10.0` -> `0.11.0`
* `TelegramBotAPI` version `0.12.5` -> `0.12.6`
* `ListSerializer` renamed to `PluginsListSerializer` and now just delegated by `ArrayListSerializer`
with new `PluginSerializer`

### 1.2.8

* `TelegramBotAPI` version `0.12.6` -> `0.12.7`

## 1.1.0

* Update version of libraries
* Remove deprecations
* Fix errors

### 1.1.1

* Fix publishing of media groups
* Fix BotLogger scope
* Fix problem with repeated media group triggering of posts

## 1.0.0

* Rewrite to use new version of library for telegram bot API
* Add automatic connect by after configuration into database

### 1.0.1

* Fix of [#27](https://github.com/InsanusMokrassar/AutoPostTelegramBot/issues/27)

### 1.0.2

* Update `TelegramBotAPI` version `0.8.4` -> `0.9.0`

### 1.0.3

* Update version of TelegramBotAPI
* Update output of errors in `PostPublisher` and `RegisteredRefresher`
* `disableLikesForPost` now execute requests unsafely (for avoid cases of unavailable deleting messages)
* `PostPublisher` now use `createResends` for messages

### 1.0.4

* `sendToLogger` now is extension for `Any` for correct sending of source class and method
* `BotLogger` will try to send log messages with `Markdown` parse mod and, if not success, without
* `TimerTriggerStrategy` now will await near time to trigger publish
* Experimentally replace all `LaunchKt` broadcast channels by custom `UnlimitedBroadcastChannel`
* Experimentally replace all BroadcastChannel factory invocations by creating of `UnlimitedBroadcastChannel`
* Update `TelegramBotAPI` `0.9.1` -> `0.9.2`
* `registerPostMessage` by default use 3 retries to register post message

### 1.0.5

* Add registering of messages for on the init of `DefaultPostRegisteredMessage`
* Rename `PostMessagesRegistrant` and replace its registering function inside
* Add `PostsMessagesTable#findPostByMessageId`
* Add `/renewRegistered` (or `/renewRegisteredMessage`) command

### 1.0.6

* Fix `Config` data

### 1.0.7

* Revert experimental using of `UnlimitedBroadcastChannel`
* Experimentally replace all `Channel.CONFLATE` data by exact values
* Update version of `TelegramBotAPI`: `0.9.2` -> `0.9.3`

### 1.0.8

* Update `TelegramBotAPI` version `0.9.3` -> `0.10.0`
* Fix problem of replying for message in post which is absent. Now when
message is absent registerer will try to get a new one (first one) until
it will find some present or delete post for the reason that there is no
available messages for post
* Add optional otherwise field
* Add custom scope for launch

### 1.0.9

* Fix renew registered message and autoremove of post when there is no available post messages
* Add method `chooseCapacity` for cases when capacity of channels can be `CONFLATED`
* Add usage `chooseCapacity` to all default BroadcastChannels instantiating

### 1.0.10

* Update version of `TelegramBotAPI` from `0.10.0` to `0.10.3`
* Enable and disable of ratings now are using with commands
* Fixes in `RegisteredRefresher`

## 0.7.0

* Add `CallbackQueryReceivers` package with receivers interface and implementations
* Rewrite rating receivers to use `SafeCallbackQueryReceiver`
* Now rating receivers will automatically register message for post as likes message if have no registered
* `TimerTriggerStrategy` now do not using synchronisations
* Add `Iterable<CalculatedDateTime>#nearDateTime`
* `TimerTriggerStrategy` config now receive time as parameter
* Now `DatabaseConfig` is independent class which can connect to Database

## 0.6.0

* Changed version naming strategy: now it is `x.y.z` where:
    * x - major version (release version)
    * y - minor version (feature version)
    * z - fix version
* Add util for work with time
(`String.parseDateTimes(): List<CalculatedDateTime>`)
* Added extension for planning actions
(`Iterable<CalculatedDateTime>.launchNearFuture(suspend () -> R): Deferred<R>`)
* Added extension for pairs of dates and times
(`Iterable<CalculatedDateTime>.asPairs(): List<CalculatedPeriod>`)
* Rewrite plugins to use new dates utils:
    * `SmartChooser`
    * `Scheduler`

### 0.6.1

Fixed SmartChooser wrong time checker

### 0.6.2

* Add `CalculatedDateTime#nearInPast`
* Add `CalculatedDateTime#asFor`
* Add `CalculatedDateTime#asFutureFor`
* Add `CalculatedDateTime#asPastFor`
* Add `CalculatedDateTime#isBetween`
* Rewrite `SmartTimer` item method `isActual` to use `isBetween` for pairs

### 0.6.3

* Optimise `CalculatedDateTime#isBetween`
* Fix `CalculatedDateTime#asPartFor`

### 0.6.4

Fix of get scheduled posts command

### 0.6.5

Fix of recognising of get command argument

### 0.6.6

Fix of calculating of future and past

### 0.6.7

* Now `GarbageCollector` receive `skipTime` instead of `trackingDelay` in format of standard date time
* Now `GarbageCollector` receive `manualCheckTime` instead of `manualCheckDelay` in format of standard date time
* Renaming and improvement for `List<CalculatedDateTime>#executeNearFuture`

### 0.6.8

Fix of `CalculatedDateTime#asPastFor` - now it will decrease time even if source is equal

### 0.6.9

* Fix of `GarbageCollector#check`
* Add extension `DateTime#withoutTimeZoneOffset`
* Fix of `GarbageCollector` time detection
* `CalculatedDateTime#asFutureFor` will guarantee return future datetime even if `source` is equal to generated `now`

## 0.5.0

**BREAK CHANGES**

This update contains changes which related to behaviour of
publishing of posts. If to be exactly - now you do not need
to use raw markdown for posts

* Now you can subscribe directly on `ReceiveChannel` updates
* Rewrite `debounce`, now it is more compact and effective
* Proxy settings was replaced into `clientConfig` settings
* Add `Message#textOrCaptionToMarkdown()`, now you can use
simple way to preformat messages
* Forwarders now work with preformatted text (like
[this](#0.5.0) or **this**)
* Now `BotLogger` may have `params` section which look like
`commonBot` section in general config for creating
independent bot instance. It will be useful for free real
bot
* Added `BotConfig` and temporary keep old settings,
but update instruction to use new config schema

## 0.4.0

* Updated dependencies
    * `Kotlin` -> 1.2.61
    * `Kotlin Coroutines` -> 0.25.0
    * `TelegramBot` -> 4.1.0
* Added `TelegramBot#executeSync` (as a replacement for `execute` of `TelegramBot`,
which block all requests data thread)
* Added `BotLogger` plugin and removed init by default of `LogHandler`
* Small fixes

### 0.4.1

* Hotfixes for `0.4.0` version
* Update `Kotlin Coroutines` -> 0.25.3

### 0.4.2

* Fixes in scheduler plugin
* Added `/getPublishSchedule` command
* Added `/disableSchedulePublish` command
* Now publisher will automatically remove `/publishPost` message

### 0.4.3

* **Now posts contain creation datetime** (look at `PostsTable#getPostCreationDateTime(Int)`)
* Update `BotLogger` behaviour
* Fix problem with canceling of next scheduled job for `SchedulerPlugin`
* Fix behaviour of GetSchedulesCommand when queue is empty
* Add for items of `SmartChooser` plugin config options `minAge` and `maxAge` for prefilter
posts which too old or too young

### 0.4.4

**BREAK CHANGES**

* **Update Java version from `1.7` -> `1.8`**
* Disabled availability to use infinity `executeSync` and `executeAsync`
* `PostTransactionTable` now closeable - you can use `use` and other
* Fix problem with forward posts which contain deleted posts

### 0.4.5

**BREAK CHANGES**

* Added `SemaphoreK`
* Add work with semaphore in execution bot extensions
* Add availibility to set up requests regen settings
* Add `PostTransaction`
* **BREAK CHANGE**: `PostTransactionTable` was removed
* **BREAK CHANGE**: Remove `executeSync`
* Add `executeBlocking(T, TelegramBot, Int, Long)`
    * Please, note that old methods as `executeAsync`
    is not recommended to use
* **ALL BROADCASTS NOW ARE CONFLATED**
* `BotIncomeMessagesListener` version `0.9b` -> `0.9`
* Fixes in Scheduler plugin

### 0.4.6

* Add handling of possible throws from `resume` in `Semaphore`
* GarbageCollector can receive parameter `trackingDelay`
* Removed redundant `PluginVersion` typealias
* Fixes in `BotLogger`
    * Note: temporary you will not be able to change log level
    of `commonLogger` (now it is FINER)

### 0.4.7

* Hotfix for `0.4.6`
    * Fix of GarbageCollector

## 0.3.2

* Deprecate old remove post messages mechanism and add more informative mechanism
of notifying about removed messages
* Add extension for `BroadcaseChannel` subscribing for more comfortable interact
with events channels

### 0.3.3

* Hotfix for problem with new extension for BroadcastChannels

### 0.3.4

* Changed return type of forwarders
* Now you can subscribe onto post published events

### 0.3.5

* Fixed problem with scheduler on initialization (without any event scheduler
will not update job, but now fixed)

### 0.3.6

* Add `BroadcastReceiver#debounce`

### 0.3.7

* Fix `TimerTriggerStrategy` calculating delay and put trigger work in
different async block

### 0.3.8

**BREAK CHANGES**

* Add `PostsUsedTable`, now any plugin or action which can potentially publish
recommended to register/unregister in this table
* Rename methods of `PostsLikesMessagesTable` to be consistent with their
behaviours
* ***Now plugins `HAVE NO VERSIONS` and must not implement `onInit` method***

### 0.3.9

* Added `Throwable#collectStackTrace` which return as string stacktrace of
caller
* Added `String#splitByStep`, `String#splitForMessage` and
`String#splitForMessageWithAdditionalStep`
* Added logger handler for all logger events which was created by bot by
default
* Added `commonLogger` and deprecated `pluginLogger`
