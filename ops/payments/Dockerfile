FROM postgres:18-bookworm
RUN apt-get update && apt-get install -y --no-install-recommends python3 openssl ca-certificates && rm -rf /var/lib/apt/lists/*
COPY finance_backup.py availability_probe.py /opt/olp/
ENTRYPOINT ["python3", "/opt/olp/finance_backup.py"]
