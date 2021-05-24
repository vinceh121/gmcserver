PATH_CONFIG = /etc/gmcserver/config.properties
PATH_CONFIG_VERTX = /etc/gmcserver/vertx.json
PATH_CONFIG_MAIL = /etc/gmcserver/mail.json
PATH_MAIL_TEMPLATES = /etc/gmcserver/mail-templates/

server:
		mvn -Dgmc.config.path=$(PATH_CONFIG) \
		-Dgmc.vertx.config.path=$(PATH_CONFIG_VERTX) \
		-Dgmc.vertx.mail.config.path=$(PATH_CONFIG_MAIL) \
		-Dgmc.vertx.mail.templates.path=$(PATH_MAIL_TEMPLATES) \
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

