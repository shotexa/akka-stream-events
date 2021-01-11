# Bet History

## Starting the project

To run the project: `$ cd bet-history && ./run.sh` to run it inside docker container

_this will take some time, until it downloads java 11 image, downloads `coursier`, downloads `sbt`, downloads all project dependencies and then assembles everything in jar file._

Alternatively, if you have sbt installed, just `$ sbt run`, this will be way faster then with docker.

## Endpoints

Once started, application will expose an HTTP endpoint on port `8080`.

There is only one endpoint - `GET /bets/<account-id>` which will return a list of placed bets.

Endpoint supports cursor-based pagination, bet id is the cursor.

available query params

* `first=<number>` - number of items to return, default 10
* `before=<bet-id>` - for pagination, will return bets before given bet id
* `after=<bet-id>` - for pagination, will return bets after given bet id (will be ignored if passed together with before)

example: 
```
GET localhost:8080/bets/acc-0001?first=5&before=bet-0004
```

## Events

Once started, application will start listening for bet placement events first, and once placement event stream is drained, it will start listening for bet settlement stream. I've decided to go with this approach because the bet settlement event handling logic depends on bets already existing in database (in memory in this case). It is, of course, not ideal to depend on bet placement and bet settlement events coming in the right order, because in real life something could go wrong and they can come in reverse order. In this case I would probably implement some kind of retry mechanism, which, in case bet settlement is received for a bet that does not exist, will queue this event and try to handle it later. For this test application I felt that would be an overkill.

___

Generated from template: https://github.com/shotexa/scala-seed.g8