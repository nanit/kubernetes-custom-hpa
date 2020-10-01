FROM clojure:lein-2.9.1

RUN mkdir -p /usr/src/app

WORKDIR /usr/src/app
COPY project.clj /usr/src/app/

RUN lein deps

COPY . /usr/src/app

EXPOSE 3000

RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" app-standalone.jar

CMD ["java", "-jar", "app-standalone.jar"]