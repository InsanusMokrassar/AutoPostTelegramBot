# Auto Post Telegram Bot

[![Download](https://api.bintray.com/packages/insanusmokrassar/StandardRepository/AutoPostTelegramBot/images/download.svg)](https://bintray.com/insanusmokrassar/StandardRepository/AutoPostTelegramBot/_latestVersion) [![Build Status](https://travis-ci.com/InsanusMokrassar/AutoPostTelegramBot.svg?branch=master)](https://travis-ci.com/InsanusMokrassar/AutoPostTelegramBot)

[![Get automatic notifications about new "AutoPostTelegramBot" versions](https://www.bintray.com/docs/images/bintray_badge_color.png)](https://bintray.com/insanusmokrassar/StandardRepository/AutoPostTelegramBot?source=watch)

## Как начать использовать?

Если необходимо самостоятельно собрать проект, можно воспользоваться простой инструкцией:

1. Скачать проект с [Github](https://github.com/InsanusMokrassar/AutoPostTelegramBot/)
2. Сделать `./gradlew` исполняемым (`chmod 455 ./gradlew` для `*nix`)
3. Выполнить `./gradlew build`

В зависимости от дистрибутива и целей, инструкция запуска будет несколько отличаться:

* В случае самостоятельной сборки (а также в случае скачивания архива с [jenkins](https://jenkins.insanusmokrassar.com/job/AutoPostTelegramBot_master/) страницы)
    1. Разархивировать в директорию, где будет находиться бот
    2. Найти исполнительный файл в папке `$WORK_DIRECTORY/AutoPostTelegramBot-$version/bin/` (для `Windows` файл имеет расширение `.bat`)
    3. Запустите из консоли соответствующий файл, передав первым аргументом путь до JSON конфига
* В случае использования проекта как библиотеки желательно указать как главный класс `com.github.insanusmokrassar.AutoPostTelegramBot.LaunchKt`
* В случае запуска в Intellij Idea рекомендуется
    * Использовать шаблон `Jar Application`
    * Как аргумент устанавливать путь до JSON конфига
    * Путь до `Jar` будет выглядеть как `$PROJECT_DIR/build/libs/$FILENAME.jar`
    * Перед сборкой как запускать задачу gradle `./gradlew clean build`

5. Profit

## Возможности

### Что бот может?

Если кратко, назначением бота является автоматизация и нормализация создания и
публикации постов в каком-либо чате/канале. Под постом в данном случае понимается
совокупность сообщений, которые должны быть опубликованы одновременно. Публикация
происходит из чата/канала, выбранного как `sourceChat` путем выставления
соответствующего `sourceChatId`.

### A little bit deep или капелька взгляда изнутри

Данный раздел не требуется к прочтению для тех, кто не собирается использовать
проект как библиотеку и писать для него плагины.

#### База

Данный бот в основе своей имеет абсолютный минимум для запуска:

* Пост - совокупность сообщений и сообщения-уведомлений о регистрации
* Базовая БД хранится в двух публичных классах, имеющих `coroutine`-каналы для подписки
* Класс запуска имеет `coroutine`-каналы для подписки на сообщения, приходящие к боту
* Система плагинов

Если посмотреть на пакет с
[базовым кодом](src/main/kotlin/com/github/insanusmokrassar/AutoPostTelegramBot/base), вы увидите
только базу данных, модельки настроек и плагин-корень. Весь прочий функционал достигается плагинами,
описанными после этого раздела.

#### Плагины

Любой плагин наследуется от класса
[Plugin](src/main/kotlin/com/github/insanusmokrassar/AutoPostTelegramBot/base/plugins/Plugin.kt)

На данный момент большая часть базовых плагинов включена в пакет распространения, но в планах их
вынести в отдельные проекты. Кроме того, вы можете не включать в конфигурацию ни один из плагинов.

#### Остальное

В остальном плагины вольны делать что им вздумается с той лишь оговоркой, что они не
способны изменить настройки других плагинов. По этой причине стоит крайне осторожно
подходить к выбору и подключению сторонних плагинов к боту.

### Формат времени

После длительного тестирования и проверок было решено добавить стандартизированную работу с временем,
которая позволила бы разработчикам плагинов не писать лишний и повторяющийся код, а пользователям
(администраторам каналов и групп) - не забивать голову лишними форматами конфигураций. Итак, что
из себя представляет простейший формат времени?

* Время: `[часы:]минуты[:секунды[.миллисекунды]]` - стоит учесть, что если вы хотите указать только
секунды, следует использовать запись `00:00:секунды`
* Дата: `[[год.]месяц.]день` - то есть верным будет указание просто числа `15`, если вы имеете ввиду
15 день месяца. В зависимости от контекста это может быть ближайший 15 день месяца, то есть если
сегодня 16 число - ближайшее 15 число будет в следующем месяце.
**NOTE: НЕ ИСПОЛЬЗУЕТСЯ БЕЗ УКАЗАНИЯ ВРЕМЕНИ (см. Дата и время)**
* Дата и время: `[дата ]время` - то есть в большинстве случаев (если не всегда) выбудете обязаны
написать в качестве времени хотя бы `00`, что будет расценено как полуночь или `00` минут каждого
часа (см. последовательность времени) (стоит понимать, что дата тут необязательный параметр)
* Пара даты и времени: `Дата и время-Дата и время` - используется для указания пары даты-времени или
некоторого промежутка (см. последовательность времени)
* Последовательность времени: `Дата и время-Дата и время [step ]Дата и время` - тут стоит сказать
больше. Когда вы планируете некоторое действие повторять (например, публикация по времени) -
гораздо удобней использовать некоторые `hotkeys`. Тут мы используем третий параметр дата-время,
чтобы указать задержку между датами. Параметр `step` не обязателен, но он позволит быть уверенными,
что последовательность будет построена корректно и пробел не будет распознан как разделитель
даты-времени.

Примеры:

* `12:30-12:40 01` - создаёт последовательность из времени
    * `12:30`
    * `12:31`
    * `12:32`
    * `...`
    * `12:39`
* `10-20 step 5` - без `step` промежуток `5` будет распознан как значение минут. Кроме того,
если вы будете использовать этот формат для задания последовательности событий в будущем,
поскольку вы не указали часы - считается, что это `вольный параметр`*. В таком случае при
необходимости можно получить следующую последовательность:
    * `00:10`
    * `00:15`
    * `01:10`
    * `01:15`
    * `...`
* `15 12:35` - фактически, будет распознаваться как `ближайшее 15 число, 12 часов 35 минут`

***Вольный параметр*** - значение, которое может увеличиться до следующего, если, например, вы
запрашиваете нечто в будущем. Для формата `15` вольным будет час, для `12:15` - день и так далее.

### Плагины, включенные в базовый пакет

* [BasePlugin](src/main/kotlin/com/github/insanusmokrassar/AutoPostTelegramBot/plugins/base/BasePlugin.kt)
    \- предоставляет доступ к основному функционалу и в большинстве случаев обязателен к подключению:
    ```json
    [
      "com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.BasePlugin",
      {}
    ]
    ```
    Включает следующий функционал:
    * `/startPost` используется в паре с `/fixPost`, сообщения между ними будут прикреплены к одному посту.
    Можно не использовать для галерей (медиагрупп), поскольку такие группы определяются автоматически в
    один пост. Пример (каждый пункт - отдельное сообщение):
        1. `/startPost`
        2. Тут какой-то текст
        3. Картиночка
        4. Еще что-то
        5. `/fixPost`
        6. Далее вас оповестят, что серия сообщений закреплена за одним постом и будет опубликована.
        Записи публикуются не по состоянию на момент создания, а по состоянию на момент перед
        публикацией. Кроме того, учтите, что бот не сможет удалять сообщения из постов, если они были
        отправлены более двух дней назад (специфика `Telegram`) - бот об этом оповестит в канале с
        логами и скажет, что он не может удалить (приведет еще причину, если вдруг там чего странного).
    * `/deletePost` - используйте через `reply` сообщения с текстом `Post registered`, будет удален пост
    вместе с его сообщениями. Важно: `/startPost`/`/fixPost` таким образом не удаляются - их нужно чистить
    вручную.
    * `/renewRegistered` или `/renewRegisteredMessage` - при реплае одного из сообщений поста удаляет старое сообщение
    "Post registered" (если оно есть) и отправляет новое
* [PostPublisher](src/main/kotlin/com/github/insanusmokrassar/AutoPostTelegramBot/plugins/publishers/PostPublisher.kt)
    \- предоставляет возможность публиковать посты без необходимости вручную писать форвард поста. Данный плагин содержит
    интерфейс `Publisher`, рекомендуемый для имплементации в случае, если вы хотите создать свою реализацию публикатора.
    Подключение плагина:
    ```json
    {
        "com.github.insanusmokrassar.AutoPostTelegramBot.plugins.publishers.PostPublisher",
        {}
    }
    ```
    Предоставляет:
    * `/publishPost` - команда, используемая для немедленной публикации пост(а)(ов). Может использоваться в
    трех вариантах:
        * `/publishPost` - публикуется один пост из тех, что выбираются выбиралкой из конфига
        * `/publishPost [число]` - публикается какое-то число постов, выбранных инстансом `Chooser` из конфига.
        Стоит учесть, что при первом совпадении между уже выбранными командой и выданным ей списком из
        `Chooser` команда прекращает выборку и публикует то, что уже выбрано. Само собой, требуется подключение
        одного из `Chooser` плагинов.
        * `/publishPost` с `reply` какого-то из постов (как для deletePost) - публикует только выбранный пост
* [RatingPlugin](src/main/kotlin/com/github/insanusmokrassar/AutoPostTelegramBot/plugins/rating/RatingPlugin.kt)
    \- плагин для подключения рейтингов. Позволяет команде голосовать за/против определенные посты и на основании
    голосов выбирать посты для публикации. Подключение:
    ```json
    [
      "com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.RatingPlugin",
      {}
    ]
    ```
    Подключение предоставляет:
    * Возможность командой оценивать посты
    * Подключение большого числа плагинов, в том числе
        * Многие `Chooser` плагины
        * `GarbageCollector`
        * `PostPublisher` - публикация по рейтингу
    * `/mostRated` - выводит список самых топовых на данный момент постов. Если ваша планерка - не публичный
    канал, топовые посты будут переотправлены. Обычно можно нажать на название после `forwarded from` и
    вам откроется сам пост.
    * `/availableRatings` - выводит список пар "рейтинг" - "число постов", зарегестрированных в системе
    * `/enableRating` - включение рейтинга для поста
    * `/disableRating` - выключение рейтинга для поста
* [Chooser](src/main/kotlin/com/github/insanusmokrassar/AutoPostTelegramBot/plugins/choosers)'ы - набор
    классов, имплементирующих интерфейс `Chooser`, по-умолчанию используемый во всех других плагинах
    для выбора списка постов для публикации. Все плагин `Chooser` (за исключением `None`) требуют
    подключения `RatingPlugin`. Доступные плагины-`Chooser`'ы по-умолчанию:
    * [SmartChooser](src/main/kotlin/com/github/insanusmokrassar/AutoPostTelegramBot/plugins/choosers/SmartChooser.kt)
        \- наиболее гибкий из представленных по-умолчанию `Chooser` реализаций. Публикует в разное время
        разные посты в зависимости от настроек. Подключение:
        ```json
        [
          "com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers.SmartChooser",
          {
            "times": [
              {
                "minRate": -2,
                "maxRate": 1,
                "sort": "ascend|descend|random",
                "time": "16:00-17:00",
                "times": [
                    "16:00-17:00",
                    "20:00-21:00"
                ],
                "timeOffset": "+03:00",
                "minAge": 86400000
              }
            ]
          }
        ]
        ```
        имеет следующую структуру для параметра `params`:
        * `times` - список временнЫх настроек для выборщика, каждый объект
            которого содержит следующие параметры:
            * `minRate` - минимальный рейтинг для попадания в список
            кандидатов на выбор, если не указано - считается, что
            минимума нет
            * `maxRate` - максимальный рейтинг для попадания в список
            кандидатов на выбор, если не указано - считается, что
            максимума нет
            * `sort` - режим сортировки кандидатов на выборку
            * `time` - время в стандартном формате (или см. ниже)
            * `times` - массив времени в стандартном формате (учитывается в первую очередь)
            * `count` - число постов, производимых за раз
            * `minAge` - число в миллисекундах, отвечающее за минимальный
            возраст поста попадания в кандидаты для выборки
            * `maxAge` - число в миллисекундах, отвечающее за максимальный
            возраст поста попадания в кандидаты для выборки
    * [MostRated](src/main/kotlin/com/github/insanusmokrassar/AutoPostTelegramBot/plugins/choosers/MostRatedChooser.kt)
        \- выбирает всегда самые старые из самых популярных постов. Подключение:
        ```json
        [
          "com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers.MostRatedChooser",
          {}
        ]
        ```
    * [MostRatedRandomly](src/main/kotlin/com/github/insanusmokrassar/AutoPostTelegramBot/plugins/choosers/MostRatedRandomChooser.kt)
        \- выбирает случайные из самых популярных постов. Подключение:
        ```json
        [
          "com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers.MostRatedRandomChooser",
          {}
        ]
        ```
    * [None](src/main/kotlin/com/github/insanusmokrassar/AutoPostTelegramBot/plugins/choosers/NoneChooser.kt)
        \- всегда возвращает пустой список постов. Подключение:
        ```json
        [
          "com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers.NoneChooser",
          {}
        ]
        ```
* [TimerTrigger](src/main/kotlin/com/github/insanusmokrassar/AutoPostTelegramBot/plugins/triggers/TimerTriggerStrategy.kt)
    \- предоставляет возможность осуществлять автоматическую публикацию раз в строго определённое время. Для
    работы требует подключенные `Chooser` и `Publisher` плагины. В момент триггера спросит у `Chooser`
    посты для публикации и передаст их `Publisher`. Как параметр принимает `time` принимает стандартный формат времени. Подключение:
    ```json
        [
          "com.github.insanusmokrassar.AutoPostTelegramBot.plugins.triggers.TimerTriggerStrategy",
          {
            "time": "00:30-00:30 01:00"
          }
        ]
    ```
    Кроме того, добавляет доступ к команде `getAutoPublications`, которая возвращает ближайшую публикацию или
    несколько публикаций, если использована с числом
* [SchedulerPlugin](src/main/kotlin/com/github/insanusmokrassar/AutoPostTelegramBot/plugins/scheduler/SchedulerPlugin.kt)
    \- позволяет ставить посты на публикацию в определенное время (в том числе на конкретную дату). Добавляет
    команды:
    * `/setPublishTime (time format)` - установка таймера на пост
    * `/getPublishSchedule[ [число]|[time пара]]` - пересылает несколько постов выставленных на таймер. Если указано
    число - будут выбранны ближайшие записи в количестве, указанном в параметре. Если указана пара времени в стандартном
    формате - даты будут взяты по две (нужно помнить, что при указании периода будет создана последовательность
    дата-время) и для каждой найдены все публикации на таймере 
    * `/disableSchedulePublish` - исключает из публикации по таймеру пост
    
    Подключение:
    ```json
        [
          "com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler.SchedulerPlugin"
        ]
    ```
* [GarbageCollector](src/main/kotlin/com/github/insanusmokrassar/AutoPostTelegramBot/plugins/GarbageCollector.kt) - как 
    следует из названия, собирает мусор. Если точнее, удаляет записи с рейтингом ниже установленного минимума. Подключение:
    ```json
        [
          "com.github.insanusmokrassar.AutoPostTelegramBot.plugins.GarbageCollector",
          {
            "minimalRate": -2,
            "skipTime": "23:59",
            "manualCheckTime": "00:00-00:00 03:00"
          }
        ]
    ```
    Отсутствие `manualCheckTime` в настройках отключит проверку по времени и будет реагировать
    только на события изменений в плагине `RatingPlugin`. `skipTime` позволит включить режим неприкосновенности
    на время с момента поста - `GarbageCollector` будет игнорировать пост пока пост попадает в промежутки времени этого
    параметра. Например выше указано `23:59` - это значит, что пост будет неприкосновенен в течение `23` часов и `59`
    минут
* [BotLogger](src/main/kotlin/com/github/insanusmokrassar/AutoPostTelegramBot/plugins/BotLogger.kt) - по-факту,
инициализирует `LogHandler` через вызов
[initHandler](src/main/kotlin/com/github/insanusmokrassar/AutoPostTelegramBot/utils/BotLogger.kt#97) в момент
инициализации плагина. Подключение:
    ```json
        [
          "com.github.insanusmokrassar.AutoPostTelegramBot.plugins.BotLogger",
          {
            "config": // опциональное поле, заполняемое как commonBot в корне конфига
          }
        ]
    ```

## Секции конфигурации

В корне конфигурации лежат следующие параметры (учтите, что `ChatId` - именно **`id`**, не `username`):

* `targetChatId` - идентификатор чата, куда в итоге будут отправляться посты; число
* `sourceChatId` - идентификатор чата, откуда в итоге будут браться посты; число
* `logsChatId` - идентификатор чата, куда будут отправляться системные сообщения,
по-умолчанию будет равен `sourceChatId`; число
* `commonBot` - настройка для общего бота, используемого остальными плагинами и частями системы по-умолчанию
    * `botToken` - токен `Telegram Bot` для доступа к API вида `123456789:ABCDEFGHIGKLMNOPQRSTUVWXYZabcdefghi`
    * `clientConfig` - конфигурация клиента для доступа в интернет ботом
        * `proxy` - объект настройки `Socks5` прокси. Достаточно указать `{}`, если
        вы используете `shadowsocks`, запущенный в вашей системе
            * `host` - `url` прокси, по-умолчанию - `localhost`
            * `port` - порт прокси, по-умолчанию - `1080`
            * `username` - имя пользователя для доступа к прокси через авторизацию
            * `password` - пароль для доступа к прокси через авторизацию
        * `connectTimeout` - задержка для подключения, по-умолчанию: `0` (нет ограничения)
        * `writeTimeout` - задержка для отправки данных, по-умолчанию: `0` (нет ограничения)
        * `readTimeout` - задержка для чтения данных, по-умолчанию: `0` (нет ограничения)
        * `debug` - настройка для вывода отладочной информации по запросам бота контроля действий бота
    * `webhookConfig` - конфигурация для установки вебхука (опциональное поле)
        * `url` - URL адрес, на который должны приходить обновления
        * `port` - внутренний порт, использованный для прокидывания через `reverse proxy`
        * `certificatePath` - путь до публичного файла сертификата
        * `maxConnections` - максимальное число подключений (1 - 100) (опциональное поле)
* `databaseConfig` - объект настройки базы данных. Включает следующие поля:
    * `username` - имя пользователя для доступа к базе данных
    * `password` - пароль для доступа к базе данных
    * `url` - путь для доступа к базе данных. Например,
    `jdbc:h2:mem:test;DB_CLOSE_DELAY=-1` может использоваться для базы данных
    "на раз", то есть, в памяти с использованием базы данных `h2`
    * `driver` - `classname` драйвера базы данных. Например, `org.h2.Driver` для
    использования драйвера `h2`
* `plugins` - список подключенных плагинов. Каждый плагин имеет следующие поля:
    * `classname` - полное пакетное имя класса плагина. Например,
    `com.github.insanusmokrassar.AutoPostTelegramBot.plugins.triggers.TimerTriggerStrategy`
    для подключения плагина триггеринга постов по времени
    * `params` - объект настроек плагина.

## Заметки

Бот игнорирует сообщения, начинающиеся с `/` и не являющиеся известной ему командой. Это значит, что
какие-то объявления можно писать, начиная сообщение со знака `/`. Учтите, что если вам понадобится
закрепить такое сообщение - сообщение о закреплении будет являться полноценным сообщением и будет
отмечено как пост. В таком случае его следует удалить как пост - через команду `/deletePost`.

## Послесловие

Так или иначе, жду отзывы и предложения в [Telegram](https://t.me/insanusmokrassar) и по
[email](mailto://ovsyannikov.alexey95@gmail.com).

