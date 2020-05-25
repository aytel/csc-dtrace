import time
import gunicorn
import gunicorn.glogging


class CSCDtraceGLogger(gunicorn.glogging.Logger):
    def now(self):
        t = time.time()
        msec = round((t - int(t))*1000)
        t_str = time.strftime('%Y-%m-%dT%H:%M:%S.{}%z', time.localtime(t))
        return t_str.format(msec)
