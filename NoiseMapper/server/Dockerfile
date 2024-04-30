FROM python:3.11-alpine
RUN apk update && apk upgrade
RUN apk add sqlite
COPY requirements.txt requirements.txt
RUN pip install -r requirements.txt
COPY . .
EXPOSE 5002
CMD ["python", "server.py"]