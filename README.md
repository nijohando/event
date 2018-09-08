# Event

[![Clojars Project](https://img.shields.io/clojars/v/jp.nijohando/event.svg)](https://clojars.org/jp.nijohando/event)
[![CircleCI](https://circleci.com/gh/nijohando/event.svg?style=shield)](https://circleci.com/gh/nijohando/event)

core.async channel based event bus for Clojure(Script)

## Rationale

Main motivations for writing this library are:

* Based on core.async channel
* Fine grain event listening more than core.async pub/sub
* Request-reply communication between event emitter and listener
* Available on both Clojure and ClojureScript

## Installation

#### Ligningen / Boot

```clojure
[jp.nijohando/event "0.1.3"]
```

#### Clojure CLI / deps.edn

```clojure
jp.nijohando/event {:mvn/version "0.1.3"}
```

## Usage

Clojure

```clojure
(require '[jp.nijohando.event :as ev]
         '[clojure.core.async :as ca])
```

CloureScript

```clojure
(require '[jp.nijohando.event :as ev :include-macros true]
         '[cljs.core.async :as ca :include-macros true])
```

### Event

An event is expressed as a map. It can be created in literals or by function `event`.

```clojure
(ev/event "/messages/10/delete")
;;=> {:path "/messages/10/delete"}
```

```clojure
(ev/event "/messages/post" 
          {:name "taro" 
           :text "hello!"})
;;=> {:path "/messages/post"
;;    :value {:name "taro"
;;            :text "hello!"}}
```

An event has three keys `path`, `header` and `value`

`path` is set of a resource and operation, which is similar to the path of RESTful API, but it may contain not only a noun but also a verb.

`header` is Control parameters which is appended by the library.

`value` is body data of the event. It's optional depends on event type.

### Bus

Function `bus` creates an event bus that is a main line for propagating events.

```clojure
(def bus (ev/bus))
```

### Emitter channel

Function `emitize` connects the channel to the bus as an emitter channel.

```clojure
(def emitter (ca/chan))
(ev/emitize bus emitter)
```

Writing an event to the channel, the event is emitted to the bus.

```clojure
(ca/go
  (ca/>! emitter {:path "/messages/post"
                  :value {:name "taro"
                          :text "hello!"}}))
```

### Listener channel

Function `listen` connects the channel to the bus as a listener channel and listens to events matching the specified path.

```clojure
(def listener (ca/chan))
(ev/listen bus "/messages/post" listener)
```

Events matching the path can be read from the channel.

```clojure
(ca/go-loop []
  (when-some [{:keys [path value] :as event} (ca/<! listener)]
    (println "from:" (:name value) "msg:" (:text value)))
    (recur))
```

Path can be specified by [metosin/reitit](https://github.com/metosin/reitit) [route syntax](https://metosin.github.io/reitit/basics/route_syntax.html) (internaly uses [reitit-core](https://github.com/metosin/reitit/tree/master/modules/reitit-core))

```clojure
;; Listen any message id's delete event
(ev/listen bus "/messages/:id/delete" listener)
;; Listen any message id's any event
(ev/listen bus "/messages/:id/*" listener)
;; Listen multiple type of events
(ev/listen bus ["/messages" 
                 ["/post"]
                 ["/:id/delete"]] listener)
```

Matched route information is added to the header of the read event.

```clojure
{:path "/messages/1/delete"
 :header {:route #reitit.core.Match{:template "/messages/:id/delete",
                                    :data {}
                                    :result nil
                                    :path-params {:id "1"}
                                    :path "/messages/1/delete"}}}

```

### Message exchange pattern

Normaly, emitting event is one-way communication, but the emitter can also receive the reply event.

Each emitter channel has a unique id and endpoint path `/emitters/:id/reply` to receive a reply.  
Function `emitize` can connect the channels to the bus as an emitter and a reply channel.

```clojure
(def emitter (ca/chan))
(def reply-ch (ca/chan))
(ev/emitize bus emitter reply-ch)
```

A reply can be read from the reply channel.

```clojure
;; Emit an event and listen the reply
(ca/go
  (let [emitter (ca/chan)
        reply-ch (ca/chan)]
    (ev/emitize bus emitter reply-ch)
    (ca/>! emitter (ev/event "/messages/post"))
    (when-some [{:keys [value]} (ca/<! reply-ch)]
      (prn "message" (:msg-id value) "created!"))))
```

Function `reply-to` creates a reply event for the source event and it can be emitted via emitter channel.

```clojure
;; Listen an event and emit the reply
(ca/go
  (let [emitter (ca/chan)
        listener (ca/chan)
        msg-id (atom 0)]
    (ev/emitize bus emitter)
    (ev/listen bus "/messages/post" listener)
    (ca/go-loop []
      (when-some [event (ca/<! listener)]
        (let [reply (ev/reply-to event {:status :created
                                        :msg-id (swap! msg-id inc)})]
          (ca/>! emitter reply)
          (recur))))))
```

When an emitter channel is created per request (emitting an event), It becomes the request-reply pattern because the emitter-id has a unique value for each request.

## License

© 2018 nijohando  

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.

