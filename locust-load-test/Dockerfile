FROM python:3.11-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements and install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy locust files
COPY locustfile.py .

# Expose Locust web interface port
EXPOSE 8089

# Default command to run Locust
CMD ["locust", "--host=http://payments-orchestrator:8080"]