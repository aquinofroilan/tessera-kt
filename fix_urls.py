import os
import re

dirs = ['src/test/kotlin/com/aquinofroilan/tessera/controller']
replacements = {
    'acc-123': '12a14436-99e0-5e9d-9396-3a670fc505c0',
    'wh-123': '22a14436-99e0-5e9d-9396-3a670fc505c0',
    'nonexistent': '00000000-0000-0000-0000-000000000000',
    'key-123': '32a14436-99e0-5e9d-9396-3a670fc505c0',
    'rr-1': '42a14436-99e0-5e9d-9396-3a670fc505c0',
    'inv-123': '52a14436-99e0-5e9d-9396-3a670fc505c0'
}

for root, _, files in os.walk(dirs[0]):
    for file in files:
        if file.endswith('.kt'):
            path = os.path.join(root, file)
            with open(path, 'r', encoding='utf-8') as f:
                content = f.read()
            original = content
            for k, v in replacements.items():
                content = re.sub(r'(/[^/]+)' + f'/{k}', r'\1' + f'/{v}', content)
                content = content.replace(f'"{k}"', f'"{v}"')
            if original != content:
                with open(path, 'w', encoding='utf-8') as f:
                    f.write(content)
                print(f'Updated {path}')
