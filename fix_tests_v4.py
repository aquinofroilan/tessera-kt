import re
import os
import sys
import uuid

def string_to_uuid_str(s):
    if re.match(r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$', s):
        return s
    return str(uuid.uuid3(uuid.NAMESPACE_DNS, s))

def fix_errors(log_path):
    with open(log_path, 'r', encoding='utf-8') as f:
        log_content = f.read()

    error_pattern = re.compile(r'e: file:///(.+?):(\d+):(\d+) (.*)')
    
    files_to_fix = {}
    
    for match in error_pattern.finditer(log_content):
        file_path = match.group(1).replace('/', '\\')
        line_num = int(match.group(2))
        msg = match.group(4)
        
        if file_path not in files_to_fix:
            files_to_fix[file_path] = []
        files_to_fix[file_path].append((line_num, msg))

    for file_path, errors in files_to_fix.items():
        if not os.path.exists(file_path):
            continue
            
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()
            
        needs_uuid_import = False
        
        for line_num, msg in errors:
            idx = line_num - 1
            if idx >= len(lines):
                continue
                
            line = lines[idx]
            
            if "Unresolved reference 'UUID'" in msg or "Unresolved reference: UUID" in msg:
                needs_uuid_import = True
                
            elif "String" in msg and "UUID" in msg:
                # 1. Type declarations `Id: String` or `id: String?` -> `UUID`
                # e.g. "actual type is 'UUID!', but 'String' was expected."
                line = re.sub(r'([iI]d)\s*:\s*String(\?)?', r'\1: UUID\2', line)
                
                # 2. String literal passed to an ID argument
                # E.g., `createTaxRate("tr-a", ...)`
                # We can wrap any short string literals on this line that look like IDs.
                # To be safe, we only wrap string literals that don't contain spaces and are not purely digits (unless very short).
                def repl_str(match):
                    s = match.group(1)
                    if len(s) == 36 and s.count('-') == 4:
                        return match.group(0)
                    if " " in s or "." in s:
                        return match.group(0)
                    # Don't replace things like 'MAIN' unless they are on a line with type mismatch
                    # Actually, if there is a type mismatch, it's highly likely this string is the culprit.
                    u = string_to_uuid_str(s)
                    return f'java.util.UUID.fromString("{u}")'
                
                # Wait! We need to avoid replacing `name = "MAIN"` on the same line.
                # So we ONLY replace strings that are assigned to an ID, or are part of `listOf(...)`.
                # Or just strings that are passed as arguments where parameter is unnamed (like `createTaxRate("tr-a")`).
                # Let's write specific regexes.
                
                # A) `somethingId = "..."`
                line = re.sub(r'([a-zA-Z0-9_]*[iI]d\w*)\s*=\s*"([^"]+)"', 
                              lambda m: f'{m.group(1)} = java.util.UUID.fromString("{string_to_uuid_str(m.group(2))}")', 
                              line)
                
                # B) `listOf("...", "...")`
                def repl_list(m):
                    args = m.group(1)
                    new_args = re.sub(r'"([^"]+)"', repl_str, args)
                    return f'listOf({new_args})'
                line = re.sub(r'listOf\((.*?)\)', repl_list, line)
                
                # C) `== "..."`
                line = re.sub(r'==\s*"([^"]+)"', lambda m: f'== java.util.UUID.fromString("{string_to_uuid_str(m.group(1))}")', line)

                # D) Unnamed string arguments in function calls, if the string looks like an ID
                # We match `("id-string"` or `, "id-string"`
                # But this is tricky. Let's just do `repl_str` for ALL strings on the line IF it's not assigned to a known non-ID parameter.
                # Let's replace any `"..."` that is NOT preceded by `name = ` or `code = ` or `description = `
                def safe_repl_str(match):
                    s = match.group(1)
                    start = match.start()
                    prefix = line[max(0, start-15):start]
                    
                    if "fromString(" in prefix or "BigDecimal(" in prefix:
                        return match.group(0)
                        
                    if re.search(r'(name|code|description|type|status)\s*=\s*$', prefix):
                        return match.group(0)
                        
                    if len(s) == 36 and s.count('-') == 4:
                        return match.group(0)
                    if " " in s or "." in s:
                        return match.group(0)
                        
                    u = string_to_uuid_str(s)
                    return f'java.util.UUID.fromString("{u}")'
                    
                line = re.sub(r'"([^"]+)"', safe_repl_str, line)
                
                # Fix list declarations
                line = re.sub(r'List<String>', r'List<UUID>', line)
                line = re.sub(r'Map<String,', r'Map<UUID,', line)
                line = re.sub(r'Iterable<String>', r'Iterable<UUID>', line)
                line = re.sub(r'Set<String>', r'Set<UUID>', line)

            lines[idx] = line
            
        if needs_uuid_import:
            for i, line in enumerate(lines):
                if line.startswith('package '):
                    lines.insert(i + 1, '\nimport java.util.UUID\n')
                    break
                    
        with open(file_path, 'w', encoding='utf-8') as f:
            f.writelines(lines)
            
    print(f"Applied fixes to {len(files_to_fix)} files.")

if __name__ == "__main__":
    fix_errors(sys.argv[1])
