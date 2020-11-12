backend:
		mvn -Dgmc.config.path=/etc/gmcserver/config.properties \
		-Dgmc.vertx.config.path=/etc/gmcserver/vertx.json \
		-Dgmc.vertx.mail.config.path=/etc/gmcserver/mail.json \
		-Dgmc.vertx.mail.templates.path=/etc/gmcserver/mail-templates/ \
		compile assembly:single

clear:
	rm -r target/

