import sys
content = open(sys.argv[1]).read()

start = content.find("<<<<<<< HEAD")
end = content.find(">>>>>>> 13c876d", start)
end = content.find("\n", end) + 1

replacement = "                isLotTracked = request.isLotTracked ?: false,\n                isSerialized = request.isSerialized,\n"

content = content[:start] + replacement + content[end:]
open(sys.argv[1], 'w').write(content)
