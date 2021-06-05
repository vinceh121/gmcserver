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
