# GMCServer, lightweight service to log, analyze and proxy Geiger counter data.
# Copyright (C) 2020 Vincent Hyvert
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public for more details.
#
# You should have received a copy of the GNU Affero General Public
# along with this program.  If not, see <https://www.gnu.org/licenses/>.

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
DEST_JAVA = $(DESTDIR)/usr/share/java

install-server:
	install -T -D target/*-jar-with-dependencies.jar $(DEST_JAVA)/gmcserver.jar
	install -T -D gmcserver.sh $(DEST_BIN)/gmcserver
	install -T -D config.example.properties $(DEST_CONF)/config.properties
	install -T -D vertx.json $(DEST_CONF)/vertx.json
	install -T -D gmcserver.service $(DEST_SERVICE)/gmcserver.service

clear:
	rm -r target/

