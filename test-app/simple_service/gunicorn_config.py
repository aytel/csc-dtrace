import simple_service as srv

workers = 2
loglevel = 'info'
errorlog = '-'
accesslog = '-'
access_log_format = f'%(t)s %(h)s %({{x-caller-id}}i)s {srv.SERVECE_ID} %({{x-request-id}}i)s "%(r)s" %(D)s %(s)s'
logger_class = 'simple_service.GunicornLogger'