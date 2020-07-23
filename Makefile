all: | web server

server:
	mvn compile assembly:single


web:
	$(MAKE) -C gmcserver-web


DEST_BIN = $(DESTDIR)/usr/bin
DEST_WEB = $(DESTDIR)/var/www/html

install-server:
	install -T -D target/*-jar-with-dependencies.jar $(DEST_BIN)/gmcserver

install-web:
	install -d gmcserver-web/dist/gmcserver-web $(DEST_WEB)/gmcserver

install: | install-server install-web

#clean:
#	#mvn -o clean
#	rm -rI target
#	$(MAKE) -C gmcserver-web clean
