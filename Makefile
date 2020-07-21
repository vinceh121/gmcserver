
server:
	mvn compile assembly:single


web:
	$(MAKE) -C gmcserver-web

all: | web server

