/* Start an emulator independently of Terminal and its process group. */
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>
#include <errno.h>
int main(int argc, char **argv) {
    if (argc < 4) { fputs("Usage: portable-run LOG PROGRAM ARG...\n", stderr); return 2; }
    long maxfd = sysconf(_SC_OPEN_MAX);
    pid_t first = fork();
    if (first < 0) { perror("fork"); return 1; }
    if (first > 0) {
        int result;
        while (waitpid(first, &result, 0) < 0) { if (errno != EINTR) return 1; }
        return WIFEXITED(result) ? WEXITSTATUS(result) : 1;
    }
    if (setsid() < 0) _exit(1);
    pid_t second = fork();
    if (second < 0) _exit(1);
    if (second > 0) _exit(0);
    int log = open(argv[1], O_CREAT | O_WRONLY | O_APPEND, 0600);
    int input = open("/dev/null", O_RDONLY);
    if (log < 0 || input < 0) _exit(1);
    if (dup2(input, 0) < 0 || dup2(log, 1) < 0 || dup2(log, 2) < 0) _exit(1);
    for (int fd = 3; fd < maxfd; ++fd) close(fd);
    execv(argv[2], argv + 2);
    perror("emulator launch");
    _exit(127);
}
