FROM python:3.12-alpine
WORKDIR /app
COPY tm.py .
ENV PYTHONUNBUFFERED=1 \
    HOME=/data \
    TM_API_BASE=http://api:8080/api/v1
RUN mkdir -p /data \
    && printf '#!/bin/sh\nexec python /app/tm.py "$@"\n' > /usr/local/bin/tm \
    && chmod +x /usr/local/bin/tm
VOLUME ["/data"]
ENTRYPOINT ["tm"]
CMD ["health"]
