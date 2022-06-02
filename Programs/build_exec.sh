#!/bin/bash

if [ $# -eq 0 ]; then
  echo "No arguments supplied"
  exit 1
fi

source=$1
output=${source::(-1)}o
binary=${source::(-2)}

as $source -o $output
ld $output -o $binary
echo "executing"
./$binary
echo "result: $(echo $?)"
