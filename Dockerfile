FROM java
RUN apt-get update && apt-get install -y make
COPY . /app
