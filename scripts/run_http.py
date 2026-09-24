#!/usr/bin/env python3
"""`.http` 실행 케이스를 재연하고 @expect와 @db를 대조한다 (/verify-http 3, 4단계).

사용: python3 scripts/run_http.py http/{name}.http
- 서버(localhost)와 PostgreSQL 컨테이너가 떠 있어야 한다
- DB 접속은 환경 변수로 바꾼다: PG_CONTAINER, PG_USER, PG_PASSWORD, PG_DATABASE
- 멀티파트 요청은 지원하지 않는다 — 해당 요청은 curl -F로 따로 확인한다
- 불일치가 있으면 종료 코드 1
"""
import json
import os
import re
import subprocess
import sys

# -tA: 헤더 없이 값만, -F 탭: 여러 컬럼은 탭으로 구분한다
PSQL = [
    "docker", "exec", "-e", "PGPASSWORD=" + os.environ.get("PG_PASSWORD", "app"),
    os.environ.get("PG_CONTAINER", "inventory-system-postgres"), "psql", "-h", "localhost",
    "-U", os.environ.get("PG_USER", "app"), "-d", os.environ.get("PG_DATABASE", "inventory-system"),
    "-tA", "-F", "\t", "-v", "ON_ERROR_STOP=1", "-c",
]


def parse(path):
    text = open(path, encoding="utf-8").read()
    host = re.search(r"^@host\s*=\s*(\S+)", text, re.M).group(1)
    blocks = re.split(r"^### ", text, flags=re.M)[1:]
    reqs = []
    for b in blocks:
        lines = b.split("\n")
        m = re.match(r"\[([^\]]+)\]\s*(.*)", lines[0])
        rid, title = m.group(1), m.group(2)
        uses, expects, dbs = [], [], []
        i = 1
        while i < len(lines) and (lines[i].startswith("#") or not lines[i].strip()):
            c = lines[i]
            if c.startswith("# @uses"):
                um = re.match(r"# @uses\s+(\w+)\s*=\s*\[([^\]]+)\]\.\$\.(\S+)", c)
                uses.append(um.groups())
            elif c.startswith("# @expect"):
                expects.append(c[len("# @expect"):].strip())
            elif c.startswith("# @db"):
                sql, exp = c[len("# @db"):].rsplit("=>", 1)
                dbs.append((sql.strip(), exp.strip()))
            i += 1
        method, url = lines[i].split(None, 1)
        i += 1
        headers = []
        while i < len(lines) and lines[i].strip():
            headers.append(lines[i].strip())
            i += 1
        body = "\n".join(lines[i:]).strip()
        reqs.append(dict(id=rid, title=title, uses=uses, expects=expects, dbs=dbs,
                         method=method, url=url.replace("{{host}}", host), headers=headers, body=body))
    return reqs


def get_path(obj, path):
    for p in path.split("."):
        if isinstance(obj, list):
            obj = obj[int(p)]
        else:
            obj = obj[p]
    return obj


def main(path):
    responses = {}
    rows, fails = [], []
    for r in parse(path):
        vars_ = {}
        for name, ref, jp in r["uses"]:
            vars_[name] = get_path(responses[ref], jp)

        def sub(s):
            for k, v in vars_.items():
                s = s.replace("{{" + k + "}}", str(v))
            return s

        cmd = ["curl", "-s", "-w", "\n%{http_code}", "-X", r["method"], sub(r["url"])]
        for h in r["headers"]:
            cmd += ["-H", sub(h)]
        if r["body"]:
            cmd += ["--data-binary", sub(r["body"])]
        out = subprocess.run(cmd, capture_output=True, text=True).stdout
        body, status = out.rsplit("\n", 1)
        try:
            js = json.loads(body) if body.strip() else None
        except json.JSONDecodeError:
            js = None
        responses[r["id"]] = js
        problems = []
        status_ok = body_ok = db_ok = None
        for e in r["expects"]:
            if re.fullmatch(r"\d{3}", e):
                status_ok = status == e
                if not status_ok:
                    problems.append(f"status 기대 {e} / 실제 {status} body={body[:200]}")
            else:
                jp, val = [x.strip() for x in e.split("==", 1)]
                expected = json.loads(sub(val))
                try:
                    actual = get_path(js, jp[2:])
                except Exception:
                    actual = "<없음>"
                ok = actual == expected
                body_ok = ok if body_ok is None else (body_ok and ok)
                if not ok:
                    problems.append(f"{jp} 기대 {expected!r} / 실제 {actual!r}")
        if not any(re.fullmatch(r"\d{3}", e) for e in r["expects"]):
            problems.append("@expect 상태코드 없음 (작성 규칙 위반)")
        for sql, exp in r["dbs"]:
            res = subprocess.run(PSQL + [sub(sql)], capture_output=True, text=True)
            actual = res.stdout.strip()
            ok = actual == exp
            db_ok = ok if db_ok is None else (db_ok and ok)
            if not ok:
                problems.append(f"DB `{sub(sql)}` 기대 {exp} / 실제 {actual or res.stderr.strip()}")
        mark = lambda v: "—" if v is None else ("✅" if v else "❌")
        result = "❌" if problems else "✅"
        rows.append(f"| {r['id']} | {r['method']} {sub(r['url']).split('8080')[-1]} | {status} {mark(status_ok)} | {mark(body_ok)} | {mark(db_ok)} | {result} |")
        if problems:
            fails.append(f"- [{r['id']}] " + "; ".join(problems))
    print(f"## 실측 결과 — {path} ({len(rows)}건)\n")
    print("| TC | 요청 | 상태 | 본문 | DB | 결과 |\n|---|---|---|---|---|---|")
    print("\n".join(rows))
    print("\n## 불일치")
    print("\n".join(fails) if fails else "- 없음")
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
