import re
import uuid

path = r"d:\Project\backend\tessera-kt\src\test\kotlin\com\aquinofroilan\tessera\service\TaxRateServiceTest.kt"
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace all occurrences of short string literals that represent IDs
def repl(m):
    s = m.group(1)
    if "other-org" in s or "tr-" in s or "tg-" in s:
        u = str(uuid.uuid3(uuid.NAMESPACE_DNS, s))
        return f'java.util.UUID.fromString("{u}")'
    return m.group(0)

# 1. method calls with "tr-*" or "tg-*"
content = re.sub(r'"((?:tr|tg|other)[^"]*)"', repl, content)

# But we also have TaxGroup(..., taxRateIds = listOf(...), ...)
# The above repl should catch the strings inside listOf("tr-1") as well.

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
