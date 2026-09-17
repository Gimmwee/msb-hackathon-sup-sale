FROM python:3.11-slim

WORKDIR /app
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app

COPY deploy/requirements.txt /tmp/requirements.txt
RUN pip install --no-cache-dir -r /tmp/requirements.txt

COPY backend/app/ /app/app/
COPY backend/main_agentbase.py /app/main_agentbase.py

EXPOSE 8080
CMD ["python", "main_agentbase.py"]
