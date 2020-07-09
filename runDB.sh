docker run -it --rm --name notz -p 5432:5432 \
  -e POSTGRES_DB=notz \
  -e POSTGRES_USER=vaan \
  -e POSTGRES_PASSWORD=kuroneko \
  postgres:12.3
