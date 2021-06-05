# GMCServer
[![GitHub](https://img.shields.io/github/license/vinceh121/gmcserver?color=green&style=for-the-badge)](https://github.com/vinceh121/gmcserver/blob/master/LICENSE)
[![GitHub Workflow Status](https://img.shields.io/github/workflow/status/vinceh121/gmcserver/Java%20CI%20with%20Maven?label=Backend%20build&style=for-the-badge)](https://github.com/vinceh121/gmcserver/actions?query=workflow%3A%22Java+CI+with+Maven%22)
[![GitHub Workflow Status](https://img.shields.io/github/workflow/status/vinceh121/gmcserver/Web%20Build?label=Frontend%20build&style=for-the-badge)](https://github.com/vinceh121/gmcserver/actions?query=workflow%3A%22Web+Build%22)
![GitHub repo size](https://img.shields.io/github/repo-size/vinceh121/gmcserver?color=yellowgreen&style=for-the-badge)
[![LGTM Grade](https://img.shields.io/lgtm/grade/java/github/vinceh121/gmcserver?style=for-the-badge)](https://lgtm.com/projects/g/vinceh121/gmcserver/)
[![LGTM Grade](https://img.shields.io/lgtm/grade/javascript/github/vinceh121/gmcserver?style=for-the-badge)](https://lgtm.com/projects/g/vinceh121/gmcserver/)
[![Most used swear](https://img.shields.io/badge/dynamic/json?color=yellow&label=Most%20used%20swear&query=%24.mostUsed.word&url=https%3A%2F%2Fswear.vinceh121.me%2Fcount.json%3Furi%3Dhttps%3A%2F%2Fgithub.com%2Fvinceh121%2Fgmcserver&style=for-the-badge)](https://github.com/vinceh121/git-swears)

GMCServer is a lightweight service that allows logging, analysis, visualization and proxying of data recorded by [GMC Geiger counters](https://www.amazon.com/GQ-Radiation-Detector-Recorder-equipment/dp/B00ROHGWIQ), [Radmon counters](https://radmon.org) and [Safecast counters](https://safecast.org/devices/bgeigie-nano/)
<br> Some of the features implemented so far:
 - User accounts
 - Device logging
 - Timeline visualization
 - Device World map
 - Simple statistical analysis (min, max, average, standard deviation)
 - Proxy to other platforms

A demo/main instance is available at **https://gmc.vinceh121.me**

![Map view](https://i.vinceh121.me/Azh698yB.png)
![Timeline view](https://i.vinceh121.me/BV7wt5Dm.png)
![Calendar view](https://i.vinceh121.me/H1mENyIi.png)

## Motivation
On top of not being self-hostable and closed-source, gmcmap.com lacks some features relating to data analysis, visualization, API access, and overall security. This project aims to create a compatible, open-source and self-hostable alternative to gmcmap.com implementing those missing features and security fixes.
GMCServer can also *proxy* records, that is, report measurements itself to other platforms such as gmcmap.com and radmon.org simultaneously upon receiving a record from your counter.

## Setting up device
### GQ Electronics' GMC
You should first set up WiFi on your GMC device. In the `Server` settings, you should set the following values:
 - `Website` : to the hostname of the server where GMCServer is running (`gmc.vinceh121.me` for the main instance)
 - `URL` : to `log2.asp`
 - `User ID` : to the "GMC ID" of your account, visible next to your username when logged in on your user page. This ID is supposed to stay secret.
 - `Counter ID` : to the "GMC ID" of your device, visible next to the device's name when logged in on its page. This ID is supposed to stay secret.

Once those settings set, pressing the Test Connection button should show "Successful" after a second.


## Installing

### On Debian/Ubuntu
 - Install Java (`apt install default-jre`)
 - Install [MongoDB](https://docs.mongodb.com/manual/tutorial/install-mongodb-on-debian/)
 - Download the deb file from GitHub actions "Make debian" and install it with dpkg:
```sh
unzip gmcserver-debian-master-abcdef012345.zip
dpkg -i work/gmcserver/gmcserver_0.0.0-1_amd64.deb
```

### Building from source
The following dependencies are required to build:
 - make
 - maven
 - pnpm

```sh
git clone https://github.com/vinceh121/gmcserver
cd gmcserver
make
sudo make install
```
This will build all compenents of GMCServer (backend, frontend, emails), if you need to only build one specific component, cd to its folder and run make in it.
### Configuring web server
GMCServer can act on its own as a web server, you just need to set the `web.enabled` setting to true in `config.properties`

However, it may be more "proper" to use a separate web server/reverse proxy such as Apache or Nginx:
 - [Nginx example config](https://github.com/vinceh121/gmcserver/blob/master/nginx.example.conf)
 - TODO: Apache example config

## Licensing
Unless otherwise noted, files in this repository are licensed under the GNU AGPL V3 license.

Icon is by Lorc under the CC BY 3.0 license from game-icons.net

