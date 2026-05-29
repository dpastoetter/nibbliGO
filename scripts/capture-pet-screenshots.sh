#!/usr/bin/env bash
# Back-compat wrapper — captures pet + README screenshots.
exec "$(cd "$(dirname "$0")" && pwd)/capture-readme-screenshots.sh" "$@"
