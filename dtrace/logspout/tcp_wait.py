import itertools
import os
import socket
import time


TCP_HOST = os.getenv('TCP_HOST', '')
TCP_PORT = int(os.getenv('TCP_PORT', 10000))
TCP_RETRY_COUNT = int(os.getenv('TCP_RETRY_COUNT', -1))
TCP_RETRY_TIMEOUT = float(os.getenv('TCP_RETRY_TIMEOUT', 2))


def wait(host, port, retry_count, retry_timeout):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    if retry_count == -1:
        range_ = itertools.cycle('1')
    else:
        range_ = range(retry_count)
    for _ in range_:
        print(f'Connecting to: {host}:{port}', end=' ')
        try:
            s.connect((host, port))
            s.shutdown(socket.SHUT_RDWR)
        except socket.error:
            print('refused', flush=True)
            time.sleep(retry_timeout)
        else:
            print('done', flush=True)
            break


if __name__ == '__main__':
    wait(TCP_HOST, TCP_PORT, TCP_RETRY_COUNT, TCP_RETRY_TIMEOUT)
