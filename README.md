# Akka Bank

Small demo project for Akka, initiated originally by Heiko Seeberger @hseeberger [Akkounts](https://github.com/hseeberger/akkounts), its use case is bank accounts: deposit, withdraw and get the balance.
Akkounts is a distributed system making use of Akka Cluster. It employs Event Sourcing and CQRS
enabled by Akka Persistence and Akka Persistence Query and offers a HTTP/JSON API through Akka HTTP.
Domain logic processing is using Akka Streams and [Streamee](https://github.com/moia-dev/streamee)
processors.

## Run locally

Make sure to run Cassandra or Scylla locally using the default ports. You can use the provided
`docker-compose.yml` and simply run `docker-compose up`.

Then run `sbt r1` (r1 is defined as a command alias in `build.sbt`) to start a node with all
relevant services bound to 127.0.0.1. You can also run `sbt r2`, but first you have to add
127.0.0.2 as a network interface, e.g. via `sudo ifconfig lo0 alias 127.0.0.2` on macOS.

## Contribution policy ##

Contributions via GitHub pull requests are gladly accepted from their original author. Along with
any pull requests, please state that the contribution is your original work and that you license
the work to the project under the project's open source license. Whether or not you state this
explicitly, by submitting any copyrighted material via pull request, email, or other means you
agree to license the material under the project's open source license and warrant that you have the
legal authority to do so.

## License ##

This code is open source software licensed under the
[Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0) license.
