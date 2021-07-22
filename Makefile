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

all: | web server emails

server:
	$(MAKE) -C gmcserver-server
web:
	$(MAKE) -C gmcserver-web
emails:
	$(MAKE) -C gmcserver-email


install-server:
	$(MAKE) DESTDIR=$(DESTDIR) -C gmcserver-server install-server

install-web:
	$(MAKE) DESTDIR=$(DESTDIR) -C gmcserver-web install-web

install-emails:
	$(MAKE) DESTDIR=$(DESTDIR) -C gmcserver-email install-emails

DEST_HTTPD_NGINX = $(DESTDIR)/etc/nginx/sites-available/gmcserver

install-httpd-nginx:
	install -T -D nginx.example.conf $(DEST_HTTPD_NGINX)

install: | install-server install-web install-emails

#clean:
#	#mvn -o clean
#	rm -rI target
#	$(MAKE) -C gmcserver-web clean
