# PostgreSQL DB Tool

Неинтерактивная CLI-утилита для просмотра таблиц и управления строками PostgreSQL. Таблицы и их столбцы утилита получает из подключённой базы — привязки к структуре проекта нет.

## Установка и подключение

Из папки `db-tool`:

```sh
python -m pip install -r requirements.txt
python db_tool.py --help
```

Утилита ищет `.env` в корне проекта. Подключение также можно задать переменными окружения, они имеют приоритет:

```text
DB_HOST=localhost
DB_PORT=5432
DB_NAME=multiversecards
DB_USER=postgres
DB_PASSWORD=your-password
```

Для другой базы укажите её параметры в `.env` или окружении и запустите команду заново.

## Команды

Команды ниже запускаются из папки `db-tool`. Таблицу можно указать как `players` (схема `public`) или полным именем `public.players`.

### Список таблиц

```sh
python db_tool.py tables
```

### Столбцы таблицы

Показывает имена столбцов, типы, возможность `NULL` и значения по умолчанию:

```sh
python db_tool.py describe public.players
```

### Просмотр строк

```sh
# Первые 50 строк
python db_tool.py show public.players

# До 20 строк
python db_tool.py show public.players --limit 20

# Найти игрока по ID
python db_tool.py show public.players --where id=5400596056

# Несколько условий объединяются через AND
python db_tool.py show public.players --where name=Agaha --where cash=0

# Пропустить первые 20 строк и вывести следующие 20
python db_tool.py show public.players --limit 20 --offset 20
```

### Добавление строки: `insert`

Каждый столбец задаётся отдельным `--set column=value`. Для таблицы `players` можно добавить игрока так:

```sh
python db_tool.py insert public.players --set id=987654321 --set name=NewPlayer --set cash=0
```

Столбцы с настройками по умолчанию можно пропустить. Например, `starter_set_claimed` получит значение по умолчанию `false`. Строки с пробелами заключайте в кавычки:

```sh
python db_tool.py insert public.players --set id=987654322 --set "name=New Player"
```

### Изменение строки: `update`

`--set` задаёт новые значения, `--where` выбирает строки для изменения:

```sh
# Переименовать игрока с указанным ID
python db_tool.py update public.players --where id=5400596056 --set name=AgahaNew

# Изменить несколько столбцов одной строки
python db_tool.py update public.players --where id=5400596056 --set name=Agaha --set cash=100
```

### Удаление строки: `delete`

```sh
python db_tool.py delete public.players --where id=5400596056
```

## Формат значений и важное про изменения

- `--where` и `--set` можно повторять. Несколько условий `--where` объединяются через `AND`.
- Условия используют точное совпадение: `--where name=Agaha`.
- Для SQL `NULL` укажите `NULL` без кавычек: `--set name=NULL` или `--where name=NULL`.
- Условия `--where` обязательны для `update` и `delete`. Проверьте их через `show` перед изменением.
- Команды `insert`, `update` и `delete` выполняются сразу, без подтверждения. Если условие совпадёт с несколькими строками, изменятся или удалятся все совпавшие строки.
