#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int testUnlink() {
  int i, fd; 
  int fdMap[5];
  
  if ( (fd = creat("unlinkTest.txt")) == -1) {
      printf("Unable to create file unlinkTest.txt\n");
      return -1;
  }
  
  char writeString[] = "Test file for unlink()\n";
  write(fd, writeString, sizeof(writeString));
  /*
  for (i = 0; i < 5; i++) {
    printf("-----------------Opening file %d-------------------\n", i);
    if ( (fdMap[i] = open("unlinkTest.txt")) == -1) {
      printf("Unable to open file %d\n", i);
    }
    if (i >= 1)
      //printf("unlink() return %d\n", unlink("unlinkTest.txt"));
      unlink("unlinkTest.txt");
  }
  
  for (i = 0; i < 5; i++) {
    printf ("-----------------Closing file %d-------------------\n", i);
    close(fdMap[i]);
    //unlink("unlinkTest.txt");
  }
  */
  close(fd);
  return 0;
}

int testCreate16(){
  // SCANF doesn't work
 
  int i, fd; 
  int fdMap[16];
  char fileName[7];
  for (i = 0; i < 16; i++) {
    sprintf(fileName, "%d.test", i);
    if ( (fd = creat(fileName)) == -1) {
      printf("Unable to create file %d\n", i);
    }
    
    fdMap[i] = fd;
  }
  
  for (i = 0; i < 16; i++) {
    printf("close() return %d\n", close(fdMap[i]));
  }
  
  return 0;
}

int testClose() {
  char temp[10];
  //scanf("%9s", temp);
  //printf("%s\n", temp);
  int *testFd = null;
  printf("%d\n", testFd);
  printf("close() return %d\n", close(*testFd));
  return 0;
}

int testCreatWrite() {
  int fd1, fd2;
  if ( (fd1 = creat("AAATestResult.txt")) == -1) {
    //printf("Unable to open %s\n", argv[1]);
    return -1;
  }

    if ( (fd2 = creat("BBBTestResult.txt")) == -1) {
    //printf("Unable to open %s\n", argv[1]);
    return -1;
  }

  char writeText[] = "File Created\n";
  write(fd1, writeText, strlen(writeText));
  // test for oversize writting (write 20 bytes but we don't have 20 bytes string)
  char temp[20] = "                   "; //19 spaces
  sprintf (temp, "sizeof=%d\n", sizeof(writeText));
  write(fd1, temp, sizeof(temp));
  sprintf (temp, "strlen=%d\n", strlen(writeText));
  write(fd1, temp, sizeof(temp));
  close(fd1);
  close(fd2);

}

int tesetRead() {
  int fd2, fd3;
  if ( (fd2 = creat("BBBTestResult.txt")) == -1) {
    return -1;
  }
  char buf[2000];
  if ( (fd3 = open("bad_url.txt")) == -1) {
    return -1;
  }
  else {
    read(fd3, buf, sizeof(buf));
    write(fd2, buf, sizeof(buf));
  }

  close(fd2);
  close(fd3);

  // test if we read something not open
  /*
  int error = read(fd2, buf, sizeof(buf));
  if (error == -1)
    creat("-1.txt");
  */
}

int main(int argc, char** argv)
{
    //test what is argc
  /*
  char temp[10];
  sprintf (temp, "%d.txt", argc);
  open(temp);
  */
  /*
  if (argc != 2) {
    //printf("Usage: Require One Argument <file>\n");
    creat("Error.txt");
    return 1;
  }
  */
  //testCreatWrite();
  testUnlink();
  //testCreate16();

  return 0;
}
