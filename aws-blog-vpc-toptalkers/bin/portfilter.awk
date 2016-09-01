#!/bin/awk -f

BEGIN { FS = "[:|\t]" }
{ if($3<=1024) print $0 }
