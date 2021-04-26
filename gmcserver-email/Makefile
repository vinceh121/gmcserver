build:
	mkdir -p out
	pnpm i
	pnpm run build

DEST_CONF = $(DESTDIR)/etc/gmcserver
DEST_MAILS = $(DEST_CONF)/mail-templates

install-emails:
	mkdir -p $(DEST_MAILS)
	cp -r out/* $(DEST_MAILS)/

clean:
	rm -r out

