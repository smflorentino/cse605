#!/bin/bash
sudo perf stat -e LLC-loads -e LLC-loads -e dTLB-loads -e iTLB-loads -a ./matscope "a"
sudo perf stat -e LLC-loads -e LLC-loads -e dTLB-loads -e iTLB-loads -a ./matheapCMR "b"
sudo perf stat -e LLC-loads -e LLC-loads -e dTLB-loads -e iTLB-loads -a ./matheapHF "c"
