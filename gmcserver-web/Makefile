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

web:
	pnpm i
	pnpm run build

DEST_WEB = $(DESTDIR)/var/www/html

install-web:
	install -d $(DEST_WEB)/gmcserver
	cp -r build/* $(DEST_WEB)/gmcserver # try to find why install isn't behaving

clean:
	rm -r public
