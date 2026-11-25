# PostgreSQL DB Tool

Простая CLI-утилита для просмотра таблиц и изменения строк в PostgreSQL. Она читает схемы и столбцы из подключённой базы, поэтому не привязана к конкретному проекту.

## Установка

Нужен Python 3.10+ и пакет Psycopg 3:

```sh
python -m pip install -r requirements.txt
```

## Подключение

Для подключения можно задать стандартные переменные PostgreSQL:

```text
PGHOST=localhost
PGPORT=5432
PGDATABASE=my_database
PGUSER=my_user
PGPASSWORD=my_password
```

Также поддерживаются короткие имена `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`. Если заданы обе группы, приоритет у `PG*`. Все параметры необязательные: если их не задать, Psycopg/libpq возьмёт свои стандартные значения, включая `PGSERVICE` и локальную авторизацию без пароля.

Настройки можно положить в `.env`. Утилита ищет его в текущей папке, рядом с `db_tool.py` и на уровень выше. Путь к конкретному файлу можно передать явно:

```sh
python db_tool.py --env-file ../.env tables
```

Переменные окружения имеют приоритет над значениями из `.env`. При работе с другой базой достаточно поменять параметры подключения.

## Команды

Все команды запускаются из папки с `db_tool.py`. Таблицу можно указать коротко — тогда используется схема `public` — или полностью, например `sales.orders`.

### Таблицы и столбцы

```sh
python db_tool.py tables
python db_tool.py describe public.some_table
```

`tables` показывает таблицы и представления. `describe` выводит столбцы, их типы, возможность `NULL` и значения по умолчанию.

### Просмотр строк

```sh
# Первые 50 строк
python db_tool.py show public.some_table

# Ограничить вывод и пропустить первые строки
python db_tool.py show public.some_table --limit 20 --offset 40

# Найти запись по точному значению
python db_tool.py show public.some_table --where id=123

# Несколько условий соединяются через AND
python db_tool.py show public.some_table --where status=active --where owner_id=123
```

### Добавить строку

Каждый столбец задаётся отдельным `--set column=value`. Столбцы с DEFAULT можно пропустить:

```sh
python db_tool.py insert public.some_table --set name=Example --set status=active
```

Если в значении есть пробелы, заключи весь аргумент в кавычки:

```sh
python db_tool.py insert public.some_table --set "name=Example row"
```

### Изменить строку

```sh
python db_tool.py update public.some_table --where id=123 --set status=active

# Изменить несколько столбцов одной или нескольких строк
python db_tool.py update public.some_table --where status=waiting --set status=ready --set attempts=0
```

### Удалить строки

```sh
python db_tool.py delete public.some_table --where id=123
```

## Значения и изменения

- `--where` и `--set` можно повторять
- Условия `--where` ищут точное совпадение; несколько условий объединяются через `AND`
- Чтобы записать SQL `NULL`, укажи `NULL` без кавычек
- Для `update` и `delete` нужно указать хотя бы одно условие
- Перед изменением удобно сначала выполнить такую же команду `show`
- Изменения применяются сразу. Если условие совпало с несколькими строками, изменятся или удалятся все они
- Значения передаются в PostgreSQL параметрами, а имена таблиц и столбцов проверяются по схеме базы

Примеры для таблиц Multiverse Cards лежат отдельно в [USAGE.md](USAGE.md).
