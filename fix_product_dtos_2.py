import sys
content = open(sys.argv[1]).read()

start = content.find("<<<<<<< HEAD")
end = content.find(">>>>>>> f2e5b38", start)
end = content.find("\n", end) + 1

replacement = "    val isSerialized: Boolean? = false,\n"

content = content[:start] + replacement + content[end:]
open(sys.argv[1], 'w').write(content)
