workers = 2
loglevel = 'info'
errorlog = '-'
accesslog = '-'
access_log_format = '%(t)s %(h)s %({x-request-id}i)s "%(r)s" %(s)s'
logger_class = 'csc_dtrace_glogger.CSCDtraceGLogger'