FROM python:3.12-slim
WORKDIR /app

RUN useradd -r -s /bin/false safeher

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app/ app/

RUN chown -R safeher:safeher /app
USER safeher

EXPOSE 8085
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8085"]
