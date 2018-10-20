# Auto Post Telegram Bot Changelog

## 0.3.2

* Deprecate old remove post messages mechanism and add more informative mechanism
of notifying about removed messages
* Add extension for `BroadcaseChannel` subscribing for more comfortable interact
with events channels

## 0.3.3

* Hotfix for problem with new extension for BroadcastChannels

## 0.3.4

* Changed return type of forwarders
* Now you can subscribe onto post published events

## 0.3.5

* Fixed problem with scheduler on initialization (without any event scheduler
will not update job, but now fixed)

## 0.3.6

* Add `BroadcastReceiver#debounce`

## 0.3.7

* Fix `TimerTriggerStrategy` calculating delay and put trigger work in
different async block

## 0.3.8

**BREAK CHANGES**

* Add `PostsUsedTable`, now any plugin or action which can potentially publish
recommended to register/unregister in this table
* Rename methods of `PostsLikesMessagesTable` to be consistent with their
behaviours
* ***Now plugins `HAVE NO VERSIONS` and must not implement `onInit` method***

## 0.3.9

* Added `Throwable#collectStackTrace` which return as string stacktrace of
caller
* Added `String#splitByStep`, `String#splitForMessage` and
`String#splitForMessageWithAdditionalStep`
* Added logger handler for all logger events which was created by bot by
default
* Added `commonLogger` and deprecated `pluginLogger`

## 0.4.0

* Updated dependencies
    * `Kotlin` -> 1.2.61
    * `Kotlin Coroutines` -> 0.25.0
    * `TelegramBot` -> 4.1.0
* Added `TelegramBot#executeSync` (as a replacement for `execute` of `TelegramBot`,
which block all requests data thread)
* Added `BotLogger` plugin and removed init by default of `LogHandler`
* Small fixes

## 0.4.1

* Hotfixes for `0.4.0` version
* Update `Kotlin Coroutines` -> 0.25.3

## 0.4.2

* Fixes in scheduler plugin
* Added `/getPublishSchedule` command
* Added `/disableSchedulePublish` command
* Now publisher will automatically remove `/publishPost` message

## 0.4.3

* **Now posts contain creation datetime** (look at `PostsTable#getPostCreationDateTime(Int)`)
* Update `BotLogger` behaviour
* Fix problem with canceling of next scheduled job for `SchedulerPlugin`
* Fix behaviour of GetSchedulesCommand when queue is empty
* Add for items of `SmartChooser` plugin config options `minAge` and `maxAge` for prefilter
posts which too old or too young

## 0.4.4

**BREAK CHANGES**

* **Update Java version from `1.7` -> `1.8`**
* Disabled availability to use infinity `executeSync` and `executeAsync`
* `PostTransactionTable` now closeable - you can use `use` and other
* Fix problem with forward posts which contain deleted posts

## 0.4.5

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

## 0.4.6

* Add handling of possible throws from `resume` in `Semaphore`
* GarbageCollector can receive parameter `trackingDelay`
* Removed redundant `PluginVersion` typealias
* Fixes in `BotLogger`
    * Note: temporary you will not be able to change log level
    of `commonLogger` (now it is FINER)

## 0.4.7

* Hotfix for `0.4.6`
    * Fix of GarbageCollector

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
