# Auto Post Telegram Bot

## Как начать использовать?

1. Скачайте `.jar` (конкретные ссылки будут приведены позже) или соберите проект. Чтобы собрать проект:
    1. Убедитесь, что у вас установлена `Java` (>= 1.7) и `Maven`
    1. Склонируйте проект с [Github](https://github.com/InsanusMokrassar/AutoPostTelegramBot)
    2. В каталоге с проектом:
        1. Собираем проект с помощью команды для Maven `mvn clean package`
        2. Копируем файл из пути `$PROJECT_DIR/target/AutoPostTelegramBot-$version-jar-with-dependencies.jar` туда, где
        должен лежать бот
3. Настройте проект через написание конфига (шаблон конфигурации есть в корне 
[проекта на Github](https://github.com/InsanusMokrassar/AutoPostTelegramBot))
4. Запустите командой `java -jar ./AutoPostTelegramBot-$version-jar-with-dependencies.jar
%ПУТЬ ДО КОНФИГУРАЦИОННОГО ФАЙЛА%`
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

* Любой плагин наследуется от класса
[Plugin](src/main/kotlin/com/github/insanusmokrassar/AutoPostTelegramBot/base/plugins/Plugin.kt)

#### Остальное

В остальном плагины вольны делать что им вздумается с той лишь оговоркой, что они не
способны изменить настройки других плагинов. По этой причине стоит крайне осторожно
подходить к выбору и подключению сторонних плагинов к боту.

### Плагины, включенные в базовый пакет

* [BasePlugin](src/main/kotlin/com/github/insanusmokrassar/AutoPostTelegramBot/plugins/base/BasePlugin.kt)
    \- предоставляет доступ к основному функционалу и в большинстве случаев обязателен к подключению:
    ```json
    {
      "classname": "com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.BasePlugin"
    }
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
* [ForwardersPlugin](src/main/kotlin/com/github/insanusmokrassar/AutoPostTelegramBot/plugins/forwarders/ForwardersPlugin.kt)
    \- хранит набор `Forwarder` классов, способных корректно переносить контент из одного чата/канала в другой
    без необходимости непосредственной пересылки (`forward`), проблемой которой является упоминание источника
    пересылки. Подключение:
    ```json
        {
          "classname": "com.github.insanusmokrassar.AutoPostTelegramBot.plugins.forwarders.ForwardersPlugin"
        }
    ```
    Поддержка форматов:
        * Изображения
        * Видео
        * Аудио
        * Голос
        * Текст
        * Медиагруппы (ВСЕГДА публикуются одним постом, но могут быть включены в другие посты)
        * Контакты
        * Геолокация
        * Документы
* [PostPublisher](src/main/kotlin/com/github/insanusmokrassar/AutoPostTelegramBot/plugins/publishers/PostPublisher.kt)
    \- предоставляет возможность публиковать посты без необходимости вручную писать форвард поста. Требует
    `ForwardersPlugin` для корректной работы. Кроме того, данный плагин содержит интерфейс `Publisher`,
    рекомендуемый для имплементации в случае, если вы хотите создать свою реализацию публикатора.
    Подключение плагина:
    ```json
      {
        "classname": "com.github.insanusmokrassar.AutoPostTelegramBot.plugins.publishers.PostPublisher"
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
    {
      "classname": "com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.RatingPlugin"
    }
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
* [Chooser](src/main/kotlin/com/github/insanusmokrassar/AutoPostTelegramBot/plugins/choosers)'ы - набор
    классов, имплементирующих интерфейс `Chooser`, по-умолчанию используемый во всех других плагинах
    для выбора списка постов для публикации. Все плагин `Chooser` (за исключением `None`) требуют
    подключения `RatingPlugin`. Доступные плагины-`Chooser`'ы по-умолчанию:
    * [SmartChooser](src/main/kotlin/com/github/insanusmokrassar/AutoPostTelegramBot/plugins/choosers/SmartChooser.kt)
        \- наиболее гибкий из представленных по-умолчанию `Chooser` реализаций. Публикует в разное время
        разные посты в зависимости от настроек. Подключение:
        ```json
            {
              "classname": "com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers.SmartChooser",
              "params": {
                "times": [
                  {
                    "minRate": -2,
                    "maxRate": 1,
                    "sort": "ascend|descend|random",
                    "time": [
                      "16:00",
                      "02:00"
                    ],
                    "timeOffset": "+03:00"
                  }
                ]
              }
            }
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
            * `time` - массив, в котором каждый первый элемент - начало
            времени, когда этот объект конфигурации применим, а каждый
            второй - конец такого времени
            * `timeOffset` - опциональное поле формата `+HH:mm`, где
                * `+` - знак смещения относительно `UTC`
                * `HH` - часы смещения
                * `mm` - минуты смещения
            * `count` - число постов, производимых за раз
    * [MostRated](src/main/kotlin/com/github/insanusmokrassar/AutoPostTelegramBot/plugins/choosers/MostRatedChooser.kt)
        \- выбирает всегда самые старые из самых популярных постов. Подключение:
        ```json
        {
          "classname": "com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers.MostRatedChooser"
        }
        ```
    * [MostRatedRandomly](src/main/kotlin/com/github/insanusmokrassar/AutoPostTelegramBot/plugins/choosers/MostRatedRandomChooser.kt)
        \- выбирает случайные из самых популярных постов. Подключение:
        ```json
        {
          "classname": "com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers.MostRatedRandomChooser"
        }
        ```
    * [None](src/main/kotlin/com/github/insanusmokrassar/AutoPostTelegramBot/plugins/choosers/NoneChooser.kt)
        \- всегда возвращает пустой список постов. Подключение:
        ```json
        {
          "classname": "com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers.NoneChooser"
        }
        ```
* [TimerTrigger](src/main/kotlin/com/github/insanusmokrassar/AutoPostTelegramBot/plugins/triggers/TimerTriggerStrategy.kt)
    \- аредоставляет возможность осуществлять автоматическую публикацию раз в строго определённое время. Для
    работы требует подключенные `Chooser` и `Publisher` плагины. В момент триггера спросит у `Chooser`
    посты для публикации и передаст их `Publisher`. Подключение:
    ```json
        {
          "classname": "com.github.insanusmokrassar.AutoPostTelegramBot.plugins.triggers.TimerTriggerStrategy",
          "params": {
            "delay": 3600000
          }
        }
    ```
* [GarbageCollector](src/main/kotlin/com/github/insanusmokrassar/AutoPostTelegramBot/plugins/GarbageCollector.kt) - как 
    следует из названия, собирает мусор. Если точнее, удаляет записи с рейтингом ниже установленного минимума. Подключение:
    ```json
        {
          "classname": "com.github.insanusmokrassar.AutoPostTelegramBot.plugins.GarbageCollector",
          "params": {
            "minimalRate": -2,
            "manualCheckDelay": 3200000
          }
        }
    ```
    Отсутствие `manualCheckDelay` в настройках отключит проверку раз в какое-то время и будет реагировать
    только на события изменений в плагине `RatingPlugin`

## Секции конфигурации

В корне конфигурации лежат следующие параметры (учтите, что `ChatId` - именно **`id`**, не `username`):

* `targetChatId` - идентификатор чата, куда в итоге будут отправляться посты; число
* `sourceChatId` - идентификатор чата, откуда в итоге будут браться посты; число
* `logsChatId` - идентификатор чата, куда будут отправляться системные сообщения,
по-умолчанию будет равен `sourceChatId`; число
* `botToken` - токен `Telegram Bot` для доступа к API вида `123456789:ABCDEFGHIGKLMNOPQRSTUVWXYZabcdefghi`
* `proxy` - объект настройки `Socks5` прокси. Достаточно указать `{}` если
вы используете `shadowsocks`
    * `host` - `url` прокси, по-умолчанию - `localhost`
    * `port` - порт прокси, по-умолчанию - `1080`
    * `username` - имя пользователя для доступа к прокси через авторизацию
    * `password` - пароль для доступа к прокси через авторизацию
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

