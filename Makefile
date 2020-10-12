all: | web server emails

server:
	mvn -Dgmc.config.path=/etc/gmcserver/config.properties \
		-Dgmc.vertx.config.path=/etc/gmcserver/vertx.json \
		-Dgmc.vertx.mail.config.path=/etc/gmcserver/mail.json \
		-Dgmc.vertx.mail.templates.path=/etc/gmcserver/mail-templates/ \
		compile assembly:single

web:
	$(MAKE) -C gmcserver-web

emails:
	$(MAKE) -C gmcserver-email

DEST_BIN = $(DESTDIR)/usr/bin
DEST_WEB = $(DESTDIR)/var/www/html
DEST_CONF = $(DESTDIR)/etc/gmcserver
DEST_SERVICE = $(DESTDIR)/lib/systemd/system

DEST_MAILS = $(DEST_CONF)/mail-templates

install-server:
	install -T -D target/*-jar-with-dependencies.jar $(DEST_BIN)/gmcserver
	install -T -D config.example.properties $(DEST_CONF)/config.properties
	install -T -D vertx.json $(DEST_CONF)/vertx.json
	install -T -D gmcserver.service $(DEST_SERVICE)/gmcserver.service

install-web:
	install -d $(DEST_WEB)/gmcserver
	cp -r gmcserver-web/build/* $(DEST_WEB)/gmcserver # try to find why install isn't behaving

install-emails:
	mkdir -p $(DEST_MAILS)
	cp -r gmcserver-email/out/* $(DEST_MAILS)/

install: | install-server install-web install-emails

#clean:
#	#mvn -o clean
#	rm -rI target
#	$(MAKE) -C gmcserver-web clean
