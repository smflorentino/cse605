###################################################################################
# CSE 605: Advanced Concepts in Programming Languages(Spring 2015)
#
# Title:    malloc() and free() in Scoped Memory
#
# Problem:  Misbehavior by nodes is a common attack in networks involving the 802.11 
#           MAC Protocol. By misbehaving we mean nodes which do not adhere to the 
#           rules of the 802.11 protocol. The aim of this project is to see how 
#           changing contention window by nodes affects throughput and delay. 
#
# Authors:  Scott Florentino (scottflo)
#
#           parse.py
#           A simple parser for benchmark files
#
###################################################################################
import re, csv, sys, ast, argparse
import matplotlib.pyplot as plt

### Set up Program Arguments ###
parser = argparse.ArgumentParser(description="A Simple Parser for CSE605 Benchmarking files")
# parser.add_argument("gctype", type=int, help="the GC type of the simulation")
parser.add_argument("unmanaged", type=str, help="the unmanaged memory file to parse")
parser.add_argument("cmr", type=str, help="the cmr memory file to parse")
parser.add_argument("hf", type=str, help="the hf memory file to parse")
parser.add_argument("graphtype", type=int, help="the graphing mode you want to use")
args = parser.parse_args()

"""
The graph type to use.
	0: Don't graph
	1: Graph Execution Time
"""
graphtype = 0;