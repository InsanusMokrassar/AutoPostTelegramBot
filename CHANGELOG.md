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


