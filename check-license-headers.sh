#!/bin/bash

FIND=find
if which gfind >/dev/null; then
  FIND=gfind
fi

$FIND -name '*.java' | (while read f; do
  if ! fgrep -q "SPDX-License-Identifier" "$f"; then
    echo "File $f does not have a license header"
    exit 1
  fi
done)
