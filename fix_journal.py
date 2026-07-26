import re

path = r"d:\Project\backend\tessera-kt\src\test\kotlin\com\aquinofroilan\tessera\service\JournalEntryServiceTest.kt"
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

def repl(m):
    s = m.group(1)
    if len(s) == 36 and s.count('-') == 4:
        return f'java.util.UUID.fromString("{s}")'
    return m.group(0)

# Wrap any naked UUID strings
content = re.sub(r'(?<!fromString\()"([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})"', repl, content)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
