#Calcuates size of the heap needed for various things
import sys
rows = int(sys.argv[1])
cols = int(sys.argv[2])
elemSize = int(sys.argv[3])
arrayCount = int(sys.argv[4])
print "Array Elements: %d bytes\n" % (rows * cols)
print "Array Element Size: %d bytes\n" % elemSize
print "Space Needed per Array: %d bytes\n" % ((rows* cols) * elemSize)
print "Space Needed %d\n" % ((rows* cols) * elemSize * arrayCount)
