web:
	yarn
	yarn build

DEST_WEB = $(DESTDIR)/var/www/html

install-web:
	install -d $(DEST_WEB)/gmcserver
	cp -r build/* $(DEST_WEB)/gmcserver # try to find why install isn't behaving

clean:
	rm -r public
