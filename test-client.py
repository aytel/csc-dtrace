import argparse
import random
import requests

DEFAULT_HOST = '127.0.0.1'
DEFAULT_PORT = 5000
DEFAULT_QUERIES = 100
HEADERS = {'Content-Type': 'application/x-www-form-urlencoded'}
OP_LIST = ['+', '-', '/', '*']


def post(endpoint, params):
    return requests.post(endpoint, headers=HEADERS, data=params)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument(
        '-s', '--host',
        type=str, default=DEFAULT_HOST,
        help='The host to send requests to'
             'Default is {}'.format(DEFAULT_PORT)
    )
    parser.add_argument(
        '-p', '--port',
        type=int, default=DEFAULT_PORT,
        help='The destination port to send requests to. '
             'Default is {}'.format(DEFAULT_PORT)
    )
    parser.add_argument(
        '-q', '--queries',
        metavar='NQUERIES', type=int, default=DEFAULT_QUERIES,
        help='Set the number of queries to send. '
             'Default is {}'.format(DEFAULT_QUERIES)
    )
    parser.add_argument(
        '-d', '--data',
        type=str,
        help='Requst payload. If specified single reuest will be sent with given payload'
    )
    args = parser.parse_args()
    if args.data:
        params = {'statement': args.data}
        response = post('http://127.0.0.1:80/api/v1.0/calculator', params)
        print(f'{args.data} = {response.text}')
        exit(0)
    for i in range(args.queries):
        a = random.randint(0, 100)
        b = random.randint(0, 100)
        op = random.choice(OP_LIST)
        params = {'statement': f'{a}{op}{b}'}
        response = post('http://127.0.0.1:80/api/v1.0/calculator', params)
        print(f'{a} {op} {b} = {response.text}')
