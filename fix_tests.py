import re
import os
import sys

def parse_log_and_fix(log_path):
    with open(log_path, 'r', encoding='utf-8') as f:
        log_content = f.read()

    # Regex for kotlin compiler error
    error_pattern = re.compile(r'e: file:///(.+?):(\d+):(\d+) (.*)')
    
    file_fixes = {}
    
    for match in error_pattern.finditer(log_content):
        file_path = match.group(1).replace('/', '\\')
        line_num = int(match.group(2))
        col_num = int(match.group(3))
        msg = match.group(4)
        
        if file_path not in file_fixes:
            file_fixes[file_path] = []
        file_fixes[file_path].append({'line': line_num, 'col': col_num, 'msg': msg})

    # Read each file and apply fixes
    for file_path, fixes in file_fixes.items():
        if not os.path.exists(file_path):
            continue
            
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()
            
        # Group fixes by line number to avoid shifting issues when we replace things, 
        # though we'll just do basic replacements per line.
        # But wait, applying regex per line might be easier if we just look for patterns.
        
        # A simpler approach: Just scan through the file and replace specific patterns blindly or based on line errors.
        # Let's apply regex to the whole file instead, because many errors are similar.
        
        content = "".join(lines)
        
        # Common fixes in Kotlin tests:
        # 1. `id: String = java.util.UUID.fromString(...)` -> `id: UUID = java.util.UUID.fromString(...)`
        # 2. `parentId: String? = null` -> `parentId: UUID? = null` (if we know it's an ID)
        content = re.sub(r'(\b\w*id|Id):\s*String(\?)?\s*=\s*(java\.util\.)?UUID', r'\1: UUID\2 = \3UUID', content)
        content = re.sub(r'(\b\w*id|Id):\s*String(\?)?\s*=\s*null', r'\1: UUID\2 = null', content)
        
        # 3. `val \w+Id\s*=\s*"[^"]+"` -> `val \w+Id = java.util.UUID.fromString("...")`
        # wait, we need to be careful not to replace valid strings. 
        # Only replace UUID-like strings.
        
        # Find UUID string literals and wrap them in java.util.UUID.fromString()
        # Pattern for UUID string
        uuid_pattern = r'"([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})"'
        # If it's already wrapped, don't wrap it.
        # We can just replace all UUID string literals not preceded by fromString(
        content = re.sub(r'(?<!fromString\()' + uuid_pattern, r'java.util.UUID.fromString("\1")', content)

        # 4. In mock functions, params like `productId: String` that are supposed to be UUID
        # We can look at the errors. If an error says `actual type is 'UUID!', but 'String' was expected.` 
        # or `expected 'String', actual 'UUID!'`, we know the variable or parameter type is wrong.
        
        # 5. Fix type declarations like `List<String>` to `List<UUID>` if they are lists of IDs.
        
        # Let's write the modified content back
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
            
    print(f"Applied heuristic fixes to {len(file_fixes)} files.")

if __name__ == "__main__":
    parse_log_and_fix(sys.argv[1])
