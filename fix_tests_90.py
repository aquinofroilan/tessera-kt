import os
import re
import uuid

def string_to_uuid(s):
    return str(uuid.uuid5(uuid.NAMESPACE_DNS, s))

test_dir = 'd:/Project/backend/tessera-kt/src/test/kotlin/com/aquinofroilan/tessera'

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Replace specific known ID strings
    def replace_id(match):
        mock_id = match.group(1)
        new_uuid = string_to_uuid(mock_id)
        # If it's inside an isEqualTo or value assertion for JSON, keep it as string?
        # Let's just always convert it to UUID, and we can fix the runtime assertions later.
        return f'java.util.UUID.fromString("{new_uuid}")'

    new_content = re.sub(r'"([a-z]+-\d+)"', replace_id, content)

    # Common Mockito fixes
    new_content = re.sub(r'any\(String::class\.java\)', r'any(java.util.UUID::class.java)', new_content)
    new_content = re.sub(r'eq\(String::class\.java\)', r'eq(java.util.UUID::class.java)', new_content)

    # Fix mock helper method signatures
    # e.g. organizationId: String = orgId -> organizationId: java.util.UUID = orgId
    new_content = re.sub(r'(\w+Id):\s*String\s*=', r'\1: java.util.UUID =', new_content)
    
    # Fix variables that were explicitly typed as String
    new_content = re.sub(r'val\s+(\w+Id):\s*String\s*=', r'val \1: java.util.UUID =', new_content)

    # Write back if changed
    if new_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)

for root, _, files in os.walk(test_dir):
    for file in files:
        if file.endswith('.kt'):
            process_file(os.path.join(root, file))

print("Done fixing tests!")
