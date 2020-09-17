dev:
	source ./envfile.dev && export $(shell cut -d= -f1 envfile.dev) && cd app && lein with-profile +test repl