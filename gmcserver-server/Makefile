server:
		mvn -Dgmc.config.path=/etc/gmcserver/config.properties \
		-Dgmc.vertx.config.path=/etc/gmcserver/vertx.json \
		-Dgmc.vertx.mail.config.path=/etc/gmcserver/mail.json \
		-Dgmc.vertx.mail.templates.path=/etc/gmcserver/mail-templates/ \
		compile assembly:single

DEST_BIN = $(DESTDIR)/usr/bin
DEST_CONF = $(DESTDIR)/etc/gmcserver
DEST_SERVICE = $(DESTDIR)/lib/systemd/system

install-server:
	install -T -D target/*-jar-with-dependencies.jar $(DEST_BIN)/gmcserver
	install -T -D config.example.properties $(DEST_CONF)/config.properties
	install -T -D vertx.json $(DEST_CONF)/vertx.json
	install -T -D gmcserver.service $(DEST_SERVICE)/gmcserver.service

clear:
	rm -r target/

