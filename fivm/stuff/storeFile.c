#include <stdio.h>
#include <fcntl.h>

void storeFile(const char *filename,const char *data,size_t len) {
    int fd=open(filename,O_WRONLY|O_CREAT|O_TRUNC,0644);
    if (fd<0) {
	printk("could not open file: %s\n",filename);
	abort();
    }
    
    while (len>0) {
	size_t toWrite=1024;
	int res;
	if (toWrite>len) {
	    toWrite=len;
	}
	res=write(fd,data,toWrite);
	if (res<=0) {
	    printk("could not write to file: %s\n",filename);
	    abort();
	}
	data+=res;
	len-=res;
    }
    
    close(fd);
}


