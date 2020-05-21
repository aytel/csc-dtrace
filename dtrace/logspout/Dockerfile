FROM gliderlabs/logspout:master

COPY ./tcp_wait.py /usr/bin/tcp_wait.py
COPY ./run.sh /bin/run_logspout.sh
RUN chmod +x /bin/run_logspout.sh
RUN apk add python3

ENTRYPOINT ["/bin/run_logspout.sh"]
