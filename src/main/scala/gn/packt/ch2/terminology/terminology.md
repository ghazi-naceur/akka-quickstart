
- Concurrency is when 2 tasks may start, run and complete with overlapping time periods (multitasking on single core
 machine). It doesn't necessarily mean that both will be run at the same time.
- Concurrency is the mean of splitting up work into chunks, which may or may not be executed in parallel
- Parallelism is when literally run at the same time (multithreading on multicore processor)
- Parallelism is chunking of work to be executed in parallel

- A method call is synchronous if the caller cannot make progress until the method returns a value or throws an exception
- A method call is asynchronous if the caller can make progress after a finite number of steps, and the completion of the 
 method may be signaled via some additional mechanism (callback, future or message) 

- Akka Actors are asynchronous by nature, and Actor can progress after a message is sent without waiting for the actual 
 delivery to happen

- Blocking is when if the delay of one thread can indefinitely delay some other threads
- Non-blocking means that no thread is able to indefinitely delay others

- Race condition is when multiple threads have a shared mutable state. In fact, problems only arise if one or more threads 
 try to change this state at the same time
- Akka Actors are safe from such Race Condition, as they receive messages one by one, and at the same time the Actor processes 
 one message.