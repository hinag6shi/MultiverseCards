# 2-ая задача:

### Класс игрока.

(!) На этапе создания карточки игрока возможно придётся подключать базу данных.

- Идейно. Можно при команде /start спрашивать как обращаться и переработать команды /author, /about, /author
Что я имею в виду? Я предлагаю добавить обращение к игроку таким образом бот будет дружелюбным и более "продвинутым"

- Информацию:
  - Name: debil3000
  - ID: ... (Может быть будет применение?)
  - Cash: 6767 $
  - Cards: 17/155 (к примеру. Или добавить это число в коллекцию?)

### Класс карточек.

Архитектура (Примерно):

```
ru.himukai.multiversecards
├── Main.java                  composition root: единственное место, где всё собирается
├── bot/                       TelegramBot (как сейчас)
├── core/                      Command, Registry, Dispatcher, Response... (как сейчас)
├── commands/                  тонкие адаптеры + форматирование текста
│   ├── About/Author/HelpCommand        (есть)
│   ├── StartCommand, CardsCommand, CardCommand
│   └── CardPresenter                    карточка → текст, эмодзи, кнопки
├── service/                   сценарии (use cases) + порты
│   ├── PlayerService                    регистрация игрока, /start
│   ├── PlayerRepository                 порт хранения игроков
│   └── CardCatalog                      порт каталога карточек
├── domain/                    правила игры, ничего не знает об остальных пакетах
│   ├── card/    CardId, Rarity, Stats, TemplateId, CardTemplate, Card
│   └── player/  PlayerId, CardCollection, Player
└── infrastructure/            реализации портов
    ├── InMemoryPlayerRepository
    └── StaticCardCatalog                (временный источник данных)
```