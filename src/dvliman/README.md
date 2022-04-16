# Rate limiting library demo

> Candidates build out a rate-limiting library in the language of their choice. The rate-limiter can be configured to support blocking requests by IP, or blocking an IP if there are several similar requests within a period of time (e.g.: blocking POST requests to /login for an IP).

There are different dimensions when designing a rate-limiting library. 
- algorithm: token bucket, leaky bucket, fixed window, sliding window, genetic cell rate
- backend: memory, redis
- integration: http handler/middleware, function
- configuration: rate (capacity), per time interval (second/minute/hour)

Let's take a look at the leaky bucket approach. It is simple to understand. You can think of it as
a bucket holding the requests. When new request comes in, it is appended at the end of the queue. When request is
processed i.e returning response after making database query for your business application, that request is popped. 
The leaky bucket follows FIFO (first-in, first-out) queue. If the queue is full, then additional requests are discarded.

