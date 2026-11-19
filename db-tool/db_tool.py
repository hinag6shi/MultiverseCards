#!/usr/bin/env python3
"""Small interactive PostgreSQL table browser and row deleter."""

import os
import sys
from pathlib import Path

try:
    import psycopg
    from psycopg import sql
    from psycopg.rows import dict_row
except ImportError:
    print("Missing dependency. Run: python -m pip install -r db-tool/requirements.txt")
    raise SystemExit(1)


def load_dotenv():
    """Read simple KEY=VALUE entries from the project .env without overriding env vars."""
    env_file = Path(__file__).resolve().parent.parent / ".env"
    if not env_file.exists():
        return
    for line in env_file.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        value = value.strip().strip("\"'")
        os.environ.setdefault(key.strip(), value)


def connect():
    load_dotenv()
    config = {
        "host": os.getenv("DB_HOST", "localhost"),
        "port": os.getenv("DB_PORT", "5432"),
        "dbname": os.getenv("DB_NAME"),
        "user": os.getenv("DB_USER"),
        "password": os.getenv("DB_PASSWORD"),
    }
    missing = [key for key in ("dbname", "user", "password") if not config[key]]
    if missing:
        print("Set DB_NAME, DB_USER and DB_PASSWORD in .env or environment variables.")
        raise SystemExit(2)
    return psycopg.connect(**config, row_factory=dict_row)


def read_tables(conn):
    with conn.cursor() as cur:
        cur.execute("""
            SELECT n.nspname AS schema, c.relname AS name, c.relkind AS kind
            FROM pg_catalog.pg_class c
            JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
            WHERE c.relkind IN ('r', 'p', 'v', 'm', 'f')
              AND n.nspname <> 'information_schema'
              AND n.nspname NOT LIKE 'pg_%'
            ORDER BY n.nspname, c.relname
        """)
        return cur.fetchall()


def display_value(value, width=48):
    value = str(value).replace("\n", "\\n")
    return value if len(value) <= width else value[: width - 1] + "…"


def browse_table(conn, table):
    schema, name = table["schema"], table["name"]
    relation = sql.Identifier(schema, name)
    print(f"\n{schema}.{name}" + (" (read-only relation)" if table["kind"] not in ("r", "p") else ""))
    with conn.cursor() as cur:
        if table["kind"] in ("r", "p"):
            query = sql.SQL("SELECT tableoid::oid::text AS __db_tool_tableoid, ctid::text AS __db_tool_ctid, * FROM {} LIMIT 100")
        else:
            query = sql.SQL("SELECT * FROM {} LIMIT 100")
        cur.execute(query.format(relation))
        rows = cur.fetchall()
    if not rows:
        print("  (empty)")
        input("Enter to continue… ")
        return

    internal_columns = {"__db_tool_ctid", "__db_tool_tableoid"}
    columns = [key for key in rows[0] if key not in internal_columns]
    print("  #  " + " | ".join(columns))
    for i, row in enumerate(rows, 1):
        print(f"{i:>3}  " + " | ".join(display_value(row[col]) for col in columns))
    if table["kind"] not in ("r", "p"):
        input("\nEnter to continue… ")
        return

    choice = input("\nНомер строки для удаления (Enter — назад): ").strip()
    if not choice:
        return
    try:
        index = int(choice) - 1
        row = rows[index]
    except (ValueError, IndexError):
        print("Нет строки с таким номером.")
        return

    # ctid works for tables with or without a primary key and avoids assumptions
    # about a particular application's schema.
    with conn.cursor() as cur:
        cur.execute(
            sql.SQL("DELETE FROM {} WHERE tableoid = %s::oid AND ctid = %s::tid").format(relation),
            (row["__db_tool_tableoid"], row["__db_tool_ctid"]),
        )
    conn.commit()
    print("Удалено." if cur.rowcount else "Строка уже отсутствует.")


def main():
    try:
        with connect() as conn:
            print(f"Подключено к PostgreSQL: {conn.info.dbname} @ {conn.info.host}:{conn.info.port}")
            while True:
                tables = read_tables(conn)
                if not tables:
                    print("Таблиц и представлений не найдено.")
                    return
                print("\nТаблицы и представления:")
                for i, table in enumerate(tables, 1):
                    suffix = " [только просмотр]" if table["kind"] not in ("r", "p") else ""
                    print(f"{i:>3}. {table['schema']}.{table['name']}{suffix}")
                choice = input("\nНомер таблицы (Enter — выход): ").strip()
                if not choice:
                    return
                try:
                    table = tables[int(choice) - 1]
                except (ValueError, IndexError):
                    print("Нет таблицы с таким номером.")
                    continue
                browse_table(conn, table)
    except (psycopg.Error, OSError) as exc:
        print(f"Ошибка: {exc}", file=sys.stderr)
        raise SystemExit(1)


if __name__ == "__main__":
    main()
