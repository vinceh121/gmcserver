build:
	mkdir -p out
	yarn
	yarn run mjml -r *.mjml -o out/

DEST_CONF = $(DESTDIR)/etc/gmcserver
DEST_MAILS = $(DEST_CONF)/mail-templates

install-emails:
	mkdir -p $(DEST_MAILS)
	cp -r out/* $(DEST_MAILS)/

clean:
	rm -r out

