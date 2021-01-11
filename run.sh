docker build -t bet-history .
docker image prune -f
docker run -p 8080:8080 --name bet-history-container -it bet-history