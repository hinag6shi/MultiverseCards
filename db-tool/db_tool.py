#!/usr/bin/env python3
"""Command-line PostgreSQL table browser and editor."""

import argparse
import os
import sys
from pathlib import Path

try:
    import psycopg
    from psycopg import sql
    from psycopg.rows import dict_row
except ImportError:
    print("Missing dependency. Run: python -m pip install -r db-tool/requirements.txt", file=sys.stderr)
    raise SystemExit(1)


def load_dotenv():
    env_file = Path(__file__).resolve().parent.parent / ".env"
    if not env_file.exists():
        return
    for line in env_file.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        os.environ.setdefault(key.strip(), value.strip().strip("\"'"))


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
        raise RuntimeError("Set DB_NAME, DB_USER and DB_PASSWORD in .env or environment variables.")
    return psycopg.connect(**config, row_factory=dict_row)


def relation_name(value):
    parts = value.split(".")
    if len(parts) == 1 and parts[0]:
        return sql.Identifier("public", parts[0])
    if len(parts) == 2 and all(parts):
        return sql.Identifier(*parts)
    raise ValueError("Table must be written as table or schema.table")


def relation_parts(value):
    parts = value.split(".")
    if len(parts) == 1 and parts[0]:
        return "public", parts[0]
    if len(parts) == 2 and all(parts):
        return parts
    raise ValueError("Table must be written as table or schema.table")


def read_relations(conn):
    with conn.cursor() as cur:
        cur.execute("""
            SELECT n.nspname AS schema, c.relname AS name,
                   CASE c.relkind WHEN 'r' THEN 'table' WHEN 'p' THEN 'partitioned table'
                        WHEN 'v' THEN 'view' WHEN 'm' THEN 'materialized view'
                        ELSE 'foreign table' END AS type
            FROM pg_catalog.pg_class c
            JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
            WHERE c.relkind IN ('r', 'p', 'v', 'm', 'f')
              AND n.nspname <> 'information_schema'
              AND n.nspname NOT LIKE 'pg_%'
            ORDER BY n.nspname, c.relname
        """)
        return cur.fetchall()


def get_columns(conn, table):
    schema, name = relation_parts(table)
    with conn.cursor() as cur:
        cur.execute("""
            SELECT column_name, data_type, is_nullable, column_default
            FROM information_schema.columns
            WHERE table_schema = %s AND table_name = %s
            ORDER BY ordinal_position
        """, (schema, name))
        columns = cur.fetchall()
    if not columns:
        raise ValueError(f"Table or view not found: {schema}.{name}")
    return columns


def parse_assignments(items, option):
    result = {}
    for item in items:
        if "=" not in item:
            raise ValueError(f"{option} expects column=value, got {item!r}")
        column, value = item.split("=", 1)
        if not column or column in result:
            raise ValueError(f"Invalid or repeated column in {option}: {column!r}")
        result[column] = None if value.upper() == "NULL" else value
    return result


def validate_columns(columns, assignments):
    known = {column["column_name"] for column in columns}
    unknown = set(assignments) - known
    if unknown:
        raise ValueError("Unknown column(s): " + ", ".join(sorted(unknown)))


def make_where(conditions):
    if not conditions:
        raise ValueError("Specify at least one --where column=value condition")
    clauses = []
    values = []
    for column, value in conditions.items():
        identifier = sql.Identifier(column)
        if value is None:
            clauses.append(sql.SQL("{} IS NULL").format(identifier))
        else:
            clauses.append(sql.SQL("{} = %s").format(identifier))
            values.append(value)
    return sql.SQL(" AND ").join(clauses), values


def print_rows(rows):
    if not rows:
        print("No rows.")
        return
    columns = list(rows[0].keys())
    widths = {name: min(36, max(len(name), *(len(str(row[name]).replace("\n", "\\n")) for row in rows)))
              for name in columns}
    print(" | ".join(name.ljust(widths[name]) for name in columns))
    print("-+-".join("-" * widths[name] for name in columns))
    for row in rows:
        cells = []
        for name in columns:
            value = "NULL" if row[name] is None else str(row[name]).replace("\n", "\\n")
            cells.append((value if len(value) <= 36 else value[:35] + "…").ljust(widths[name]))
        print(" | ".join(cells))


def build_parser():
    parser = argparse.ArgumentParser(description="Browse and edit a PostgreSQL database.")
    commands = parser.add_subparsers(dest="command", required=True)
    commands.add_parser("tables", help="list tables and views")

    describe = commands.add_parser("describe", help="show columns of a table")
    describe.add_argument("table")

    show = commands.add_parser("show", help="show rows")
    show.add_argument("table")
    show.add_argument("--limit", type=int, default=50)
    show.add_argument("--offset", type=int, default=0)
    show.add_argument("--where", action="append", default=[], metavar="COL=VALUE")

    delete = commands.add_parser("delete", help="delete rows matching conditions")
    delete.add_argument("table")
    delete.add_argument("--where", action="append", required=True, metavar="COL=VALUE")

    update = commands.add_parser("update", help="update rows matching conditions")
    update.add_argument("table")
    update.add_argument("--where", action="append", required=True, metavar="COL=VALUE")
    update.add_argument("--set", action="append", required=True, metavar="COL=VALUE")

    insert = commands.add_parser("insert", help="insert a row")
    insert.add_argument("table")
    insert.add_argument("--set", action="append", required=True, metavar="COL=VALUE")
    return parser


def run(conn, args):
    if args.command == "tables":
        for relation in read_relations(conn):
            print(f"{relation['schema']}.{relation['name']}\t{relation['type']}")
        return

    relation = relation_name(args.table)
    columns = get_columns(conn, args.table)
    if args.command == "describe":
        print("column\ttype\tnullable\tdefault")
        for column in columns:
            print(f"{column['column_name']}\t{column['data_type']}\t{column['is_nullable']}\t{column['column_default'] or ''}")
        return

    with conn.cursor() as cur:
        if args.command == "show":
            if args.limit < 1 or args.offset < 0:
                raise ValueError("--limit must be positive and --offset cannot be negative")
            conditions = parse_assignments(args.where, "--where")
            validate_columns(columns, conditions)
            query = sql.SQL("SELECT * FROM {} ").format(relation)
            values = []
            if conditions:
                where, values = make_where(conditions)
                query += sql.SQL("WHERE ") + where
            query += sql.SQL(" LIMIT %s OFFSET %s")
            cur.execute(query, (*values, args.limit, args.offset))
            print_rows(cur.fetchall())
        elif args.command == "delete":
            conditions = parse_assignments(args.where, "--where")
            validate_columns(columns, conditions)
            where, values = make_where(conditions)
            cur.execute(sql.SQL("DELETE FROM {} WHERE ").format(relation) + where, values)
            print(f"Deleted {cur.rowcount} row(s).")
        elif args.command == "update":
            conditions = parse_assignments(args.where, "--where")
            changes = parse_assignments(args.set, "--set")
            validate_columns(columns, {**conditions, **changes})
            where, where_values = make_where(conditions)
            set_sql = sql.SQL(", ").join(
                sql.SQL("{} = %s").format(sql.Identifier(column)) for column in changes
            )
            cur.execute(
                sql.SQL("UPDATE {} SET ").format(relation) + set_sql + sql.SQL(" WHERE ") + where,
                (*changes.values(), *where_values),
            )
            print(f"Updated {cur.rowcount} row(s).")
        elif args.command == "insert":
            values = parse_assignments(args.set, "--set")
            validate_columns(columns, values)
            names = list(values)
            cur.execute(
                sql.SQL("INSERT INTO {} ({}) VALUES ({})").format(
                    relation,
                    sql.SQL(", ").join(map(sql.Identifier, names)),
                    sql.SQL(", ").join(sql.Placeholder() for _ in names),
                ),
                list(values.values()),
            )
            print("Inserted 1 row.")


def main():
    args = build_parser().parse_args()
    try:
        conn = connect()
        try:
            with conn:
                run(conn, args)
        finally:
            conn.close()
    except (psycopg.Error, OSError, RuntimeError, ValueError) as exc:
        print(f"Error: {exc}", file=sys.stderr)
        raise SystemExit(1)


if __name__ == "__main__":
    main()
