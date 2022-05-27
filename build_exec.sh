#!/bin/bash

source=$1
output=${source::(-1)}o
binary=${source::(-2)}

as $source -o $output
ld $output -o $binary
echo "executing"
./$binary
echo ""
echo "result: $(echo $?)"
