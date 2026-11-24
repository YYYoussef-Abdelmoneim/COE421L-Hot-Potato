#!/bin/zsh
# Builds and starts the game. Needs Java 11 or newer.
cd -- "$(dirname -- "$0")" || exit 1
mkdir -p classes
if ! javac -cp 'lib/*' -d classes src/*.java; then
  read -r '?The code did not compile. Read the error above, then press Enter.'
  exit 1
fi
java -cp 'classes:lib/*' Main
