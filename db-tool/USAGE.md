# Мини-гайд: база через утилиту

Команды `db_tool.py` выполняются из папки `db-tool`. Подключение берётся из `.env` в текущей папке, рядом с утилитой или на уровень выше. Для этого проекта это `.env` в корне репозитория.

В примерах замени `123456789` на ID игрока, а `sanrio_badtz_common` — на нужный `template_id` из каталога.

## Посмотреть таблицы

```sh
python db_tool.py tables
```

## Посмотреть игроков и карточки каталога

```sh
python db_tool.py show public.players --limit 1000
python db_tool.py show public.card_templates --limit 1000
```

## Посмотреть карточки игрока

```sh
python db_tool.py show public.player_cards --where player_id=123456789 --limit 1000
```

В выводе будет `template_id`. Чтобы найти название и редкость, посмотри этот ID в каталоге:

```sh
python db_tool.py show public.card_templates --where id=sanrio_badtz_common
```

## Выдать карточку игроку

```sh
python db_tool.py insert public.player_cards --set player_id=123456789 --set template_id=sanrio_badtz_common
```

Игрок и шаблон должны существовать, а карточки ещё не должно быть у игрока. Повторная выдача этой командой завершится ошибкой уникальности.

Чтобы выдать ещё одну карточку, запусти такую же команду с другим `template_id`:

```sh
python db_tool.py insert public.player_cards --set player_id=123456789 --set template_id=sanrio_kuromi_common
```

### Выдать все карточки сразу

`db_tool.py insert` добавляет по одной строке. Если нужно выдать игроку весь каталог одной командой, используй `psql` и замени параметры подключения и ID:

```sh
psql -h localhost -p 5432 -U postgres -d multiverse_cards -W -v ON_ERROR_STOP=1 -c "INSERT INTO public.player_cards (player_id, template_id) SELECT 123456789, id FROM public.card_templates ON CONFLICT (player_id, template_id) DO NOTHING; UPDATE public.players SET starter_set_claimed = true WHERE id = 123456789;"
```

Уже выданные карточки останутся без изменений. Флаг `starter_set_claimed` не даст следующему `/start` выдать стартовый набор повторно.

## Изменить усиления карточки

Сначала посмотри текущие счётчики, затем укажи новые значения. `update` задаёт значение целиком, а не прибавляет к старому:

```sh
python db_tool.py update public.player_cards --where player_id=123456789 --where template_id=sanrio_badtz_common --set hp_boost=3
```

Можно поменять несколько статов разом:

```sh
python db_tool.py update public.player_cards --where player_id=123456789 --where template_id=sanrio_badtz_common --set hp_boost=3 --set st_boost=2 --set df_boost=0
```

В игре общий уровень карточки — сумма `hp_boost + st_boost + df_boost`, максимум 21. `db_tool.py` — универсальный редактор таблиц и это правило сам не проверяет, поэтому проверь сумму перед изменением.

## Изменить осколки или золото

Осколки хранятся у конкретной карточки:

```sh
python db_tool.py update public.player_cards --where player_id=123456789 --where template_id=sanrio_badtz_common --set shards=20
```

Золото хранится в `players.cash`:

```sh
python db_tool.py update public.players --where id=123456789 --set cash=5000
```

Чтобы именно прибавить сумму, сначала посмотри текущее значение через `show`, сложи его с нужной суммой и запиши результат через `update`.

## Переименовать игрока

```sh
python db_tool.py update public.players --where id=123456789 --set name=НовоеИмя
```

## Удалить карточку или игрока

Удалить одну карточку у игрока:

```sh
python db_tool.py delete public.player_cards --where player_id=123456789 --where template_id=sanrio_badtz_common
```

Удалить игрока вместе с его карточками:

```sh
python db_tool.py delete public.players --where id=123456789
```

В этой схеме карточки удалятся каскадно вместе с игроком.

## Перед `update` и `delete`

Команды выполняются сразу. Если условия совпали с несколькими строками, изменятся или удалятся все они. Сначала проверь выборку через `show`, а для карточки указывай оба условия: `player_id` и `template_id`.
