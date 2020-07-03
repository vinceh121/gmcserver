import { Component } from '@angular/core';
import { RequestService } from './request.service';
import { Intent } from './types';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  title = 'gmcserver-web';
  silentNames: string[] = ['LOG2_RECORD', 'LOG_CLASSIC_RECORD'];

  notifications: Intent[] = [];

  constructor(public req: RequestService) {
    if (this.req.checkAuth()) {
      this.req.connectWebsocket();
      this.req.getWebsocketObservable().subscribe(next => {
        if (this.notifications.length == 0 && next.name === 'HANDSHAKE_COMPLETE') // ignore the notification for this login
          return;
        this.notifications.push(next);
      });
    }
  }

  getDisplayNotifNumber(): number {
    return this.notifications.filter(i => { return this.silentNames.includes(i.name) }).length;
  }
}
